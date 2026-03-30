# Technology Stack

**Analysis Date:** 2026-03-30

## Languages

**Primary:**
- Kotlin 1.9.24 - Android 应用源码位于 `app/src/main/java/com/opencode/nfccardmanager/**/*.kt`，Kotlin Android 插件定义在 `build.gradle.kts`

**Secondary:**
- Kotlin DSL - Gradle 构建脚本位于 `build.gradle.kts`、`app/build.gradle.kts`、`settings.gradle.kts`
- XML - Android 清单与资源位于 `app/src/main/AndroidManifest.xml`、`app/src/main/res/values/themes.xml`、`app/src/main/res/values/strings.xml`
- YAML - CI/CD 工作流位于 `.github/workflows/android-build.yml`、`.github/workflows/android-release.yml`
- Markdown - 项目说明与设计文档位于 `README.md`、`docs/PRD.md`、`docs/技术方案文档.md`

## Runtime

**Environment:**
- Android Runtime / ART - 运行目标由 `app/build.gradle.kts` 中 `minSdk = 26`、`targetSdk = 35`、`compileSdk = 35` 定义
- Java 17 - `app/build.gradle.kts` 通过 `sourceCompatibility = JavaVersion.VERSION_17`、`targetCompatibility = JavaVersion.VERSION_17`、`jvmTarget = "17"` 固定

**Package Manager:**
- Gradle Wrapper 8.7 - 版本定义在 `gradle/wrapper/gradle-wrapper.properties`
- Lockfile: 未发现 Gradle 版本锁定文件；仓库仅包含 `gradle/wrapper/gradle-wrapper.properties` 与 Wrapper 脚本 `gradlew`、`gradlew.bat`

## Frameworks

**Core:**
- Android Application / Jetpack - 应用入口位于 `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt` 与 `app/src/main/java/com/opencode/nfccardmanager/NfcCardManagerApp.kt`
- Jetpack Compose + Material 3 - UI 由 `app/build.gradle.kts` 的 Compose 依赖启用，页面实现在 `app/src/main/java/com/opencode/nfccardmanager/feature/**/*.kt`
- Navigation Compose 2.7.7 - 导航图位于 `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- Android NFC API - NFC 会话与标签操作位于 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`、`TagParser.kt`、`NdefWriter.kt`、`NdefLocker.kt`、`NdefFormatter.kt`

**Testing:**
- JUnit 4.13.2 - JVM 单元测试依赖声明在 `app/build.gradle.kts`
- AndroidX Test JUnit 1.2.1 - 仪器测试依赖声明在 `app/build.gradle.kts`
- Espresso 3.6.1 - UI/集成测试依赖声明在 `app/build.gradle.kts`
- Compose UI Test - `androidx.compose.ui:ui-test-junit4` 声明在 `app/build.gradle.kts`

**Build/Dev:**
- Android Gradle Plugin 8.5.2 - 根构建脚本 `build.gradle.kts`
- Kotlin Android Plugin 1.9.24 - 根构建脚本 `build.gradle.kts`
- Compose Compiler Extension 1.5.14 - `app/build.gradle.kts`
- GitHub Actions - 构建与发布流程定义在 `.github/workflows/android-build.yml`、`.github/workflows/android-release.yml`

## Key Dependencies

