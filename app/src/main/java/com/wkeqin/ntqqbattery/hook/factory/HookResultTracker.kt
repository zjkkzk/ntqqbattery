package com.wkeqin.ntqqbattery.hook.factory

import com.highcapable.yukihookapi.hook.log.YLog
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 追踪每个 feature 的 hook 成功/失败计数，用于自动降级判定。
 *
 * 使用方式：
 * ```
 * val tracker = HookResultTracker("MSFWakeLock")
 * tracker.tryHook("a()") { method { name = "a"; paramCount = 1 }.hookAll().before { ... } }
 * tracker.tryHook("b()") { method { name = "b"; emptyParam() }.hook().before { ... } }
 * tracker.report()  // 汇总日志 + 返回是否降级
 * ```
 */
class HookResultTracker(private val featureName: String) {

    private val successCount = AtomicInteger(0)
    private val failureCount = AtomicInteger(0)
    private val failureDetails = ConcurrentHashMap.newKeySet<String>()

    /**
     * 尝试执行一个 hook 操作。成功则计数+1，失败则记录详情并继续。
     * @param methodLabel 方法的可读标签，用于日志（如 "a(1 param)"）
     * @param block 实际的 hook 代码
     */
    fun tryHook(methodLabel: String, block: () -> Unit) {
        runCatching {
            block()
            successCount.incrementAndGet()
        }.onFailure { e ->
            failureCount.incrementAndGet()
            failureDetails.add("$methodLabel: ${e.message?.take(80)}")
            YLog.warn("$featureName: hook $methodLabel failed, skipped")
        }
    }

    /**
     * 汇总报告，返回是否应该降级（所有 hook 都失败时降级）。
     * @return true 表示应降级（所有方法都 hook 失败）
     */
    fun report(): Boolean {
        val s = successCount.get()
        val f = failureCount.get()
        val total = s + f
        if (total == 0) return false

        val degraded = s == 0 && f > 0
        if (degraded) {
            YLog.warn("$featureName: all $f hook(s) failed, feature degraded")
            failureDetails.forEach { YLog.warn("  - $it") }
        } else if (f > 0) {
            YLog.info("$featureName: $s/$total hooks active, $f skipped")
        } else {
            YLog.info("$featureName: all $s hook(s) active")
        }
        return degraded
    }

    val hasAnySuccess get() = successCount.get() > 0
    val hasAnyFailure get() = failureCount.get() > 0
}
