package com.wkeqin.ntqqbattery.ui.activity

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.highcapable.yukihookapi.hook.factory.applyModuleTheme
import com.highcapable.yukihookapi.hook.xposed.parasitic.activity.proxy.ModuleActivity
import com.wkeqin.ntqqbattery.R

abstract class BaseHostActivity : AppCompatActivity(), ModuleActivity {

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
        // 注意：applyModuleTheme 只在寄生模式下使用
        // 独立运行时，子类应在 onCreate 中调用 setTheme
        val wrappedBase = if (newBase != null && moduleTheme != -1) {
            // 检查是否在宿主进程中运行（寄生模式）
            val isParasitic = newBase.packageName != "com.wkeqin.ntqqbattery"
            if (isParasitic) {
                runCatching { newBase.applyModuleTheme(moduleTheme) }.getOrElse { newBase }
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
