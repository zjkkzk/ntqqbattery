package com.wkeqin.ntqqbattery.hook.entity.hooks.system

import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.hooks.optimization.BeaconHookSupport
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.constructor
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClassOrNull
import com.highcapable.yukihookapi.hook.log.YLog

object BeaconEarlyHook : YukiBaseHooker() {

    val plan = HookPlan(
        id = "beacon-early-attach",
        stage = HookStage.EARLY_ATTACH
    ) {
        loadHooker(BeaconEarlyHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockBeacon).not()) return
        YLog.debug("Beacon-Early: Installing early Beacon hooks...")

        runCatching {
            "com.tencent.beacon.a.b.l".toClassOrNull()?.apply {
                YLog.debug("Beacon-Early: ThreadFactory found -> $name")
                method {
                    name = "newThread"
                    param(Runnable::class.java)
                }.hook().before {
                    result = Thread { }.apply { isDaemon = true; name = "beacon-noop" }
                }
            }
        }

        runCatching {
            "com.tencent.beacon.event.open.BeaconReport".toClassOrNull()?.apply {
                YLog.debug("Beacon-Early: BeaconReport found -> $name")
                method {
                    name = "start"
                    paramCount { it >= 2 }
                }.hookAll().before {
                    result = null
                    YLog.debug("Beacon-Early: Blocked BeaconReport.start()")
                }
            }
        }

        runCatching {
            "com.tencent.mobileqq.statistics.QQBeaconReport".toClassOrNull()?.apply {
                YLog.debug("Beacon-Early: QQBeaconReport found -> $name")
                method { name = "start" }.hookAll().before {
                    result = null
                    YLog.debug("Beacon-Early: Blocked QQBeaconReport.start()")
                }
            }
        }

        runCatching {
            "com.tencent.beacon.a.b.k".toClassOrNull()?.apply {
                YLog.debug("Beacon-Early: TaskManager k found -> $name")
                constructor { paramCount >= 0 }.hookAll().after {
                    BeaconHookSupport.replaceBeaconExecutor(instance ?: return@after)
                    YLog.debug("Beacon-Early: Replaced TaskManager executor in constructor")
                }
            }
        }
    }
}
