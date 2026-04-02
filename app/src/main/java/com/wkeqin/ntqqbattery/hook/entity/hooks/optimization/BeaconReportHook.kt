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

object BeaconReportHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_beacon",
        titleRes = R.string.block_beacon,
        category = FeatureCategory.CORE,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "beacon-report-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(BeaconReportHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockBeacon).not()) return
        YLog.debug("Beacon: installing hooks...")

        PerfFeatures.BeaconReportClass?.apply {
            runCatching {
                method {
                    name { it == "start" }
                    paramCount { it >= 2 }
                }.hookAll().before {
                    result = null
                    YLog.debug("Beacon: Blocked BeaconReport.start() initialization")
                }
            }
            runCatching {
                method { name = "setLogAble"; param(Boolean::class.javaPrimitiveType!!) }.hook().before { args[0] = false }
            }
        }

        PerfFeatures.BeaconTaskManagerClass?.apply {
            runCatching {
                constructor { paramCount >= 0 }.hookAll().after {
                    BeaconHookSupport.replaceBeaconExecutor(instance ?: return@after)
                    YLog.debug("Beacon: Replaced TaskManager executor in constructor")
                }
            }
        }

        val threadFactoryClass = "com.tencent.beacon.a.b.l".toClassOrNull(appClassLoader, false)
        if (threadFactoryClass != null) {
            runCatching {
                threadFactoryClass.method {
                    name = "newThread"
                    param(Runnable::class.java)
                }.hook().before {
                    result = Thread { }.apply { isDaemon = true; name = "beacon-noop" }
                    YLog.debug("Beacon: ThreadFactory.newThread() intercepted")
                }
            }
        }

        PerfFeatures.QQBeaconReportClass?.apply {
            runCatching {
                method { name { it == "start" } }.hookAll().before { result = null }
            }
        }

        PerfFeatures.NTBeaconReportClass?.apply {
            runCatching {
                method {
                    paramCount = 7
                    param(
                        String::class.java,
                        String::class.java,
                        String::class.java,
                        Boolean::class.javaPrimitiveType!!,
                        Map::class.java,
                        Boolean::class.javaPrimitiveType!!,
                        Boolean::class.javaPrimitiveType!!
                    )
                }.hookAll().before { result = null }
            }
        }
        ConfigData.setHooked(FeatureRegistry.blockBeacon, true)
    }
}
