package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*

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

        "com.tencent.mobileqq.msf.sdk.MsfSdkUtils".toClassOrNull()?.method {
            name = "isPowerSaveMode"; emptyParam()
        }?.hook()?.before { result = true }
        ConfigData.setHooked(FeatureRegistry.forcePowerSaveMode, true)
    }
}
