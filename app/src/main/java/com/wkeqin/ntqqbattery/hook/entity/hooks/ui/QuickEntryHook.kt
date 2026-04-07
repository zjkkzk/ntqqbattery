package com.wkeqin.ntqqbattery.hook.entity.hooks.ui

import android.app.Activity
import android.content.Context
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.factory.openModuleSettings
import com.wkeqin.ntqqbattery.ui.activity.BaseHostActivity
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Proxy

object QuickEntryHook : YukiBaseHooker() {

    private const val ENTRY_ID = 0x4E545142
    private const val ENTRY_TITLE = "NTQQBattery"

    val plan = HookPlan(
        id = "quick-entry-app-create",
        stage = HookStage.APP_CREATE
    ) {
        registerHostActivityProxy(it)
        loadHooker(QuickEntryHook)
    }

    override fun onHook() {
        hookSettingsProviders()
    }

    private fun registerHostActivityProxy(context: Context?) {
        val hostContext = context ?: return
        runCatching {
            hostContext.registerModuleAppActivities(proxy = BaseHostActivity.FALLBACK_PROXY_ACTIVITY)
            YLog.info("QuickEntryHook: registered module activity proxy -> ${BaseHostActivity.FALLBACK_PROXY_ACTIVITY}")
        }.onFailure {
            YLog.error("QuickEntryHook: failed to register module activity proxy: ${it.message}")
        }
    }

    private fun hookSettingsProviders() {
        runCatching {
            val mainSettingConfigProvider = "com.tencent.mobileqq.setting.main.MainSettingConfigProvider".toClassOrNull()
            val newSettingConfigProvider = "com.tencent.mobileqq.setting.main.NewSettingConfigProvider".toClassOrNull()
            val newSettingConfigProviderObf = "com.tencent.mobileqq.setting.main.b".toClassOrNull()

            val oldMethod = mainSettingConfigProvider?.declaredMethods?.firstOrNull {
                it.returnType == List::class.java &&
                    it.parameterTypes.size == 1 &&
                    it.parameterTypes[0] == Context::class.java
            }
            val newMethod = newSettingConfigProvider?.declaredMethods?.firstOrNull {
                it.returnType == List::class.java &&
                    it.parameterTypes.size == 1 &&
                    it.parameterTypes[0] == Context::class.java
            }
            val newObfMethod = newSettingConfigProviderObf?.declaredMethods?.firstOrNull {
                it.returnType == List::class.java &&
                    it.parameterTypes.size == 1 &&
                    it.parameterTypes[0] == Context::class.java
            }

            if (oldMethod == null && newMethod == null && newObfMethod == null) {
                YLog.debug("QuickEntryHook: no settings provider method found.")
                return@runCatching
            }

            val abstractItemProcessor = sequenceOf(
                "com.tencent.mobileqq.setting.main.processor.AccountSecurityItemProcessor",
                "com.tencent.mobileqq.setting.main.processor.AboutItemProcessor"
            ).mapNotNull { it.toClassOrNull() }
                .mapNotNull { it.superclass }
                .firstOrNull()
                ?: error("QuickEntryHook: abstract item processor not found")

            val simpleItemProcessor = sequenceOf(
                "com.tencent.mobileqq.setting.processor.g",
                "com.tencent.mobileqq.setting.processor.h",
                "com.tencent.mobileqq.setting.processor.i",
                "com.tencent.mobileqq.setting.processor.j",
                "as3.i"
            ).mapNotNull { it.toClassOrNull() }
                .firstOrNull { it.superclass == abstractItemProcessor }
                ?: error("QuickEntryHook: simple item processor not found")

            val setClickListenerMethods = simpleItemProcessor.declaredMethods
                .filter { method ->
                    val argTypes = method.parameterTypes
                    method.returnType == Void.TYPE &&
                        argTypes.size == 1 &&
                        argTypes[0].name == "kotlin.jvm.functions.Function0"
                }
                .sortedBy { it.name }
            if (setClickListenerMethods.isEmpty()) {
                error("QuickEntryHook: click listener method not found")
            }

            val callback = createProviderHook(simpleItemProcessor, setClickListenerMethods)
            oldMethod?.let { XposedBridge.hookMethod(it, callback) }
            newMethod?.let { XposedBridge.hookMethod(it, callback) }
            newObfMethod?.let { XposedBridge.hookMethod(it, callback) }
            YLog.info("QuickEntryHook: settings provider hooks installed.")
        }.onFailure {
            YLog.error("QuickEntryHook provider hook failed: ${it.message}")
        }
    }

