package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog

object QLogHook : YukiBaseHooker() {

    val plan = HookPlan(
        id = "qlog-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(QLogHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockQQBatteryMonitor).not()) return

        "com.tencent.qphone.base.util.QLog".toClassOrNull()?.apply {
            method { name = "setUIN_REPORTLOG_LEVEL"; param(Int::class.java) }.hook().before { args[0] = 1 }
            runCatching {
                method { name = "initQlog" }.hookAll().before {
                    YLog.debug("QLog: Intercepted initialization")
                    result = NTQQHooker.safeReturn(method)
                }
            }
            runCatching {
                getDeclaredField("UIN_REPORTLOG_LEVEL").apply { isAccessible = true; set(null, 1) }
                getDeclaredField("isDebug").apply { isAccessible = true; set(null, false) }
                YLog.debug("QLog: Successfully disabled via static field reflection")
            }
        }
    }
}
