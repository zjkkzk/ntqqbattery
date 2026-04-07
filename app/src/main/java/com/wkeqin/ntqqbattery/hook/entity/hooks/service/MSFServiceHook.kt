package com.wkeqin.ntqqbattery.hook.entity.hooks.service

import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker

object MSFServiceHook : YukiBaseHooker() {

    val plan = HookPlan(
        id = "msf-service-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(MSFServiceHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.aggressiveMsfOptimization).not()) return

        // 保留独立 Hook 位，方便后续做更细粒度实验。
        // 目前不再直接拦截 MsfService/MsfCoreService，避免触碰消息主链。
    }
}
