package com.wkeqin.ntqqbattery.hook.entity.hooks.feature

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

object SplashPreloadHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_splash_preload",
        titleRes = R.string.block_splash_preload,
        category = FeatureCategory.COMPONENT,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "splash-preload-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(SplashPreloadHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockSplashPreload).not()) return

        "com.tencent.mobileqq.activity.SplashActivity".toClassOrNull()?.apply {
            method { name = "preloadUi"; emptyParam() }.hook().before { result = NTQQHooker.safeReturn(method) }
            method { name = "preloadUi"; paramCount = 1 }.hook().before { result = HashMap<String, Runnable>() }
            method { name = "loadSplashMainLayout"; emptyParam() }.hook().before { result = NTQQHooker.safeReturn(method) }
            method { name = "loadChatsListDependencies"; emptyParam() }.hook().before { result = NTQQHooker.safeReturn(method) }
            ConfigData.setHooked(FeatureRegistry.blockSplashPreload, true)
        }
    }
}
