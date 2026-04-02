package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.features.PerfFeatures
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog

object TVKReportHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_tvk_report",
        titleRes = R.string.block_tvk_report,
        category = FeatureCategory.CORE,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "tvk-report-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(TVKReportHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockTVKReport).not()) return

        PerfFeatures.TVKBeaconReportClass?.apply {
            runCatching {
                method { name { it.startsWith("track") } }.hookAll().before { result = null }
            }
        }

        "com.tencent.qqlive.tvkplayer.tools.utils.TVKThreadPoolExecutor".toClassOrNull()?.apply {
            "com.tencent.thread.monitor.plugin.proxy.BaseThreadPoolExecutor".toClassOrNull()?.apply {
                constructor { paramCount >= 0 }.hookAll().after {
                    val executor = instance as? java.util.concurrent.ThreadPoolExecutor ?: return@after
                    if (executor::class.java.name.contains("TVK")) {
                        executor.corePoolSize = 1
                        executor.maximumPoolSize = 1
                        executor.setKeepAliveTime(10, java.util.concurrent.TimeUnit.SECONDS)
                        executor.allowCoreThreadTimeOut(true)
                        YLog.info("Optimized TVKExecutor: ${executor::class.java.name}")
                    }
                }
            }
        }
        ConfigData.setHooked(FeatureRegistry.blockTVKReport, true)
    }
}
