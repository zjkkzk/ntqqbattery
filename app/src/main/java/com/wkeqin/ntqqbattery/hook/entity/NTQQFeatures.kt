package com.wkeqin.ntqqbattery.hook.entity

import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * NTQQ 混淆特征定位工具类
 */
object NTQQFeatures {

    @Volatile
    var classLoader: ClassLoader? = null
        private set

    fun init(classLoader: ClassLoader) {
        this.classLoader = classLoader
        YLog.info("NTQQFeatures initialized")
    }

    /**
     * 安全查找类，支持硬编码路径和特征搜索回退
     */
    fun findClassSafe(cacheName: String, primaryName: String, packageName: String, constant: String): Class<*>? {
        val loader = classLoader ?: return null
        
        // 1. 优先尝试硬编码名称
        primaryName.toClassOrNull(loader)?.let { return it }

        // 2. 备选：使用特征搜索 (仅在 hardcoded 失败时触发)
        YLog.warn("NTQQFeatures: Hardcoded class $primaryName not found, falling back to feature search...")
        return loader.searchClass(name = "NTQQ_Features_$cacheName") {
            fullName { it.startsWith(packageName) }
        }.all().firstOrNull { clazz ->
            runCatching {
                clazz.declaredFields.any { field ->
                    // 仅当字段为 static 时才尝试读取，允许匹配非 final 的特征字符串
                    val isStatic = Modifier.isStatic(field.modifiers)
                    
                    if (isStatic && field.type == String::class.java) {
                        field.isAccessible = true
                        (field.get(null) as? String) == constant 
                    } else {
                        false
                    }
                }
            }.getOrDefault(false)
        }
    }

    /**
     * 寻找配置开关方法 (支持明确名称查找)
     */
    fun findConfigToggleMethods(clazz: Class<*>, vararg names: String): List<Method> {
        val result = mutableListOf<Method>()
        val allMethods = clazz.declaredMethods

        for (name in names) {
            allMethods.find { it.name == name }?.let { result.add(it) }
        }

        if (result.isEmpty()) {
            YLog.debug("NTQQFeatures: No specific toggle methods found in ${clazz.name}.")
        } else {
            YLog.debug("NTQQFeatures: Found ${result.size} toggle methods successfully.")
        }
        return result
    }

    /**
     * 启发式查找：查找所有 static boolean 且无参数的方法
     * 通常用于匹配混淆后的配置/开关方法
     */
    fun findStaticBooleanToggles(clazz: Class<*>): List<Method> {
        return clazz.declaredMethods.filter { 
            Modifier.isStatic(it.modifiers) && 
            it.returnType == Boolean::class.javaPrimitiveType && 
            it.parameterCount == 0 
        }
    }
}
