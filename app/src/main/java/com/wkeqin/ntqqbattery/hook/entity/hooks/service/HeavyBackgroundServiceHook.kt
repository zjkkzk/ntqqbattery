package com.wkeqin.ntqqbattery.hook.entity.hooks.service

import android.app.Activity
import android.app.Service
import android.content.Intent
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

object HeavyBackgroundServiceHook : YukiBaseHooker() {

    val feature = FeatureDefinition(
        key = "block_heavy_background_service",
        titleRes = R.string.block_heavy_background_service,
        summaryRes = R.string.block_heavy_background_service,
        category = FeatureCategory.OPTIMIZATION,
        defaultEnabled = true
    )

    val plan = HookPlan(
        id = "heavy-background-service-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(HeavyBackgroundServiceHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockHeavyBackgroundService).not()) return

        "com.tencent.mobileqq.gamecenter.api.impl.YunGameManagerServiceImpl".toClassOrNull()?.method {
            name = "startPluginServiceInternal"
        }?.hook()?.before {
            if (NTQQHooker.isBackgroundRestrictedProcess()) {
                YLog.debug("Blocked YunGameManagerServiceImpl.startPluginServiceInternal in background")
                result = NTQQHooker.safeReturn(method)
            }
        }

        listOf("android.app.ContextImpl", "android.content.ContextWrapper").forEach { ctxClass ->
            ctxClass.toClassOrNull()?.apply {
                method {
                    name { it == "startService" || it == "startForegroundService" }
                    param(Intent::class.java)
                }.hookAll().before {
                    val intent = args[0] as? Intent
                    if (NTQQHooker.shouldBlockServiceIntent(intent)) {
                        YLog.debug("Blocked service launch: ${intent?.component?.className} in $ctxClass")
                        result = null
                    }
                }

                method {
                    name = "bindService"
                }.hookAll().before {
                    val intent = args.firstOrNull() as? Intent
                    if (NTQQHooker.shouldBlockServiceIntent(intent)) {
                        YLog.debug("Blocked service bind: ${intent?.component?.className} in $ctxClass")
                        result = false
                    }
                }
            }
        }

        listOf(
            "com.tencent.mobileqq.music.QQPlayerService",
            "com.tencent.mobileqq.winkpublish.service.WinkPublishService",
            "com.tencent.gamecenter.wadl.api.impl.WadlProxyService",
            "com.tencent.gamecenter.wadl.biz.service.WadlJsBridgeService",
            "com.tencent.gamecenter.wadl.notification.WadlNotificationService",
            "com.tencent.mobileqq.gamecenter.yungame.YunGameService"
        ).forEach { className ->
            className.toClassOrNull()?.apply {
                method { name = "onCreate" }.hook().after {
                    if (NTQQHooker.isBackgroundRestrictedProcess()) {
                        YLog.debug("Prevented ${className.substringAfterLast('.')}.onCreate in background, executing stopSelf()")
                        (instance as? Service)?.stopSelf()
                    }
                }

                if (className.endsWith("QQPlayerService")) {
                    method { name = "a0" }.hook().before {
                        if (NTQQHooker.isBackgroundRestrictedProcess()) {
                            YLog.debug("Blocked QQPlayerService.a0() (MediaPlayer init) in background")
                            result = NTQQHooker.safeReturn(method)
                        }
                    }

                    method { name = "b0" }.hook().before {
                        if (NTQQHooker.isBackgroundRestrictedProcess()) {
                            YLog.debug("Blocked QQPlayerService.b0() (HandlerThread init) in background")
                            result = NTQQHooker.safeReturn(method)
                        }
                    }
                }

                method { name = "onBind" }.hookAll().before {
                    if (NTQQHooker.isBackgroundRestrictedProcess()) {
                        YLog.debug("Blocked ${className.substringAfterLast('.')}.onBind")
                        result = null
                    }
                }

                method { name = "onStartCommand" }.hookAll().before {
                    val intent = args.firstOrNull() as? Intent
                    val action = intent?.getIntExtra("musicplayer.action", 0) ?: 0

                    if (NTQQHooker.isBackgroundRestrictedProcess()) {
                        if (action == 3 || action == 6) return@before

                        YLog.debug("Blocked ${className.substringAfterLast('.')}.onStartCommand with action=$action in background")
                        result = Service.START_NOT_STICKY
                        (instance as? Service)?.stopSelf()
                    }
                }
            }
        }

        "com.tencent.gamecenter.wadl.base.WadlPluginProxyActivity".toClassOrNull()?.apply {
            method { name = "onCreate" }.hookAll().before {
                if (NTQQHooker.isBackgroundRestrictedProcess()) {
                    YLog.debug("Blocked WadlPluginProxyActivity.onCreate in background")
                    (instance as? Activity)?.finish()
                    result = NTQQHooker.safeReturn(method)
                }
            }
        }
    }
}
