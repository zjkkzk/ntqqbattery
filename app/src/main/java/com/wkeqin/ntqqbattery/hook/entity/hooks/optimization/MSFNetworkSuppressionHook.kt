package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.wkeqin.ntqqbattery.hook.entity.features.MSFFeatures
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*

object MSFNetworkSuppressionHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "suppress_network_request",
        titleRes = R.string.suppress_network_request,
        summaryRes = R.string.suppress_network_request_summary,
        category = FeatureCategory.OPTIMIZATION,
        defaultEnabled = false
    )

    val plan = HookPlan(
        id = "msf-network-suppression-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(MSFNetworkSuppressionHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.suppressNetworkRequest).not()) return

        MSFFeatures.MsfPullConfigUtilClass?.let { clazz ->
            runCatching {
                clazz.method {
                    name { it == "pullConfigRequest" || it == "getPullConfigInterval" }
                }.hookAll().before {
                    if (args.isEmpty()) return@before
                    if ((args[0] as? Boolean) == false) result = NTQQHooker.safeReturn(method)
                }
            }
        }
    }
}
