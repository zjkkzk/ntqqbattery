package com.wkeqin.ntqqbattery.hook.entity.hooks.feature

import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.wkeqin.ntqqbattery.hook.entity.features.PerfFeatures
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method

object QQBatteryMonitorHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_qq_battery_monitor",
        titleRes = R.string.block_qq_battery_monitor,
        summaryRes = R.string.block_qq_battery_monitor,
        category = FeatureCategory.CORE,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "qq-battery-monitor-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(QQBatteryMonitorHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockQQBatteryMonitor).not()) return

        PerfFeatures.BatteryMonitorClass?.apply {
            runCatching {
                method {
                    name { it == "b" || it == "init" }
                    emptyParam()
                }.hook().before { result = NTQQHooker.safeReturn(method) }
            }

            runCatching {
                method {
                    name { it == "a" || it == "checkCpuUsage" }
                    param(String::class.java)
                }.hook().before { result = NTQQHooker.safeReturn(method) }
            }
        }

        PerfFeatures.QQBatteryMonitorCoreClass?.apply {
            runCatching {
                method {
                    name { it == "d" || it == "onTurnOn" }
                    emptyParam()
                }.hook().before { result = NTQQHooker.safeReturn(method) }
            }

            runCatching {
                method {
                    name { it == "a" || it == "onForeground" }
                    paramCount = 1
                }.hook().before { result = NTQQHooker.safeReturn(method) }
            }
        }
        ConfigData.setHooked(FeatureRegistry.blockQQBatteryMonitor, true)
    }
}
