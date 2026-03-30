# Coding Conventions

**Analysis Date:** 2026-03-30

## Naming Patterns

**Files:**
- Kotlin 源文件统一使用 PascalCase，按职责命名；界面文件以 `Screen.kt` 结尾，如 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateManagementScreen.kt`。
- 状态/契约文件以 `Contract.kt` 结尾，如 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanContract.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteContract.kt`。
- ViewModel 文件以 `ViewModel.kt` 结尾，如 `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`。
- 共享 UI 组件集中在 `app/src/main/java/com/opencode/nfccardmanager/ui/component/AppUi.kt`，组件名与文件职责一致。

**Functions:**
- 函数统一使用 camelCase，如 `startScan()`、`onTagDiscovered()`、`resetResult()`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`。
- 事件处理函数使用 `onXxx` 前缀，如 `onUsernameChange()`、`onPasswordChange()`、`onError()`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginViewModel.kt`。
- 页面内部派生文本常用私有函数或 `when` 表达式，如 `titleForMode()` 位于 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`。

**Variables:**
- Compose 状态变量使用 `uiState` 命名，并通过 `val uiState by ...collectAsStateWithLifecycle()` 暴露到界面层，见 `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`。
- ViewModel 内部状态遵循 `_uiState` / `uiState` 双变量模式；副作用流使用 `_uiEffect` / `uiEffect`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`。
- 局部依赖常用 `activity`、`sessionManager`、`tagParser`、`scope` 这类直白命名，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`。

**Types:**
- UI 状态采用 `XxxUiState`，如 `LoginUiState`、`ScanUiState`、`TemplateManagementUiState`，分别位于 `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanContract.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateManagementViewModel.kt`。
- 阶段枚举采用 `XxxStage`，如 `ScanStage`、`WriteStage`，位于 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanContract.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteContract.kt`。
- 单次事件使用 sealed interface，如 `ScanUiEffect` 位于 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanContract.kt`。
- 全局管理器/仓储常用 `object` 或简洁类名，如 `SecurityManager`、`AuditLogManager`、`AuditLogRepository`，位于 `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt`、`app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogManager.kt`、`app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogRepository.kt`。

## Code Style

**Formatting:**
- 未检测到 `.editorconfig`、`ktlint`、`detekt` 或 Spotless 配置；格式主要依赖 Kotlin 官方风格和 IDE 自动排版，证据为仓库根目录未发现相关配置文件。
- Kotlin 代码普遍使用 4 空格缩进、尾随逗号、链式调用换行和命名参数，见 `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`、`app/src/main/java/com/opencode/nfccardmanager/ui/component/AppUi.kt`。
- Compose 参数布局倾向“每行一个参数 + 结尾逗号”，例如 `AppTopBar(...)`、`PrimaryActionButton(...)`，见 `app/src/main/java/com/opencode/nfccardmanager/ui/component/AppUi.kt`。

**Linting:**
- 未检测到独立 lint/静态检查配置文件；仓库仅包含 Android Gradle 基础配置 `app/build.gradle.kts` 与根构建文件 `build.gradle.kts`。
- 现有自动验证主要依赖 GitHub Actions 中的 `./gradlew test` 和 `./gradlew assembleDebug`，见 `.github/workflows/android-build.yml`、`.github/workflows/android-release.yml`。

## Import Organization

**Order:**
1. Android / AndroidX 导入，如 `android.nfc.Tag`、`androidx.compose.runtime.*`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`。
2. 项目内 `com.opencode.nfccardmanager...` 导入，如 `com.opencode.nfccardmanager.core.nfc.TagParser`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`。
3. Kotlin / coroutines 导入，如 `kotlinx.coroutines.launch`、`kotlinx.coroutines.withContext`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`。

**Path Aliases:**
- 未使用 import alias；所有项目引用均使用完整包路径，根包为 `com.opencode.nfccardmanager`，见 `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`。

## UI 与状态管理模式

**Compose 页面组织：**
- 每个功能页面以 `Screen + ViewModel (+ Contract)` 组合实现，例如 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanContract.kt`。
- 页面内部通过 `Scaffold`、`LazyColumn`、`AppCard`、`AppTopBar` 组织结构；通用视觉元素统一复用 `app/src/main/java/com/opencode/nfccardmanager/ui/component/AppUi.kt`。

**状态管理：**
- 持久界面状态使用 `MutableStateFlow` + `asStateFlow()`，并在 Compose 中使用 `collectAsStateWithLifecycle()` 订阅，见 `app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateManagementViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateManagementScreen.kt`。
- 单次导航/事件使用 `MutableSharedFlow`，当前明确实现出现在 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`。
- 简单跨页面共享数据使用全局 `object` + `StateFlow`，如 `app/src/main/java/com/opencode/nfccardmanager/feature/read/ReadResultStore.kt`。

