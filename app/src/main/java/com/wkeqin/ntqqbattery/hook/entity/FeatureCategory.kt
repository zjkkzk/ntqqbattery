package com.wkeqin.ntqqbattery.hook.entity

import androidx.annotation.StringRes
import com.wkeqin.ntqqbattery.R

enum class FeatureCategory(@param:StringRes val titleRes: Int) {
    CORE(R.string.category_core),
    COMPONENT(R.string.category_component),
    OPTIMIZATION(R.string.category_optimization)
}
