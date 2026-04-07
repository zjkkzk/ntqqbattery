package com.wkeqin.ntqqbattery.ui.activity

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.log.YLog
import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry

class QQSettingsActivity : BaseHostActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NTQQBattery)
        super.onCreate(savedInstanceState)
        YLog.info("QQSettingsActivity: onCreate")
        runCatching {
            setContentView(R.layout.activity_main)
            applyWindowBackground()

            ConfigData.init(this)

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

            renderFeatureSettings(findViewById(R.id.settingsContainer))
            YLog.info("QQSettingsActivity: renderFeatureSettings done")
        }.onFailure {
            YLog.error("QQSettingsActivity: onCreate failed: ${it.stackTraceToString()}")
            Toast.makeText(this, "NTQQBattery 设置页初始化失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyWindowBackground() {
        val backgroundColor = resolveColor(android.R.attr.windowBackground)
        if (backgroundColor != null) {
            window.setBackgroundDrawable(backgroundColor.toDrawable())
        }
    }

    private fun resolveColor(@AttrRes attr: Int): Int? {
        val typedValue = TypedValue()
        if (!theme.resolveAttribute(attr, typedValue, true)) return null
        return when {
            typedValue.resourceId != 0 -> ContextCompat.getColor(this, typedValue.resourceId)
            typedValue.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT -> typedValue.data
            else -> null
        }
    }

    private fun renderFeatureSettings(container: LinearLayout) {
        container.removeAllViews()
        FeatureCategory.values().forEachIndexed { index, category ->
            val features = FeatureRegistry.all.filter { it.category == category }
            if (features.isEmpty()) return@forEachIndexed

            container.addView(createCategoryTitle(category, index > 0))
            container.addView(createFeatureCard(features))
        }
        container.addView(createFooterNote())
    }

    private fun createCategoryTitle(category: FeatureCategory, addTopMargin: Boolean): TextView {
        return TextView(this).apply {
            text = getString(category.titleRes)
            setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                if (addTopMargin) topMargin = dp(24)
            }
        }
    }

    private fun createFeatureCard(features: List<FeatureDefinition>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }

            features.forEachIndexed { index, feature ->
                addView(createFeatureSwitch(feature, addTopMargin = index > 0))
            }
        }
    }

    private fun createFeatureSwitch(feature: FeatureDefinition, addTopMargin: Boolean): SwitchCompat {
        return SwitchCompat(this).apply {
            text = getString(feature.titleRes)
            showText = false
            thumbTintList = createThumbTint()
            trackTintList = createTrackTint()
            switchMinWidth = dp(52)
            setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            isChecked = ConfigData.isEnabled(feature)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                if (addTopMargin) topMargin = dp(10)
            }
            setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) {
                    ConfigData.setEnabled(feature, isChecked)
                }
            }
        }
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

    private fun createFooterNote(): TextView {
        return TextView(this).apply {
            text = getString(R.string.first_build_note)
            setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(24)
            }
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