**Critical:**
- `androidx.compose:compose-bom:2024.09.00` - 统一 Compose 组件版本；定义在 `app/build.gradle.kts`
- `androidx.activity:activity-compose:1.9.1` - Compose Activity 宿主；`MainActivity` 位于 `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`
- `androidx.navigation:navigation-compose:2.7.7` - 页面路由与权限分发；实现位于 `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- `androidx.lifecycle:lifecycle-runtime-compose:2.8.4` 与 `androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4` - `collectAsStateWithLifecycle` 与 ViewModel 驱动 UI；见 `AppNavGraph.kt`、`feature/*ViewModel.kt`
- `androidx.core:core-ktx:1.13.1` - Android KTX 基础扩展；依赖声明在 `app/build.gradle.kts`

**Infrastructure:**
- `com.google.android.material:material:1.12.0` - Material 组件与设计语言；声明在 `app/build.gradle.kts`
- Android 平台 NFC/SQLite/SharedPreferences API - 未通过外部库封装，直接在 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/*.kt`、`core/database/*.kt`、`core/security/SessionStore.kt` 中调用系统 API
- Kotlin Coroutines Flow API - 状态流用于会话、模板、读卡缓存，见 `core/security/SecurityManager.kt`、`feature/template/LocalTemplateRepository.kt`、`feature/read/ReadResultStore.kt`

## Android & Gradle Configuration

**App module:**
- 应用模块仅包含 `:app`，定义在 `settings.gradle.kts`
- 命名空间与包名为 `com.opencode.nfccardmanager`，定义在 `app/build.gradle.kts`
- Release 构建关闭混淆：`app/build.gradle.kts` 中 `isMinifyEnabled = false`
- ProGuard 文件已声明但规则文件为空：`app/build.gradle.kts` 引用 `app/proguard-rules.pro`，而 `app/proguard-rules.pro` 当前为空文件
- Compose 已启用：`app/build.gradle.kts` 中 `buildFeatures { compose = true }`
- 资源排除策略存在：`app/build.gradle.kts` 中排除 `/META-INF/{AL2.0,LGPL2.1}`

**Manifest & permissions:**
- 仅声明 NFC 权限：`app/src/main/AndroidManifest.xml` 中 `<uses-permission android:name="android.permission.NFC" />`
- 仅声明 NFC 硬件特性，且 `android:required="false"`：`app/src/main/AndroidManifest.xml`
- 未发现 `android.permission.INTERNET`、蓝牙、相机、存储等其他运行时权限；证据是 `app/src/main/AndroidManifest.xml` 仅包含 NFC 权限与 NFC feature

## Configuration

**Environment:**
- 本地 Android SDK 路径通过 `local.properties` 配置；示例文件为 `local.properties.example`
- CI 在 `.github/workflows/android-build.yml` 与 `.github/workflows/android-release.yml` 中使用 `ANDROID_SDK_ROOT` 动态生成 `local.properties`
- `README.md` 记录开发机通常需要 `JAVA_HOME`、Android SDK Platform 35、Build-Tools 35.0.0 与可用设备
- 未发现 `.env` 文件；仓库扫描结果为空，说明项目未使用 dotenv 型配置文件

**Build:**
- 根插件配置：`build.gradle.kts`
- 模块构建配置：`app/build.gradle.kts`
- 仓库与模块声明：`settings.gradle.kts`
- 全局 Gradle 参数：`gradle.properties`
- Gradle 发行版：`gradle/wrapper/gradle-wrapper.properties`
- CI 构建配置：`.github/workflows/android-build.yml`
- 发布配置：`.github/workflows/android-release.yml`

## Platform Requirements

**Development:**
- JDK 17；证据见 `README.md` 与 `.github/workflows/android-build.yml`
- Android SDK Platform 35 / Build-Tools 35.0.0；证据见 `README.md` 与工作流中的 `sdkmanager` 命令
- 推荐 Android Studio；证据见 `README.md`
- NFC 真机优先；`README.md` 明确说明模拟器通常不能完整验证 NFC 流程，实际扫描依赖 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`

**Production:**
- 产物为 Android APK，默认输出 `app/build/outputs/apk/debug/app-debug.apk`；见 `README.md` 与 `.github/workflows/android-build.yml`
- 发布渠道为 GitHub Releases 预发布/正式发布；见 `.github/workflows/android-build.yml` 与 `.github/workflows/android-release.yml`
- 未发现 Play Store、Firebase App Distribution、企业 MDM 等分发配置；证据是仓库仅存在 GitHub Actions 发布 APK 的流程文件

---

*Stack analysis: 2026-03-30*
