package com.wkeqin.ntqqbattery.hook.entity.hooks.system

import android.app.PendingIntent
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method

object PendingIntentTrackerHook : YukiBaseHooker() {

    val plan = HookPlan(
        id = "pending-intent-tracker-early-attach",
        stage = HookStage.EARLY_ATTACH
    ) {
        loadHooker(PendingIntentTrackerHook)
    }

    override fun onHook() {
        PendingIntent::class.java.method {
            name { it in setOf("getBroadcast", "getService", "getForegroundService", "getActivity") }
        }.hookAll().after {
            val intent = args.firstOrNull { it is android.content.Intent } as? android.content.Intent
            val action = intent?.action ?: return@after
            val pi = result as? PendingIntent ?: return@after
            NTQQHooker.pendingIntentActionMap.put(pi.hashCode(), action)
        }
    }
}
