package com.wkeqin.ntqqbattery.data

import android.content.Context
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.highcapable.yukihookapi.hook.factory.prefs
import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookPrefsBridge

object ConfigData {

    private const val PREFS_NAME = "ntqqbattery_config"
    const val FEATURE_CACHE_VERSION = "feature_cache_version"

    // Hook 状态追踪 (后缀 _status)
    private fun getStatusKey(configKey: String) = "${configKey}_status"

    private var prefs: YukiHookPrefsBridge? = null

    fun init(context: Context) {
        prefs = context.prefs(name = PREFS_NAME).native()
    }

    private fun getBoolean(key: String, defValue: Boolean) = prefs?.getBoolean(key, defValue) ?: defValue

    private fun putBoolean(key: String, value: Boolean) {
        prefs?.edit { putBoolean(key, value) }
    }

    fun getString(key: String, defValue: String = "") = prefs?.getString(key, defValue) ?: defValue

    fun putString(key: String, value: String) {
        prefs?.edit { putString(key, value) }
    }

    // 状态读写接口
    fun isEnabled(feature: FeatureDefinition) = getBoolean(feature.key, feature.defaultEnabled)

    fun setEnabled(feature: FeatureDefinition, value: Boolean) {
        putBoolean(feature.key, value)
    }

    fun isHooked(feature: FeatureDefinition) = getBoolean(getStatusKey(feature.key), false)

    fun setHooked(feature: FeatureDefinition, value: Boolean) {
        putBoolean(getStatusKey(feature.key), value)
    }
}
