package com.wkeqin.ntqqbattery.hook.entity.hooks.feature

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
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClassOrNull

object MiniAppPreloadHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_mini_app_preload",
        titleRes = R.string.block_mini_app_preload,
        category = FeatureCategory.COMPONENT,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "mini-app-preload-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(MiniAppPreloadHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockMiniAppPreload).not()) return

        UIFeatures.MiniAppLauncherImpl?.method { name = "preloadMiniApp" }?.hookAll()?.before {
            result = NTQQHooker.safeReturn(method)
        }
        "com.tencent.mobileqq.mini.launch.MiniAppMainServiceApiManager".toClassOrNull()?.apply {
            method { name = "preloadMiniApp" }.hookAll().before { result = NTQQHooker.safeReturn(method) }
            method { name = "preloadMiniAppLibs" }.hookAll().before { result = NTQQHooker.safeReturn(method) }
        }
        "com.tencent.mobileqq.mini.api.impl.MiniAppServiceImpl".toClassOrNull()?.method { name = "preloadMiniApp" }?.hookAll()?.before {
            result = NTQQHooker.safeReturn(method)
        }
        ConfigData.setHooked(FeatureRegistry.blockMiniAppPreload, true)
    }
}
