package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.factory.HookResultTracker
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog

object PowerSaveModeHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "force_power_save_mode",
        titleRes = R.string.force_power_save_mode,
        category = FeatureCategory.OPTIMIZATION,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "power-save-mode-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(PowerSaveModeHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.forcePowerSaveMode).not()) return

        val tracker = HookResultTracker("PowerSaveMode")
        val clazz = "com.tencent.mobileqq.msf.sdk.MsfSdkUtils".toClassOrNull()
        if (clazz != null && clazz.declaredMethods.any { it.name == "isPowerSaveMode" && it.parameterCount == 0 }) {
            tracker.tryHook("MsfSdkUtils.isPowerSaveMode()") {
                clazz.method {
                    name = "isPowerSaveMode"; emptyParam()
                }.hook().before { result = true }
            }
        }
        val degraded = tracker.report()
        ConfigData.setHooked(FeatureRegistry.forcePowerSaveMode, tracker.hasAnySuccess)
        ConfigData.setDegraded(FeatureRegistry.forcePowerSaveMode, degraded)
    }
}
