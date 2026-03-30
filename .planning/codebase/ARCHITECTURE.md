# Architecture

**Analysis Date:** 2026-03-30

## Pattern Overview

**Overall:** 单模块 Android 应用 + Jetpack Compose 界面 + 以 `feature` 包为边界的轻量 MVVM，底层能力集中在 `core`，`domain` 层已预留但当前基本未落地。

**Key Characteristics:**
- 入口集中在 `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`，UI 根节点通过 `AppNavGraph()` 驱动所有页面。
- 大多数业务页面采用“`Screen` + `ViewModel` + `Contract`”组织方式，例如 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteContract.kt`。
- NFC、权限会话、本地 SQLite、模板仓库等共享能力都放在 `app/src/main/java/com/opencode/nfccardmanager/core/` 下，页面直接调用这些对象，没有统一依赖注入层。
- `app/src/main/java/com/opencode/nfccardmanager/domain/usecase/*.kt` 存在，但当前四个 UseCase 仅为空类；据 `app/src/main/java/com/opencode/nfccardmanager/feature/*` 页面直接调用 `core` 推断，真实业务编排尚未下沉到 `domain`。

## Layers

**Application / Bootstrap:**
- Purpose: 进程启动、全局单例初始化。
- Location: `app/src/main/java/com/opencode/nfccardmanager/NfcCardManagerApp.kt`
- Contains: `Application` 子类。
- Depends on: `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogManager.kt`、`app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/template/LocalTemplateRepository.kt`
- Used by: `app/src/main/AndroidManifest.xml`

**Android Entry / Host UI:**
- Purpose: 装载 Compose 树与主题。
- Location: `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`
- Contains: `ComponentActivity`、`setContent`、主题包裹。
- Depends on: `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`、`app/src/main/java/com/opencode/nfccardmanager/ui/theme/Theme.kt`
- Used by: Launcher Activity，声明于 `app/src/main/AndroidManifest.xml`

**Navigation / Route Coordination:**
- Purpose: 页面路由、底部导航、登录态切换、页面级权限守卫。
- Location: `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- Contains: `Routes`、`BottomNavItem`、`NavHost`、各页面 `composable`。
- Depends on: `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt` 与所有 `feature/*Screen.kt`
- Used by: `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`

**Feature Presentation:**
- Purpose: 每个业务场景的界面、页面状态与交互编排。
- Location: `app/src/main/java/com/opencode/nfccardmanager/feature/`
- Contains: 登录、首页、扫描、读卡结果、写卡、格式化、锁卡、解锁、模板、审计、设置、通用拒绝页。
- Depends on: `core` 共享能力、`ui/component` 组件、Compose Lifecycle。
- Used by: `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`

**Core NFC / Shared Services:**
- Purpose: NFC ReaderMode 生命周期、标签解析、NDEF 写入/格式化/锁定、能力识别。
- Location: `app/src/main/java/com/opencode/nfccardmanager/core/nfc/`
- Contains: `NfcSessionManager`、`TagParser`、`NdefWriter`、`NdefFormatter`、`NdefLocker`、`PasswordProtectedLocker`、`UnlockExecutor`、`CardCapabilityResolver`
- Depends on: Android NFC API、`app/src/main/java/com/opencode/nfccardmanager/core/nfc/model/`
- Used by: `feature/scan`、`feature/write`、`feature/format`、`feature/lock`、`feature/unlock`

**Local Persistence / Session:**
- Purpose: 本地审计日志、模板数据、登录态持久化。
- Location: `app/src/main/java/com/opencode/nfccardmanager/core/database/`、`app/src/main/java/com/opencode/nfccardmanager/core/security/SessionStore.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateDbHelper.kt`
- Contains: SQLiteOpenHelper、Repository、StateFlow 仓库、SharedPreferences 存储。
- Depends on: Android Context、SQLite、SharedPreferences。
- Used by: `NfcCardManagerApp.kt` 初始化；各 `ViewModel` 与 `Screen` 读写。

**UI Design System:**
- Purpose: 统一页面容器、按钮、状态标签、主题色。
- Location: `app/src/main/java/com/opencode/nfccardmanager/ui/component/AppUi.kt`、`app/src/main/java/com/opencode/nfccardmanager/ui/theme/`
- Contains: `AppTopBar`、`AppCard`、`PrimaryActionButton`、`StatusPill` 等。
- Depends on: Compose Material3。
- Used by: 几乎所有 `feature/*Screen.kt`

## Data Flow

**应用启动流：**

1. 系统从 `app/src/main/AndroidManifest.xml` 启动 `app/src/main/java/com/opencode/nfccardmanager/NfcCardManagerApp.kt` 与 `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`。
2. `NfcCardManagerApp.kt` 初始化审计、会话、模板三个全局单例存储。
3. `MainActivity.kt` 创建 Compose 根树，并在 `AppNavGraph.kt` 中根据 `SecurityManager.isLoggedIn()` 决定初始路由。

**登录与权限流：**

1. `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginScreen.kt` 收集 `LoginViewModel` 状态。
2. `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginViewModel.kt` 直接调用 `SecurityManager.login()`。
3. `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt` 更新 `currentSession`、`currentRole`，并通过 `SessionStore.kt` 落盘。
4. `AppNavGraph.kt` 订阅 `currentSession/currentRole`，控制登录重定向、底栏显示和页面访问权限。

**读卡流：**

1. 首页在 `app/src/main/java/com/opencode/nfccardmanager/feature/home/HomeScreen.kt` 触发跳转到 `Routes.scan(ScanMode.READ)`。
2. `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt` 创建 `NfcSessionManager`，在 `DisposableEffect` 中开启 ReaderMode。
3. Reader callback 把 `Tag` 交给 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt`，产出 `ReadCardResult`。
4. `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt` 更新 `ScanUiState`，写入 `AuditLogManager.save(...)`，再把完整结果放入 `app/src/main/java/com/opencode/nfccardmanager/feature/read/ReadResultStore.kt`。
5. `ScanViewModel` 通过 `ScanUiEffect.NavigateToReadResult` 驱动跳转，`app/src/main/java/com/opencode/nfccardmanager/feature/read/ReadResultScreen.kt` 从 `ReadResultStore` 展示详情。

**写卡流：**

1. `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt` 从 `LocalTemplateRepository` 提供模板选择与文本输入。
2. `WriteViewModel.startWriting()` 将状态推进到 `WriteStage.WRITING`。
3. 页面开启 ReaderMode，检测到标签后先调用 `TagParser.parse(tag)` 与 `NdefWriter.precheck(tag, request)`。
4. 页面把预检信息回填给 `WriteViewModel.onRawTagDetected()` / `onTagDetected()`，再在 IO 线程执行 `NdefWriter.writeText(...)`。
5. `WriteViewModel.onWriteResult()` 写审计日志并更新结果卡片。

**格式化流：**

1. `app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatCardScreen.kt` 进入后调用 `FormatViewModel.start()`。
2. 页面开启 ReaderMode，标签到达后交给 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NdefFormatter.kt`。
3. `FormatViewModel.onFormatResult()` 记录审计并回填 UI；成功后页面允许继续跳转到 `Routes.WRITE_EDITOR`。

**锁卡 / 解锁流：**

1. 锁卡页面 `app/src/main/java/com/opencode/nfccardmanager/feature/lock/LockRiskScreen.kt` 与解锁页面 `app/src/main/java/com/opencode/nfccardmanager/feature/unlock/UnlockVerifyScreen.kt` 都先收集人工确认字段，再启动 NFC 扫描。
2. 两者都先使用 `TagParser.kt` 识别卡片能力。
3. 锁卡根据 `CardCapability.lockMode` 在 `NdefLocker.kt` 与 `PasswordProtectedLocker.kt` 间分支；解锁统一走 `UnlockExecutor.kt`。
4. 结果进入各自 ViewModel，并通过 `AuditLogManager.kt` 落到本地 SQLite。

**State Management:**
- 页面状态统一采用 `MutableStateFlow` + `asStateFlow()`，典型文件见 `feature/scan/ScanViewModel.kt`、`feature/write/WriteViewModel.kt`、`feature/audit/AuditLogViewModel.kt`。
- 一次性导航副作用仅在 `feature/scan/ScanViewModel.kt` 使用 `MutableSharedFlow<ScanUiEffect>`；其余页面多数直接由屏幕参数回调处理。
- 跨页面临时共享数据使用进程内单例缓存 `feature/read/ReadResultStore.kt`，不是 SavedStateHandle。

## Key Abstractions

**Route Registry:**
- Purpose: 集中管理页面路由字符串与动态参数编码。
- Examples: `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- Pattern: `object Routes` + `fun buildRoute(...)`

**Screen + ViewModel + Contract:**
- Purpose: 把 UI、状态定义、状态流转分离到同一 feature 包内。
- Examples: `feature/scan/ScanScreen.kt` + `feature/scan/ScanViewModel.kt` + `feature/scan/ScanContract.kt`；`feature/lock/LockRiskScreen.kt` + `feature/lock/LockViewModel.kt` + `feature/lock/LockContract.kt`
- Pattern: Screen 订阅 `uiState`，ViewModel 只暴露事件函数，Contract 定义 Stage/UIState。

**Card Domain Models:**
- Purpose: 在页面间传递统一的卡片信息、能力与操作结果。
- Examples: `app/src/main/java/com/opencode/nfccardmanager/core/nfc/model/CardModels.kt`、`ReadModels.kt`、`WriteModels.kt`
- Pattern: `data class` + enum 状态码。

**Session / Role Policy:**
- Purpose: 登录态恢复、角色切换、页面级权限判断。
- Examples: `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt`、`SessionStore.kt`
- Pattern: `object` + `StateFlow` + `SharedPreferences`

**Local Repository Objects:**
- Purpose: 把 SQLite/本地列表对上层隐藏成简单 API。
- Examples: `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogRepository.kt`、`feature/template/LocalTemplateRepository.kt`
- Pattern: `Repository` 包装 `SQLiteOpenHelper`；模板仓库额外暴露 `StateFlow<List<WriteTemplate>>`

**NFC Operation Executors:**
- Purpose: 把具体标签操作从页面中拆出。
- Examples: `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt`、`NdefWriter.kt`、`NdefFormatter.kt`、`NdefLocker.kt`、`UnlockExecutor.kt`
- Pattern: 无状态类 + 同步执行方法；页面负责 ReaderMode 生命周期，执行器负责业务结果对象。

## Entry Points

**Application:**
- Location: `app/src/main/java/com/opencode/nfccardmanager/NfcCardManagerApp.kt`
- Triggers: 进程创建。
- Responsibilities: 初始化审计日志仓库、登录态仓库、模板仓库。

**Main Activity:**
- Location: `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`
- Triggers: Launcher Activity。
- Responsibilities: 注入主题、承载 Compose、进入 `AppNavGraph()`。

**Navigation Graph:**
- Location: `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- Triggers: Compose 根内容创建。
- Responsibilities: 路由分发、登录态重定向、底部导航、权限拦截。

**NFC Reader Sessions:**
- Location: `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`
- Triggers: `ScanScreen.kt`、`WriteEditorScreen.kt`、`FormatCardScreen.kt`、`LockRiskScreen.kt`、`UnlockVerifyScreen.kt`
- Responsibilities: `enableReaderMode` / `disableReaderMode` 生命周期管理。

## Error Handling

**Strategy:** 页面层将大多数异常转为可展示文案，核心层返回结果对象或 `Result.failure(...)`，未引入统一异常总线。

**Patterns:**
- ReaderMode 启动失败统一在页面 `onFailure { viewModel.onError(...) }` 中处理，例见 `feature/format/FormatCardScreen.kt`、`feature/unlock/UnlockVerifyScreen.kt`。
- NFC 执行类将底层异常包装成人类可读文本，例见 `core/nfc/NdefWriter.kt` 的 `buildExceptionDetail()`。
- ViewModel 在错误分支通常同时更新 Stage 与 message，并写审计日志，例见 `feature/write/WriteViewModel.kt`、`feature/lock/LockViewModel.kt`。
- 访问控制不是抛异常，而是在 `navigation/AppNavGraph.kt` 中直接渲染 `feature/common/PermissionDeniedScreen.kt`。

## Cross-Cutting Concerns

**Logging:**
- 业务审计统一走 `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogManager.kt`，数据最终写入 `AuditLogDbHelper.kt` 的本地 SQLite。
- 系统调试日志仅在 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt` 使用 `android.util.Log.w(...)`。

**Validation:**
- 登录校验集中在 `core/security/SecurityManager.kt`。
- 表单前置校验分散在各 ViewModel，如 `feature/write/WriteViewModel.kt` 检查内容非空，`feature/lock/LockViewModel.kt` 检查确认词与勾选状态。
- NFC 标签可操作性校验主要在 `core/nfc/NdefWriter.kt` 的 `precheck()`。

**Authentication:**
- 当前是本地演示型认证：`SecurityManager.kt` 内置账号与固定密码，并通过 `SessionStore.kt` 记住登录态。
- 路由授权同样集中在 `SecurityManager.kt` 的 `canRead/canWrite/canLock/...`，由 `AppNavGraph.kt` 在进入页面前执行。

**Android Component Relationships:**
- `AndroidManifest.xml` 仅声明一个 `Application` 与一个 `Activity`；其余业务都在 Compose 页面内完成，没有 `Fragment`、`Service`、`BroadcastReceiver`。
- 需要 Activity 的 NFC 页面通过 `core/common/ContextExt.kt` 的 `findActivity()` 从 Compose `LocalContext` 反查宿主 Activity。

**推断（基于代码证据）:**
- 由于 `domain/usecase` 为空，而所有 `feature/*Screen.kt` 直接 new `NfcSessionManager`、`TagParser`、`NdefWriter` 等，可推断当前架构更接近“页面直接编排 core 服务”，而非严格 Clean Architecture。
- 由于没有 DI 框架或 Service Locator，且页面内部通过 `remember { ClassName() }` 创建执行器，可推断共享能力替换与单元测试注入成本较高。

---

*Architecture analysis: 2026-03-30*
