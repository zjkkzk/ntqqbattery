package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.wkeqin.ntqqbattery.hook.entity.features.PerfFeatures
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog

object PandoraExHook : YukiBaseHooker() {

    val plan = HookPlan(
        id = "pandora-ex-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(PandoraExHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockQQBatteryMonitor).not()) return

        PerfFeatures.PandoraEventReportHelperClass?.apply {
            runCatching {
                method { name = "c"; emptyParam() }.hook().before {
                    if (NTQQHooker.isBackground()) {
                        YLog.debug("Blocked background PandoraEventReportHelper reporting cycle")
                        result = NTQQHooker.safeReturn(method)
                    }
                }
            }
        }

        PerfFeatures.MonitorReporterClass?.apply {
            runCatching {
                field { name = "sReportCheckRunnable" }.get().any()?.let { runnable ->
                    runnable.javaClass.method { name = "run"; emptyParam() }.hook().before {
                        if (NTQQHooker.isBackground()) {
                            YLog.debug("Blocked background MonitorReporter sReportCheckRunnable")
                            result = NTQQHooker.safeReturn(method)
                        }
                    }
                }
            }
        }
    }
}
