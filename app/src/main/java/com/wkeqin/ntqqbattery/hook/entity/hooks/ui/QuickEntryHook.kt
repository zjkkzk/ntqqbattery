package com.wkeqin.ntqqbattery.hook.entity.hooks.ui

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.factory.openModuleSettings
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog

object QuickEntryHook : YukiBaseHooker() {

    val plan = HookPlan(
        id = "quick-entry-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(QuickEntryHook)
    }

    override fun onHook() {
        "com.tencent.mobileqq.activity.JumpActivity".toClassOrNull()?.method {
            name = "doOnCreate"
            paramCount = 1
        }?.hook()?.after {
            val activity = instance as? Activity ?: return@after
            val isSettingsIntent = runCatching {
                activity.intent?.getBooleanExtra("ntqqbattery_open_settings", false) == true
            }.getOrDefault(false)

            if (isSettingsIntent) {
                activity.openModuleSettings()
                activity.finish()
            }
        }

        "com.tencent.mobileqq.activity.GeneralSettingActivity".toClassOrNull()?.method {
            name = "doOnCreate"
            paramCount = 1
        }?.hook()?.after {
            val activity = instance as? Activity ?: return@after
            val contentView = activity.findViewById<ViewGroup>(android.R.id.content)
            contentView?.let { parent ->
                findFirstViewGroup(parent)?.let { container ->
                    injectSettingItem(activity, container)
                }
            }
        }
    }

    private fun findFirstViewGroup(viewGroup: ViewGroup): ViewGroup? {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup) return child
        }
        return null
    }

    private fun injectSettingItem(activity: Activity, container: ViewGroup) {
        try {
            val formSimpleItemClass = "com.tencent.mobileqq.widget.FormSimpleItem".toClassOrNull()
            formSimpleItemClass?.let { clazz ->
                val constructor = clazz.getConstructor(Context::class.java)
                val item = constructor.newInstance(activity) as View
                item.current().method {
                    name = "setLeftText"
                    param(CharSequence::class.java)
                }.invoke<Unit>("NTQQBattery 设置")
                item.setOnClickListener { activity.openModuleSettings() }
                container.addView(item, 0)
                YLog.debug("Successfully injected NTQQBattery entry.")
            }
        } catch (e: Exception) {
            YLog.error("Failed to inject setting item: ${e.message}")
        }
    }
}
