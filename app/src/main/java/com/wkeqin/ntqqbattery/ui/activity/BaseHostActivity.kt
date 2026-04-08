package com.wkeqin.ntqqbattery.ui.activity

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import com.highcapable.yukihookapi.hook.factory.applyModuleTheme
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.xposed.parasitic.activity.proxy.ModuleActivity
import com.wkeqin.ntqqbattery.R

abstract class BaseHostActivity : Activity(), ModuleActivity {

    companion object {
        const val PRIMARY_PROXY_ACTIVITY = "com.tencent.mobileqq.activity.QQSettingSettingActivity"
        const val FALLBACK_PROXY_ACTIVITY = "com.tencent.mobileqq.activity.QPublicFragmentActivity"
    }

    override val moduleTheme
        get() = R.style.Theme_NTQQBattery

    override val proxyClassName
        get() = FALLBACK_PROXY_ACTIVITY

    override fun getClassLoader() = delegate.getClassLoader()

    override fun attachBaseContext(newBase: Context?) {
        val wrappedBase = if (newBase != null && moduleTheme != -1) {
            val isParasitic = newBase.packageName != "com.wkeqin.ntqqbattery"
            if (isParasitic) {
                runCatching { newBase.applyModuleTheme(moduleTheme) }.getOrElse {
                    YLog.error(
                        "BaseHostActivity: applyModuleTheme failed for ${javaClass.name}, " +
                            "base=${newBase.javaClass.name}, package=${newBase.packageName}, " +
                            "proxy=$proxyClassName, theme=$moduleTheme, error=${it.stackTraceToString()}"
                    )
                    newBase
                }
            } else newBase
        } else newBase
        super.attachBaseContext(wrappedBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        delegate.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        delegate.onConfigurationChanged(newConfig)
        super.onConfigurationChanged(newConfig)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        delegate.onRestoreInstanceState(savedInstanceState)
        super.onRestoreInstanceState(savedInstanceState)
    }
}
