package com.wkeqin.ntqqbattery.hook.entity.hooks.system

import android.app.AlarmManager
import android.app.PendingIntent
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog

object FrameworkSchedulerHook : YukiBaseHooker() {

    val plan = HookPlan(
        id = "framework-scheduler-early-attach",
        stage = HookStage.EARLY_ATTACH
    ) {
        loadHooker(FrameworkSchedulerHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.aggressiveMsfOptimization).not()) return

        val allAlarmMethods = listOf("set", "setExact", "setAndAllowWhileIdle", "setExactAndAllowWhileIdle", "setWindow", "setRepeating", "setInexactRepeating", "setAlarmClock")
        AlarmManager::class.java.method {
            name { it in allAlarmMethods }
        }.hookAll().before {
            val operation = args.firstOrNull { it is PendingIntent } as? PendingIntent
            val shouldBlock = if (operation != null) NTQQHooker.shouldBlockAlarm(operation) else false
            if (NTQQHooker.isBackground() && shouldBlock == true) {
                YLog.debug("Blocked app-process AlarmManager.${method.name} (Tag/Action: ${operation?.creatorPackage})")
                result = null
                ConfigData.setHooked(FeatureRegistry.blockSystemWakeLock, true)
            }
        }

        // 注意：不要直接 hook JobScheduler.schedule/enqueue。
        // 这两个在框架层是抽象方法，LSPosed 会直接抛出 Cannot hook abstract methods。
    }
}
