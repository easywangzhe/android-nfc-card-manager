# Codebase Structure

**Analysis Date:** 2026-03-30

## Directory Layout

```text
android-nfc-card-manager/
├── app/                                  # Android 应用模块
│   ├── build.gradle.kts                  # app 模块构建配置
│   └── src/main/
│       ├── AndroidManifest.xml           # 应用组件与 NFC 权限声明
│       ├── java/com/opencode/nfccardmanager/
│       │   ├── core/                     # 共享底层能力
│       │   ├── domain/                   # 预留领域层（当前基本为空）
│       │   ├── feature/                  # 按业务场景拆分的页面模块
│       │   ├── navigation/               # NavHost 与路由常量
│       │   ├── ui/                       # 主题与可复用 Compose 组件
│       │   ├── MainActivity.kt           # Activity 入口
│       │   └── NfcCardManagerApp.kt      # Application 入口
│       └── res/values/                   # 字符串、主题等资源
├── docs/                                 # 产品、权限、原型、方案文档
├── .github/workflows/                    # CI 工作流
├── .planning/codebase/                   # 代码库分析文档输出目录
├── build.gradle.kts                      # 根 Gradle 配置
├── settings.gradle.kts                   # Gradle 模块声明
├── gradle.properties                     # Gradle 全局属性
├── local.properties.example              # 本地 SDK 配置示例
└── README.md                             # 项目说明与运行文档
```

## Directory Purposes

**`app/`:**
- Purpose: 唯一 Android 应用模块。
- Contains: `build.gradle.kts`、`src/main`、构建产物目录 `build/`。
- Key files: `app/build.gradle.kts`、`app/src/main/AndroidManifest.xml`

**`app/src/main/java/com/opencode/nfccardmanager/core/`:**
- Purpose: 可跨 feature 复用的底层能力。
- Contains: NFC 操作类、本地数据库、登录与角色、Context 扩展。
- Key files: `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`、`core/nfc/TagParser.kt`、`core/database/AuditLogManager.kt`、`core/security/SecurityManager.kt`

**`app/src/main/java/com/opencode/nfccardmanager/domain/`:**
- Purpose: 预留领域层。
- Contains: `domain/usecase/ReadCardUseCase.kt`、`WriteCardUseCase.kt`、`LockCardUseCase.kt`、`UnlockCardUseCase.kt`
- Key files: `app/src/main/java/com/opencode/nfccardmanager/domain/usecase/ReadCardUseCase.kt`
- Note: 当前文件仅为空类；新增真实业务编排时，优先考虑补齐这一层，而不是继续把流程堆进 Screen。

**`app/src/main/java/com/opencode/nfccardmanager/feature/`:**
- Purpose: 按页面/场景组织业务模块。
- Contains: `audit/`、`auth/`、`common/`、`format/`、`home/`、`lock/`、`read/`、`scan/`、`settings/`、`template/`、`unlock/`、`write/`
- Key files: `feature/home/HomeScreen.kt`、`feature/scan/ScanViewModel.kt`、`feature/write/WriteEditorScreen.kt`

**`app/src/main/java/com/opencode/nfccardmanager/navigation/`:**
- Purpose: 统一定义路由与 Compose Navigation 图。
- Contains: `AppNavGraph.kt`
- Key files: `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`

**`app/src/main/java/com/opencode/nfccardmanager/ui/`:**
- Purpose: 共享 UI 基建。
- Contains: `component/` 通用组件、`theme/` 颜色/排版/主题。
- Key files: `app/src/main/java/com/opencode/nfccardmanager/ui/component/AppUi.kt`、`ui/theme/Theme.kt`

**`app/src/main/res/`:**
- Purpose: Android 资源目录。
- Contains: 当前仅有 `values/`。
- Key files: `app/src/main/res/values/strings.xml`、`app/src/main/res/values/themes.xml`

**`docs/`:**
- Purpose: 产品与方案文档，不参与运行时逻辑。
- Contains: `docs/PRD.md`、`docs/技术方案文档.md`、`docs/权限矩阵文档.md` 等。
- Key files: `docs/PRD.md`

## Key File Locations

**Entry Points:**
- `app/src/main/AndroidManifest.xml`: 注册 `NfcCardManagerApp`、`MainActivity`、NFC 权限与特性。
- `app/src/main/java/com/opencode/nfccardmanager/NfcCardManagerApp.kt`: 初始化全局仓库。
- `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`: Compose Activity 入口。
- `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`: 应用级导航入口。

**Configuration:**
- `settings.gradle.kts`: 声明唯一模块 `:app`。
- `build.gradle.kts`: 根工程配置。
- `app/build.gradle.kts`: Android SDK、Compose、依赖、测试运行器。
- `gradle.properties`: Gradle 属性。
- `local.properties.example`: 本地 Android SDK 路径模板。

**Core Logic:**
- `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt`: 标签识别与读卡结果聚合。
- `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NdefWriter.kt`: NDEF 预检、写入、回读校验。
- `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NdefFormatter.kt`: NDEF 格式化与清空。
- `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NdefLocker.kt`: 永久只读锁卡。
- `app/src/main/java/com/opencode/nfccardmanager/core/nfc/UnlockExecutor.kt`: 解锁流程骨架。
- `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt`: 登录与权限。
- `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogManager.kt`: 审计入口。

**Testing:**
- `app/src/test/`: 未检测到实际测试文件。
- `app/src/androidTest/`: 未检测到实际测试文件。
- `app/build.gradle.kts`: 已声明 `testInstrumentationRunner` 与 JUnit / Espresso / Compose Test 依赖。

