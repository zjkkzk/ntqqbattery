package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.wkeqin.ntqqbattery.hook.entity.features.PerfFeatures
import com.wkeqin.ntqqbattery.hook.factory.HookResultTracker
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

        val tracker = HookResultTracker("PandoraEx")

        PerfFeatures.PandoraEventReportHelperClass?.apply {
            tracker.tryHook("PandoraEventReportHelper.c()") {
                method { name = "c"; emptyParam() }.hook().before {
                    YLog.debug("Blocked PandoraEventReportHelper reporting cycle")
                    result = NTQQHooker.safeReturn(method)
                }
            }
        }

        PerfFeatures.MonitorReporterClass?.apply {
            tracker.tryHook("MonitorReporter.sReportCheckRunnable") {
                field { name = "sReportCheckRunnable" }.get().any()?.let { runnable ->
                    runnable.javaClass.method { name = "run"; emptyParam() }.hook().before {
                        YLog.debug("Blocked MonitorReporter sReportCheckRunnable")
                        result = NTQQHooker.safeReturn(method)
                    }
                }
            }
        }

        tracker.report()
    }
}
