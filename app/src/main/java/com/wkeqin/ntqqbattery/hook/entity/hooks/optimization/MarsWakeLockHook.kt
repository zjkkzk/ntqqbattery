package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog

/**
 * Hook Mars WakerLock to suppress background WakeLock acquisitions.
 *
 * Mars is Tencent's networking library used by QQ for long connections.
 * It has its own WakeLock system (com.tencent.mars.comm.WakerLock) that creates
 * PARTIAL_WAKE_LOCK with tag "MicroMsg.WakerLock".
 *
 * The WakerLock has multiple variants:
 * - com.tencent.mars.comm.WakerLock (main, with reentrant guard)
 * - com.tencent.mars.ilink.comm.WakerLock (iLink, simpler)
 * - com.tencent.mars.game.comm.WakerLock (game, with QFix patches)
 *
 * In background, these locks are unnecessary because:
 * - The network library doesn't need CPU to maintain connections
 * - Push notifications arrive via the persistent connection
 * - The reentrant guard WakeLock is especially wasteful
 *
 * Strategy: Skip lock() calls entirely in background-restricted processes.
 */
object MarsWakeLockHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_mars_wake_lock",
        titleRes = R.string.block_mars_wake_lock,
        category = FeatureCategory.OPTIMIZATION,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "mars-wakelock-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(MarsWakeLockHook)
    }

    private val MARS_WAKERLOCK_CLASSES = listOf(
        "com.tencent.mars.comm.WakerLock",
        "com.tencent.mars.ilink.comm.WakerLock",
        "com.tencent.mars.game.comm.WakerLock"
    )

    override fun onHook() {
        if (!ConfigData.isEnabled(FeatureRegistry.blockMarsWakeLock)) return

        var hookedCount = 0
        MARS_WAKERLOCK_CLASSES.forEach { className ->
            runCatching {
                val clazz = className.toClassOrNull() ?: return@runCatching

                // Hook all lock() variants
                // lock(long, String) - primary with timeout and trace
                // lock(long) - timeout only
                // lock(String) - trace only
                // lock() - no args
                clazz.method {
                    name = "lock"
                }.hookAll().before {
                    if (NTQQHooker.isBackgroundRestrictedProcess()) {
                        YLog.debug("MarsWakeLock: blocked ${clazz.simpleName}.lock() in background")
                        result = null
                    }
                }

                hookedCount++
                YLog.info("MarsWakeLock: hooked $className")
            }.onFailure {
                YLog.error("MarsWakeLock: failed to hook $className: ${it.message}")
            }
        }

        if (hookedCount > 0) {
            ConfigData.setHooked(FeatureRegistry.blockMarsWakeLock, true)
        } else {
            YLog.warn("MarsWakeLock: no Mars WakerLock classes found")
        }
    }
}
