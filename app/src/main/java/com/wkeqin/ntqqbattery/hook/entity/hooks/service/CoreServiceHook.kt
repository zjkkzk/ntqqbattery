package com.wkeqin.ntqqbattery.hook.entity.hooks.service

import android.app.Service
import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClassOrNull
import com.highcapable.yukihookapi.hook.log.YLog

object CoreServiceHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_core_service",
        titleRes = R.string.block_core_service,
        summaryRes = R.string.block_core_service,
        category = FeatureCategory.CORE,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "core-service-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(CoreServiceHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockCoreService).not()) return

        "com.tencent.mobileqq.app.CoreService".toClassOrNull()?.apply {
            method {
                name = "startCoreService"
                param(Boolean::class.javaPrimitiveType!!)
            }.hook().before {
                val shouldBlock = NTQQHooker.isBackgroundRestrictedProcess()
                YLog.info("CoreService.startCoreService called, process=$processName, main=$mainProcessName, bg=${NTQQHooker.isBackground()}, restricted=$shouldBlock")
                if (shouldBlock) {
                    YLog.info("Blocked CoreService.startCoreService in background")
                    NTQQHooker.stopCoreServices()
                    result = null
                }
            }

            method { name = "onCreate" }.hookAll().after {
                if (NTQQHooker.isBackgroundRestrictedProcess()) {
                    YLog.info("Stopping CoreService immediately after onCreate in background")
                    (instance as? Service)?.apply {
                        runCatching { stopForeground(true) }
                        stopSelf()
                    }
                }
            }

            method { name = "onStartCommand" }.hookAll().before {
                if (NTQQHooker.isBackgroundRestrictedProcess()) {
                    YLog.info("Blocked CoreService.onStartCommand in background")
                    result = Service.START_NOT_STICKY
                    (instance as? Service)?.stopSelf()
                }
            }
        }

        "com.tencent.mobileqq.app.CoreService${'$'}KernelService".toClassOrNull()?.apply {
            method { name = "onCreate" }.hookAll().before {
                if (NTQQHooker.isBackgroundRestrictedProcess()) {
                    YLog.info("Blocked KernelService.onCreate in background")
                    result = NTQQHooker.safeReturn(method)
                }
            }
            method { name = "onStartCommand" }.hookAll().before {
                if (NTQQHooker.isBackgroundRestrictedProcess()) {
                    YLog.info("Blocked KernelService.onStartCommand in background")
                    result = Service.START_NOT_STICKY
                    (instance as? Service)?.stopSelf()
                }
            }
        }
        ConfigData.setHooked(FeatureRegistry.blockCoreService, true)
    }
}