    private fun createProviderHook(
        simpleItemProcessor: Class<*>,
        setClickListenerMethods: List<Method>
    ): XC_MethodHook {
        val constructorInfo = findSimpleItemProcessorConstructor(simpleItemProcessor)

        return object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val result = param.result as? MutableList<Any?> ?: return
                val context = param.args.firstOrNull() as? Context ?: return
                if (containsEntry(result)) return

                runCatching {
                    val entryItem = createEntryItem(
                        context = context,
                        constructor = constructorInfo.first,
                        constructorArgCount = constructorInfo.second,
                        setClickListenerMethods = setClickListenerMethods
                    )

                    val group = createItemGroup(result, entryItem)
                    val providerClassName = param.thisObject.javaClass.name
                    val indexToInsert = if (providerClassName.contains("NewSettingConfigProvider") || providerClassName == "com.tencent.mobileqq.setting.main.b") 2 else 1
                    result.add(indexToInsert.coerceAtMost(result.size), group)
                    YLog.debug("QuickEntryHook: injected settings entry into $providerClassName")
                }.onFailure {
                    YLog.error("QuickEntryHook inject failed: ${it.message}")
                }
            }
        }
    }

    private fun findSimpleItemProcessorConstructor(simpleItemProcessor: Class<*>): Pair<Constructor<*>, Int> {
        return runCatching {
            simpleItemProcessor.getDeclaredConstructor(
                Context::class.java,
                Int::class.javaPrimitiveType,
                CharSequence::class.java,
                Int::class.javaPrimitiveType,
                String::class.java
            ) to 5
        }.getOrElse {
            simpleItemProcessor.getDeclaredConstructor(
                Context::class.java,
                Int::class.javaPrimitiveType,
                CharSequence::class.java,
                Int::class.javaPrimitiveType
            ) to 4
        }
    }

    private fun createEntryItem(
        context: Context,
        constructor: Constructor<*>,
        constructorArgCount: Int,
        setClickListenerMethods: List<Method>
    ): Any {
        val iconResId = findSettingsIconResId(context)
        val entryItem = if (constructorArgCount == 5) {
            constructor.newInstance(context, ENTRY_ID, ENTRY_TITLE, iconResId, null)
        } else {
            constructor.newInstance(context, ENTRY_ID, ENTRY_TITLE, iconResId)
        }

        val function0Class = setClickListenerMethods.first().parameterTypes[0]
        val unitInstance = function0Class.classLoader
            ?.loadClass("kotlin.Unit")
            ?.getField("INSTANCE")
            ?.get(null)

        val clickCallback = Proxy.newProxyInstance(
            function0Class.classLoader,
            arrayOf(function0Class)
        ) { _, method, _ ->
            if (method.name == "invoke") {
                YLog.info("QuickEntryHook: settings entry clicked")
                runCatching {
                    val activity = context as? Activity
                        ?: error("QuickEntryHook: context is not an Activity: ${context.javaClass.name}")
                    activity.openModuleSettings()
                }
                    .onFailure { YLog.error("QuickEntryHook click failed: ${it.message}") }
                unitInstance
            } else {
                null
            }
        }

        setClickListenerMethods.forEach { method ->
            runCatching {
                method.isAccessible = true
                method.invoke(entryItem, clickCallback)
                YLog.debug("QuickEntryHook: bound click callback via ${method.name}")
            }.onFailure {
                YLog.error("QuickEntryHook: failed to bind ${method.name}: ${it.message}")
            }
        }
        return entryItem
    }

    private fun createItemGroup(result: MutableList<Any?>, entryItem: Any): Any {
        val groupClass = result.firstOrNull()?.javaClass
            ?: error("QuickEntryHook: empty setting groups")
        val list = arrayListOf(entryItem)

        return runCatching {
            groupClass.getDeclaredConstructor(List::class.java, CharSequence::class.java, CharSequence::class.java)
                .newInstance(list, "", "")
        }.getOrElse {
            val defaultMarkerClass = "kotlin.jvm.internal.DefaultConstructorMarker".toClassOrNull()
                ?: error("QuickEntryHook: DefaultConstructorMarker not found")
            groupClass.getDeclaredConstructor(
                List::class.java,
                CharSequence::class.java,
                CharSequence::class.java,
                Int::class.javaPrimitiveType,
                defaultMarkerClass
            ).newInstance(list, "", "", 6, null)
        }
    }

    private fun containsEntry(groups: List<Any?>): Boolean {
        return groups.any { group ->
            runCatching {
                group?.javaClass?.declaredFields
                    ?.onEach { it.isAccessible = true }
                    ?.mapNotNull { it.get(group) }
                    ?.any { value -> value.toString().contains(ENTRY_TITLE, ignoreCase = true) }
                    ?: false
            }.getOrDefault(false)
        }
    }

    private fun findSettingsIconResId(context: Context): Int {
        return listOf("qui_tuning", "ic_setting_me", "qq_setting_me_item_nor")
            .asSequence()
            .map { context.resources.getIdentifier(it, "drawable", context.packageName) }
            .firstOrNull { it != 0 }
            ?: 0
    }
}
