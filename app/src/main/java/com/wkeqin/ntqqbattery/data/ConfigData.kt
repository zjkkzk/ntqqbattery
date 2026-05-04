package com.wkeqin.ntqqbattery.data

import android.content.Context
import android.content.SharedPreferences
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.highcapable.yukihookapi.hook.factory.prefs
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookPrefsBridge
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import android.widget.Toast
import android.os.Handler
import android.os.Looper

object ConfigData {

    private const val SHARED_PREFS_NAME = "ntqqbattery_config"
    private const val RUNTIME_PREFS_NAME = "ntqqbattery_runtime"
    private const val HIDE_DESKTOP_ICON = "hide_desktop_icon"
    const val FEATURE_CACHE_VERSION = "feature_cache_version"

    // Hook 状态追踪 (后缀 _status / _degraded)
    private fun getStatusKey(configKey: String) = "${configKey}_status"
    private fun getDegradedKey(configKey: String) = "${configKey}_degraded"

    private var sharedPrefs: YukiHookPrefsBridge? = null
    private var sharedWritablePrefs: SharedPreferences? = null
    private var runtimePrefs: YukiHookPrefsBridge? = null

    // FeatureLocator 类搜索缓存 — 直接用文件，绕开 SharedPreferences 跨进程问题
    private var cacheFile: File? = null
    private val cacheMap: MutableMap<String, String> = ConcurrentHashMap()
    @Volatile private var cacheDirty = false
    @Volatile private var batchMode = false
    private var appContext: Context? = null

    @Volatile private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        // config 和 runtime 统一使用 getSharedPreferences，确保在所有环境下都能正常生成文件
        sharedPrefs = appContext!!.prefs(name = SHARED_PREFS_NAME)
        sharedWritablePrefs = appContext!!.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        runtimePrefs = appContext!!.prefs(name = RUNTIME_PREFS_NAME).native()

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


    private fun getBoolean(key: String, defValue: Boolean): Boolean {
        sharedWritablePrefs?.takeIf { it.contains(key) }?.let { return it.getBoolean(key, defValue) }
        return sharedPrefs?.getBoolean(key, defValue) ?: defValue
    }

    private fun putBoolean(key: String, value: Boolean) {
        val wrote = sharedWritablePrefs?.let {
            it.edit().putBoolean(key, value).apply()
            true
        } ?: false
        if (!wrote) {
            YLog.error("ConfigData: putBoolean($key, $value) failed — sharedWritablePrefs is null")
            Handler(Looper.getMainLooper()).post {
                appContext?.let { ctx -> Toast.makeText(ctx, "NTQQBattery: 设置保存失败", Toast.LENGTH_SHORT).show() }
            }
        }
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
