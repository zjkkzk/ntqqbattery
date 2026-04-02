package com.wkeqin.ntqqbattery.hook.entity

import androidx.annotation.StringRes

data class FeatureDefinition(
    val key: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val summaryRes: Int? = null,
    @param:StringRes val noteRes: Int? = null,
    val category: FeatureCategory,
    val defaultEnabled: Boolean
)
