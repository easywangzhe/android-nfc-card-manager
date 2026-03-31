<!-- GSD:project-start source:PROJECT.md -->
## Project

**Android NFC Card Manager**

这是一个面向 NFC 卡片操作场景的 Android 本地应用，当前已具备登录、角色权限、读卡、写卡、锁卡、解锁、模板管理、日志审计和设置等基础能力。本轮项目初始化聚焦在现有产品上继续演进，目标不是重写底层能力，而是在保留既有 NFC 业务流程的前提下，系统性优化 UI 体验、信息层级和关键状态反馈。

**Core Value:** 让用户在执行 NFC 卡片关键操作时，始终能清楚理解当前状态、风险提示和下一步动作，从而更安全、更高效地完成任务。

### Constraints

- **Tech stack**: 保持 Android + Kotlin + Jetpack Compose 现有技术栈 — 避免为了 UI 优化引入不必要的重型重构
- **Brownfield**: 必须在已有业务流程上渐进优化 — 当前读卡、写卡、模板、日志等能力已存在，不能破坏现有功能
- **NFC safety**: 锁卡、解锁等高风险流程必须保留清晰风险提示与确认机制 — 这些操作具有不可逆或高误操作成本
- **Role compatibility**: 现有角色权限边界需要继续成立 — 首页入口、路由与页面呈现不能绕过已有权限判断
- **Offline-first**: 当前主要是本地演示型应用 — 设计和需求不应默认依赖后端服务或在线能力
<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->
## Technology Stack

