# Technology Stack Recommendation

**Project:** Android NFC Card Manager  
**Scope:** Android NFC 管理工具 App 的 brownfield 栈补强建议（功能优化 + UI 升级）  
**Researched:** 2026-03-31  
**Overall confidence:** HIGH

## 1. 当前项目证据

基于已读文件，当前项目已经具备一条正确且稳妥的主线，不应推倒重来：

- `app/build.gradle.kts`：Kotlin Android + Compose + Material3 + Navigation Compose，`minSdk = 26`，`targetSdk/compileSdk = 35`
- `.planning/codebase/STACK.md`：当前依赖为 Compose BOM `2024.09.00`、`activity-compose 1.9.1`、`navigation-compose 2.7.7`、`lifecycle-* 2.8.4`
- `.planning/codebase/ARCHITECTURE.md`：当前是单模块、`Screen + ViewModel + Contract`、`MutableStateFlow` 驱动 UI、无 DI 框架、NFC 能力集中在 `core/nfc`
- `.planning/PROJECT.md`：本轮目标明确是“保留既有 NFC 流程，渐进优化 UI 体验、信息层级和状态反馈”，不是重写底层系统

结论：**继续沿用 Android + Kotlin + Compose + 本地 NFC API 是正确方向；本轮只做稳妥补强，不做架构翻修。**

## 2. 建议继续沿用的栈

| 类别 | 继续沿用 | 采用理由 | 置信度 |
|---|---|---|---|
| 语言与平台 | Kotlin + Android SDK 35 + Java 17 | 与现有代码完全一致，Android 官方主线，适合持续维护 | HIGH |
| UI 框架 | Jetpack Compose | 当前全站已在 Compose 上，UI 升级成本最低，官方主推 | HIGH |
| 设计体系基础 | Material 3 | 官方 Compose 设计系统主线，适合做统一组件、风险态、状态卡片与视觉层级 | HIGH |
| 页面状态 | ViewModel + StateFlow + `collectAsStateWithLifecycle()` | 已是当前代码主流写法，也符合官方 state holder 建议 | HIGH |
| 导航 | Navigation Compose | 当前已使用，适合渐进升级，不必更换导航范式 | HIGH |
| NFC 能力 | Android 原生 NFC API + 现有 `core/nfc/*` | 这是项目核心能力，且当前需求不是重写底层 | HIGH |
| 本地存储 | 现有 SQLiteOpenHelper / SharedPreferences 方案 | 本轮重点不是数据层重构；只要没有明确痛点，不必引入 Room/DataStore 迁移 | MEDIUM |
| 测试基线 | JUnit + AndroidX Test + Compose UI Test | 当前已接入，补强即可，不必换整套测试体系 | HIGH |

## 3. 必要补强项（只列建议新增或升级的最小集合）

### 3.1 Compose 设计系统：补“令牌层”和“页面骨架层”，不换框架

**建议：继续用 Material 3，但把现有 `ui/theme` 和 `ui/component` 提升成显式设计系统。**

最小补强内容：

1. **升级 Compose 依赖到 2025-2026 稳定线**  
   现有 Compose BOM `2024.09.00` 偏旧；建议升级到与当前 Kotlin / AGP 兼容的**最新稳定 Compose BOM**，并同步对齐 Material3 稳定版。

2. **建立 Design Tokens**  
   新增统一的：
   - `AppColorScheme`（尤其是 success / warning / danger / info）
   - `AppSpacing`
   - `AppRadius`
   - `AppTypography`
   - `AppElevation`

3. **建立页面级骨架组件**  
   对工业/运维/工具型 App，比“炫”更重要的是“稳”和“可读”。建议抽出：
   - `AppScreenScaffold`
   - `SectionCard`
   - `StatusBanner`
   - `RiskNoticeCard`
   - `StepIndicator`
   - `ResultPanel`
   - `EmptyState` / `ErrorState`

4. **动态色（dynamic color）谨慎使用**  
   可以支持，但**默认不应主导高风险操作色语义**。锁卡/解锁/格式化这类页面的 danger/warning 色应保持固定且可预测。

5. **若目标设备包含平板/横屏设备，再补 `material3-adaptive`**  
   这是 2025-2026 很稳妥的补强方向，适合做首页双栏、日志/模板列表详情、设置分栏；如果现场主要是手机，则先不引入。

