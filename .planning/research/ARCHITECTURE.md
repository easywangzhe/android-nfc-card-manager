# Architecture Patterns

**Domain:** Android NFC 卡片管理应用的 brownfield UI / 功能优化
**Researched:** 2026-03-31

## Recommended Architecture

结论：**保留单模块与现有 `feature + Screen/ViewModel/Contract + core` 骨架，不做一次性 Clean Architecture 重写；先补“壳层边界”和“流程边界”，再逐页迁移。**

当前代码库的问题不是“层次完全错误”，而是**页面直接编排过多 core 能力、状态表达不统一、导航承担了过多权限与流程判断、跨页数据用进程内单例缓存**。这类问题适合做**渐进式架构演进**，而不是模块拆分或全量 DI 改造。

推荐把后续演进收敛到 4 条边界：

1. **App Shell 边界**：导航、权限守卫、底栏/顶栏、全局反馈只留在壳层。
2. **Design System 边界**：页面容器、状态卡、风险卡、表单区块、CTA 区块统一抽到 UI 层。
3. **Flow Coordinator 边界**：读卡/写卡/格式化/锁卡/解锁这类流程页，逐步从“Screen 直接编排”收敛为“ViewModel 驱动流程状态，Screen 只处理渲染与 Activity/NFC 生命周期”。
4. **Shared Operation State 边界**：跨页结果与临时会话不再继续散落为 ad-hoc 单例，统一收敛到可替换的 session/result store，优先解决读卡结果、写卡结果、风险操作确认链路。

**不建议本轮做的事：**
- 不建议先拆多模块。
- 不建议先引入完整 DI 框架。
- 不建议先补全 `domain/usecase` 再改 UI。
- 不建议一次性重写所有 Screen。

推荐目标形态：

```text
MainActivity
  -> AppShell
      -> AppNavGraph
          -> Route Guards / Route Metadata
          -> Feature Screens

Feature Screen
  -> ViewModel (唯一状态源)
  -> Flow Reducer / Presenter helper（可先在 feature 内部落地）
  -> core/nfc + core/security + repository

ui/designsystem
  -> PageScaffold / SectionCard / StatusBanner / RiskPanel / ActionBar

shared/flow
  -> OperationSessionStore / result passing / flow event contract
```

### Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| AppShell / AppNavGraph | 登录态监听、路由注册、权限守卫、底栏显示规则 | SecurityManager、各 feature Screen |
| Route Metadata / Guard helper | 把“是否显示底栏、需要何种权限、是否高风险页”从 `when/if` 逻辑抽离 | AppNavGraph、SecurityManager |
| Design System | 统一页面骨架、风险区块、状态区块、按钮层级、空态/错态 | 各 feature Screen |
| Feature ViewModel | 唯一 UI 状态源、事件处理、流程阶段推进、错误文案映射 | core、repository、session store |
| Screen | 收集状态、渲染 UI、绑定 NFC 生命周期与导航回调 | ViewModel、Design System、Activity/NfcSessionManager |
| Flow Coordinator helper | 抽取读/写/格式化/锁/解锁共通流程阶段与状态映射 | ViewModel、shared/flow |
| OperationSessionStore | 保存跨页临时结果与阶段性上下文，替代松散 object 缓存 | ViewModel、后续结果页 |
| core/nfc | Tag 解析、预检、写入、格式化、锁定、解锁执行 | Feature ViewModel / Screen |

## Data Flow

### 推荐的数据/状态分层

**第一优先级要改的不是 NFC 核心，而是状态流。** 当前大多数页已有 `StateFlow` 基础，但流程型页面仍存在“ViewModel 管一部分、Screen 管一部分、导航再管一部分”的分散编排。推荐改为：

