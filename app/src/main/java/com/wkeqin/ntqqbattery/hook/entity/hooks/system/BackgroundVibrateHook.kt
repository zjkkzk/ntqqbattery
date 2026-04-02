package com.wkeqin.ntqqbattery.hook.entity.hooks.system

import android.os.Vibrator
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
import com.highcapable.yukihookapi.hook.factory.toClassOrNull

object BackgroundVibrateHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_background_vibrate",
        titleRes = R.string.block_background_vibrate,
        category = FeatureCategory.OPTIMIZATION,
        defaultEnabled = false
    )

    val plan = HookPlan(
        id = "background-vibrate-early-attach",
        stage = HookStage.EARLY_ATTACH
    ) {
        loadHooker(BackgroundVibrateHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockBackgroundVibrate).not()) return

        "android.os.SystemVibrator".toClassOrNull()?.method {
            name = "vibrate"
        }?.hookAll()?.before {
            if (processName != mainProcessName) result = NTQQHooker.safeReturn(method)
        } ?: run {
            Vibrator::class.java.method {
                name = "vibrate"
            }.hookAll().before {
                if (processName != mainProcessName) result = NTQQHooker.safeReturn(method)
            }
        }
    }
}
