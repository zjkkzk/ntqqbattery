package com.wkeqin.ntqqbattery.hook.entity.hooks.optimization

import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker

object BeaconHookSupport {

    fun replaceBeaconExecutor(instance: Any) {
        instance.javaClass.declaredFields.firstOrNull {
            java.util.concurrent.ScheduledExecutorService::class.java.isAssignableFrom(it.type)
        }?.apply {
            isAccessible = true
            (get(instance) as? java.util.concurrent.ScheduledExecutorService)?.shutdownNow()
            set(instance, NTQQHooker.createNoopScheduledExecutor())
        }
        instance.javaClass.declaredFields
            .filter { it.type == Boolean::class.javaPrimitiveType }
            .lastOrNull()
            ?.apply {
                isAccessible = true
                set(instance, true)
            }
    }
}
