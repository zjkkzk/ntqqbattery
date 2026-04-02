package com.wkeqin.ntqqbattery.hook.entity.hooks.feature

import android.os.Bundle
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

object ZPlanRenderHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_zplan_ue_render",
        titleRes = R.string.block_zplan_ue_render,
        category = FeatureCategory.COMPONENT,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "zplan-render-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(ZPlanRenderHook)
    }

    override fun onHook() {
        if (
            ConfigData.isEnabled(FeatureRegistry.blockZPlanUERender).not() &&
            ConfigData.isEnabled(FeatureRegistry.blockGPUResources).not()
        ) return

        "com.tencent.mobileqq.zplan.avatar.impl.fragment.ZPlanUEWithHippyFragment".toClassOrNull()?.method {
            name = "onCreate"
            param(Bundle::class.java)
        }?.hook()?.before { result = NTQQHooker.safeReturn(method) }
    }
}
