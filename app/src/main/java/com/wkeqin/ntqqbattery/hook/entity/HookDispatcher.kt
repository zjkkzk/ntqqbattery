package com.wkeqin.ntqqbattery.hook.entity

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

object HookDispatcher {

    private val installedPlanIds = ConcurrentHashMap.newKeySet<String>()

    fun install(
        owner: NTQQHooker,
        stage: HookStage,
        context: Context? = null,
        isMainProcess: Boolean
    ) {
        HookPlanRegistry.plans
            .asSequence()
            .filter { it.stage == stage }
            .filter { it.processScope == ProcessScope.ALL || isMainProcess }
            .forEach { plan ->
                if (installedPlanIds.add(plan.id)) {
                    plan.install.invoke(owner, context)
                }
            }
    }
}
