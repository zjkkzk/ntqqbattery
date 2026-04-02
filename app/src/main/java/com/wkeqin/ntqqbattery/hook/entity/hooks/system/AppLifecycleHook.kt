package com.wkeqin.ntqqbattery.hook.entity.hooks.system

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.wkeqin.ntqqbattery.hook.entity.HookPlan
import com.wkeqin.ntqqbattery.hook.entity.HookStage
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.wkeqin.ntqqbattery.hook.entity.ProcessScope
import com.highcapable.yukihookapi.hook.log.YLog

object AppLifecycleHook {

    val plan = HookPlan(
        id = "main-process-lifecycle",
        stage = HookStage.APP_CREATE,
        processScope = ProcessScope.MAIN_ONLY
    ) { context ->
        (context as? Application)?.let { hookUniversalLifecycle(it) }
    }

    fun hookUniversalLifecycle(application: Application) {
        if (NTQQHooker.isLifecycleRegistered) return

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                if (NTQQHooker.activeActivities.getAndIncrement() == 0) {
                    NTQQHooker.syncBackgroundState(activity, false)
                }
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                if (NTQQHooker.activeActivities.decrementAndGet() == 0) {
                    NTQQHooker.syncBackgroundState(activity, true)
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
        NTQQHooker.isLifecycleRegistered = true
        YLog.debug("Universal Lifecycle callbacks registered")
    }
}
