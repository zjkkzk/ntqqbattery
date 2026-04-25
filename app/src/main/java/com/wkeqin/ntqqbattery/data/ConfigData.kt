package com.wkeqin.ntqqbattery.data

import android.content.Context
import android.content.SharedPreferences
import com.wkeqin.ntqqbattery.BuildConfig
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.highcapable.yukihookapi.hook.factory.prefs
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookPrefsBridge
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object ConfigData {

    private const val SHARED_PREFS_NAME = "ntqqbattery_config"
    private const val RUNTIME_PREFS_NAME = "ntqqbattery_runtime"
    private const val HIDE_DESKTOP_ICON = "hide_desktop_icon"
    const val FEATURE_CACHE_VERSION = "feature_cache_version"

    // Hook 状态追踪 (后缀 _status / _degraded)
    private fun getStatusKey(configKey: String) = "${configKey}_status"
    private fun getDegradedKey(configKey: String) = "${configKey}_degraded"

    private var sharedPrefs: YukiHookPrefsBridge? = null
    private var featurePrefs: YukiHookPrefsBridge? = null
    private var sharedWritablePrefs: SharedPreferences? = null
    private var runtimePrefs: YukiHookPrefsBridge? = null

    // FeatureLocator 类搜索缓存 — 直接用文件，绕开 SharedPreferences 跨进程问题
    private var cacheFile: File? = null
    private val cacheMap: MutableMap<String, String> = ConcurrentHashMap()
    @Volatile private var cacheDirty = false
    @Volatile private var batchMode = false

    fun init(context: Context) {
        val prefsContext = resolvePrefsContext(context)
        // 桥接配置用于兼容模块进程读取。
        sharedPrefs = prefsContext.prefs(name = SHARED_PREFS_NAME)
        // 当前进程内的功能开关统一走 native，保证寄生活动与 Hook 读写同一份配置。
        featurePrefs = prefsContext.prefs(name = SHARED_PREFS_NAME).native()
        sharedWritablePrefs = resolveModulePrefs(prefsContext, SHARED_PREFS_NAME)

        // 运行时缓存/状态只在当前进程使用，继续走 native。
        runtimePrefs = prefsContext.prefs(name = RUNTIME_PREFS_NAME).native()

        // FeatureLocator 缓存：直接读写文件，不依赖 SharedPreferences
        initFileCache(context)
    }

    private fun initFileCache(context: Context) {
        runCatching {
            val dir = File(context.filesDir, "locator_cache")
            dir.mkdirs()
            cacheFile = File(dir, "classes.json")
            loadCache()
            YLog.info("ConfigData: loaded ${cacheMap.size} cached entries from ${cacheFile?.absolutePath}")
        }.onFailure {
            YLog.error("ConfigData: failed to init file cache: ${it.message}")
        }
    }

    private fun loadCache() {
        val file = cacheFile ?: return
        if (!file.exists()) return
        runCatching {
            val json = JSONObject(file.readText())
            cacheMap.clear()
            json.keys().forEach { cacheMap[it] = json.getString(it) }
        }.onFailure {
            YLog.error("ConfigData: cache file corrupted, resetting: ${it.message}")
            cacheMap.clear()
        }
    }

    private fun saveCache() {
        val file = cacheFile ?: return
        if (!cacheDirty) return
        runCatching {
            val json = JSONObject(cacheMap)
            file.writeText(json.toString())
            cacheDirty = false
        }.onFailure {
            YLog.error("ConfigData: failed to save cache: ${it.message}")
        }
    }

    /** 批量写入场景：先写内存，最后一并落盘 */
    fun putStringBatch(block: () -> Unit) {
        batchMode = true
        try { block() } finally {
            batchMode = false
            saveCache()
        }
    }

    private fun resolvePrefsContext(context: Context): Context {
        if (context.packageName == BuildConfig.APPLICATION_ID) return context
        return runCatching {
            context.createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY)
        }.getOrElse { context }
    }

    private fun resolveModulePrefs(context: Context, name: String): SharedPreferences? {
        return runCatching {
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
        }.getOrNull()
    }

    private fun getBoolean(key: String, defValue: Boolean): Boolean {
        featurePrefs?.takeIf { it.contains(key) }?.let { return it.getBoolean(key, defValue) }
        sharedWritablePrefs?.takeIf { it.contains(key) }?.let { return it.getBoolean(key, defValue) }
        return sharedPrefs?.getBoolean(key, defValue) ?: defValue
    }

    private fun putBoolean(key: String, value: Boolean) {
        featurePrefs?.edit { putBoolean(key, value) }
        sharedWritablePrefs?.edit()?.putBoolean(key, value)?.apply()
    }

    fun getString(key: String, defValue: String = ""): String {
        return cacheMap[key]?.takeIf { it.isNotEmpty() } ?: defValue
    }

    fun putString(key: String, value: String) {
        cacheMap[key] = value
        cacheDirty = true
        if (!batchMode) saveCache()
    }

    // 状态读写接口
    fun isEnabled(feature: FeatureDefinition) = getBoolean(feature.key, feature.defaultEnabled)

    fun setEnabled(feature: FeatureDefinition, value: Boolean) {
        putBoolean(feature.key, value)
    }

    fun isHooked(feature: FeatureDefinition) = runtimePrefs?.getBoolean(getStatusKey(feature.key), false) ?: false

    fun setHooked(feature: FeatureDefinition, value: Boolean) {
        runtimePrefs?.edit { putBoolean(getStatusKey(feature.key), value) }
    }

    fun isDegraded(feature: FeatureDefinition) = runtimePrefs?.getBoolean(getDegradedKey(feature.key), false) ?: false

    fun setDegraded(feature: FeatureDefinition, value: Boolean) {
        runtimePrefs?.edit { putBoolean(getDegradedKey(feature.key), value) }
    }

    fun isDesktopIconHidden() = getBoolean(HIDE_DESKTOP_ICON, false)

    fun setDesktopIconHidden(value: Boolean) {
        putBoolean(HIDE_DESKTOP_ICON, value)
    }
}
