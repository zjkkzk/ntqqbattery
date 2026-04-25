package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.wkeqin.ntqqbattery.hook.entity.features.MSFFeatures
import com.wkeqin.ntqqbattery.hook.factory.HookResultTracker
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog

/**
 * Hook MSFWakeUpLockManager (f0/c.java) to suppress MSF WakeLock acquisitions.
 *
 * The MSF WakeLock system acquires PARTIAL_WAKE_LOCK for various triggers:
 * - Alarm periodic: every intervalTime ms via setExactAndAllowWhileIdle
 * - MsgPush: on CMD_TRPC_MSG_PUSH when not foreground+screen-on
 * - Heartbeat: on heartbeat commands in background
 * - Screen on/off transitions
 * - Foreground/background transitions
 * - Connection open
 *
 * Strategy:
 * - Alarm WakeLock: skip entirely (alarm itself fires, but no need to hold CPU lock)
 * - Status transition WakeLocks (screen/fg/bg): skip (CPU is already awake or irrelevant)
 * - MsgPush WakeLock: shorten to 2s (enough to process one push, default was server-configured)
 * - Heartbeat WakeLock: skip (heartbeat is a network op, doesn't need CPU lock)
 * - ConnOpen WakeLock: skip (connection setup is network-bound)
 *
 * This is SAFE because WakeLock only controls CPU wake, not message delivery.
 */
object MSFWakeLockHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_msf_wake_lock",
        titleRes = R.string.block_msf_wake_lock,
        category = FeatureCategory.OPTIMIZATION,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "msf-wakelock-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(MSFWakeLockHook)
    }

    /** Maximum WakeLock hold time for MsgPush (ms) */
    private const val MAX_MSG_PUSH_LOCK_MS = 2000L

    /** 纯反射预检方法是否存在，避免触发 YukiHookAPI finder 内部的 NoSuchMethod 错误日志 */
    private fun Class<*>.hasMethod(name: String, paramCount: Int): Boolean =
        declaredMethods.any { it.name == name && it.parameterCount == paramCount }

    override fun onHook() {
        if (!ConfigData.isEnabled(FeatureRegistry.blockMsfWakeLock)) return

        val tracker = HookResultTracker("MSFWakeLock")
        hookMSFWakeUpLockManager(tracker)
        hookMSFWakeLockWrapper(tracker)
        val degraded = tracker.report()
        ConfigData.setDegraded(FeatureRegistry.blockMsfWakeLock, degraded)
    }

    /**
     * Hook the orchestrator class MSFWakeUpLockManager (f0/c.java).
     * This class decides *when* and *how long* to acquire WakeLock based on event type.
     */
    private fun hookMSFWakeUpLockManager(tracker: HookResultTracker) {
        MSFFeatures.MSFWakeUpLockManagerClass?.apply {
            // Hook all single-param "a" methods, filter by argument type inside callback.
            // Covers: a(int), a(long), a(ToServiceMsg), a(FromServiceMsg)
            if (hasMethod("a", 1)) tracker.tryHook("a(1 param)") {
                method {
                    name = "a"
                    paramCount = 1
                }.hookAll().before {
                    val arg = args(0).any() ?: return@before
                    when (arg) {
                        // a(int) - Core WakeLock acquire
                        is Int -> {
                            val requestedMs = arg as Int
                            if (requestedMs > MAX_MSG_PUSH_LOCK_MS) {
                                YLog.debug("MSFWakeLock: shortened acquire from ${requestedMs}ms to ${MAX_MSG_PUSH_LOCK_MS}ms")
                                args(0).set(MAX_MSG_PUSH_LOCK_MS.toInt())
                            }
                        }
                        // a(long) - private alarm registration via setExactAndAllowWhileIdle
                        is Long -> {
                            YLog.debug("MSFWakeLock: blocked periodic alarm registration")
                            result = null
                        }
                        // a(ToServiceMsg) - Heartbeat handler
                        // Skip entirely - heartbeat is network-bound
                        else -> {
                            val className = arg.javaClass.name
                            if (className.contains("ToServiceMsg")) {
                                YLog.debug("MSFWakeLock: blocked heartbeat WakeLock acquire")
                                result = null
                            }
                            // a(FromServiceMsg) - MsgPush handler: let it proceed,
                            // the a(int) case above caps the duration.
                        }
                    }
                }
            }

            // b() - ConnOpenPrepare: acquires for connOpenLockTime ms
            // Skip - connection setup is network-bound.
            if (hasMethod("b", 0)) tracker.tryHook("b()") {
                method {
                    name = "b"
                    emptyParam()
                }.hook().before {
                    YLog.debug("MSFWakeLock: blocked ConnOpenPrepare WakeLock")
                    result = null
                }
            }

            // c() - ScreenOff handler: acquires for backgroundLockTime ms
            // Skip - screen off doesn't need CPU lock for MSF.
            if (hasMethod("c", 0)) tracker.tryHook("c()") {
                method {
                    name = "c"
                    emptyParam()
                }.hook().before {
                    YLog.debug("MSFWakeLock: blocked screenOff WakeLock")
                    result = null
                }
            }

            // d() - ScreenOn handler: acquires for foreground/background lock time
            // Skip - screen on means CPU is already awake.
            if (hasMethod("d", 0)) tracker.tryHook("d()") {
                method {
                    name = "d"
                    emptyParam()
                }.hook().before {
                    YLog.debug("MSFWakeLock: blocked screenOn WakeLock")
                    result = null
                }
            }

            // f() - Foreground event: acquires for foregroundLockTime
            // Skip - app is coming to foreground, CPU is awake.
            if (hasMethod("f", 0)) tracker.tryHook("f()") {
                method {
                    name = "f"
                    emptyParam()
                }.hook().before {
                    YLog.debug("MSFWakeLock: blocked foreground WakeLock")
                    result = null
                }
            }

            // h() - Background event: acquires for backgroundLockTime
            // Skip - entering background doesn't need to hold CPU lock.
            if (hasMethod("h", 0)) tracker.tryHook("h()") {
                method {
                    name = "h"
                    emptyParam()
                }.hook().before {
                    YLog.debug("MSFWakeLock: blocked background WakeLock")
                    result = null
                }
            }

            if (tracker.hasAnySuccess) {
                ConfigData.setHooked(FeatureRegistry.blockMsfWakeLock, true)
            }
        } ?: YLog.warn("MSFWakeLock: MSFWakeUpLockManagerClass not found")
    }

    /**
     * Hook the low-level WakeLock wrapper (f0/a.java) as a safety net.
     * Tag is "MSF:WakeLock", timeout is 300000ms (5 min).
     * If any WakeLock slips through the orchestrator hook, cap it here.
     */
    private fun hookMSFWakeLockWrapper(tracker: HookResultTracker) {
        MSFFeatures.MSFWakeLockWrapperClass?.apply {
            // a(long) - Acquire for duration: calls wakeLock.acquire(300000) then posts delayed release
            // Cap the duration to MAX_MSG_PUSH_LOCK_MS as a safety net.
            // Only one a() method with 1 param in this class, no ambiguity.
            if (hasMethod("a", 1)) tracker.tryHook("wrapper.a(long)") {
                method {
                    name = "a"
                    paramCount = 1
                }.hook().before {
                    val requestedMs = args(0).long()
                    if (requestedMs > MAX_MSG_PUSH_LOCK_MS) {
                        YLog.debug("MSFWakeLock wrapper: capped from ${requestedMs}ms to ${MAX_MSG_PUSH_LOCK_MS}ms")
                        args(0).set(MAX_MSG_PUSH_LOCK_MS)
                    }
                }
            }
        } ?: YLog.warn("MSFWakeLock: MSFWakeLockWrapperClass not found")
    }
}
