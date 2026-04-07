package com.wkeqin.ntqqbattery.hook.factory

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.highcapable.yukihookapi.hook.log.YLog
import com.wkeqin.ntqqbattery.ui.activity.QQSettingsActivity

fun Activity.openModuleSettings() {
    val intent = Intent(this, QQSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching {
        YLog.info("QuickEntryHook: opening settings with ${intent.component} from ${javaClass.name}")
        startActivity(intent)
        YLog.info("QuickEntryHook: startActivity dispatched")
    }.onFailure {
        Toast.makeText(this, "无法打开 NTQQBattery 设置页", Toast.LENGTH_SHORT).show()
        YLog.error("QuickEntryHook: open settings failed: ${it.stackTraceToString()}")
    }
}