**建议依赖：**

```kotlin
implementation(platform("androidx.compose:compose-bom:<latest-stable>"))
implementation("androidx.compose.material3:material3")
// 仅当确实需要大屏/分栏时再加
implementation("androidx.compose.material3.adaptive:adaptive:1.2.0")
```

**采用理由：** 官方 Material 3 for Compose 已明确覆盖 color / typography / shape 主题体系；industrial 工具类产品最该做的是统一信息密度、风险语义和状态反馈，而不是换 UI 框架。  
**置信度： HIGH**

### 3.2 导航：继续 Navigation Compose，但升级到 typed routes 思路

当前项目已经有 `AppNavGraph.kt` 和集中式路由注册，这很好，**不建议换 Voyager / Decompose / 自研路由系统**。

**建议补强：**

1. **升级 `navigation-compose` 到 2.9.x 稳定线**（官方 2026-01 仍在持续维护）
2. **从字符串拼路由，渐进迁移到 type-safe routes**
3. **路由只传最小参数（ID / mode / enum），不要传复杂对象**
4. **跨页面临时数据优先迁到 `SavedStateHandle` 或单一数据源，而不是进程内单例缓存**

**建议依赖：**

```kotlin
implementation("androidx.navigation:navigation-compose:2.9.7")
```

**采用理由：** Android 官方文档明确建议导航时只传最小必要信息，并在目标 ViewModel 中通过 `SavedStateHandle` 取参；这比继续放大 `ReadResultStore` 这类进程内缓存更稳。  
**置信度： HIGH**

### 3.3 状态管理：保留 ViewModel + StateFlow，不引入 Redux/MVI 框架

当前项目的 `Screen + ViewModel + Contract + StateFlow` 已经足够支撑本轮 UI 优化。

**建议补强：**

1. **升级 Lifecycle Compose 相关依赖到 2.10.0 稳定线**
2. **给关键页面补 `SavedStateHandle`**：如扫描模式、筛选条件、当前阶段、回跳恢复
3. **统一状态分层**：
   - `ScreenUiState`：页面可渲染数据
   - `UiEvent`：用户动作
   - `UiEffect`：一次性导航/Toast/震动
4. **局部交互状态用 plain state holder / `rememberSaveable`，不要全部塞进 ViewModel**

**建议依赖：**

```kotlin
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
```

**采用理由：** 官方 state holder 文档明确区分 screen UI state 与 UI element state；当前项目只需要把这套边界补清楚，不需要再引入 Orbit / Mavericks / Redux。  
**置信度： HIGH**

### 3.4 测试与质量：补齐“可回归”能力，比引入新架构更值

这是本轮最值得新增的工程补强。

**建议补强：**

1. **升级 AndroidX Test 栈到稳定线**
2. **补 `kotlinx-coroutines-test`**，覆盖 ViewModel / Flow / dispatcher 切换
3. **把 Compose UI Test 从“有依赖”提升到“有用例”**，优先覆盖：
   - 首页入口层级
   - 扫描中 / 成功 / 失败状态切换
   - 锁卡/解锁的风险确认与禁用态
   - 模板/日志筛选与空态
4. **给关键 Compose 节点补稳定语义标签**（Semantics / `testTag`）
5. **补 Baseline Profile（可选但推荐）**：对启动、首页、扫描页进入速度有直接收益，属于低侵入质量补强

**建议依赖：**

