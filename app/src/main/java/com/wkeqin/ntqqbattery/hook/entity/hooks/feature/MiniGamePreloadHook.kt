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

object MiniGamePreloadHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_mini_game_preload",
        titleRes = R.string.block_mini_game_preload,
        category = FeatureCategory.COMPONENT,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "mini-game-preload-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(MiniGamePreloadHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockMiniGamePreload).not()) return

        "com.tencent.mobileqq.minigame.splash.SplashMiniGameStarter".toClassOrNull()?.method {
            name { it in setOf("preloadGameProcess", "preloadMiniGame", "preloadProcess") }
        }?.hookAll()?.before { result = NTQQHooker.safeReturn(method) }

        "com.tencent.mobileqq.mini.api.impl.MiniAppServiceImpl".toClassOrNull()?.method {
            name = "preloadMiniGame"
        }?.hookAll()?.before { result = NTQQHooker.safeReturn(method) }
    }
}
