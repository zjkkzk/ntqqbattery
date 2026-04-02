package com.wkeqin.ntqqbattery.hook.entity

import android.content.Context

enum class HookStage {
    EARLY_ATTACH,
    APP_CREATE
}

enum class ProcessScope {
    ALL,
    MAIN_ONLY
}

data class HookPlan(
    val id: String,
    val stage: HookStage,
    val processScope: ProcessScope = ProcessScope.ALL,
    val install: NTQQHooker.(Context?) -> Unit
)
