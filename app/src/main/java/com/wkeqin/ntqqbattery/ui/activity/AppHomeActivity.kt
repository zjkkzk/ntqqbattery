package com.wkeqin.ntqqbattery.ui.activity

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.log.YLog
import com.wkeqin.ntqqbattery.R

open class AppHomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        YLog.info("AppHomeActivity: onCreate")
        setContentView(R.layout.activity_app_home)

        val statusText = findViewById<TextView>(R.id.statusText)
        val executorText = findViewById<TextView>(R.id.executorText)

        statusText.text = if (YukiHookAPI.Status.isModuleActive) {
            getString(R.string.module_active)
        } else {
            getString(R.string.module_inactive)
        }
        executorText.text = if (YukiHookAPI.Status.Executor.apiLevel > 0) {
            "Activated by ${YukiHookAPI.Status.Executor.name} API ${YukiHookAPI.Status.Executor.apiLevel}"
        } else {
            "Activated by ${YukiHookAPI.Status.Executor.name}"
        }
    }
}
