package com.wkeqin.ntqqbattery.hook.entity

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.wkeqin.ntqqbattery.const.PackageName
import com.wkeqin.ntqqbattery.data.ConfigData
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import java.lang.reflect.Method
import java.util.Collections
import java.util.concurrent.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * NTQQBattery Hooker 主入口
 * 负责状态管理、跨进程同步以及子 Hooker 的分发加载
 */
object NTQQHooker : YukiBaseHooker() {
    private const val ACTION_BG_SYNC = "com.wkeqin.ntqqbattery.ACTION_BG_SYNC"
    private const val TOMBSTONE_DELAY_MS = 30_000L

    internal val activeActivities = AtomicInteger(0)
    @Volatile internal var isAppInBackground = false
    @Volatile private var hasSyncedBackgroundState = false

    /** 前后台状态变更回调，由各 Hook 按需注册 */
    private val backgroundStateCallbacks = CopyOnWriteArrayList<(Boolean) -> Unit>()

    /**
     * 注册前后台状态变更回调。
     * 回调在状态变更时被调用，参数 true = 后台，false = 前台。
     */
    fun addBackgroundStateCallback(callback: (Boolean) -> Unit) {
        backgroundStateCallbacks.add(callback)
    }

    private fun notifyBackgroundStateCallbacks(isBg: Boolean) {
        backgroundStateCallbacks.forEach { callback ->
            runCatching { callback(isBg) }.onFailure {
                YLog.error("BackgroundStateCallback error: ${it.message}")
            }
        }
    }

    // 使用 LRU 缓存存储 PendingIntent Action
    internal val pendingIntentActionMap = android.util.LruCache<Int, String>(500)
    
