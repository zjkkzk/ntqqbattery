package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import android.content.Context
import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.wkeqin.ntqqbattery.hook.entity.features.PerfFeatures
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog

object GPUResourceHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_gpu_resources",
        titleRes = R.string.block_gpu_resources,
        summaryRes = R.string.block_gpu_resources,
        category = FeatureCategory.COMPONENT,
        defaultEnabled = false
    )

    val plan = HookPlan(
        id = "gpu-resource-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(GPUResourceHook)
    }

    override fun onHook() {
        if (!ConfigData.isEnabled(FeatureRegistry.blockGPUResources)) return

        val glThreadManagerClass = PerfFeatures.GLThreadManagerClass ?: return
        val hookInstalled = runCatching {
            glThreadManagerClass.installGpuBlockHook()
        }.getOrElse {
            YLog.error("GPUResourceHook: failed to install hook: ${it.message}")
            false
        }

        if (hookInstalled) {
            ConfigData.setHooked(FeatureRegistry.blockGPUResources, true)
        }
    }

    private fun Class<*>.installGpuBlockHook(): Boolean {
        val candidates = listOf("e", "c", "init")
        for (candidate in candidates) {
            val hooked = runCatching {
                method {
                    name = candidate
                    param(Context::class.java)
                }.hook().before {
                    result = NTQQHooker.safeReturn(method)
                }
                true
            }.getOrElse {
                false
            }
            if (hooked) return true
        }
        return false
    }
}