## Languages
- Kotlin 1.9.24 - Android 应用源码位于 `app/src/main/java/com/opencode/nfccardmanager/**/*.kt`，Kotlin Android 插件定义在 `build.gradle.kts`
- Kotlin DSL - Gradle 构建脚本位于 `build.gradle.kts`、`app/build.gradle.kts`、`settings.gradle.kts`
- XML - Android 清单与资源位于 `app/src/main/AndroidManifest.xml`、`app/src/main/res/values/themes.xml`、`app/src/main/res/values/strings.xml`
- YAML - CI/CD 工作流位于 `.github/workflows/android-build.yml`、`.github/workflows/android-release.yml`
- Markdown - 项目说明与设计文档位于 `README.md`、`docs/PRD.md`、`docs/技术方案文档.md`
## Runtime
- Android Runtime / ART - 运行目标由 `app/build.gradle.kts` 中 `minSdk = 26`、`targetSdk = 35`、`compileSdk = 35` 定义
- Java 17 - `app/build.gradle.kts` 通过 `sourceCompatibility = JavaVersion.VERSION_17`、`targetCompatibility = JavaVersion.VERSION_17`、`jvmTarget = "17"` 固定
- Gradle Wrapper 8.7 - 版本定义在 `gradle/wrapper/gradle-wrapper.properties`
- Lockfile: 未发现 Gradle 版本锁定文件；仓库仅包含 `gradle/wrapper/gradle-wrapper.properties` 与 Wrapper 脚本 `gradlew`、`gradlew.bat`
## Frameworks
- Android Application / Jetpack - 应用入口位于 `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt` 与 `app/src/main/java/com/opencode/nfccardmanager/NfcCardManagerApp.kt`
- Jetpack Compose + Material 3 - UI 由 `app/build.gradle.kts` 的 Compose 依赖启用，页面实现在 `app/src/main/java/com/opencode/nfccardmanager/feature/**/*.kt`
- Navigation Compose 2.7.7 - 导航图位于 `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- Android NFC API - NFC 会话与标签操作位于 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`、`TagParser.kt`、`NdefWriter.kt`、`NdefLocker.kt`、`NdefFormatter.kt`
- JUnit 4.13.2 - JVM 单元测试依赖声明在 `app/build.gradle.kts`
- AndroidX Test JUnit 1.2.1 - 仪器测试依赖声明在 `app/build.gradle.kts`
- Espresso 3.6.1 - UI/集成测试依赖声明在 `app/build.gradle.kts`
- Compose UI Test - `androidx.compose.ui:ui-test-junit4` 声明在 `app/build.gradle.kts`
- Android Gradle Plugin 8.5.2 - 根构建脚本 `build.gradle.kts`
- Kotlin Android Plugin 1.9.24 - 根构建脚本 `build.gradle.kts`
- Compose Compiler Extension 1.5.14 - `app/build.gradle.kts`
- GitHub Actions - 构建与发布流程定义在 `.github/workflows/android-build.yml`、`.github/workflows/android-release.yml`
## Key Dependencies
- `androidx.compose:compose-bom:2024.09.00` - 统一 Compose 组件版本；定义在 `app/build.gradle.kts`
- `androidx.activity:activity-compose:1.9.1` - Compose Activity 宿主；`MainActivity` 位于 `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`
- `androidx.navigation:navigation-compose:2.7.7` - 页面路由与权限分发；实现位于 `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- `androidx.lifecycle:lifecycle-runtime-compose:2.8.4` 与 `androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4` - `collectAsStateWithLifecycle` 与 ViewModel 驱动 UI；见 `AppNavGraph.kt`、`feature/*ViewModel.kt`
- `androidx.core:core-ktx:1.13.1` - Android KTX 基础扩展；依赖声明在 `app/build.gradle.kts`
- `com.google.android.material:material:1.12.0` - Material 组件与设计语言；声明在 `app/build.gradle.kts`
- Android 平台 NFC/SQLite/SharedPreferences API - 未通过外部库封装，直接在 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/*.kt`、`core/database/*.kt`、`core/security/SessionStore.kt` 中调用系统 API
- Kotlin Coroutines Flow API - 状态流用于会话、模板、读卡缓存，见 `core/security/SecurityManager.kt`、`feature/template/LocalTemplateRepository.kt`、`feature/read/ReadResultStore.kt`
## Android & Gradle Configuration
- 应用模块仅包含 `:app`，定义在 `settings.gradle.kts`
- 命名空间与包名为 `com.opencode.nfccardmanager`，定义在 `app/build.gradle.kts`
- Release 构建关闭混淆：`app/build.gradle.kts` 中 `isMinifyEnabled = false`
- ProGuard 文件已声明但规则文件为空：`app/build.gradle.kts` 引用 `app/proguard-rules.pro`，而 `app/proguard-rules.pro` 当前为空文件
- Compose 已启用：`app/build.gradle.kts` 中 `buildFeatures { compose = true }`
- 资源排除策略存在：`app/build.gradle.kts` 中排除 `/META-INF/{AL2.0,LGPL2.1}`
- 仅声明 NFC 权限：`app/src/main/AndroidManifest.xml` 中 `<uses-permission android:name="android.permission.NFC" />`
- 仅声明 NFC 硬件特性，且 `android:required="false"`：`app/src/main/AndroidManifest.xml`
- 未发现 `android.permission.INTERNET`、蓝牙、相机、存储等其他运行时权限；证据是 `app/src/main/AndroidManifest.xml` 仅包含 NFC 权限与 NFC feature
## Configuration
- 本地 Android SDK 路径通过 `local.properties` 配置；示例文件为 `local.properties.example`
- CI 在 `.github/workflows/android-build.yml` 与 `.github/workflows/android-release.yml` 中使用 `ANDROID_SDK_ROOT` 动态生成 `local.properties`
- `README.md` 记录开发机通常需要 `JAVA_HOME`、Android SDK Platform 35、Build-Tools 35.0.0 与可用设备
- 未发现 `.env` 文件；仓库扫描结果为空，说明项目未使用 dotenv 型配置文件
- 根插件配置：`build.gradle.kts`
- 模块构建配置：`app/build.gradle.kts`
- 仓库与模块声明：`settings.gradle.kts`
- 全局 Gradle 参数：`gradle.properties`
- Gradle 发行版：`gradle/wrapper/gradle-wrapper.properties`
- CI 构建配置：`.github/workflows/android-build.yml`
- 发布配置：`.github/workflows/android-release.yml`
## Platform Requirements
- JDK 17；证据见 `README.md` 与 `.github/workflows/android-build.yml`
- Android SDK Platform 35 / Build-Tools 35.0.0；证据见 `README.md` 与工作流中的 `sdkmanager` 命令
- 推荐 Android Studio；证据见 `README.md`
- NFC 真机优先；`README.md` 明确说明模拟器通常不能完整验证 NFC 流程，实际扫描依赖 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`
- 产物为 Android APK，默认输出 `app/build/outputs/apk/debug/app-debug.apk`；见 `README.md` 与 `.github/workflows/android-build.yml`
- 发布渠道为 GitHub Releases 预发布/正式发布；见 `.github/workflows/android-build.yml` 与 `.github/workflows/android-release.yml`
- 未发现 Play Store、Firebase App Distribution、企业 MDM 等分发配置；证据是仓库仅存在 GitHub Actions 发布 APK 的流程文件
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

## Naming Patterns
- Kotlin 源文件统一使用 PascalCase，按职责命名；界面文件以 `Screen.kt` 结尾，如 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateManagementScreen.kt`。
- 状态/契约文件以 `Contract.kt` 结尾，如 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanContract.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteContract.kt`。
- ViewModel 文件以 `ViewModel.kt` 结尾，如 `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`。
- 共享 UI 组件集中在 `app/src/main/java/com/opencode/nfccardmanager/ui/component/AppUi.kt`，组件名与文件职责一致。
- 函数统一使用 camelCase，如 `startScan()`、`onTagDiscovered()`、`resetResult()`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`。
- 事件处理函数使用 `onXxx` 前缀，如 `onUsernameChange()`、`onPasswordChange()`、`onError()`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginViewModel.kt`。
- 页面内部派生文本常用私有函数或 `when` 表达式，如 `titleForMode()` 位于 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`。
- Compose 状态变量使用 `uiState` 命名，并通过 `val uiState by ...collectAsStateWithLifecycle()` 暴露到界面层，见 `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`。
- ViewModel 内部状态遵循 `_uiState` / `uiState` 双变量模式；副作用流使用 `_uiEffect` / `uiEffect`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`。
- 局部依赖常用 `activity`、`sessionManager`、`tagParser`、`scope` 这类直白命名，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`。
- UI 状态采用 `XxxUiState`，如 `LoginUiState`、`ScanUiState`、`TemplateManagementUiState`，分别位于 `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanContract.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateManagementViewModel.kt`。
- 阶段枚举采用 `XxxStage`，如 `ScanStage`、`WriteStage`，位于 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanContract.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteContract.kt`。
- 单次事件使用 sealed interface，如 `ScanUiEffect` 位于 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanContract.kt`。
- 全局管理器/仓储常用 `object` 或简洁类名，如 `SecurityManager`、`AuditLogManager`、`AuditLogRepository`，位于 `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt`、`app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogManager.kt`、`app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogRepository.kt`。
## Code Style
- 未检测到 `.editorconfig`、`ktlint`、`detekt` 或 Spotless 配置；格式主要依赖 Kotlin 官方风格和 IDE 自动排版，证据为仓库根目录未发现相关配置文件。
- Kotlin 代码普遍使用 4 空格缩进、尾随逗号、链式调用换行和命名参数，见 `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`、`app/src/main/java/com/opencode/nfccardmanager/ui/component/AppUi.kt`。
- Compose 参数布局倾向“每行一个参数 + 结尾逗号”，例如 `AppTopBar(...)`、`PrimaryActionButton(...)`，见 `app/src/main/java/com/opencode/nfccardmanager/ui/component/AppUi.kt`。
- 未检测到独立 lint/静态检查配置文件；仓库仅包含 Android Gradle 基础配置 `app/build.gradle.kts` 与根构建文件 `build.gradle.kts`。
- 现有自动验证主要依赖 GitHub Actions 中的 `./gradlew test` 和 `./gradlew assembleDebug`，见 `.github/workflows/android-build.yml`、`.github/workflows/android-release.yml`。
## Import Organization
- 未使用 import alias；所有项目引用均使用完整包路径，根包为 `com.opencode.nfccardmanager`，见 `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`。
## UI 与状态管理模式
- 每个功能页面以 `Screen + ViewModel (+ Contract)` 组合实现，例如 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanContract.kt`。
- 页面内部通过 `Scaffold`、`LazyColumn`、`AppCard`、`AppTopBar` 组织结构；通用视觉元素统一复用 `app/src/main/java/com/opencode/nfccardmanager/ui/component/AppUi.kt`。
- 持久界面状态使用 `MutableStateFlow` + `asStateFlow()`，并在 Compose 中使用 `collectAsStateWithLifecycle()` 订阅，见 `app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateManagementViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateManagementScreen.kt`。
- 单次导航/事件使用 `MutableSharedFlow`，当前明确实现出现在 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`。
- 简单跨页面共享数据使用全局 `object` + `StateFlow`，如 `app/src/main/java/com/opencode/nfccardmanager/feature/read/ReadResultStore.kt`。
- 导航常量集中在 `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt` 的 `Routes` 对象。
- 页面权限校验在导航层完成，再决定渲染目标页面或 `PermissionDeniedScreen`，见 `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`。
## Error Handling
- 低层 NFC 与会话调用优先使用 `runCatching` / `Result` 包装异常，见 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`、`app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt`。
- ViewModel 将异常转换为用户可读消息并写回 `uiState.message` 或 `errorMessage`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatViewModel.kt`。
- 失败时通常同时记录审计日志，见 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatViewModel.kt`。
- Compose NFC 页面在 `DisposableEffect` 中启动 reader mode，并在 `onDispose` 中停止，避免资源泄漏，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`。
## Logging
- 运行时调试日志极少；仅在 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt` 使用 `Log.w("TagParser", "读取 NDEF 失败", it)`。
- 业务操作日志主要通过 `AuditLogManager.save(...)` 落库，见 `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogManager.kt`。
- 对外展示错误文案时优先使用中文业务提示，而不是直接暴露异常堆栈，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`。
## Comments
- 未观察到常规业务注释风格；源码几乎不写注释，主要靠命名和 `UiState` 字段表达语义，证据为 `app/src/main/java/com/opencode/nfccardmanager` 下未发现 `TODO`、`FIXME` 或大段注释块。
- 未检测到 KDoc 习惯；新增代码应优先延续“清晰命名替代注释”的现状，除非涉及复杂 NFC 协议解析。
## Async Patterns
- ViewModel 侧异步统一使用 `viewModelScope.launch { ... }`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateManagementViewModel.kt`。
- Compose 页面需在 NFC 回调中启动协程时，使用 `rememberCoroutineScope()`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`。
- 阻塞式 I/O 会显式切到 `Dispatchers.IO`，当前明确示例是 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt` 中的 `withContext(Dispatchers.IO)`。
- 超时与演示延迟通过 `delay(...)` 实现，见 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatCardScreen.kt`。
## Dependency Injection
- 未接入 Hilt、Dagger、Koin 或其他 DI 框架；证据为仓库下未检测到 `@Inject`、`Hilt`、`Koin` 等标记。
- 依赖注入主要依赖三种轻量方式：
## Function Design
- ViewModel 方法普遍保持单一事件处理职责，如 `onContentChange()`、`startWriting()`；复杂 UI 组合可以较长，当前典型是 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`。
- 公共 UI 入口函数显式接收导航回调，如 `onBack`、`onReadResult`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`。
- 数据对象大量使用命名参数构建，尤其是 `copy(...)` 和数据类实例化，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`。
- 领域/底层写操作多返回 `Result` 或业务结果对象；UI 事件处理函数多返回 `Unit` 并通过状态流更新界面，见 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`。
## Module Design
- 未使用 barrel 文件；Kotlin 通过包结构直接导出类型和顶层函数，见 `app/src/main/java/com/opencode/nfccardmanager/feature/*` 目录。
- `object` 用于全局状态或管理器，`class` 用于有实例语义的解析器、会话管理器和仓储，见 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt`、`app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogRepository.kt`。
- Not applicable；当前代码库未使用 re-export 聚合文件。
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

## Pattern Overview
- 入口集中在 `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`，UI 根节点通过 `AppNavGraph()` 驱动所有页面。
- 大多数业务页面采用“`Screen` + `ViewModel` + `Contract`”组织方式，例如 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteContract.kt`。
- NFC、权限会话、本地 SQLite、模板仓库等共享能力都放在 `app/src/main/java/com/opencode/nfccardmanager/core/` 下，页面直接调用这些对象，没有统一依赖注入层。
- `app/src/main/java/com/opencode/nfccardmanager/domain/usecase/*.kt` 存在，但当前四个 UseCase 仅为空类；据 `app/src/main/java/com/opencode/nfccardmanager/feature/*` 页面直接调用 `core` 推断，真实业务编排尚未下沉到 `domain`。
## Layers
- Purpose: 进程启动、全局单例初始化。
- Location: `app/src/main/java/com/opencode/nfccardmanager/NfcCardManagerApp.kt`
- Contains: `Application` 子类。
- Depends on: `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogManager.kt`、`app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/template/LocalTemplateRepository.kt`
- Used by: `app/src/main/AndroidManifest.xml`
- Purpose: 装载 Compose 树与主题。
- Location: `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`
- Contains: `ComponentActivity`、`setContent`、主题包裹。
- Depends on: `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`、`app/src/main/java/com/opencode/nfccardmanager/ui/theme/Theme.kt`
- Used by: Launcher Activity，声明于 `app/src/main/AndroidManifest.xml`
- Purpose: 页面路由、底部导航、登录态切换、页面级权限守卫。
- Location: `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- Contains: `Routes`、`BottomNavItem`、`NavHost`、各页面 `composable`。
- Depends on: `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt` 与所有 `feature/*Screen.kt`
- Used by: `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`
- Purpose: 每个业务场景的界面、页面状态与交互编排。
- Location: `app/src/main/java/com/opencode/nfccardmanager/feature/`
- Contains: 登录、首页、扫描、读卡结果、写卡、格式化、锁卡、解锁、模板、审计、设置、通用拒绝页。
- Depends on: `core` 共享能力、`ui/component` 组件、Compose Lifecycle。
- Used by: `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- Purpose: NFC ReaderMode 生命周期、标签解析、NDEF 写入/格式化/锁定、能力识别。
- Location: `app/src/main/java/com/opencode/nfccardmanager/core/nfc/`
- Contains: `NfcSessionManager`、`TagParser`、`NdefWriter`、`NdefFormatter`、`NdefLocker`、`PasswordProtectedLocker`、`UnlockExecutor`、`CardCapabilityResolver`
- Depends on: Android NFC API、`app/src/main/java/com/opencode/nfccardmanager/core/nfc/model/`
- Used by: `feature/scan`、`feature/write`、`feature/format`、`feature/lock`、`feature/unlock`
- Purpose: 本地审计日志、模板数据、登录态持久化。
- Location: `app/src/main/java/com/opencode/nfccardmanager/core/database/`、`app/src/main/java/com/opencode/nfccardmanager/core/security/SessionStore.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateDbHelper.kt`
- Contains: SQLiteOpenHelper、Repository、StateFlow 仓库、SharedPreferences 存储。
- Depends on: Android Context、SQLite、SharedPreferences。
- Used by: `NfcCardManagerApp.kt` 初始化；各 `ViewModel` 与 `Screen` 读写。
- Purpose: 统一页面容器、按钮、状态标签、主题色。
- Location: `app/src/main/java/com/opencode/nfccardmanager/ui/component/AppUi.kt`、`app/src/main/java/com/opencode/nfccardmanager/ui/theme/`
- Contains: `AppTopBar`、`AppCard`、`PrimaryActionButton`、`StatusPill` 等。
- Depends on: Compose Material3。
- Used by: 几乎所有 `feature/*Screen.kt`
## Data Flow
- 页面状态统一采用 `MutableStateFlow` + `asStateFlow()`，典型文件见 `feature/scan/ScanViewModel.kt`、`feature/write/WriteViewModel.kt`、`feature/audit/AuditLogViewModel.kt`。
- 一次性导航副作用仅在 `feature/scan/ScanViewModel.kt` 使用 `MutableSharedFlow<ScanUiEffect>`；其余页面多数直接由屏幕参数回调处理。
- 跨页面临时共享数据使用进程内单例缓存 `feature/read/ReadResultStore.kt`，不是 SavedStateHandle。
## Key Abstractions
- Purpose: 集中管理页面路由字符串与动态参数编码。
- Examples: `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- Pattern: `object Routes` + `fun buildRoute(...)`
- Purpose: 把 UI、状态定义、状态流转分离到同一 feature 包内。
- Examples: `feature/scan/ScanScreen.kt` + `feature/scan/ScanViewModel.kt` + `feature/scan/ScanContract.kt`；`feature/lock/LockRiskScreen.kt` + `feature/lock/LockViewModel.kt` + `feature/lock/LockContract.kt`
- Pattern: Screen 订阅 `uiState`，ViewModel 只暴露事件函数，Contract 定义 Stage/UIState。
- Purpose: 在页面间传递统一的卡片信息、能力与操作结果。
- Examples: `app/src/main/java/com/opencode/nfccardmanager/core/nfc/model/CardModels.kt`、`ReadModels.kt`、`WriteModels.kt`
- Pattern: `data class` + enum 状态码。
- Purpose: 登录态恢复、角色切换、页面级权限判断。
- Examples: `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt`、`SessionStore.kt`
- Pattern: `object` + `StateFlow` + `SharedPreferences`
- Purpose: 把 SQLite/本地列表对上层隐藏成简单 API。
- Examples: `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogRepository.kt`、`feature/template/LocalTemplateRepository.kt`
- Pattern: `Repository` 包装 `SQLiteOpenHelper`；模板仓库额外暴露 `StateFlow<List<WriteTemplate>>`
- Purpose: 把具体标签操作从页面中拆出。
- Examples: `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt`、`NdefWriter.kt`、`NdefFormatter.kt`、`NdefLocker.kt`、`UnlockExecutor.kt`
- Pattern: 无状态类 + 同步执行方法；页面负责 ReaderMode 生命周期，执行器负责业务结果对象。
## Entry Points
- Location: `app/src/main/java/com/opencode/nfccardmanager/NfcCardManagerApp.kt`
- Triggers: 进程创建。
- Responsibilities: 初始化审计日志仓库、登录态仓库、模板仓库。
- Location: `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`
- Triggers: Launcher Activity。
- Responsibilities: 注入主题、承载 Compose、进入 `AppNavGraph()`。
- Location: `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- Triggers: Compose 根内容创建。
- Responsibilities: 路由分发、登录态重定向、底部导航、权限拦截。
- Location: `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`
- Triggers: `ScanScreen.kt`、`WriteEditorScreen.kt`、`FormatCardScreen.kt`、`LockRiskScreen.kt`、`UnlockVerifyScreen.kt`
- Responsibilities: `enableReaderMode` / `disableReaderMode` 生命周期管理。
## Error Handling
- ReaderMode 启动失败统一在页面 `onFailure { viewModel.onError(...) }` 中处理，例见 `feature/format/FormatCardScreen.kt`、`feature/unlock/UnlockVerifyScreen.kt`。
- NFC 执行类将底层异常包装成人类可读文本，例见 `core/nfc/NdefWriter.kt` 的 `buildExceptionDetail()`。
- ViewModel 在错误分支通常同时更新 Stage 与 message，并写审计日志，例见 `feature/write/WriteViewModel.kt`、`feature/lock/LockViewModel.kt`。
- 访问控制不是抛异常，而是在 `navigation/AppNavGraph.kt` 中直接渲染 `feature/common/PermissionDeniedScreen.kt`。
## Cross-Cutting Concerns
- 业务审计统一走 `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogManager.kt`，数据最终写入 `AuditLogDbHelper.kt` 的本地 SQLite。
- 系统调试日志仅在 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt` 使用 `android.util.Log.w(...)`。
- 登录校验集中在 `core/security/SecurityManager.kt`。
- 表单前置校验分散在各 ViewModel，如 `feature/write/WriteViewModel.kt` 检查内容非空，`feature/lock/LockViewModel.kt` 检查确认词与勾选状态。
- NFC 标签可操作性校验主要在 `core/nfc/NdefWriter.kt` 的 `precheck()`。
- 当前是本地演示型认证：`SecurityManager.kt` 内置账号与固定密码，并通过 `SessionStore.kt` 记住登录态。
- 路由授权同样集中在 `SecurityManager.kt` 的 `canRead/canWrite/canLock/...`，由 `AppNavGraph.kt` 在进入页面前执行。
- `AndroidManifest.xml` 仅声明一个 `Application` 与一个 `Activity`；其余业务都在 Compose 页面内完成，没有 `Fragment`、`Service`、`BroadcastReceiver`。
- 需要 Activity 的 NFC 页面通过 `core/common/ContextExt.kt` 的 `findActivity()` 从 Compose `LocalContext` 反查宿主 Activity。
- 由于 `domain/usecase` 为空，而所有 `feature/*Screen.kt` 直接 new `NfcSessionManager`、`TagParser`、`NdefWriter` 等，可推断当前架构更接近“页面直接编排 core 服务”，而非严格 Clean Architecture。
- 由于没有 DI 框架或 Service Locator，且页面内部通过 `remember { ClassName() }` 创建执行器，可推断共享能力替换与单元测试注入成本较高。
<!-- GSD:architecture-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd:quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd:debug` for investigation and bug fixing
- `/gsd:execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
