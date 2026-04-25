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
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog

/**
 * Hook MSFProbeNewManager and legacy MsfProbeManager to suppress connection probes.
 *
 * MSF Probe runs two types of probes:
 * - MSF Ping: every ~3min (180s), alarm ID 101, sends CMD_HEARTBEAT_ALIVE
 * - MSF Hello: every ~7.5min (450s), sends CMD_STATUS_SVC_MSF_HELLO
 *
 * Both probes trigger alarm → WakeLock → network request → release cycle.
 * In background this is pure waste: the connection is already established,
 * and push notifications arrive via the persistent connection anyway.
 *
 * Strategy:
 * - Extend MSF Ping interval to 15min (900s) in background
 * - Extend MSF Hello interval to 30min (1800s) in background
 * - In foreground, let probes run normally (connection health matters when user is active)
 *
 * This is SAFE because:
 * - Probes don't affect message delivery (they're health checks, not keep-alive)
 * - An established TCP connection stays alive without probes
 * - If the connection drops, the push channel will detect it on next message
 */
object MSFProbeHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_msf_probe",
        titleRes = R.string.block_msf_probe,
        category = FeatureCategory.OPTIMIZATION,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "msf-probe-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(MSFProbeHook)
    }

    /** Extended MSF Ping interval for background (ms) */
    private const val BG_PING_INTERVAL_MS = 900_000L  // 15 min
    /** Extended MSF Hello interval for background (ms) */
    private const val BG_HELLO_INTERVAL_MS = 1_800_000L  // 30 min

    override fun onHook() {
        if (!ConfigData.isEnabled(FeatureRegistry.blockMsfProbe)) return

        hookMSFProbeNewManager()
        hookLegacyMsfProbeManager()
        hookMSFAlarmManager()
    }

    /**
     * Hook MSFProbeNewManager (d.java) - the newer probe manager.
     * Key methods:
     * - f(): gets MSF Ping interval from config (default 180s, range 60-600s)
     * - j(): registers MSF Ping alarm via MSFAlarmManager
     * - g(): on alarm callback, sends ping and checks hello
     * - e(): checkMSFHelloSend, checks if hello interval elapsed
     */
    private fun hookMSFProbeNewManager() {
        MSFFeatures.MSFProbeNewManagerClass?.apply {
            // Hook f() - getMSFPingIntervalTime
            // Returns the interval in ms (default 180000, range 60000-600000)
            // In background, extend to BG_PING_INTERVAL_MS
            runCatching {
                method {
                    name = "f"
                    emptyParam()
                }.hook().after {
                    if (NTQQHooker.isBackground()) {
                        val original = result as? Long ?: return@after
                        if (original < BG_PING_INTERVAL_MS) {
                            result = BG_PING_INTERVAL_MS
                            YLog.debug("MSFProbe: extended ping interval from ${original}ms to ${BG_PING_INTERVAL_MS}ms")
                        }
                    }
                }
            }.onFailure {
                YLog.error("MSFProbe: failed to hook MSFProbeNewManager.f(): ${it.message}")
            }

            // Hook the hello interval field (mutable private int, default 450000)
            // Find it dynamically: the only mutable private int instance field in this class
            runCatching {
                val helloIntervalField = declaredFields.firstOrNull {
                    it.type == Int::class.javaPrimitiveType &&
                        !java.lang.reflect.Modifier.isStatic(it.modifiers) &&
                        !java.lang.reflect.Modifier.isFinal(it.modifiers)
                }?.apply { isAccessible = true }

                if (helloIntervalField != null) {
                    method {
                        name = "e"
                        emptyParam()
                    }.hook().before {
                        if (NTQQHooker.isBackground()) {
                            val current = helloIntervalField.getInt(instance)
                            if (current < BG_HELLO_INTERVAL_MS.toInt()) {
                                helloIntervalField.setInt(instance, BG_HELLO_INTERVAL_MS.toInt())
                                YLog.debug("MSFProbe: extended hello interval from ${current}ms to ${BG_HELLO_INTERVAL_MS}ms")
                            }
                        }
                    }
                    YLog.info("MSFProbe: found hello interval field '${helloIntervalField.name}'")
                } else {
                    YLog.warn("MSFProbe: no mutable int field found for hello interval, skipping")
                }
            }.onFailure {
                YLog.error("MSFProbe: failed to hook MSFProbeNewManager hello interval: ${it.message}")
            }

            // Hook j() - registerMSFPingAlarm
            // In background, skip alarm registration entirely (freeze probes)
            runCatching {
                method {
                    name = "j"
                    emptyParam()
                }.hook().before {
                    if (NTQQHooker.isBackground()) {
                        YLog.debug("MSFProbe: blocked MSF Ping alarm registration in background")
                        result = null
                    }
                }
            }.onFailure {
                YLog.error("MSFProbe: failed to hook MSFProbeNewManager.j(): ${it.message}")
            }

            // Hook g() - onAlarm callback
            // In background, skip sending probes
            runCatching {
                method {
                    name = "g"
                    emptyParam()
                }.hook().before {
                    if (NTQQHooker.isBackground()) {
                        YLog.debug("MSFProbe: blocked onAlarm probe in background")
                        result = null
                    }
                }
            }.onFailure {
                YLog.error("MSFProbe: failed to hook MSFProbeNewManager.g(): ${it.message}")
            }

            YLog.info("MSFProbe: hooked MSFProbeNewManager successfully")
            ConfigData.setHooked(FeatureRegistry.blockMsfProbe, true)
        } ?: YLog.warn("MSFProbe: MSFProbeNewManagerClass not found")
    }

    /**
     * Hook legacy MsfProbeManager (e.java) - the older probe manager.
     * Uses direct AlarmManager reflection instead of MSFAlarmManager.
     * Key methods:
     * - e(): gets probe interval (default 450000ms, range 180000-3600000)
     * - b(long): doRegisterAlarm via setExactAndAllowWhileIdle
     * - onReceive(): alarm fires, sends hello + re-registers
     */
    private fun hookLegacyMsfProbeManager() {
        MSFFeatures.LegacyMsfProbeManagerClass?.apply {
            // Hook e() - get probe interval
            runCatching {
                method {
                    name = "e"
                    emptyParam()
                }.hook().after {
                    if (NTQQHooker.isBackground()) {
                        val original = result as? Long ?: return@after
                        if (original < BG_PING_INTERVAL_MS) {
                            result = BG_PING_INTERVAL_MS
                            YLog.debug("MSFProbe (legacy): extended probe interval from ${original}ms to ${BG_PING_INTERVAL_MS}ms")
                        }
                    }
                }
            }.onFailure {
                YLog.error("MSFProbe: failed to hook legacy e(): ${it.message}")
            }

            // Hook b(long) - doRegisterAlarm
            // In background, skip alarm registration
            // Hook all single-param b() and filter by arg type (Long vs FromServiceMsg)
            runCatching {
                method {
                    name = "b"
                    paramCount = 1
                }.hookAll().before {
                    val arg = args(0).any()
                    if (arg is Long) {
                        if (NTQQHooker.isBackground()) {
                            YLog.debug("MSFProbe (legacy): blocked alarm registration in background")
                            result = null
                        }
                    }
                    // b(FromServiceMsg) - let it proceed
                }
            }.onFailure {
                YLog.error("MSFProbe: failed to hook legacy b(long): ${it.message}")
            }

            // Hook onReceive() - alarm broadcast handler
            // In background, skip probe execution
            runCatching {
                method {
                    name = "onReceive"
                    paramCount = 2
                }.hook().before {
                    if (NTQQHooker.isBackground()) {
                        YLog.debug("MSFProbe (legacy): blocked onReceive probe in background")
                        result = null
                    }
                }
            }.onFailure {
                YLog.error("MSFProbe: failed to hook legacy onReceive(): ${it.message}")
            }

            YLog.info("MSFProbe: hooked legacy MsfProbeManager successfully")
        } ?: YLog.warn("MSFProbe: LegacyMsfProbeManagerClass not found")
    }

    /**
     * Hook MSFAlarmManager - the alarm scheduling layer used by MSFProbeNewManager.
     * This is a safety net: if probe hooks miss, we still suppress the alarm registration.
     *
     * Hook b(int) - tryWakeLock: acquires WakeLock on alarm fire.
     * In background, skip the WakeLock acquisition.
     */
    private fun hookMSFAlarmManager() {
        MSFFeatures.MSFAlarmManagerClass?.apply {
            // The alarm callback f (coroutine handler) calls b(int) to acquire WakeLock
            // We hook b(int) to skip WakeLock in background
            // Only one b() with 1 param in this class, no ambiguity.
            runCatching {
                method {
                    name = "b"
                    paramCount = 1
                }.hook().before {
                    if (NTQQHooker.isBackground()) {
                        YLog.debug("MSFAlarmManager: blocked probe WakeLock in background")
                        result = null
                    }
                }
            }.onFailure {
                YLog.error("MSFProbe: failed to hook MSFAlarmManager.b(int): ${it.message}")
            }

            YLog.info("MSFProbe: hooked MSFAlarmManager successfully")
        } ?: YLog.warn("MSFProbe: MSFAlarmManagerClass not found")
    }
}
