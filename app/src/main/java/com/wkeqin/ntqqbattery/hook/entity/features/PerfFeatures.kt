package com.wkeqin.ntqqbattery.hook.entity.features

import com.wkeqin.ntqqbattery.hook.entity.NTQQFeatures
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog

/**
 * 性能与监控相关特征定位
 */
object PerfFeatures {

    /**
     * 电池监控类
     */
    val BatteryMonitorClass by lazy {
        NTQQFeatures.findClassSafe("BatteryMonitor", "com.tencent.mobileqq.perf.battery.a", "com.tencent.mobileqq.perf.battery", "QQBatteryMonitor")?.also {
            YLog.info("Locate BatteryMonitorClass -> ${it.name}")
        }
    }

    /**
     * QQ 电池监控核心
     */
    val QQBatteryMonitorCoreClass by lazy {
        NTQQFeatures.findClassSafe("QQBatteryMonitorCore", "com.tencent.mobileqq.qqbattery.g", "com.tencent.mobileqq.qqbattery", "QQBattery_QQBatteryMonitorCore")?.also {
            YLog.info("Locate QQBatteryMonitorCoreClass -> ${it.name}")
        }
    }

    /**
     * Apollo GL 线程管理器
     */
    val GLThreadManagerClass by lazy {
        NTQQFeatures.findClassSafe("GLThreadManager", "com.tencent.mobileqq.apollo.view.opengl.GLThreadManager", "com.tencent.mobileqq.apollo.view.opengl", "[ApolloGL][GLThreadManager]")?.also {
            YLog.info("Locate GLThreadManagerClass -> ${it.name}")
        }
    }

    /**
     * PandoraEx 隐私事件报告助手
     */
    val PandoraEventReportHelperClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        // 1. 尝试硬编码
        "com.tencent.mobileqq.qmethodmonitor.pandoraevent.PandoraEventReportHelper".toClassOrNull(loader) ?: loader.searchClass(name = "NTQQ_Features_PandoraEventReportHelper") {
            // 2. 特征：在指定包下，且具有一个类型为 AtomicBoolean 的静态字段
            fullName { it.startsWith("com.tencent.mobileqq.qmethodmonitor.pandoraevent.") }
            field {
                modifiers { isStatic }
                type = "java.util.concurrent.atomic.AtomicBoolean"
            }
        }.get()?.also {
            YLog.info("Locate PandoraEventReportHelperClass -> ${it.name}")
        }
    }

    /**
     * Monitor报告器
     */
    val MonitorReporterClass by lazy {
        NTQQFeatures.findClassSafe("MonitorReporter", "com.tencent.qmethod.pandoraex.core.MonitorReporter", "com.tencent.qmethod.pandoraex.core", "MonitorReporter")?.also {
            YLog.info("Locate MonitorReporterClass -> ${it.name}")
        }
    }

    /**
     * XWeb 下载器
     */
    val XWebDownloaderClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        "com.tencent.xweb.XWebDownloader".toClassOrNull(loader)?.also {
            YLog.info("Locate XWebDownloaderClass -> ${it.name}")
        }
    }

    /**
     * XWeb HTTP 下载任务类
     */
    val XWebHttpDownloadTaskClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        "com.tencent.xweb.XWebDownloader${'$'}HttpDownloadTask".toClassOrNull(loader)?.also {
            YLog.info("Locate XWebHttpDownloadTaskClass -> ${it.name}")
        }
    }

    /**
     * 腾讯 Beacon 核心
     */
    val BeaconReportClass by lazy {
        NTQQFeatures.findClassSafe("BeaconReport", "com.tencent.beacon.event.open.BeaconReport", "com.tencent.beacon.event.open", "BeaconReport")
    }

    /**
     * 腾讯 Beacon 任务调度管理器（用于彻底阻断所有遥测任务）
     */
    val BeaconTaskManagerClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        "com.tencent.beacon.a.b.k".toClassOrNull(loader) ?: loader.searchClass(name = "NTQQ_Features_BeaconTaskManager") {
            fullName { it.startsWith("com.tencent.beacon.") }
            method { name = "a"; param(Runnable::class.java); returnType = Void.TYPE }
            method { name = "a"; param(Int::class.javaPrimitiveType!!, Long::class.javaPrimitiveType!!, Long::class.javaPrimitiveType!!, Runnable::class.java); returnType = Void.TYPE }
            method { name = "a"; param(Long::class.javaPrimitiveType!!, Runnable::class.java); returnType = Void.TYPE }
        }.get()?.also {
            YLog.info("Locate BeaconTaskManagerClass -> ${it.name}")
        }
    }

    /**
     * 腾讯 Beacon 内部任务包装器（用于阻断已存在的定频任务抛出的死循环）
     */
    val BeaconTaskWrapperClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        "com.tencent.beacon.a.b.j".toClassOrNull(loader) ?: loader.searchClass(name = "NTQQ_Features_BeaconTaskWrapper") {
            fullName { it.startsWith("com.tencent.beacon.") }
            method { name = "run"; returnType = Void.TYPE; emptyParam() }
            // 由于混淆，可能实现了 Runnable
            implements("java.lang.Runnable")
        }.get()
    }

    /**
     * QQ Beacon 包装器
     */
    val QQBeaconReportClass by lazy {
        NTQQFeatures.findClassSafe("QQBeaconReport", "com.tencent.mobileqq.statistics.QQBeaconReport", "com.tencent.mobileqq.statistics", "QQBeaconReport")
    }

    /**
     * NT 架构专属 Beacon 包装器
     */
    val NTBeaconReportClass by lazy {
        NTQQFeatures.findClassSafe("NTBeaconReport", "com.tencent.qqnt.beacon.NTBeaconReport", "com.tencent.qqnt.beacon", "NTBeaconReport")
    }

    /**
     * 视频库 TVK 上报核心
     */
    val TVKBeaconReportClass by lazy {
        NTQQFeatures.findClassSafe("TVKBeaconReport", "com.tencent.qqlive.tvkplayer.report.api.TVKBeaconReport", "com.tencent.qqlive.tvkplayer.report.api", "TVKBeaconReport")
    }
}
