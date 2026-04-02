package com.wkeqin.ntqqbattery.hook.entity.features

import com.wkeqin.ntqqbattery.hook.entity.FeatureLocator
import com.wkeqin.ntqqbattery.hook.entity.NTQQFeatures
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog

/**
 * MSF 进程相关特征定位
 */
object MSFFeatures {

    /**
     * MSF 配置策略类
     */
    val MSFConfigClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        FeatureLocator.getMSFConfig(loader)?.also {
            YLog.info("Locate MSFConfigClass -> ${it.name}")
        }
    }

    /**
     * MSF 配置拉取工具
     */
    val MsfPullConfigUtilClass by lazy {
        NTQQFeatures.findClassSafe(
            cacheName = "MsfPullConfigUtil",
            primaryName = "com.tencent.mobileqq.msf.core.net.utils.MsfPullConfigUtil",
            packageName = "com.tencent.mobileqq.msf.core.",
            constant = "MsfPullConfigUtil"
        )?.also {
            YLog.info("Locate MsfPullConfigUtilClass -> ${it.name}")
        }
    }

    /**
     * MSF 线程管理器
     */
    val MsfThreadManagerClass by lazy {
        NTQQFeatures.findClassSafe(
            cacheName = "MsfThreadManager",
            primaryName = "com.tencent.mobileqq.msf.core.p",
            packageName = "com.tencent.mobileqq.msf.core.",
            constant = "MsfThreadManager"
        )?.also {
            YLog.info("Locate MsfThreadManagerClass -> ${it.name}")
        }
    }

    /**
     * MSF Alive JobService
     */
    val MSFAliveJobServiceClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        "com.tencent.mobileqq.msf.service.MSFAliveJobService".toClassOrNull(loader)?.also {
            YLog.info("Locate MSFAliveJobServiceClass -> ${it.name}")
        }
    }
}