1. **ViewModel 持有完整流程状态**：`stage`、`ctaState`、`riskState`、`hintMessage`、`resultSummary`、`isBusy`。
2. **Screen 只持有纯 UI 局部态**：输入焦点、展开收起、一次性弹窗显隐。
3. **导航层不再判断业务阶段**：导航只判断“能不能进”，不判断“该显示哪一步”。
4. **跨页结果统一出口**：读卡结果、格式化后去写卡、锁/解锁结果页都通过统一 result/session 机制传递。

### 推荐的流程页状态模型

对读卡、写卡、格式化、锁卡、解锁统一成一套心智模型：

```kotlin
data class OperationUiState(
    val stage: OperationStage,
    val status: StatusLevel,
    val primaryAction: ActionState,
    val secondaryAction: ActionState?,
    val guidance: String,
    val risk: RiskUiModel?,
    val result: OperationResultUiModel?,
    val error: String?,
)
```

其中 `OperationStage` 建议至少统一为：

```text
Idle -> Editing/Preparing -> AwaitingTag -> Processing -> Success / Failure
```

高风险流再加：

```text
RiskAcknowledge -> CredentialVerify -> AwaitingTag -> Processing -> Success / Failure
```

这不是要做通用抽象库，而是要**先统一页面状态语义**，这样 UI 重构才不会一页一个表达体系。

## Patterns to Follow

### Pattern 1: 先壳层、后页面
**What:** 先把导航、权限、页面骨架、设计 tokens 收口，再进入业务页改造。  
**When:** brownfield 项目且页面多、风险页多时。  
**Why:** 如果先逐页重写，而壳层规则未定，后面会反复返工页面结构与导航行为。

### Pattern 2: Route Metadata 驱动导航
**What:** 给每个路由补元数据，而不是在 `AppNavGraph` 里散落权限/底栏/风险判断。  
**When:** 当前 `AppNavGraph` 已成为权限和页面装配中心时。  
**Example:**

```kotlin
data class RouteSpec(
    val route: String,
    val showBottomBar: Boolean = false,
    val requiredPermission: ((Role?) -> Boolean)? = null,
    val riskLevel: RiskLevel = RiskLevel.Normal,
)
```

这样首页、模板、日志、设置属于 shell tab；读写锁解锁属于 task flow route；权限判断从大段 if/else 变成统一 guard。

### Pattern 3: Screen 负责生命周期，ViewModel 负责流程
**What:** `NfcSessionManager` 仍可留在 Screen 层绑定 `DisposableEffect`，但“扫描到了之后怎么推进步骤”必须回到 ViewModel。  
**When:** 当前页面直接 new parser/writer/formatter 并夹杂协程编排时。  
**Example:**

```kotlin
DisposableEffect(Unit) {
    sessionManager.enableReaderMode(
        onTagDiscovered = { tag -> viewModel.onTagDetected(tag) }
    )
    onDispose { sessionManager.disableReaderMode() }
}
```

然后由 ViewModel 决定：预检、处理中、成功失败、按钮文案、是否允许下一步。

### Pattern 4: 高频页先统一状态组件，再做视觉精修
**What:** 先建立 `StatusBanner / StepIndicator / ActionFooter / RiskPanel / ResultCard`，再改读写流程页。  
**When:** 当前问题主要是信息层级乱、状态不清晰。  
**Why:** 这会直接决定后续所有页的布局语言，是前置 phase，不是收尾 phase。

### Pattern 5: 跨页结果从单例 object 迁移到可控 session store
**What:** 读卡结果页、格式化后跳写卡、风险操作结果等，不要继续扩散新的 `object XxxStore`。  
**When:** 需要跨页保存临时结果、又不想马上做大规模导航重构时。  
**Instead:** 先引入轻量 `OperationSessionStore`，后续再视情况迁到 `SavedStateHandle` / route result。

## Anti-Patterns to Avoid

### Anti-Pattern 1: 为了“架构正确”先拆模块
**What:** 在 UI 重构前先拆 `app/domain/data/designsystem` 多模块。  
**Why bad:** 当前主要痛点在流程表达与页面边界，不在编译单元；先拆模块会显著增加搬家成本与回归风险。  
**Instead:** 先在单模块内做包级边界：`ui/designsystem`、`navigation/spec`、`shared/flow`。

