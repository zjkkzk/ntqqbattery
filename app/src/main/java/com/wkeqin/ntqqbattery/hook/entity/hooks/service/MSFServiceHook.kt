package com.wkeqin.ntqqbattery.hook.entity.hooks.service

import android.app.Service
import android.content.Intent
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClassOrNull
import com.highcapable.yukihookapi.hook.log.YLog

object MSFServiceHook : YukiBaseHooker() {

    val plan = HookPlan(
        id = "msf-service-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(MSFServiceHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.aggressiveMsfOptimization).not()) return

        "com.tencent.mobileqq.msf.core.NetConnInfoCenter".toClassOrNull()?.method {
            name = "startOrBindService"
            param(Intent::class.java)
        }?.hookAll()?.before {
            val intent = args[0] as? Intent
            if (NTQQHooker.isBackground() && intent?.component?.className?.endsWith("MsfService") == true) {
                YLog.debug("Blocked NetConnInfoCenter.startOrBindService ${intent.component?.className}")
                result = NTQQHooker.safeReturn(method)
            }
        }

        "com.tencent.mobileqq.msf.service.i".toClassOrNull()?.method {
            name = "a"
            emptyParam()
        }?.hook()?.before {
            if (NTQQHooker.isBackground()) {
                YLog.debug("Blocked MSFAliveManager job registration")
                result = NTQQHooker.safeReturn(method)
            }
        }

        listOf(
            "com.tencent.mobileqq.msf.service.MsfService",
            "com.tencent.mobileqq.msf.service.MsfCoreService"
        ).forEach { className ->
            className.toClassOrNull()?.apply {
                runCatching {
                    method { name = "onBind" }.hookAll().before {
                        if (NTQQHooker.isBackgroundRestrictedProcess()) {
                            YLog.debug("Blocked ${className.substringAfterLast('.')}.onBind in background")
                            (instance as? Service)?.stopSelf()
                            result = null
                        }
                    }
                }

                runCatching {
                    method { name = "onStartCommand" }.hookAll().before {
                        if (NTQQHooker.isBackgroundRestrictedProcess()) {
                            YLog.debug("Blocked ${className.substringAfterLast('.')}.onStartCommand in background")
                            result = Service.START_NOT_STICKY
                            (instance as? Service)?.stopSelf()
                        }
                    }
                }
            }
        }
    }
}
