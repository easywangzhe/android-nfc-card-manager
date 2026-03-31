# Project Research Summary

**Project:** Android NFC Card Manager
**Domain:** brownfield Android NFC 卡片管理工具的 UI 与流程优化
**Researched:** 2026-03-31
**Confidence:** HIGH

## Executive Summary

这是一个以本地 NFC 操作为核心的 Android 工具型应用，不是 greenfield 新产品，也不是后端化改造项目。研究结论高度一致：当前正确方向是保留既有 Kotlin + Android SDK + Compose + 原生 NFC API 主线，在不重写底层执行能力的前提下，系统性优化首页信息架构、流程状态表达、高风险操作确认与结果反馈。专家做法不是先拆架构或换框架，而是先收口导航壳层、设计系统、流程状态模型与临时会话边界，再逐步改造页面。

推荐路线很明确：先解决“真假能力不清、权限边界松动、ReaderMode 会话失控、审计上下文不足”这些会直接损害可信度的问题，再做高频流程页和高风险流程页的 UI 提升。首页、读卡、写卡、格式化、锁卡、解锁、模板/日志/设置应按“壳层 → 高频流程 → 高风险流程 → 辅助页”推进，而不是并行大改或一次性重写。

本项目最大的风险不是技术栈老旧，而是 UI 优化过程中把安全边界和流程真实性做坏：把 demo 能力包装成成功、把自动扫描当效率、把权限判断削成仅入口可见性、把写入失败和验证失败混为一谈。规避方式同样明确：统一流程状态机、三层权限校验、显式真实性标记、单次 NFC 会话控制、审计字段补齐，并用 Compose UI Test + 真机生命周期回归把这些约束固化下来。

## Key Findings

### Recommended Stack

研究建议是“沿用主栈、只做最小必要补强”。现有 Kotlin、Android SDK 35、Jetpack Compose、Material 3、Navigation Compose、ViewModel + StateFlow、原生 NFC API 都应继续保留；本轮不建议引入 DI、MVI 框架、多模块、Room/DataStore 全量迁移或任何服务化改造。

真正值得做的是把依赖升级到稳定线，并补齐设计系统与测试基线：Compose BOM 升到最新稳定版，`activity-compose` 升到 `1.13.0`，`navigation-compose` 升到 `2.9.7`，`lifecycle-*` 升到 `2.10.0`，补 `kotlinx-coroutines-test`、AndroidX Test、Compose UI Test，并在代码中建立 design tokens、页面骨架、风险/状态组件体系。若目标设备明确包含平板或横屏，再评估 `material3-adaptive:1.2.0`。

**Core technologies:**
- Kotlin + Android SDK 35 + Java 17：主平台能力与现有工程完全一致 — 延续性最好、风险最低。
- Jetpack Compose + Material 3：现有 UI 主线 — 最适合做信息层级、状态卡片与风险语义统一。
- Navigation Compose：保留现有导航体系 — 只需渐进升级到 typed routes / route metadata。
- ViewModel + StateFlow + `collectAsStateWithLifecycle()`：现有状态管理已足够 — 重点是统一状态语义，不是换框架。
- Android 原生 NFC API + 现有 `core/nfc`：核心业务能力边界 — 本轮不应重写底层执行器。
- SQLiteOpenHelper / SharedPreferences：当前本地存储足够 — 在未出现明确痛点前不做数据层迁移。
- JUnit + AndroidX Test + Compose UI Test：回归安全网 — 用于固化 UI 与生命周期改造后的稳定性。

### Expected Features

本轮不是“再加功能”，而是把现有能力做得更清楚、更安全、更高效。研究共识是：用户最需要的是首页任务分层、统一流程状态模型、显式开始扫描与单次会话控制、写入与回读验证拆分、高风险操作强化确认、能力真实性标记、审计上下文补齐、敏感信息分级展示以及权限三层一致。

在 roadmap 上，这意味着必须优先处理同时影响可用性与可信度的要求，不能把模板、日志、设置等辅助页的视觉整理放到前面，也不能把服务化、新协议接入、批量自动执行或“更现代”的大规模视觉特效混入本轮范围。

