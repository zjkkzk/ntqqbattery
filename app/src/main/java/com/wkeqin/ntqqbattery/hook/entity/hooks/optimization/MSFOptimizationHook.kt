package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQFeatures
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.wkeqin.ntqqbattery.hook.entity.features.MSFFeatures
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog
import java.lang.reflect.Modifier

object MSFOptimizationHook : YukiBaseHooker() {

    val optimizeFeature = FeatureDefinition(
        key = "optimize_msf_strategy",
        titleRes = R.string.optimize_msf_strategy,
        category = FeatureCategory.OPTIMIZATION,
        defaultEnabled = true
    )

    val aggressiveFeature = FeatureDefinition(
        key = "aggressive_msf_optimization",
        titleRes = R.string.aggressive_msf_optimization,
        category = FeatureCategory.OPTIMIZATION,
        defaultEnabled = false
    )

    val plan = HookPlan(
        id = "msf-optimization-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(MSFOptimizationHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.optimizeMsfStrategy).not()) return

        MSFFeatures.MSFConfigClass?.apply {
            runCatching {
                declaredMethods
                    .filter {
                        Modifier.isStatic(it.modifiers) &&
                            it.parameterCount == 0 &&
                            it.returnType == Int::class.javaPrimitiveType &&
                            it.name in setOf("getHeartbeatInterval", "K0", "I0")
                    }
                    .forEach { target ->
                        YLog.info("Hook MSF heartbeat interval -> ${this.name}.${target.name}()")
                        target.hook().before { result = 300000 }
                    }
            }

            runCatching {
                method { name { it in setOf("K", "L", "O", "M") }; emptyParam() }.hookAll().before {
                    when (method.name) {
                        "K", "L" -> result = 60000
                        "O", "M" -> if (ConfigData.isEnabled(FeatureRegistry.aggressiveMsfOptimization)) result = 1800000
                    }
                }
            }

            NTQQFeatures.findStaticBooleanToggles(this).forEach { target ->
                val name = target.name
                if (name.length > 3 && !name.contains("Connected") && !name.contains("Loaded")) {
                    target.hook().before { if (NTQQHooker.isBackground()) result = false }
                }
            }
        }

        MSFFeatures.MSFAliveJobServiceClass?.apply {
            method {
                name = "onStartJob"
                paramCount = 1
            }.hook().before {
                if (NTQQHooker.isBackgroundRestrictedProcess()) {
                    YLog.debug("Blocked MSFAliveJobService.onStartJob")
                    result = false
                }
            }
        }
    }
}