### Anti-Pattern 2: 每改一页就发明一套状态模型
**What:** 读卡页用 `message + result`，写卡页用 `stage + detail`，锁卡页再来一套 `confirmStep + processing`。  
**Why bad:** 设计系统无法复用，测试口径也不统一。  
**Instead:** 统一流程 stage、状态等级、CTA 结构。

### Anti-Pattern 3: 导航层继续承担业务流编排
**What:** 在 `AppNavGraph` 里不断追加“如果来自 A 则去 B，否则去 C”。  
**Why bad:** 导航文件会继续膨胀，并把流程知识固化在最难维护的地方。  
**Instead:** 流程跳转由 feature 输出明确 effect，导航只执行 route。

### Anti-Pattern 4: 先改高风险页视觉，再补状态语义
**What:** 先给锁卡/解锁换样式，但确认条件、阶段切换、失败恢复路径还散落在页面里。  
**Why bad:** 高风险页最怕“看起来更好看，但仍然更容易误操作”。  
**Instead:** 先统一风险确认模型，再做视觉升级。

## Recommended Build Order

这是本项目最重要的 roadmap 含义：**先搭地基，再动高频页，最后动高风险页与辅助页。**

### Phase 0: 前置治理层（必须先做）
**目的：** 给后续 UI/功能优化建立稳定边界。  
**应做：**
- 建立 route metadata / guard helper，收敛 `AppNavGraph` 中重复权限判断与底栏规则。
- 在 `ui/` 下补 design tokens 与基础容器：页面骨架、Section、状态提示、风险提示、底部操作区。
- 统一流程型页面的基础 `UiState` 语义：stage/status/action/result/risk。
- 定义轻量 `OperationSessionStore` 方案，优先覆盖读卡结果链路。

**这是前置 phase 的原因：** 不做这一步，后续每个页面都会各自实现一套布局、状态和跳转方式。

### Phase 1: 首页 / 导航壳层改造（优先）
**目的：** 先把信息架构理顺，让用户知道主任务、风险操作、管理入口分别在哪。  
**应做：**
- 首页重新分主任务区、风险操作区、管理入口区。
- 明确角色切换与权限反馈的位置与文案。
- 整理 bottom navigation 与二级任务页关系，减少“底栏页”和“流程页”混杂感。

**为什么先做：** 首页和导航是所有页面的入口。入口结构不稳定，后面各页优化会失去统一归属。

### Phase 2: 高频低风险流程页（读卡 → 写卡 → 格式化）
**目的：** 先跑通最常用的流程状态表达，再把模式复制到风险页。  
**建议顺序：**
- 先读卡：状态最单纯，适合定义扫描中/成功/失败/下一步。
- 再写卡：可在读卡状态模式基础上扩展预检、写入、回读校验。
- 再格式化：和写卡邻近，可复用大量状态组件。

**为什么先这三页：**
- 使用频率高，收益最大。
- 风险相对低，适合作为新状态模型与设计系统的试点。
- 能先验证 `OperationSessionStore`、结果卡片、步骤反馈是否足够稳定。

### Phase 3: 高风险流程页（锁卡 → 解锁）
**目的：** 在已有统一状态模型基础上，补强风险确认、阶段提示、失败恢复。  
**建议顺序：**
- 先锁卡：不可逆风险更高，确认与后果提示应最先严格化。
- 再解锁：流程更像“凭据校验 + 执行反馈”，可以复用锁卡的风险框架。

**为什么延后到 Phase 3：** 高风险页不是视觉优先，而是“风险信息结构”优先；应建立在前两阶段的组件与状态约束上。

### Phase 4: 管理辅助页（模板 / 日志 / 设置）
**目的：** 用前面沉淀下来的设计系统收尾，提升整体一致性。  
**应做：**
- 模板页统一列表、编辑、空态与 CTA。
- 日志页统一筛选、详情信息层级。
- 设置页统一账户信息、角色说明、设备/NFC 状态反馈。