**Must have (table stakes):**
- 首页任务分层重组 — 用户默认入口应按主任务 / 高风险 / 管理入口清晰分区。
- 统一流程状态模型 — 读写锁解格式化必须用一致的阶段与结果语义。
- 显式开始扫描与单次会话控制 — NFC 工具场景中这属于安全基线。
- 写卡结果拆分为写入结果与回读验证结果 — 避免误导与重复写卡。
- 高风险操作前置确认强化 — 锁卡/解锁/格式化必须突出风险、条件与结果来源。
- 能力真实性标记与 demo 骨架隔离 — 不能继续把未实现能力包装成真实成功。
- 审计上下文补齐 — 日志必须能追责，而不仅是“能看”。
- 权限可见性、路由可达性、执行校验三层一致 — 防止 UI 改版造成越权。

**Should have (competitive):**
- 审计导出 / 只读分享包 — 便于外部复核，但前提是日志字段先记对。
- 模板增强（收藏、最近使用、分类、版本备注） — 提升高频写卡效率。
- 最近任务快捷入口 / 上次操作恢复 — 可提升班组作业效率，但不能绕开风险提示。
- 平板/横屏双栏布局 — 对特定设备形态有价值，非当前默认前置项。
- 卡型能力提示增强与失败恢复助手 — 应建立在统一错误模型之后。

**Defer (v2+):**
- 后端服务化、在线同步、远程账号体系 — 与 offline-first 范围冲突。
- 真实通用解锁协议接入与大规模新卡型扩展 — 属于底层能力扩张，不是本轮主题。
- 大规模架构重写、多模块、全量状态框架替换 — 收益远低于迁移风险。
- 批量自动写卡 / 自动连续执行高风险操作 — 明显放大误操作与追责风险。

### Architecture Approach

推荐架构不是“重做”，而是“收边界”。保留单模块与现有 `feature + Screen/ViewModel/Contract + core` 结构，优先建立 4 条边界：App Shell 边界（导航、权限守卫、底栏/顶栏）、Design System 边界（页面骨架、状态卡、风险卡）、Flow Coordinator 边界（流程状态统一由 ViewModel 驱动）、Shared Operation State 边界（跨页结果与会话不再用 ad-hoc 单例缓存）。

路线重点是：先壳层、后页面；导航只判断能不能进，不判断业务阶段；Screen 负责生命周期，ViewModel 负责流程；跨页结果逐步从 `object Store` 迁到可控的 `OperationSessionStore`。这能在不推翻现有代码的情况下，把 UI 改造变成可复用、可测试、可审计的渐进演进。

**Major components:**
1. AppShell / AppNavGraph / Route Metadata — 负责路由注册、权限守卫、底栏规则与风险页元数据。
2. Design System — 负责统一页面骨架、状态区块、风险区块、CTA 层级与空/错态表达。
3. Feature ViewModel — 负责唯一 UI 状态源、事件处理、流程阶段推进、错误映射。
4. Screen — 负责渲染、导航回调、Activity/NFC 生命周期绑定，不再直接编排业务。
5. Flow Coordinator helper — 负责读/写/格式化/锁/解等流程共通状态与映射。
6. OperationSessionStore — 负责跨页临时结果与会话上下文传递。

### Critical Pitfalls

风险研究最有价值的结论是：这类项目最容易在“看起来只是 UI 优化”的过程中引入真实业务风险，因此 roadmap 必须把安全、真实性与生命周期控制当成基础设施，而不是 polish 阶段补丁。

