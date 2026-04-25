package com.wkeqin.ntqqbattery.hook.entity

import android.content.Context
import com.highcapable.yukihookapi.hook.log.YLog
import java.util.concurrent.ConcurrentHashMap

object HookDispatcher {

    private val installedPlanIds = ConcurrentHashMap.newKeySet<String>()

    fun install(
        owner: NTQQHooker,
        stage: HookStage,
        context: Context? = null,
        isMainProcess: Boolean
    ) {
        val start = System.currentTimeMillis()
        HookPlanRegistry.plans
            .asSequence()
            .filter { it.stage == stage }
            .filter { it.processScope == ProcessScope.ALL || isMainProcess }
            .forEach { plan ->
                if (installedPlanIds.add(plan.id)) {
                    val planStart = System.currentTimeMillis()
                    runCatching {
                        plan.install.invoke(owner, context)
                    }.onFailure {
                        YLog.error("HookDispatcher: ${plan.id} failed: ${it.message}\n${it.stackTraceToString()}")
                    }
                    val planElapsed = System.currentTimeMillis() - planStart
                    if (planElapsed > 50) {
                        YLog.debug("HookDispatcher: ${plan.id} took ${planElapsed}ms")
                    }
                }
            }
        val elapsed = System.currentTimeMillis() - start
        YLog.debug("HookDispatcher: stage=$stage done in ${elapsed}ms")
    }
}
