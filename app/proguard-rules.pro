# 反射与框架兼容性
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn de.robv.android.xposed.**

# KavaRef 核心保留
-keep class com.highcapable.kavaref.** { *; }
-dontwarn com.highcapable.kavaref.**

# YukiHookAPI 核心保留
-keep interface com.highcapable.yukihookapi.** { *; }
-keep class * implements com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit { *; }
-dontwarn com.highcapable.yukihookapi.**

# 允许混淆和优化子 Hooker 及其内部调用
-keep class com.wkeqin.ntqqbattery.hook.HookEntry { *; }
-keep class com.wkeqin.ntqqbattery.BuildConfig { *; }

# R8 强力剥离日志 (需要开启 optimization 模式)
-assumenosideeffects class com.highcapable.yukihookapi.hook.log.YLog {
    *** debug(...);
    *** info(...);
    *** warn(...);
}

# 同时剥离 Android 原生标准的 Debug/Info/Warn 级别日志
-assumenosideeffects class android.util.Log {
    static *** v(...);
    static *** d(...);
    static *** i(...);
    static *** w(...);
}