1. **把界面优化做成权限绕行** — 必须坚持入口可见、路由访问、操作执行三层校验，并先补权限回归测试再改首页与导航。
2. **把骨架能力包装成已成功执行** — 必须显式建模 `DEMO_ONLY`、`NOT_IMPLEMENTED`、`UNVERIFIED`，并让审计区分模拟成功与真实成功。
3. **ReaderMode / 前台 NFC 生命周期与 Compose 状态机耦合失控** — 必须采用单次会话控制、in-flight 锁与真机生命周期回归，防止重复回调和重复执行。
4. **把自动开始扫描/执行当作高效体验** — 高频低风险流程最多自动准备，高风险流程必须使用分阶段确认。
5. **把写后回读失败误判成写入失败** — 必须拆分写入结果与验证结果，支持独立二次贴卡验证与恢复路径。
6. **审计只记做了什么，不记谁、凭什么、在什么上下文做的** — 必须补齐操作者、角色快照、真实性、阶段、卡片脱敏标识与失败原因。

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: 基础治理与壳层收口
**Rationale:** 这是所有后续页面改版的前置地基；如果不先统一壳层、状态语义和权限边界，后面每一页都会重复返工。  
**Delivers:** route metadata / guard helper、design tokens 与基础容器、统一流程状态模型、真实性状态枚举、OperationSessionStore、ReaderMode 单次会话控制、审计模型扩展。  
**Addresses:** 权限三层一致、能力真实性标记、统一流程状态模型、显式开始扫描、审计上下文补齐。  
**Avoids:** 权限绕行、假成功、ReaderMode 生命周期失控、假重构。

### Phase 2: 首页与导航信息架构优化
**Rationale:** 入口结构决定用户如何理解整个产品；先把主任务、高风险任务和管理入口分清，后续页面才有稳定归属。  
**Delivers:** 首页主任务/高风险/管理入口三分区、角色与权限反馈、更清晰的底栏与流程页关系、风险入口前置表达。  
**Uses:** Compose + Material 3 设计系统、Navigation Compose typed route / route metadata。  
**Implements:** AppShell / AppNavGraph / Route Metadata 边界。  
**Avoids:** 为减少层级而暴露高风险入口、入口整理后权限边界变松。

### Phase 3: 高频主流程优化（读卡 → 写卡 → 格式化）
**Rationale:** 这些流程频率最高、风险相对可控，最适合作为统一状态模型与设计系统的首批落地场景。  
**Delivers:** 读卡的清晰阶段反馈、写卡的预检/写入/验证拆分、格式化的阶段提示与失败恢复、稳定结果卡与步骤提示组件。  
**Addresses:** 统一流程状态模型、显式开始扫描、写卡结果拆分、失败恢复助手。  
**Avoids:** 自动执行、写入与验证混淆、ReaderMode 重复触发。

### Phase 4: 高风险流程优化（锁卡 → 解锁）
**Rationale:** 高风险页必须建立在前序阶段沉淀出的状态模型、风险组件、会话控制与审计能力之上，不能先美化再补规则。  
**Delivers:** 风险摘要、前置条件与确认链路、真实性标记、处理中不可误退、结果来源说明、凭据/理由/验证状态的审计落库。  
**Addresses:** 高风险操作前置确认强化、能力真实性标记、权限三层一致、敏感信息分级展示。  
**Avoids:** 假成功、误操作、越权执行、高风险语义被普通错误卡片稀释。

### Phase 5: 辅助页统一收尾（模板 / 日志 / 设置）
**Rationale:** 这些页面能复用前面沉淀的设计系统与数据模型，不应抢占主流程与高风险治理的优先级。  
**Delivers:** 模板页信息层级整理、日志筛选与详情结构统一、设置页账户/NFC 状态/清理入口重构、敏感字段脱敏与必要防旁观策略。  
**Addresses:** 模板/审计/设置页的信息层级整理、敏感信息分级展示、审计可读性提升。  
**Avoids:** 先做日志页美化却保留错误模型、把缓存清理做成危险维护入口。

### Phase Ordering Rationale

