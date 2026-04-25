package com.wkeqin.ntqqbattery.ui.activity

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.highcapable.yukihookapi.YukiHookAPI
import com.wkeqin.ntqqbattery.BuildConfig
import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData

open class AppHomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_home)
        ConfigData.init(this)

        val statusText = findViewById<TextView>(R.id.statusText)
        val executorText = findViewById<TextView>(R.id.executorText)
        val hideDesktopIconSwitch = findViewById<Switch>(R.id.hideDesktopIconSwitch)

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

        hideDesktopIconSwitch.thumbTintList = createThumbTint()
        hideDesktopIconSwitch.trackTintList = createTrackTint()
        hideDesktopIconSwitch.isChecked = ConfigData.isDesktopIconHidden()
        hideDesktopIconSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                ConfigData.setDesktopIconHidden(isChecked)
                setDesktopIconHidden(isChecked)
            }
        }
    }

    private fun setDesktopIconHidden(hidden: Boolean) {
        val launcherAlias = ComponentName(BuildConfig.APPLICATION_ID, "${BuildConfig.APPLICATION_ID}.Home")
        val newState = if (hidden) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        }
        val flags = if (hidden) 0 else PackageManager.DONT_KILL_APP
        packageManager.setComponentEnabledSetting(launcherAlias, newState, flags)
    }

    private fun createThumbTint(): ColorStateList {
        val checked = ContextCompat.getColor(this, R.color.accent)
        val unchecked = ContextCompat.getColor(this, R.color.white)
        return ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(checked, unchecked)
        )
    }

    private fun createTrackTint(): ColorStateList {
        val checked = ContextCompat.getColor(this, R.color.accent_dark)
        val unchecked = ContextCompat.getColor(this, R.color.on_surface_variant)
        return ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(checked, unchecked)
        )
    }
}