```kotlin
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

androidTestImplementation("androidx.test:core-ktx:1.7.0")
androidTestImplementation("androidx.test.ext:junit:1.3.0")
androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

**采用理由：** 当前项目的问题主要在 UI 清晰度和流程反馈，最需要的是“改 UI 后不回归”；Compose Testing、AndroidX Test 和 coroutines-test 都是标准、稳妥、低风险补强。  
**置信度： HIGH**

### 3.5 Activity / Edge-to-edge：做小升级，不做视觉重构

建议顺手把 `activity-compose` 从 `1.9.1` 升到 `1.13.0` 稳定线，并补规范的 edge-to-edge / insets 处理。  
这会直接改善顶部栏、底部导航、系统栏遮挡等 UI 细节问题，收益高，改动小。

```kotlin
implementation("androidx.activity:activity-compose:1.13.0")
```

**置信度： HIGH**

## 4. 明确不建议引入的库或方向

| 不建议项 | 原因 |
|---|---|
| Hilt / Koin 作为本轮前置改造 | 当前是单模块、无复杂对象图；本轮目标是 UI/UX 优化，不值得先做 DI 改造 |
| Redux / MVI 框架（Orbit、Mavericks、Mobius 等） | 现有 `ViewModel + StateFlow` 足够；再引框架只会增加迁移成本 |
| 更换导航体系（Voyager、Decompose、自研路由） | 现有 Navigation Compose 已满足需求，换栈收益远小于风险 |
| Room / DataStore 全量迁移 | 当前重点不是本地数据层重构；除非后续需求明确触发，否则先不动 |
| 多模块拆分 | 这是典型 greenfield/中后期治理动作，不适合作为当前里程碑前置项 |
| KMP / Compose Multiplatform | 与现有 Android 本地 NFC 能力和项目目标无关，收益极低 |
| 追新使用大量 alpha / experimental UI 库 | 工业工具类 App 更重稳定和可预测，不应为视觉升级引入实验性依赖 |
| 为 UI 升级引入远程后端/BFF/在线配置系统 | `PROJECT.md` 已明确 offline-first、本轮不做服务化改造 |
| 大型图片加载/动画框架作为先决条件 | 本项目不是内容消费型产品，UI 提升主要靠信息架构与状态组件，不靠重视觉特效 |

## 5. 推荐的最终补强组合（brownfield 最小可行版）

**建议继续沿用：**

- Kotlin
- Android SDK / 原生 NFC API
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel + StateFlow
- 现有本地 SQLite / SharedPreferences

**建议新增或升级：**

1. 升级 Compose BOM 到最新稳定线
2. 升级 `activity-compose` 到 `1.13.0`
3. 升级 `navigation-compose` 到 `2.9.7`
4. 升级 `lifecycle-*` 到 `2.10.0`
5. 补 `kotlinx-coroutines-test`
6. 补 AndroidX Test 稳定线依赖
7. 仅在存在平板/横屏诉求时补 `material3-adaptive:1.2.0`
8. 在代码层建立 Design Tokens + Screen Scaffold + Risk/Status 组件体系

## 6. 采用建议顺序

1. **先升级依赖到稳定线**（Compose / Activity / Lifecycle / Navigation / Test）
2. **再收敛设计系统**（tokens + 状态组件 + 风险组件）
3. **再整理导航与状态边界**（typed route / SavedStateHandle / effect）
4. **最后补测试与 Baseline Profile**

这个顺序最符合 brownfield：**先稳依赖，再统一 UI，再清状态，最后固化质量。**

## 7. Sources

- 当前项目证据：
  - `/Users/wangzhe/Desktop/opencode/android-nfc-card-manager/.planning/PROJECT.md`
  - `/Users/wangzhe/Desktop/opencode/android-nfc-card-manager/.planning/codebase/STACK.md`
  - `/Users/wangzhe/Desktop/opencode/android-nfc-card-manager/.planning/codebase/ARCHITECTURE.md`
  - `/Users/wangzhe/Desktop/opencode/android-nfc-card-manager/app/build.gradle.kts`
- Android 官方：
  - Material 3 in Compose: https://developer.android.com/develop/ui/compose/designsystems/material3
  - Navigation Compose: https://developer.android.com/develop/ui/compose/navigation
  - State holders and UI state: https://developer.android.com/topic/architecture/ui-layer/stateholders
  - Compose testing: https://developer.android.com/develop/ui/compose/testing
  - Baseline Profiles overview: https://developer.android.com/topic/performance/baselineprofiles/overview
  - AndroidX Activity releases: https://developer.android.com/jetpack/androidx/releases/activity
  - AndroidX Lifecycle releases: https://developer.android.com/jetpack/androidx/releases/lifecycle
  - AndroidX Navigation releases: https://developer.android.com/jetpack/androidx/releases/navigation
  - AndroidX Test releases: https://developer.android.com/jetpack/androidx/releases/test
- Kotlin 官方：
  - kotlinx-coroutines-test: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/
