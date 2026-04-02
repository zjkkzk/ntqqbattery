# NTQQBattery

`NTQQBattery` 是一个面向 NTQQ（`com.tencent.mobileqq`）的 Xposed / LSPosed 省电优化模块。

项目基于 Kotlin、YukiHookAPI 和 KSP 构建，主要针对 NTQQ 常见的耗电来源进行拦截和优化，例如频繁 WakeLock、前台保活服务、预加载、行为上报以及重型后台组件等。

## 功能特性

当前模块提供的可选功能包括：

- 拦截系统 WakeLock 的申请与释放
- 拦截 NTQQ 电量监控
- 拦截 CoreService 前台保活
- 拦截小程序预加载
- 拦截小游戏闪屏 / 进程预加载
- 拦截 SplashActivity UI 预加载
- 隐藏超级 QQ 秀弹窗
- 拦截主题视频初始化与动画
- 抑制重型引擎资源占用
- 拦截重型后台服务
- 启用后台墓碑 / 深度休眠模式
- 抑制高频网络请求
- 强制启用 PowerSaveMode 行为
- 优化 MSF 内部策略
- 启用激进型 MSF 优化
- 拦截后台振动
- 拦截 Beacon 行为上报
- 拦截 TVK 视频统计上报
- 优化 GIF 渲染行为

## 适用范围

- 目标应用：`com.tencent.mobileqq`
- Android `minSdk`：26
- Android `targetSdk`：36
- Xposed API：82
- 清单中声明的最低 Xposed 版本：93

## 项目结构

模块通过 YukiHookAPI 注入 NTQQ，并从主入口分发各项功能 Hook：

- 入口文件：[HookEntry.kt](app/src/main/java/com/wkeqin/ntqqbattery/hook/HookEntry.kt)
- Hook 调度核心：[NTQQHooker.kt](app/src/main/java/com/wkeqin/ntqqbattery/hook/entity/NTQQHooker.kt)
- 功能注册表：[FeatureRegistry.kt](app/src/main/java/com/wkeqin/ntqqbattery/hook/entity/FeatureRegistry.kt)
- Hook 计划注册表：[HookPlanRegistry.kt](app/src/main/java/com/wkeqin/ntqqbattery/hook/entity/HookPlanRegistry.kt)

模块设置页提供开关界面，可以直接对各项优化进行启用或关闭，无需修改代码。

## 使用方式

1. 安装支持 Xposed 的框架，例如 LSPosed。
2. 编译或安装 `NTQQBattery` APK。
3. 在 Xposed 管理器中启用本模块。
4. 将作用域勾选到 `QQ / com.tencent.mobileqq`。
5. 重启 NTQQ，然后在模块设置页中按需开启功能。

## 构建说明

环境要求：

- JDK 17
- Android SDK API 36
- 项目自带 Gradle Wrapper

构建 Debug 版本：

```bash
cd NTQQBattery
./gradlew :app:assembleDebug
```

构建 Release 版本：

```bash
cd NTQQBattery
./gradlew :app:assembleRelease
```

如果需要签名 Release 包，请在 `local.properties` 中配置：

```properties
signing.storePassword=...
signing.keyAlias=...
signing.keyPassword=...
```

默认签名文件路径为：

```text
app/release.jks
```

## 技术栈

- Kotlin
- Android Gradle Plugin 8.13.2
- Kotlin 2.2.21
- KSP
- YukiHookAPI 1.3.1
- KavaRef 1.0.2

## 注意事项

- 本项目专门面向 NTQQ，不是通用 QQ Hook 框架。
- 某些激进优化可能影响部分功能行为，建议逐项开启并自行验证常用功能是否正常。
- 一部分优化本质上是用后台活动能力、上报能力或保活能力来换取更低功耗。

## 免责声明

本项目仅供学习、研究与个人使用。对商业应用进行 Hook 可能违反目标应用条款，也可能在应用更新后失效或引发异常行为。使用前请自行评估风险。
