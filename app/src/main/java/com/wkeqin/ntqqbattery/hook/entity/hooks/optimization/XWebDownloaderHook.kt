package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.wkeqin.ntqqbattery.hook.entity.features.PerfFeatures
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog

object XWebDownloaderHook : YukiBaseHooker() {

    val plan = HookPlan(
        id = "xweb-downloader-app-create",
        stage = HookStage.APP_CREATE
    ) {
        loadHooker(XWebDownloaderHook)
    }

    override fun onHook() {
        if (ConfigData.isEnabled(FeatureRegistry.blockHeavyBackgroundService).not()) return

        PerfFeatures.XWebHttpDownloadTaskClass?.apply {
            method {
                name { it == "a" || it == "doInBackground" }
                paramCount = 1
                returnType = java.lang.Integer::class.java
            }.hook().before {
                if (NTQQHooker.isBackground()) {
                    YLog.debug("XWeb: Blocked background download task (${instance?.javaClass?.simpleName})")
                    result = 0
                }
            }
        }
    }
}
