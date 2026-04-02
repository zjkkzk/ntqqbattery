package com.wkeqin.ntqqbattery.hook.entity.hooks.system

import android.os.PowerManager
import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog

object SystemWakeLockHook : YukiBaseHooker() {
    private var wakeLockTagField: java.lang.reflect.Field? = null

    val feature = FeatureDefinition(
        key = "block_system_wake_lock",
        titleRes = R.string.block_system_wake_lock,
        category = FeatureCategory.CORE,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "system-wakelock-early-attach",
        stage = HookStage.EARLY_ATTACH
    ) {
        loadHooker(SystemWakeLockHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockSystemWakeLock).not()) return

        PowerManager.WakeLock::class.java.method {
            name { it == "acquire" || it == "acquireLocked" }
        }.hookAll().before {
            val tag = runCatching {
                if (wakeLockTagField == null) {
                    wakeLockTagField = instance?.javaClass?.getDeclaredField("mTag")?.apply { isAccessible = true }
                }
                wakeLockTagField?.get(instance)?.toString()
            }.getOrNull().orEmpty()

            val shouldBlock = NTQQHooker.isBackgroundRestrictedProcess() && (tag == "QQLSActivity" || tag == "test")
            if (shouldBlock) {
                YLog.debug("Blocked background MSF WakeLock.acquire (Tag: $tag)")
                result = null
            }
        }
        ConfigData.setHooked(FeatureRegistry.blockSystemWakeLock, true)
    }
}
