package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.wkeqin.ntqqbattery.hook.entity.features.UIFeatures
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*

object ThemeVideoOptimizationHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_theme_video",
        titleRes = R.string.block_theme_video,
        summaryRes = R.string.block_theme_video,
        category = FeatureCategory.COMPONENT,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "theme-video-optimization-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(ThemeVideoOptimizationHook)
    }

    override fun onHook() {
        if (!ConfigData.isEnabled(FeatureRegistry.blockThemeVideo)) return

        UIFeatures.SplashActivityClass?.apply {
            runCatching {
                method { name = "initSuperTheme"; emptyParam() }.hook().before {
                    if (NTQQHooker.isBackgroundRestrictedProcess()) {
                        NTQQHooker.releaseThemeVideoController()
                        result = NTQQHooker.safeReturn(method)
                    }
                }
            }

            runCatching {
                method { name { it == "doOnStop" || it == "onStop" } }.hookAll().after {
                    if (NTQQHooker.isBackground()) NTQQHooker.releaseThemeVideoController()
                }
            }
        }

        listOfNotNull(UIFeatures.BaseVideoControllerClass).forEach { NTQQHooker.hookThemeVideoControllerGuards(it) }
        ConfigData.setHooked(FeatureRegistry.blockThemeVideo, true)
    }
}
