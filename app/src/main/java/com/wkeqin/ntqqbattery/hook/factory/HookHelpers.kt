package com.wkeqin.ntqqbattery.hook.factory

import android.app.Activity
import android.content.Intent
import com.wkeqin.ntqqbattery.ui.activity.MainActivity

fun Activity.openModuleSettings() {
    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
