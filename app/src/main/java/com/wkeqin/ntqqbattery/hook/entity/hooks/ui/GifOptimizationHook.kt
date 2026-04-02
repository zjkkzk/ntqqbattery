package com.wkeqin.ntqqbattery.hook.entity.hooks.ui

import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.wkeqin.ntqqbattery.hook.entity.features.UIFeatures
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.HookParam
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

object GifOptimizationHook : YukiBaseHooker() {
    private val gifBombCache = Collections.synchronizedMap(WeakHashMap<Any, Boolean>())

    val feature = FeatureDefinition(
        key = "block_gif_optimization",
        titleRes = R.string.block_gif_optimization,
        category = FeatureCategory.COMPONENT,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "gif-optimization-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(GifOptimizationHook)
    }

    override fun onHook() {
        if (!ConfigData.isEnabled(FeatureRegistry.blockGifOptimization)) return
        hookGifExecutor()
        hookGifRuntimeControl()
    }

    private fun hookGifExecutor() {
        UIFeatures.LibraGifExecutorClass?.constructor { paramCount >= 0 }?.hookAll()?.after { optimizeExecutor(this) }
        UIFeatures.ComponentGifExecutorClass?.constructor { paramCount >= 0 }?.hookAll()?.after { optimizeExecutor(this) }
    }

    private fun optimizeExecutor(hookParam: HookParam) {
        val instance = hookParam.instance
        runCatching {
            val executor = instance as? ScheduledThreadPoolExecutor ?: return@runCatching
            executor.setRemoveOnCancelPolicy(true)
            executor.setKeepAliveTime(10, TimeUnit.SECONDS)
            executor.allowCoreThreadTimeOut(true)
            YLog.info("Optimized GifExecutor: ${executor::class.java.name} -> removeOnCancelPolicy=true, allowCoreThreadTimeOut=true")
        }.onFailure {
            YLog.error("Failed to optimize GifExecutor: ${it.message}")
        }
    }

    private fun hookGifRuntimeControl() {
        "com.tencent.libra.extension.gif.GifDrawable".toClassOrNull()?.apply {
            method { name = "start" }.hook().before {
                if (NTQQHooker.isBackground()) {
                    runCatching { instance?.current()?.method { name = "stop" }?.call() }
                    result = NTQQHooker.safeReturn(method)
                }
            }
        }
        "com.tencent.component.media.gif.NewGifDrawable".toClassOrNull()?.apply {
            method { name = "start" }.hook().before {
                if (NTQQHooker.isBackground()) {
                    runCatching { instance?.current()?.method { name = "stop" }?.call() }
                    result = NTQQHooker.safeReturn(method)
                }
            }
        }

        "com.tencent.libra.extension.gif.RenderTask".toClassOrNull()?.apply {
            method { name = "e" }.hook().before {
                if (NTQQHooker.isBackground()) {
                    val drawable = runCatching { instance?.current()?.field { name = "f96954d" }?.any() }.getOrNull()
                    runCatching { drawable?.current()?.method { name = "stop" }?.call() }
                    result = NTQQHooker.safeReturn(method)
                }
            }
        }
        "com.tencent.component.media.gif.RenderTask".toClassOrNull()?.apply {
            method { name = "doWork" }.hook().before {
                if (NTQQHooker.isBackground()) {
                    val drawable = runCatching { instance?.current()?.field { name = "mGifDrawable" }?.any() }.getOrNull()
                    runCatching { drawable?.current()?.method { name = "stop" }?.call() }
                    result = NTQQHooker.safeReturn(method)
                }
            }
        }
        "com.tencent.component.media.gif.PrepareAndRenderTask".toClassOrNull()?.apply {
            method { name = "doWork" }.hook().before {
                if (NTQQHooker.isBackground()) {
                    val drawable = runCatching { instance?.current()?.field { name = "mGifDrawable" }?.any() }.getOrNull()
                    runCatching { drawable?.current()?.method { name = "stop" }?.call() }
                    result = NTQQHooker.safeReturn(method)
                }
            }
        }

        UIFeatures.LibraGifInfoHandleClass?.apply {
            method { name = "w" }.hook().apply {
                before {
                    val gifInstance = instance
                    val isBomb = gifBombCache[gifInstance]
                    if (isBomb == true) {
                        result = 60_000L
                        return@before
                    }
                    if (isBomb == null) {
                        val width = runCatching { gifInstance.current().method { name = "p" }.int() }.getOrDefault(0)
                        val height = runCatching { gifInstance.current().method { name = "h" }.int() }.getOrDefault(0)
                        if (width > 4096 || height > 4096) {
                            YLog.error("Blocked Libra GIF Bomb: ${width}x${height}, throttling...")
                            gifBombCache[gifInstance] = true
                            result = 60_000L
                            return@before
                        }
                        gifBombCache[gifInstance] = false
                    }
                }
                after {
                    val delay = result as? Long ?: return@after
                    if (delay in 0..20) result = 100L
                }
            }
        }
        UIFeatures.ComponentGifInfoHandleClass?.apply {
            method { name { it == "renderFrame" || it == "renderFrameForGifPlay" } }.hookAll().apply {
                before {
                    val gifInstance = instance
                    val isBomb = gifBombCache[gifInstance]
                    if (isBomb == true) {
                        result = 60_000L
                        return@before
                    }
                    if (isBomb == null) {
                        val width = runCatching { gifInstance.current().field { name = "width" }.int() }.getOrDefault(0)
                        val height = runCatching { gifInstance.current().field { name = "height" }.int() }.getOrDefault(0)
                        if (width > 4096 || height > 4096) {
                            YLog.error("Blocked Component GIF Bomb: ${width}x${height}, throttling...")
                            gifBombCache[gifInstance] = true
                            result = 60_000L
                            return@before
                        }
                        gifBombCache[gifInstance] = false
                    }
                }
                after {
                    val delay = result as? Long ?: return@after
                    if (delay in 0..20) result = 100L
                }
            }
        }
    }
}
