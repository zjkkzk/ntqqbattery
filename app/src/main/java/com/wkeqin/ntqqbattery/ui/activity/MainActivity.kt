package com.wkeqin.ntqqbattery.ui.activity

import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.wkeqin.ntqqbattery.R
import com.wkeqin.ntqqbattery.data.ConfigData
import com.wkeqin.ntqqbattery.hook.entity.FeatureCategory
import com.wkeqin.ntqqbattery.hook.entity.FeatureDefinition
import com.wkeqin.ntqqbattery.hook.entity.FeatureRegistry
import com.google.android.material.materialswitch.MaterialSwitch
import com.highcapable.yukihookapi.YukiHookAPI

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

    private fun createFeatureSwitch(feature: FeatureDefinition, addTopMargin: Boolean): MaterialSwitch {
        return MaterialSwitch(this).apply {
            text = getString(feature.titleRes)
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
