package com.wkeqin.ntqqbattery.hook.entity

import android.content.Context
import android.os.Build
import com.wkeqin.ntqqbattery.data.ConfigData
import com.highcapable.yukihookapi.hook.factory.searchClass
import com.highcapable.yukihookapi.hook.factory.toClassOrNull
import com.highcapable.yukihookapi.hook.log.YLog

/**
 * 在 Hook 早期统一定位高风险目标，并按 QQ 版本缓存结果。
 */
object FeatureLocator {

    private const val KEY_MINI_APP_LAUNCHER = "feature_cache_mini_app_launcher"
    private const val KEY_MSF_CONFIG = "feature_cache_msf_config"

    private fun buildVersionKey(context: Context): String {
        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkgInfo.longVersionCode else @Suppress("DEPRECATION") pkgInfo.versionCode.toLong()
        return "${pkgInfo.versionName}($versionCode)"
    }

    private fun cacheKey(name: String) = "locator_$name"

    private fun readCachedClass(name: String): String? {
        val value = ConfigData.getString(cacheKey(name))
        return value.ifBlank { null }
    }

    private fun writeCachedClass(name: String, className: String?) {
        ConfigData.putString(cacheKey(name), className.orEmpty())
    }

    private fun ensureVersionCache(context: Context) {
        val versionKey = buildVersionKey(context)
        val cachedVersion = ConfigData.getString(ConfigData.FEATURE_CACHE_VERSION)
        if (cachedVersion != versionKey) {
            ConfigData.putString(ConfigData.FEATURE_CACHE_VERSION, versionKey)
            writeCachedClass(KEY_MINI_APP_LAUNCHER, null)
            writeCachedClass(KEY_MSF_CONFIG, null)
            YLog.info("FeatureLocator cache reset for QQ $versionKey")
        }
    }

    private fun Class<*>.hasMiniAppPreloadMethod() = declaredMethods.any {
        it.name == "preloadMiniApp" &&
            it.parameterCount == 1 &&
            it.parameterTypes.firstOrNull()?.name?.contains("IECMiniAppLauncher\$MiniAppType") == true
    }

    fun warmup(context: Context, loader: ClassLoader) {
        ensureVersionCache(context)
        warmupMiniAppLauncher(loader)
        warmupMSFConfig(loader)
    }

    private fun warmupMiniAppLauncher(loader: ClassLoader) {
        val resolved = resolveMiniAppLauncher(loader) ?: return
        writeCachedClass(KEY_MINI_APP_LAUNCHER, resolved.name)
        YLog.info("FeatureLocator cached MiniAppLauncher -> ${resolved.name}")
    }

    private fun warmupMSFConfig(loader: ClassLoader) {
        val resolved = resolveMSFConfig(loader) ?: return
        writeCachedClass(KEY_MSF_CONFIG, resolved.name)
        YLog.info("FeatureLocator cached MSFConfig -> ${resolved.name}")
    }

    fun getMiniAppLauncher(loader: ClassLoader): Class<*>? {
        readCachedClass(KEY_MINI_APP_LAUNCHER)
            ?.toClassOrNull(loader)
            ?.takeIf { it.hasMiniAppPreloadMethod() }
            ?.let { return it }
        return resolveMiniAppLauncher(loader)?.also { writeCachedClass(KEY_MINI_APP_LAUNCHER, it.name) }
    }

    fun getMSFConfig(loader: ClassLoader): Class<*>? {
        readCachedClass(KEY_MSF_CONFIG)
            ?.toClassOrNull(loader)
            ?.let { return it }
        return resolveMSFConfig(loader)?.also { writeCachedClass(KEY_MSF_CONFIG, it.name) }
    }

    private fun resolveMiniAppLauncher(loader: ClassLoader): Class<*>? {
        return sequenceOf("du.a", "qu.a")
            .mapNotNull { it.toClassOrNull(loader) }
            .firstOrNull { it.hasMiniAppPreloadMethod() }
            ?: loader.searchClass(name = "NTQQ_Features_MiniAppLauncher") {
                implements("com.tencent.ecommerce.base.miniapp.api.IECMiniAppLauncher")
            }.all().firstOrNull { it.hasMiniAppPreloadMethod() }
    }

    private fun resolveMSFConfig(loader: ClassLoader): Class<*>? {
        return NTQQFeatures.findClassSafe(
            cacheName = "MSFConfig",
            primaryName = "com.tencent.mobileqq.msf.core.w.d",
            packageName = "com.tencent.mobileqq.msf.core.w.",
            constant = "ConfigID{"
        )
    }
}