**为什么可延后：** 这些页更多是信息展示和轻交互，不应阻塞主流程体验升级。

## 并行与延后建议

### 可并行

1. **Design System 基础组件** 与 **首页信息架构** 可以并行。  
2. **Route Metadata/Guard helper** 与 **OperationSessionStore** 可以并行。  
3. **模板 / 日志 / 设置的视觉整理** 可在 Phase 2 之后与锁卡/解锁并行，但不要先于主流程落地。

### 应延后

1. **DI 框架引入**：当前不是瓶颈，延后到测试或模块化需求明确时。  
2. **真实 domain/usecase 体系补全**：可以伴随重点流程局部引入，但不应作为 UI 重构前置。  
3. **多模块拆分**：除非后续编译、团队协作、可见性边界已成为痛点，否则延后。

## 数据 / 状态 / 导航：应先改哪里

### 数据层
- **先不动 core/nfc 执行器。** 这一轮优先保持 `TagParser`、`NdefWriter`、`NdefFormatter`、`NdefLocker`、`UnlockExecutor` 稳定。
- **先改跨页临时数据传递。** `ReadResultStore` 暴露出当前流程结果传递边界较弱，建议最先抽象为统一 session/result 机制。

### 状态层
- **第一优先级。** 给所有流程页统一 `stage/status/action/result/risk` 语义。
- 把“页面直接处理业务阶段”的逻辑逐步回收到 ViewModel。
- 为高风险页增加显式 `RiskAcknowledge` / `PrerequisiteSatisfied` 状态，不再只靠若干布尔值拼装。

### 导航层
- **第二优先级。** 先把 `AppNavGraph` 的重复权限判断、底栏显示规则、route spec 抽出来。
- 不要一开始就大改导航库或多 back stack；先降低 `AppNavGraph` 的认知复杂度。

## 风险页改造顺序

1. **首页风险入口表达先改**：先让“锁卡/解锁是风险操作”在入口层就被感知。  
2. **锁卡页先于解锁页**：锁卡不可逆性更高，最需要先明确风险摘要、确认条件、处理中不可返回状态、结果说明。  
3. **解锁页后改**：在锁卡页沉淀出的 RiskPanel、StepIndicator、ResultCard 基础上复用。  

## Scalability Considerations

| Concern | At 100 users | At 10K users | At 1M users |
|---------|--------------|--------------|-------------|
| 页面一致性 | 主要靠人工约束 | 若无 design system 会快速分裂 | 必须有稳定组件与状态规范 |
| 导航可维护性 | `AppNavGraph` 还能承受 | route 判断会持续膨胀 | 需要 route spec / guard 层 |
| 流程可测试性 | 手测可覆盖 | ViewModel 若不统一难回归 | 需要稳定状态机与 session 边界 |
| 高风险操作安全性 | 依赖页面文案 | 需要统一风险确认结构 | 必须有强约束的流程模板 |

## Build Order Implications for Roadmap

- **前置 phase 必须包含：** design system 基础件、route metadata/guard、统一流程状态语义、session/result 边界。
- **随后 phase 应先做：** 首页 + 读卡/写卡/格式化，因为它们决定主任务流的通用模式。
- **高风险页不应第一批直接大改：** 先让通用状态与交互模式稳定，再迁移锁卡/解锁。
- **辅助页适合后置或并行：** 模板、日志、设置可复用既有模式，不应抢占前置建设顺序。
- **整轮优化应按“壳层 → 高频流程 → 高风险流程 → 辅助页”推进，避免全量重写。**

## Sources

- 项目上下文：`.planning/PROJECT.md`（HIGH）
- 现有代码架构分析：`.planning/codebase/ARCHITECTURE.md`（HIGH）
- 现有编码与状态管理约定：`.planning/codebase/CONVENTIONS.md`（HIGH）
- 当前导航与权限实现：`app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`（HIGH）