**导航与权限：**
- 导航常量集中在 `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt` 的 `Routes` 对象。
- 页面权限校验在导航层完成，再决定渲染目标页面或 `PermissionDeniedScreen`，见 `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`。

## Error Handling

**Patterns:**
- 低层 NFC 与会话调用优先使用 `runCatching` / `Result` 包装异常，见 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`、`app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt`。
- ViewModel 将异常转换为用户可读消息并写回 `uiState.message` 或 `errorMessage`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatViewModel.kt`。
- 失败时通常同时记录审计日志，见 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatViewModel.kt`。
- Compose NFC 页面在 `DisposableEffect` 中启动 reader mode，并在 `onDispose` 中停止，避免资源泄漏，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`。

## Logging

**Framework:** Android `Log` + 审计记录。

**Patterns:**
- 运行时调试日志极少；仅在 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt` 使用 `Log.w("TagParser", "读取 NDEF 失败", it)`。
- 业务操作日志主要通过 `AuditLogManager.save(...)` 落库，见 `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogManager.kt`。
- 对外展示错误文案时优先使用中文业务提示，而不是直接暴露异常堆栈，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`。

## Comments

**When to Comment:**
- 未观察到常规业务注释风格；源码几乎不写注释，主要靠命名和 `UiState` 字段表达语义，证据为 `app/src/main/java/com/opencode/nfccardmanager` 下未发现 `TODO`、`FIXME` 或大段注释块。

**JSDoc/TSDoc / KDoc:**
- 未检测到 KDoc 习惯；新增代码应优先延续“清晰命名替代注释”的现状，除非涉及复杂 NFC 协议解析。

## Async Patterns

**Coroutines:**
- ViewModel 侧异步统一使用 `viewModelScope.launch { ... }`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateManagementViewModel.kt`。
- Compose 页面需在 NFC 回调中启动协程时，使用 `rememberCoroutineScope()`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`。
- 阻塞式 I/O 会显式切到 `Dispatchers.IO`，当前明确示例是 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt` 中的 `withContext(Dispatchers.IO)`。
- 超时与演示延迟通过 `delay(...)` 实现，见 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatCardScreen.kt`。

## Dependency Injection

**Approach:**
- 未接入 Hilt、Dagger、Koin 或其他 DI 框架；证据为仓库下未检测到 `@Inject`、`Hilt`、`Koin` 等标记。
- 依赖注入主要依赖三种轻量方式：
  - 构造函数默认参数，如 `ScanViewModel(private val capabilityResolver: CardCapabilityResolver = CardCapabilityResolver())` 位于 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`。
  - Compose 层 `remember { ... }` 直接创建对象，如 `TagParser()`、`NdefWriter()`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`。
  - `Application` 启动时初始化全局单例，如 `AuditLogManager.init(this)`、`SecurityManager.init(this)`、`LocalTemplateRepository.init(this)`，见 `app/src/main/java/com/opencode/nfccardmanager/NfcCardManagerApp.kt`。

## Function Design

**Size:**
- ViewModel 方法普遍保持单一事件处理职责，如 `onContentChange()`、`startWriting()`；复杂 UI 组合可以较长，当前典型是 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`。

**Parameters:**
- 公共 UI 入口函数显式接收导航回调，如 `onBack`、`onReadResult`，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`。
- 数据对象大量使用命名参数构建，尤其是 `copy(...)` 和数据类实例化，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`。

**Return Values:**
- 领域/底层写操作多返回 `Result` 或业务结果对象；UI 事件处理函数多返回 `Unit` 并通过状态流更新界面，见 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`。

## Module Design

**Exports:**
- 未使用 barrel 文件；Kotlin 通过包结构直接导出类型和顶层函数，见 `app/src/main/java/com/opencode/nfccardmanager/feature/*` 目录。
- `object` 用于全局状态或管理器，`class` 用于有实例语义的解析器、会话管理器和仓储，见 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt`、`app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogRepository.kt`。

**Barrel Files:**
- Not applicable；当前代码库未使用 re-export 聚合文件。

---

*Convention analysis: 2026-03-30*
