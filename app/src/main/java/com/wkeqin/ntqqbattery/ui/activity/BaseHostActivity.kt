package com.wkeqin.ntqqbattery.ui.activity

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import com.highcapable.yukihookapi.hook.factory.applyModuleTheme
import com.highcapable.yukihookapi.hook.factory.injectModuleAppResources
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.xposed.parasitic.activity.proxy.ModuleActivity
import com.wkeqin.ntqqbattery.R

abstract class BaseHostActivity : Activity(), ModuleActivity {

    companion object {
        const val PRIMARY_PROXY_ACTIVITY = "com.tencent.mobileqq.activity.QQSettingSettingActivity"
        const val FALLBACK_PROXY_ACTIVITY = "com.tencent.mobileqq.activity.QPublicFragmentActivity"

        private var moduleResources: Resources? = null
        private var modulePackageName: String? = null

        /**
         * 通过 AssetManager.addAssetPath 创建独立的模块 Resources 对象
         * 寄生 Activity 在宿主进程中运行，无法通过 PackageManager 访问模块包
         * 需要直接使用模块 APK 路径注入资源
         */
        internal fun getModuleResources(context: Context): Resources? {
            moduleResources?.let { return it }
            val pkgName = modulePackageName ?: R::class.java.`package`?.name ?: return null
            modulePackageName = pkgName
            return runCatching {
                // 方式1: 尝试通过 PackageManager（可能在某些环境下可用）
                val appInfo = context.packageManager.getApplicationInfo(pkgName, 0)
                context.packageManager.getResourcesForApplication(appInfo).also {
                    moduleResources = it
                    YLog.info("BaseHostActivity: created module Resources via PackageManager for $pkgName")
                }
            }.getOrElse { pmError ->
                YLog.info("BaseHostActivity: PackageManager approach failed: ${pmError.message}, trying YukiXposedModule approach")
                // 方式2: 通过反射获取 YukiXposedModule.moduleAppFilePath（yukihookapi 内部存储的模块 APK 路径）
                runCatching {
                    val yukiXposedModuleClass = Class.forName("com.highcapable.yukihookapi.hook.xposed.bridge.YukiXposedModule")
                    val moduleAppFilePathField = yukiXposedModuleClass.getDeclaredField("moduleAppFilePath")
                    moduleAppFilePathField.isAccessible = true
                    val moduleApkPath = moduleAppFilePathField.get(null) as String

                    if (moduleApkPath.isBlank()) {
                        throw IllegalStateException("YukiXposedModule.moduleAppFilePath is empty")
                    }
                    YLog.info("BaseHostActivity: got module APK path from YukiXposedModule: $moduleApkPath")

                    // 创建新的 AssetManager 并添加模块资源路径
                    val assetManager = android.content.res.AssetManager::class.java.newInstance()
                    val addAssetPathMethod = android.content.res.AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java)
                    addAssetPathMethod.isAccessible = true
                    val cookie = addAssetPathMethod.invoke(assetManager, moduleApkPath) as Int
                    YLog.info("BaseHostActivity: addAssetPath returned cookie=$cookie")

                    if (cookie == 0) {
                        throw IllegalStateException("addAssetPath returned 0, module APK not found: $moduleApkPath")
                    }

                    // 创建模块 Resources
                    val moduleRes = Resources(assetManager, context.resources.displayMetrics, context.resources.configuration)
                    moduleResources = moduleRes
                    YLog.info("BaseHostActivity: created module Resources via YukiXposedModule approach")
                    moduleRes
                }.getOrElse { yukiError ->
                    YLog.error("BaseHostActivity: YukiXposedModule approach also failed: ${yukiError.message}")
                    null
                }
            }
        }
    }

    /**
     * ContextWrapper 子类，重写 getResources() 返回模块的 Resources
     * 使 setTheme() → onApplyThemeResource() 能正确解析 0x64 资源 ID
     */
    private class ModuleResourcesContextWrapper(
        base: Context,
        private val moduleRes: Resources
    ) : ContextWrapper(base) {
        override fun getResources(): Resources = moduleRes
    }

    override val moduleTheme
        get() = R.style.Theme_NTQQBattery

    override val proxyClassName
        get() = FALLBACK_PROXY_ACTIVITY

    override fun getClassLoader() = delegate.getClassLoader()

    /**
     * 获取模块的独立 Resources 对象
     * 用于直接加载模块资源，避免宿主 Resources 错误解析模块资源 ID
     */
    protected fun getModuleResources(): Resources? = getModuleResources(this)

    override fun getResources(): Resources {
        // 优先返回模块 Resources（如果已通过 fallback 注入）
        // 避免宿主 Resources 错误解析 0x64 开头的模块资源 ID
        return moduleResources ?: super.getResources()
    }

    override fun attachBaseContext(newBase: Context?) {
        val isParasitic = newBase != null && newBase.packageName != "com.wkeqin.ntqqbattery"
        val wrappedBase = if (newBase != null && moduleTheme != -1) {
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
        // fallback: applyModuleTheme 可能返回 wrapper 但 injectModuleAppResources 内部静默失败
        // 通过 ContextWrapper 重写 getResources() 注入模块 Resources，
        // 确保 setTheme() → onApplyThemeResource() 能正确解析 0x64 资源 ID
        if (isParasitic) {
            injectModuleResourcesFallback(newBase!!)
        }
    }

    /**
     * 当 applyModuleTheme 内部的 AssetManager.addAssetPath 注入静默失败时，
     * 通过 ContextWrapper 将模块 Resources 注入 Context 链，
     * 使 Context.getResources() 返回模块的 Resources
     */
    private fun injectModuleResourcesFallback(context: Context) {
        runCatching {
            // 直接应用 fallback 注入，不依赖不可靠的资源可用性测试
            // 宿主 Resources 可能巧合地解析模块资源 ID（如 drawable 通过但 color 失败），
            // 导致测试通过但实际模块资源并未真正注入

            // 创建模块 Resources 并用 ContextWrapper 注入 Context 链
            val moduleRes = getModuleResources(context) ?: return
            // 重设 mBase 为包含模块 Resources 的 ContextWrapper
            // mBase 字段在 ContextThemeWrapper 中，不在 Activity 中
            val contextThemeWrapperClass = Class.forName("android.view.ContextThemeWrapper")
            val mBaseField = try {
                contextThemeWrapperClass.getDeclaredField("mBase")
            } catch (e: NoSuchFieldException) {
                // 某些 Android 版本可能在 ContextWrapper 中
                Class.forName("android.content.ContextWrapper").getDeclaredField("mBase")
            }
            mBaseField.isAccessible = true
            mBaseField.set(this@BaseHostActivity, ModuleResourcesContextWrapper(context, moduleRes))
            // 同时修改 ContextThemeWrapper 的 mResources 字段，
            // 确保 setTheme() 内部使用模块的 Resources 加载主题资源
            // 避免 0x64 开头的模块资源 ID 被宿主 Resources 错误解析
            try {
                // 尝试修改 mResources 字段
                val mResourcesField = try {
                    contextThemeWrapperClass.getDeclaredField("mResources")
                } catch (e: NoSuchFieldException) {
                    // 某些 Android 版本可能没有这个字段，尝试其他名称
                    YLog.info("BaseHostActivity: mResources field not found, trying mThemeResources")
                    contextThemeWrapperClass.getDeclaredField("mThemeResources")
                }
                mResourcesField.isAccessible = true
                val oldValue = mResourcesField.get(this@BaseHostActivity)
                mResourcesField.set(this@BaseHostActivity, moduleRes)
                YLog.info("BaseHostActivity: patched ContextThemeWrapper.mResources (old=$oldValue, new=$moduleRes)")
            } catch (e: Exception) {
                YLog.error("BaseHostActivity: failed to patch mResources: ${e.message}")
            }
            // 同时重置 mTheme，确保下次 setTheme 时使用新的 Resources 创建 Theme
            try {
                val mThemeField = contextThemeWrapperClass.getDeclaredField("mTheme")
                mThemeField.isAccessible = true
                mThemeField.set(this@BaseHostActivity, null)
                YLog.info("BaseHostActivity: reset mTheme to null")
            } catch (e: Exception) {
                YLog.error("BaseHostActivity: failed to reset mTheme: ${e.message}")
            }
            YLog.info("BaseHostActivity: fallback injection SUCCESS for ${javaClass.name}")
        }.onFailure {
            YLog.error("BaseHostActivity: injectModuleResourcesFallback failed: ${it.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 在 delegate.onCreate 之前注入模块资源并设置主题
        if (moduleTheme != -1) {
            try {
                YLog.info("BaseHostActivity: preparing module resources for setTheme")
                // 获取独立的模块 Resources 对象
                val moduleRes = getModuleResources(this)
                if (moduleRes != null) {
                    YLog.info("BaseHostActivity: got module Resources, creating Theme from it")
                    // 使用模块的 Resources 创建 Theme
                    val moduleThemeObj = moduleRes.newTheme()
                    moduleThemeObj.applyStyle(moduleTheme, true)
                    // 将模块 Theme 应用到当前 Activity
                    // 注意：这需要通过反射设置 Activity 的 mTheme 字段
                    val themeField = Class.forName("android.view.ContextThemeWrapper").getDeclaredField("mTheme")
                    themeField.isAccessible = true
                    themeField.set(this, moduleThemeObj)
                    YLog.info("BaseHostActivity: applied module Theme successfully")
                } else {
                    YLog.error("BaseHostActivity: module Resources is null, falling back to injectModuleAppResources")
                    injectModuleAppResources()
                    setTheme(moduleTheme)
                }
            } catch (e: Exception) {
                YLog.error("BaseHostActivity: module Theme setup failed: ${e.message}")
                // 如果模块 Theme 设置失败，尝试直接注入资源
                try {
                    injectModuleAppResources()
                    setTheme(moduleTheme)
                } catch (e2: Exception) {
                    YLog.error("BaseHostActivity: fallback setTheme also failed: ${e2.message}")
                }
            }
        }
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