    internal var isEarlyHooked = false
    internal var isLifecycleRegistered = false
    @Volatile private var isReceiverRegistered = false
    private val tombstoneExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "ntqqbattery-tombstone").apply { isDaemon = true }
    }
    @Volatile private var tombstoneFuture: ScheduledFuture<*>? = null

    /**
     * 安全返回：防止拦截时返回类型不匹配导致的崩溃
     */
    fun safeReturn(method: Method): Any? {
        return when (method.returnType) {
            Boolean::class.javaPrimitiveType -> false
            Int::class.javaPrimitiveType -> 0
            Long::class.javaPrimitiveType -> 0L
            Float::class.javaPrimitiveType -> 0f
            Double::class.javaPrimitiveType -> 0.0
            Short::class.javaPrimitiveType -> 0.toShort()
            Byte::class.javaPrimitiveType -> 0.toByte()
            Char::class.javaPrimitiveType -> '\u0000'
            String::class.java -> "" 
            java.util.List::class.java -> emptyList<Any>() 
            else -> null 
        }
    }

    fun isBackground(): Boolean {
        // 子进程刚拉起时，主进程状态同步可能尚未到达。
        // 这里保守按“非后台”处理，避免 MSF 等关键进程在启动期被误杀。
        if (processName != mainProcessName && !hasSyncedBackgroundState) return false
        return isAppInBackground
    }

    fun isBackgroundRestrictedProcess(): Boolean {
        return isBackground() || processName != mainProcessName
    }

    internal fun shouldBlockServiceIntent(intent: Intent?): Boolean {
        val component = intent?.component ?: return false
        if (component.packageName != PackageName.QQ) return false
        if (!isBackgroundRestrictedProcess()) return false
        
        val className = component.className
        if (className.endsWith("QQPlayerService")) {
            val action = intent.getIntExtra("musicplayer.action", 0)
            if (action == 3 || action == 6) return false
        }
        
        return className.endsWith("QQPlayerService") ||
            className.endsWith("WinkPublishService") ||
            className.endsWith("ColorNoteSmallScreenService") ||
            className.endsWith("WadlProxyService") ||
            className.endsWith("WadlJsBridgeService") ||
            className.endsWith("WadlNotificationService") ||
            className.endsWith("YunGameService") ||
            className.endsWith("Ilink2Service")
    }

    internal fun shouldBlockAlarm(operation: PendingIntent?): Boolean {
        if (operation == null) return false
        val creatorPackage = operation.creatorPackage.orEmpty()
        val sender = operation.intentSender?.toString().orEmpty()
        
        val action = pendingIntentActionMap.get(operation.hashCode()) ?: run {
            runCatching {
                val intent = operation.javaClass.getDeclaredMethod("getIntent").invoke(operation) as? Intent
                intent?.action
            }.getOrNull()
        }.orEmpty()
        
        val isMsfAlarm = sender.contains(":MSF_") || sender.contains("MSFAlive", ignoreCase = true)
        val isKeepAliveAlarm = action == "CHECK_MUSIC_ACTIVE" || action.contains("monitorAudioData")

        return creatorPackage == PackageName.QQ && (isMsfAlarm || isKeepAliveAlarm)
    }

    private fun registerStateReceiver(context: Context) {
        if (isReceiverRegistered) return 
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == ACTION_BG_SYNC) {
                        if (intent.getStringExtra("sender_pkg") != context.packageName) return
                        val isBg = intent.getBooleanExtra("is_bg", true)
                        hasSyncedBackgroundState = true
                        isAppInBackground = isBg
                    YLog.info("Received background sync in $processName, isBg=$isBg")
                    
                    // 让其他所有子进程（如 :MSF）也独立休眠各自的 Beacon
                    if (processName != mainProcessName) {
                        suspendBeaconTasks(isBg)
                    }
                }
            }
        }
        val filter = IntentFilter(ACTION_BG_SYNC)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        isReceiverRegistered = true
    }

    internal fun syncBackgroundState(context: Context, isBg: Boolean) {
        hasSyncedBackgroundState = true
        isAppInBackground = isBg
        YLog.info("Sync background state in $processName -> isBg=$isBg, active=${activeActivities.get()}")
        notifyBackgroundStateCallbacks(isBg)
        if (isBg) {
            stopCoreServices()
            stopMsfServices(context)
            stopGameServices(context)
            stopQQPlayerService(context)
            stopAuxiliaryServices(context)
            releaseThemeVideoController()
            suspendBeaconTasks(true)
            scheduleTombstone(context)
        } else {
            cancelTombstone()
            suspendBeaconTasks(false)
        }
        context.sendBroadcast(Intent(ACTION_BG_SYNC).apply {
            putExtra("is_bg", isBg)
            putExtra("sender_pkg", context.packageName)
            setPackage(context.packageName)
        })
    }

    private fun scheduleTombstone(context: Context) {
        if (processName != mainProcessName) return
        if (!ConfigData.isEnabled(FeatureRegistry.enableTombstoneMode)) return
        cancelTombstone()
        val appContext = context.applicationContext
        tombstoneFuture = tombstoneExecutor.schedule({
            if (!isAppInBackground) return@schedule
            runCatching {
                enterTombstoneMode(appContext)
            }.onFailure {
                YLog.error("Failed to enter tombstone mode: ${it.message}")
            }
        }, TOMBSTONE_DELAY_MS, TimeUnit.MILLISECONDS)
        YLog.info("Scheduled tombstone mode in ${TOMBSTONE_DELAY_MS}ms")
    }

    private fun cancelTombstone() {
        tombstoneFuture?.cancel(false)
        tombstoneFuture = null
    }

    private fun enterTombstoneMode(context: Context) {
        if (!ConfigData.isEnabled(FeatureRegistry.enableTombstoneMode)) return
        if (!isAppInBackground) return

        YLog.info("Entering tombstone mode in $processName")
        stopCoreServices()
        stopMsfServices(context)
        stopGameServices(context)
        stopQQPlayerService(context)
        stopAuxiliaryServices(context)
        releaseThemeVideoController()
        suspendBeaconTasks(true)
    }

    private fun suspendBeaconTasks(suspend: Boolean) {
        if (!ConfigData.isEnabled(FeatureRegistry.blockBeacon)) return
        if (!suspend) return // 线程池已被替换为 noop，无需恢复
        runCatching {
            val clazz = "com.tencent.beacon.a.b.a".toClassOrNull(appClassLoader, false) ?: return
            val instance = clazz.getDeclaredMethod("a").invoke(null) ?: return
            // 仅设置关闭标志位，调度器构造时线程池已被替换
            clazz.getDeclaredMethod("a", Boolean::class.javaPrimitiveType).apply { isAccessible = true }.invoke(instance, true)
        }
    }

    /**
     * 创建一个不执行任何任务的 ScheduledExecutorService 代理
     * 用于替换 Beacon 调度器内部线程池，从根源阻断 beacon-thread 的产生
     */
    fun createNoopScheduledExecutor(): ScheduledExecutorService {
        return object : AbstractExecutorService(), ScheduledExecutorService {
            private val emptyFuture = CompletableFuture.completedFuture(null)

            override fun execute(command: Runnable?) { /* noop */ }
            override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> = NoopScheduledFuture
            override fun <V : Any?> schedule(callable: Callable<V>, delay: Long, unit: TimeUnit): ScheduledFuture<V> = NoopScheduledFuture as ScheduledFuture<V>
            override fun scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit): ScheduledFuture<*> = NoopScheduledFuture
            override fun scheduleWithFixedDelay(command: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): ScheduledFuture<*> = NoopScheduledFuture
            override fun shutdown() {}
            override fun shutdownNow(): MutableList<Runnable> = mutableListOf()
            override fun isShutdown(): Boolean = true
            override fun isTerminated(): Boolean = true
            override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true
        }
    }

    /** 一个已完成的、不可取消的 ScheduledFuture 空实现 */
    private object NoopScheduledFuture : ScheduledFuture<Any?> {
        override fun getDelay(unit: TimeUnit): Long = 0
        override fun compareTo(other: Delayed): Int = 0
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = true
        override fun isCancelled(): Boolean = true
        override fun isDone(): Boolean = true
        override fun get(): Any? = null
        override fun get(timeout: Long, unit: TimeUnit): Any? = null
    }

    private fun stopQQPlayerService(context: Context) {
        if (!ConfigData.isEnabled(FeatureRegistry.blockHeavyBackgroundService)) return
        runCatching {
            val clazz = "com.tencent.mobileqq.music.QQPlayerService".toClassOrNull(appClassLoader, false) ?: return
            clazz.getDeclaredMethod("L0", Context::class.java).apply {
                isAccessible = true
            }.invoke(null, context.applicationContext)
            YLog.debug("Requested QQPlayerService stop on background transition")
        }
    }

    private fun stopGameServices(context: Context) {
        if (!ConfigData.isEnabled(FeatureRegistry.blockHeavyBackgroundService)) return

        listOf(
            "com.tencent.gamecenter.wadl.api.impl.WadlProxyService",
            "com.tencent.gamecenter.wadl.biz.service.WadlJsBridgeService",
            "com.tencent.gamecenter.wadl.notification.WadlNotificationService",
            "com.tencent.mobileqq.gamecenter.yungame.YunGameService"
        ).forEach { className ->
            runCatching {
                val clazz = className.toClassOrNull(appClassLoader, false) ?: return@runCatching
                val stopped = context.stopService(Intent(context, clazz))
                YLog.debug("Requested stop for ${className.substringAfterLast('.')} on background transition, stopped=$stopped")
            }.onFailure {
                YLog.error("Failed to request stop for ${className.substringAfterLast('.')}: ${it.message}")
            }
        }
    }

    private fun stopAuxiliaryServices(context: Context) {
        if (!ConfigData.isEnabled(FeatureRegistry.blockHeavyBackgroundService)) return

        listOf(
            "com.tencent.mobileqq.winkpublish.service.WinkPublishService",
            "com.tencent.mobileqq.colornote.smallscreen.ColorNoteSmallScreenService",
            "com.tencent.luggage.login.ilink2service.Ilink2Service"
        ).forEach { className ->
            runCatching {
                val clazz = className.toClassOrNull(appClassLoader, false) ?: return@runCatching
                val stopped = context.stopService(Intent(context, clazz))
                YLog.debug("Requested stop for ${className.substringAfterLast('.')} on background transition, stopped=$stopped")
            }.onFailure {
                YLog.error("Failed to request stop for ${className.substringAfterLast('.')}: ${it.message}")
            }
        }
    }

    private fun stopMsfServices(context: Context) {
        if (!ConfigData.isEnabled(FeatureRegistry.aggressiveMsfOptimization)) return
        // MsfService/MsfCoreService 负责消息收发，不在后台切换时主动 stop。
    }

    internal fun stopCoreServices() {
        if (!ConfigData.isEnabled(FeatureRegistry.blockCoreService)) return
        runCatching {
            val clazz = "com.tencent.mobileqq.app.CoreService".toClassOrNull(appClassLoader, false) ?: return
            val coreInstance = clazz.getDeclaredField("sCore").apply {
                isAccessible = true
            }.get(null)
            YLog.info("stopCoreServices: sCore=${coreInstance?.javaClass?.name ?: "null"}")
            if (coreInstance is android.app.Service) {
                runCatching { coreInstance.stopForeground(true) }
                coreInstance.stopSelf()
                YLog.info("Requested live CoreService instance stopSelf on background transition")
            }
        }.onFailure {
            YLog.error("stopCoreServices: failed to access live CoreService instance: ${it.message}")
        }
        runCatching {
            val clazz = "com.tencent.mobileqq.app.CoreService".toClassOrNull(appClassLoader, false) ?: return
            clazz.getDeclaredMethod("stopCoreService").apply {
                isAccessible = true
            }.invoke(null)
            YLog.info("Requested CoreService stop on background transition")
        }.onFailure {
            YLog.error("stopCoreServices: stopCoreService invoke failed: ${it.message}")
        }
        runCatching {
            val clazz = "com.tencent.mobileqq.app.CoreService".toClassOrNull(appClassLoader, false) ?: return
            clazz.getDeclaredMethod("stopTempService").apply {
                isAccessible = true
            }.invoke(null)
            YLog.info("Requested CoreService.KernelService stop on background transition")
        }.onFailure {
            YLog.error("stopCoreServices: stopTempService invoke failed: ${it.message}")
        }
    }

    internal fun releaseThemeVideoController() {
        if (!ConfigData.isEnabled(FeatureRegistry.blockThemeVideo)) return
        runCatching {
            val controllerClass = com.wkeqin.ntqqbattery.hook.entity.features.UIFeatures.ThemeVideoControllerClass ?: return
            val instance = controllerClass.getDeclaredMethod("getInstance").apply {
                isAccessible = true
            }.invoke(null)
            instance?.javaClass?.getDeclaredMethod("release")?.apply {
                isAccessible = true
            }?.invoke(instance)
            YLog.debug("Released ThemeVideoController in background")
        }
    }

    internal fun hookThemeVideoControllerGuards(clazz: Class<*>) {
        runCatching {
            clazz.method {
                name { it in setOf("init", "playMainAnimation", "playDrawerAnimation", "resume", "onProcessForeground") }
            }.hookAll().before {
                if (isBackgroundRestrictedProcess()) {
                    YLog.debug("Blocked ${clazz.simpleName}.${method.name} in background")
                    result = safeReturn(method)
                }
            }
        }

        runCatching {
            clazz.method {
                name = "onProcessBackground"
                emptyParam()
            }.hook().before {
                if (isBackgroundRestrictedProcess()) {
                    runCatching {
                        instance?.javaClass?.getDeclaredMethod("release")?.apply { isAccessible = true }?.invoke(instance)
                    }
                    result = safeReturn(method)
                }
            }
        }
    }

    override fun onHook() {
        // 早期 Hook 注入点 (attachBaseContext)
        "android.content.ContextWrapper".toClass().method { 
            name = "attachBaseContext" 
            param(ContextClass) 
        }.hookAll().after {
            val context = args[0] as? Context
            if (context != null && !isEarlyHooked) {
                isEarlyHooked = true
                runCatching {
                    NTQQFeatures.init(context.classLoader)
                    // FeatureLocator.warmup() 不在 attachBaseContext 调用 —
                    // DexClassFinder 需要 BaseApplicationImpl 的 SharedPreferences，
                    // 而此时 Application 尚未创建。warmup 已在 onCreate 中调用。
                }
                HookDispatcher.install(
                    owner = this@NTQQHooker,
                    stage = HookStage.EARLY_ATTACH,
                    context = context,
                    isMainProcess = processName == mainProcessName
                )
            }
        }

        onAppLifecycle {
            onCreate {
                NTQQFeatures.init(appClassLoader!!)
                YLog.debug("NTQQBattery hook starting in $packageName ($processName)")
                ConfigData.init(this) 
                FeatureLocator.warmup(this, appClassLoader!!)
                registerStateReceiver(this)

                HookDispatcher.install(
                    owner = this@NTQQHooker,
                    stage = HookStage.APP_CREATE,
                    context = this,
                    isMainProcess = processName == mainProcessName
                )
            }
        }
    }
}
