package com.wkeqin.ntqqbattery.hook.entity.features

import com.wkeqin.ntqqbattery.hook.entity.FeatureLocator
import com.wkeqin.ntqqbattery.hook.entity.NTQQFeatures
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog

/**
 * 界面、主题与具体业务相关特征定位
 */
object UIFeatures {

    /**
     * 启动页 Activity
     */
    val SplashActivityClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        "com.tencent.mobileqq.activity.SplashActivity".toClassOrNull(loader)?.also {
            YLog.info("Locate SplashActivityClass -> ${it.name}")
        }
    }

    /**
     * 小程序预加载实现
     */
    val MiniAppLauncherImpl by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        FeatureLocator.getMiniAppLauncher(loader)?.also {
            YLog.info("Locate MiniAppLauncherImpl -> ${it.name}")
        }
    }

    /**
     * 主题视频控制器门面
     */
    val ThemeVideoControllerClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        FeatureLocator.getCachedThemeVideoController(loader)?.also {
            YLog.info("Locate ThemeVideoControllerClass -> ${it.name}")
        }
    }

    /**
     * 超级主题视频控制器
     */
    val SuperThemeVideoControllerClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        FeatureLocator.getCachedSuperThemeVideoController(loader)?.also {
            YLog.info("Locate SuperThemeVideoControllerClass -> ${it.name}")
        }
    }

    /**
     * 默认主题视频控制器
     */
    val DefaultVideoControllerClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        "com.tencent.mobileqq.vas.theme.video.DefaultVideoController".toClassOrNull(loader)?.also {
            YLog.info("Locate DefaultVideoControllerClass -> ${it.name}")
        }
    }

    /**
     * 循环主题视频控制器
     */
    val LoopVideoControllerClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        "com.tencent.mobileqq.vas.theme.video.LoopVideoController".toClassOrNull(loader)?.also {
            YLog.info("Locate LoopVideoControllerClass -> ${it.name}")
        }
    }

    /**
     * 主题视频控制器基类
     */
    val BaseVideoControllerClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        FeatureLocator.getCachedBaseVideoController(loader)?.also {
            YLog.info("Locate BaseVideoControllerClass -> ${it.name}")
        }
    }
    
    /**
     * Libra GIF 渲染执行器 (GifRenderingExe)
     */
    val LibraGifExecutorClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        FeatureLocator.getCachedLibraGifExecutor(loader)?.also {
            YLog.info("Locate LibraGifExecutorClass -> ${it.name}")
        }
    }

    /**
     * Component GIF 渲染执行器
     */
    val ComponentGifExecutorClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        "com.tencent.component.media.gif.GifRenderingExecutor".toClassOrNull(loader)?.also {
            YLog.info("Locate ComponentGifExecutorClass -> ${it.name}")
        }
    }

    /**
     * Libra GifInfoHandle (原生句柄包装器)
     */
    val LibraGifInfoHandleClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        "com.tencent.libra.extension.gif.GifInfoHandle".toClassOrNull(loader)?.also {
            YLog.info("Locate LibraGifInfoHandleClass -> ${it.name}")
        }
    }

    /**
     * Component GifInfoHandle (原生句柄包装器)
     */
    val ComponentGifInfoHandleClass by lazy {
        val loader = NTQQFeatures.classLoader ?: return@lazy null
        "com.tencent.component.media.gif.GifInfoHandle".toClassOrNull(loader)?.also {
            YLog.info("Locate ComponentGifInfoHandleClass -> ${it.name}")
        }
    }
}
