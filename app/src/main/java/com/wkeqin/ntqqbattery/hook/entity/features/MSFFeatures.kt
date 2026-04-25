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
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        FeatureLocator.getCachedMsfPullConfigUtil(loader)?.also {
            YLog.info("Locate MsfPullConfigUtilClass -> ${it.name}")
        }
    }

    /**
     * MSF 线程管理器
     */
    val MsfThreadManagerClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        FeatureLocator.getCachedMsfThreadManager(loader)?.also {
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

    /**
     * MSFWakeUpLockManager (f0/c.java)
     * Orchestrates all MSF WakeLock acquisitions based on events (alarm, msgPush, heartbeat, screen, fg/bg).
     */
    val MSFWakeUpLockManagerClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        FeatureLocator.getCachedMSFWakeUpLockManager(loader)?.also {
            YLog.info("Locate MSFWakeUpLockManagerClass -> ${it.name}")
        } ?: run {
            YLog.warn("MSFWakeUpLockManagerClass not found (f0/c obfuscated)")
            null
        }
    }

    /**
     * MSF:WakeLock wrapper (f0/a.java)
     * Low-level WakeLock wrapper that acquires PowerManager.WakeLock with tag "MSF:WakeLock".
     */
    val MSFWakeLockWrapperClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        runCatching {
            loader.loadClass("com.tencent.mobileqq.msf.core.f0.a")
        }.getOrNull()?.also {
            YLog.info("Locate MSFWakeLockWrapperClass -> ${it.name}")
        } ?: run {
            YLog.warn("MSFWakeLockWrapperClass not found (com.tencent.mobileqq.msf.core.f0.a)")
            null
        }
    }

    /**
     * MSFProbeNewManager (push/d.java)
     * Newer probe manager using MSFAlarmManager for alarm scheduling.
     */
    val MSFProbeNewManagerClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        // 先尝试硬编码名，再尝试混淆名 d
        runCatching { loader.loadClass("com.tencent.mobileqq.msf.core.push.MSFProbeNewManager") }.getOrNull()
            ?: runCatching { loader.loadClass("com.tencent.mobileqq.msf.core.push.d") }.getOrNull()
            ?.also {
                YLog.info("Locate MSFProbeNewManagerClass -> ${it.name}")
            } ?: run {
                YLog.warn("MSFProbeNewManagerClass not found")
                null
            }
    }

    /**
     * Legacy MsfProbeManager (push/e.java)
     * Older probe manager using direct AlarmManager reflection.
     */
    val LegacyMsfProbeManagerClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        runCatching {
            loader.loadClass("com.tencent.mobileqq.msf.core.push.e")
        }.getOrNull()?.also {
            YLog.info("Locate LegacyMsfProbeManagerClass -> ${it.name}")
        } ?: run {
            YLog.warn("LegacyMsfProbeManagerClass not found (com.tencent.mobileqq.msf.core.push.e)")
            null
        }
    }

    /**
     * MSFAlarmManager (push/MSFAlarmManager.java)
     * Alarm scheduling layer with 4-tier fallback and WakeLock management.
     * Discovery: try unobfuscated name first, then find via MSFProbeNewManager's
     * alarm callback field type (MSFAlarmManager$AlarmCallback interface).
     */
    val MSFAlarmManagerClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        // Try unobfuscated name first
        runCatching { loader.loadClass("com.tencent.mobileqq.msf.core.push.MSFAlarmManager") }.getOrNull()
            // Fallback: find via MSFProbeNewManager's alarm callback field type
            ?: findMSFAlarmManagerViaProbeField(loader)
            ?.also {
                YLog.info("Locate MSFAlarmManagerClass -> ${it.name}")
            } ?: run {
                YLog.warn("MSFAlarmManagerClass not found")
                null
            }
    }

    /**
     * Find MSFAlarmManager by inspecting MSFProbeNewManager's field types.
     * MSFProbeNewManager has a field of type MSFAlarmManager$AlarmCallback (inner interface).
     * The enclosing class of that interface is MSFAlarmManager.
     */
    private fun findMSFAlarmManagerViaProbeField(loader: ClassLoader): Class<*>? {
        val probeClass = MSFProbeNewManagerClass ?: return null
        return runCatching {
            for (field in probeClass.declaredFields) {
                val fieldTypeName = field.type.name
                // Look for an interface in the push package with a "$a" inner class pattern
                // (MSFAlarmManager$AlarmCallback is obfuscated to SomeName$a)
                if (field.type.isInterface &&
                    fieldTypeName.contains(".push.") &&
                    fieldTypeName.endsWith("\$a")
                ) {
                    val enclosingName = fieldTypeName.substringBeforeLast("\$a")
                    val enclosingClass = runCatching { loader.loadClass(enclosingName) }.getOrNull()
                    if (enclosingClass != null) {
                        YLog.info("MSFAlarmManager discovered via probe field '${field.name}' -> ${enclosingClass.name}")
                        return@runCatching enclosingClass
                    }
                }
            }
            null
        }.getOrNull()
    }
}