- 先 Phase 1 再做页面，是因为 stack、architecture、pitfalls 三份研究都明确指出：壳层、状态模型、权限和会话控制是共同依赖。
- 首页与导航早于业务页，是因为信息架构不稳定会导致所有页面入口、风险分层和底栏归属反复返工。
- 高频流程早于高风险流程，是因为读卡/写卡/格式化更适合验证统一状态组件与结果模型；成功后再复制到锁卡/解锁风险更低。
- 辅助页后置，是因为它们主要复用设计系统和审计模型，不应阻塞主任务流的可信化。
- 整体顺序直接对应研究中的风险规避：先处理真假能力、权限、ReaderMode 与审计，再做视觉与效率优化，避免“危险的假闭环”。

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 1：** ReaderMode 生命周期、真机 NFC 会话治理、`FLAG_SECURE` 适用边界、审计字段扩展与兼容迁移，需要结合真实设备与现有实现细节进一步验证。
- **Phase 4：** 锁卡/解锁的真实性边界、结果来源建模、风险确认链路与审计策略需要更细的业务校验，尤其是解锁仍属流程骨架。
- **Phase 5：** 若要做敏感信息防旁观、导出/分享包、日志受控清理，建议补一轮权限与合规角度的专项确认。

Phases with standard patterns (skip research-phase):
- **Phase 2：** 首页重组、导航壳层梳理、Material 3 信息层级与任务分区属于成熟模式，已有充分研究支撑。
- **Phase 3：** 基于统一状态模型改造读卡/写卡/格式化流程，模式已在本轮架构与风险研究中描述得较完整，可直接规划实施。

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | 结论基于现有仓库证据与 Android/Kotlin 官方文档，方向非常稳定。 |
| Features | HIGH | 对项目现状与本轮边界判断很清晰；行业优先级排序略带产品判断，但整体一致性强。 |
| Architecture | HIGH | 结论直接针对当前单模块 brownfield 现状，且与现有代码结构匹配度高。 |
| Pitfalls | HIGH | 大部分风险来自项目现状与 Android NFC 生命周期官方约束，现实相关性强。 |

**Overall confidence:** HIGH

### Gaps to Address

- **真实设备矩阵仍未具体化：** 需要在规划阶段明确目标机型、Android 版本、卡型与高风险操作真机回归清单。
- **解锁能力边界仍存在骨架属性：** 需要在需求拆解时明确哪些结果属于 demo、哪些可落真实审计，避免 roadmap 默认其已可产品化。
- **敏感信息保护策略需平衡可用性：** `FLAG_SECURE`、明文查看、日志导出等需要结合实际运营场景做权衡。
- **本地存储与审计模型扩展的迁移策略未细化：** 需在实施前确认历史数据兼容、字段默认值与升级路径。
- **平板/横屏是否在目标设备范围尚不明确：** 若存在，需要在 roadmap 中单独考虑 adaptive 布局；否则应继续后置。

## Sources

### Primary (HIGH confidence)
- `.planning/PROJECT.md` — 项目范围、约束、现有能力与 out-of-scope。
- `.planning/research/STACK.md` — brownfield 技术栈延续与最小补强建议。
- `.planning/research/FEATURES.md` — 体验型需求优先级、依赖关系与 MVP 建议。
- `.planning/research/ARCHITECTURE.md` — 渐进式架构边界、组件职责与 phase build order。
- `.planning/research/PITFALLS.md` — 高风险流程、生命周期、权限与审计方面的关键陷阱。
- Android Developers: Material 3 / Navigation Compose / State holders / Compose testing / Baseline Profiles / Activity / Lifecycle / Navigation / Test release notes — 官方推荐模式与稳定版本依据。
- Android Developers: Advanced NFC overview — ReaderMode 与前台 NFC 生命周期约束依据。

### Secondary (MEDIUM confidence)
- `.planning/codebase/STACK.md` — 当前依赖版本与技术基线。
- `.planning/codebase/ARCHITECTURE.md` — 当前单模块结构、状态管理与 NFC 能力边界。
- `.planning/codebase/CONCERNS.md` / `.planning/codebase/CONVENTIONS.md` / `.planning/codebase/TESTING.md` — 风险、编码约定与测试现状补充。
- `README.md` — 当前能力边界、演示属性与文案上下文。

### Tertiary (LOW confidence)
- 无新增低可信外部社区来源；主要不确定性来自目标设备范围、真实卡型覆盖和现网使用场景，而非资料质量本身。

---
*Research completed: 2026-03-31*
*Ready for roadmap: yes*
