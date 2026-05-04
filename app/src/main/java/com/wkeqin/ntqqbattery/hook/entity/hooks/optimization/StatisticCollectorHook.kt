package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*

object StatisticCollectorHook : YukiBaseHooker() {

    val plan = HookPlan(
        id = "statistic-collector-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(StatisticCollectorHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockQQBatteryMonitor).not()) return

        "com.tencent.mobileqq.statistics.StatisticCollector".toClassOrNull()?.apply {
            method {
                name { it == "collectPerformance" || it == "reportActionCount" }
            }.hookAll().before {
                result = NTQQHooker.safeReturn(method)
            }
        }
    }
}
