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

        // Phase 1: Block QQ alarms in background (existing behavior)
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

        // Phase 2: Downgrade setExactAndAllowWhileIdle to inexact for all QQ alarms
        // Precise alarms bypass Doze and waste scheduling resources.
        // set() on API 19+ is automatically batched to inexact by the system.
        AlarmManager::class.java.method {
            name = "setExactAndAllowWhileIdle"
        }.hookAll().before {
            val operation = args.firstOrNull { it is PendingIntent } as? PendingIntent
            if (operation != null && operation.creatorPackage == "com.tencent.mobileqq") {
                val triggerAtMillis = args.firstOrNull { it is Long } as? Long ?: return@before
                val pendingIntent = operation
                runCatching {
                    val am = instance as? AlarmManager ?: return@runCatching
                    // set() is inexact on API 19+, system batches it with other alarms
                    am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    YLog.debug("Downgraded setExactAndAllowWhileIdle to inexact for QQ alarm")
                }
                result = null // Block the original exact alarm
                ConfigData.setHooked(FeatureRegistry.aggressiveMsfOptimization, true)
            }
        }

        // 注意：不要直接 hook JobScheduler.schedule/enqueue。
        // 这两个在框架层是抽象方法，LSPosed 会直接抛出 Cannot hook abstract methods。
    }
}