## Naming Conventions

**Files:**
- `*Screen.kt`: Compose 页面组件，例如 `app/src/main/java/com/opencode/nfccardmanager/feature/read/ReadResultScreen.kt`
- `*ViewModel.kt`: 页面状态控制器，例如 `app/src/main/java/com/opencode/nfccardmanager/feature/unlock/UnlockViewModel.kt`
- `*Contract.kt`: Stage / UiState 定义，例如 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanContract.kt`
- `*Manager.kt`: 全局对象或高层服务，例如 `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogManager.kt`
- `*Repository.kt`: 数据访问包装，例如 `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogRepository.kt`
- `*DbHelper.kt`: SQLiteOpenHelper 封装，例如 `app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateDbHelper.kt`
- `*Models.kt`: 一组数据模型，例如 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/model/CardModels.kt`

**Directories:**
- feature 目录全部使用小写业务名：`feature/scan/`、`feature/write/`、`feature/audit/`
- core 子目录按技术职责命名：`core/nfc/`、`core/security/`、`core/database/`、`core/common/`
- UI 基建按“组件 / 主题”拆分：`ui/component/`、`ui/theme/`

## Where to Add New Code

**New Feature:**
- Primary code: 在 `app/src/main/java/com/opencode/nfccardmanager/feature/<feature-name>/` 新建独立目录。
- Required files: 至少对齐现有模式增加 `*Screen.kt`；如涉及状态机，再补 `*ViewModel.kt` 和 `*Contract.kt`。
- Navigation: 在 `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt` 增加 `Routes` 常量和 `composable(...)`。
- Permissions: 如有权限差异，在 `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt` 增加 `canXxx()` 规则，并在 `AppNavGraph.kt` 做路由守卫。

**New NFC Operation / Business Executor:**
- Implementation: 放入 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/`
- Models: 放入 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/model/`
- Guidance: 保持“页面负责 ReaderMode 生命周期，执行器负责 Tag 业务处理”的当前边界。

**New Local Data Source:**
- SQLite / persistence helper: 放入 `app/src/main/java/com/opencode/nfccardmanager/core/database/`，若只服务单 feature，也可像模板一样放在对应 feature 目录，例如 `feature/template/TemplateDbHelper.kt`。
- Repository facade: 优先新增 `*Repository.kt` 或 `object` 仓库，避免 Screen 直接操作 `SQLiteOpenHelper`。

**New Shared UI Component:**
- Implementation: `app/src/main/java/com/opencode/nfccardmanager/ui/component/`
- Theme tokens: `app/src/main/java/com/opencode/nfccardmanager/ui/theme/`
- Guidance: 若多个页面复用顶部栏、状态胶囊、按钮样式，应先抽到 `AppUi.kt` 同级，而不是复制到 feature 包。

**Utilities:**
- Shared helpers: `app/src/main/java/com/opencode/nfccardmanager/core/common/`
- Current example: `app/src/main/java/com/opencode/nfccardmanager/core/common/ContextExt.kt`

**New Tests:**
- Unit tests: 建议新增到 `app/src/test/java/com/opencode/nfccardmanager/...`
- Instrumentation / UI tests: 建议新增到 `app/src/androidTest/java/com/opencode/nfccardmanager/...`
- 推断依据: `app/build.gradle.kts` 已声明标准 Android Test 依赖，但仓库中尚未有对应目录内容。

## Special Directories

**`app/build/`:**
- Purpose: Gradle 构建产物，例如 `app/build/outputs/apk/debug/app-debug.apk`
- Generated: Yes
- Committed: No（推断；该目录属于典型构建产物）

**`.github/workflows/`:**
- Purpose: CI 自动构建配置。
- Generated: No
- Committed: Yes

**`.planning/codebase/`:**
- Purpose: 供后续规划/执行阶段读取的代码地图文档。
- Generated: Yes
- Committed: Yes（推断；目录用于保留分析结果）

**`docs/`:**
- Purpose: 项目文档归档。
- Generated: No
- Committed: Yes

## Module Boundaries and Placement Rules

**Android 组件边界：**
- `MainActivity.kt` 是唯一 Activity；不要新增业务 Fragment，除非要重构整体宿主方式。
- `NfcCardManagerApp.kt` 只做初始化；不要把页面业务逻辑继续塞入 Application。

**Feature 边界：**
- 一个 feature 目录负责一个用户场景；例如读卡详情归 `feature/read/`，模板管理归 `feature/template/`。
- 页面之间共享结果时，当前做法是通过单例仓库，如 `feature/read/ReadResultStore.kt`；新增临时跨页数据时，优先沿用现有方式，除非同步引入更强状态容器。

**UI 与业务逻辑组织方式：**
- UI 代码保留在 `*Screen.kt`。
- 状态推进和用户事件放在 `*ViewModel.kt`。
- 可复用的 NFC/安全/存储逻辑放在 `core/`。
- 当前不建议把 Android `Tag` 操作直接写进 ViewModel；已有实现都在 Screen 中拿到 Tag 后调用 `core/nfc/*`。

**推断（基于代码证据）:**
- 由于 `feature/*Screen.kt` 普遍直接 `remember { TagParser() }`、`remember { NdefWriter() }`，新功能若继续沿用当前风格，应把可无状态复用类放到 `core/` 并由页面直接创建。
- 由于 `domain/usecase` 尚未真正接管流程，若只是做小范围功能增量，落位到 `feature/` + `core/` 更符合当前代码库；若做架构升级，再补 `domain/` 更合适。

---

*Structure analysis: 2026-03-30*
