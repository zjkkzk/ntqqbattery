package com.wkeqin.ntqqbattery.hook.entity

import com.wkeqin.ntqqbattery.hook.entity.hooks.feature.MiniAppPreloadHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.feature.MiniGamePreloadHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.feature.QQBatteryMonitorHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.feature.SplashPreloadHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.feature.ZPlanRenderHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.service.CoreServiceHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.service.HeavyBackgroundServiceHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.service.MSFServiceHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.optimization.BeaconReportHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.optimization.GPUResourceHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.optimization.MSFNetworkSuppressionHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.optimization.MSFOptimizationHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.optimization.PandoraExHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.optimization.PerfMonitorHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.optimization.PowerSaveModeHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.optimization.QLogHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.optimization.StatisticCollectorHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.optimization.TVKReportHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.optimization.ThemeVideoOptimizationHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.optimization.XWebDownloaderHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.system.AppLifecycleHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.system.BackgroundVibrateHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.system.BeaconEarlyHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.system.FrameworkSchedulerHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.system.PendingIntentTrackerHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.system.SystemWakeLockHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.ui.GifOptimizationHook
import com.wkeqin.ntqqbattery.hook.entity.hooks.ui.QuickEntryHook

object HookPlanRegistry {

    val plans = listOf(
        BeaconEarlyHook.plan,
        FrameworkSchedulerHook.plan,
        SystemWakeLockHook.plan,
        BackgroundVibrateHook.plan,
        PendingIntentTrackerHook.plan,
        CoreServiceHook.plan,
        MSFServiceHook.plan,
        HeavyBackgroundServiceHook.plan,
        QQBatteryMonitorHook.plan,
        ZPlanRenderHook.plan,
        MiniAppPreloadHook.plan,
        MiniGamePreloadHook.plan,
        SplashPreloadHook.plan,
        QuickEntryHook.plan,
        GifOptimizationHook.plan,
        ThemeVideoOptimizationHook.plan,
        GPUResourceHook.plan,
        MSFOptimizationHook.plan,
        PandoraExHook.plan,
        PerfMonitorHook.plan,
        MSFNetworkSuppressionHook.plan,
        QLogHook.plan,
        PowerSaveModeHook.plan,
        StatisticCollectorHook.plan,
        XWebDownloaderHook.plan,
        BeaconReportHook.plan,
        TVKReportHook.plan,
        AppLifecycleHook.plan
    )
}
