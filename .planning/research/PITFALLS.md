# Domain Pitfalls

**Domain:** Android NFC 卡片管理工具（UI / UX 优化与高风险流程演进）
**Researched:** 2026-03-31

## Critical Pitfalls

### Pitfall 1: 把“界面优化”做成权限绕行
**What goes wrong:** 为了简化首页导航、减少步骤或统一入口，把原本分散在页面、路由、按钮态里的权限判断削薄，最终让无权角色看见高风险入口，甚至进入可执行流程。
**Why it happens:** 工具型应用常把“入口可见性”误当成“权限控制”；做信息架构重组时，只顾入口收纳、卡片排序、快捷操作，忽略执行层鉴权必须保留。
**Consequences:** 普通操作员可误入锁卡/解锁；审计记录与权限矩阵失真；后续 roadmap 会错误高估“已完成权限收敛”。
**Warning signs:**
- 首页改版后，角色差异只体现在文案，不体现在可进入性与可执行性
- 新增统一“快捷操作”面板，但未复用原授权判断
- 高风险页面改成先进入再提示“你可能无权限”
- 代码评审里出现“只是 UI 改动，不影响安全”
**Prevention strategy:**
- 将“入口可见”“路由访问”“操作执行”拆成三层校验，任一层都不可省略
- 所有锁卡/解锁/格式化动作必须经统一授权层，不允许 Screen 直接决定
- 先补权限回归测试，再做导航与首页重组
- PR 模板中加入“是否改变权限边界”检查项
**Better phase to handle:** Phase 0 / Phase 1（先做安全边界收口，再做首页与导航优化）

### Pitfall 2: 把骨架能力包装成“已成功执行”
**What goes wrong:** UI 为了流畅与“完整闭环”，把未接入真实协议的锁卡/解锁流程也渲染成成功路径，给出成功 toast、成功页和成功审计。
**Why it happens:** 演示版产品常追求流程完整；设计稿往往默认有“成功态”，开发为了保持视觉一致性就补了假成功。
**Consequences:** 操作员误以为物理卡状态已改变；审计日志失真；后续产品需求会基于错误前提继续叠加。
**Warning signs:**
- 结果页只区分成功/失败，不存在 `NOT_IMPLEMENTED` / `SIMULATED` / `UNVERIFIED`
- README、页面文案、审计日志三者对能力边界描述不一致
- 执行成功不依赖真实 NFC 回执或回读验证
- 演示按钮与真实入口共用同一结果模型
**Prevention strategy:**
- 对未落地能力使用显式状态：`DEMO_ONLY`、`NOT_SUPPORTED`、`UNVERIFIED`
- 审计日志把“演示成功”和“真实成功”分开建模
- 高风险流程的 UI 文案必须以设备回执/回读结果为准，不以按钮点击为准
- roadmap 中先排“能力真实性澄清”，再排“高风险页视觉精修”
**Better phase to handle:** Phase 0（立刻处理，避免后续所有需求评估被假状态污染）

### Pitfall 3: ReaderMode / 前台 NFC 生命周期与 Compose 状态机耦合失控
**What goes wrong:** 页面重组、返回前台、旋转、快速切页、重复贴卡时，ReaderMode 未及时关闭或重复开启，导致一次操作触发多次回调、并发执行或错把旧 Tag 当新 Tag。
**Why it happens:** Android 官方要求前台 NFC 分发需与 `onResume`/`onPause` 等生命周期严格配合；但 Compose 页常把启停逻辑放在 `LaunchedEffect` / `DisposableEffect`，再叠加 ViewModel 状态切换，容易出现竞态。
**Consequences:** 重复写卡、重复锁卡、重复审计、回调串页；最危险的是高风险动作被执行两次。
**Warning signs:**
- 同一张卡贴一次，日志里出现多条同类记录
- 页面离开后仍能收到 NFC 回调
- 扫描中/执行中没有 in-flight 锁或互斥状态
- 需要靠“快速移开卡片”规避重复触发
**Prevention strategy:**
- 将“开始扫描”“捕获 Tag”“执行操作”“结果确认”拆成明确状态机
- Tag 一旦捕获，立即关闭 ReaderMode 或禁用后续回调，直到本次流程结束
- 为高风险操作加互斥锁、幂等 key 或单次会话 token
- 加入真机回归清单：切后台、锁屏、旋转、重复贴卡、快速进退页面
**Better phase to handle:** Phase 1（所有读/写/锁/解 UI 优化前先统一 ReaderMode 控制）

### Pitfall 4: 把“自动开始扫描/执行”当作高效体验
**What goes wrong:** 用户一进入页面就自动启用扫描，甚至在贴卡状态下直接触发格式化、写卡、锁卡等动作。
**Why it happens:** 设计上追求少一步、快一步；但 NFC 工具场景和普通扫码场景不同，很多动作不可逆或高成本。
**Consequences:** 误操作率飙升；用户无法在动作前确认卡片、内容、角色、风险与目标；真机测试偶发问题会被误当成功能 bug。
**Warning signs:**
- 页面首帧就出现“请贴卡”，没有显式开始按钮
- 高风险页把“进入页面”视为“准备执行”
- 设计评审中出现“少一次点击更顺滑”但没有风险分级
- 埋点显示大量秒退、取消、失败后重试
**Prevention strategy:**
- 区分低风险读卡与高风险写/锁/解：仅低风险流程可考虑自动准备，不可自动执行
- 高风险流程强制采用“确认内容 → 开始扫描 → 捕获卡片 → 最终执行”四段式
- 在写卡与锁卡页展示目标摘要（卡型、内容摘要、权限身份、风险说明）
- 增加“持卡即进入页面”的真机测试用例
**Better phase to handle:** Phase 1（高频流程改版时同步处理）

### Pitfall 5: 把“写后回读失败”误判成“写入失败”
**What goes wrong:** 写入后立刻复用同一个 Tag 或同一物理贴卡姿态做校验，一旦卡片轻微移动、信号变弱或 TagLost，就把“验证失败”展示成“写入失败”。
**Why it happens:** 团队把写入与验证混成单一成功判定，UI 只想保留一个最终结果。
**Consequences:** 用户重复写卡；同一卡可能被重复写入；现场操作员失去对结果的信任。
**Warning signs:**
- 结果模型只有 `SUCCESS` / `FAILED`
- 用户反馈“明明写上了，但应用说失败”
- 失败日志集中出现在“回读校验”阶段
- 没有“请重新贴卡验证”这类恢复路径
**Prevention strategy:**
- 将结果拆分为：写入成功、验证成功、验证未完成、验证失败
- 回读校验允许独立二次贴卡，不强依赖同一 Tag 会话
- 审计中分别记录写入结果与验证结果
- UX 文案避免把“无法确认”写成“失败”
**Better phase to handle:** Phase 1（写卡流程页重构时必须一起处理）

### Pitfall 6: 审计只记“做了什么”，不记“谁、凭什么、在什么上下文做的”
**What goes wrong:** UI 优化后审计列表更好看，但日志模型仍缺真实操作者、角色快照、前置确认、卡片脱敏标识、执行阶段与能力真实性。
**Why it happens:** 团队把审计当“结果页附属功能”，只记操作类型和结果；忽略高风险流程真正需要追责上下文。
**Consequences:** 出问题时无法判断谁执行、谁审批、是否是 demo 能力、是否越权、是否因为生命周期重入导致重复执行。
**Warning signs:**
- `operatorId` 为空、固定值或来自默认值
- 审计中看不到角色、确认词、理由、凭据校验结果、设备状态
- 读卡/写卡/锁卡/解锁使用同一粗粒度结果结构
- “清理缓存”可直接删除关键操作历史
**Prevention strategy:**
- 审计最少记录：操作者、角色快照、操作阶段、卡片脱敏标识、结果来源（真实/模拟）、失败原因、时间与设备状态
- 高风险流程写入两类日志：流程日志 + 最终执行日志
- 将“清理缓存”与“清理审计”拆开，并增加受限权限或二次确认
- 先补审计模型，再做日志页视觉优化
**Better phase to handle:** Phase 1 / Phase 2（先补日志模型，再改审计页面体验）

### Pitfall 7: 把敏感信息可见性问题误认为“只是视觉细节”
**What goes wrong:** 为了让结果页更“透明”“可读”，直接展示 UID、模板明文、解锁凭据提示、设备调试信息，且支持截图、投屏、肩窥。
**Why it happens:** 工具类应用经常由内部人员使用，团队容易放松最小暴露原则；UI 优化时还会进一步放大这些信息。
**Consequences:** 敏感标识泄露；审计合规失败；角色边界在视觉层被绕开。
**Warning signs:**
- 详情页默认展示完整 UID / NDEF 内容 / 凭据提示
- 不同角色看到完全相同的敏感字段
- 敏感页面未启用防截屏策略
- 设计稿默认以完整真实数据做展示样例
**Prevention strategy:**
- 对 UID、模板内容、凭据、理由等字段按角色分级脱敏
- 为高风险和审计详情页启用 `FLAG_SECURE` 或等价策略，并评估业务可用性影响
- 增加“查看明文”显式动作并写审计
- 设计稿、测试数据、演示账号统一使用脱敏样本
**Better phase to handle:** Phase 0 / Phase 1（先定敏感信息展示基线，再做结果页与详情页优化）

### Pitfall 8: 用“页面拆分/组件化”掩盖真实问题，做成假重构
**What goes wrong:** 为了减少“工具原型感”，大量拆组件、抽主题、搬 ViewModel，但 NFC 执行、权限、审计、错误映射仍散落在 Screen 与 helper 里，结果代码更分散、风险更难查。
**Why it happens:** UI 优化天然容易先动表现层；如果没有先定义状态机和领域边界，组件化只是在移动混乱。
**Consequences:** roadmap 看似完成“大重构”，实际没降低误操作、安全、测试风险；后续每加一个卡型都要再返工。
**Warning signs:**
- PR 很大，但没有新增状态模型、用例层或测试
- “重构”主要是文件重命名、Composable 抽取、样式统一
- 同一错误文案仍在多个页面硬编码
- 高风险逻辑仍由 Screen 直接调用 NFC helper
**Prevention strategy:**
- 先定义统一的流程状态机、错误模型、审计接口，再做 UI 组件化
- 把“ReaderMode 控制”“NFC 执行”“权限校验”“审计记录”从页面层抽离
- 将重构验收标准改为：风险下降、测试可写、状态一致，而不是文件数变化
- 每次 UI 重构必须附带至少一类回归测试或手工真机清单更新
**Better phase to handle:** Phase 1（在统一流程抽象时处理），而不是放到纯视觉 polish phase

## Moderate Pitfalls

### Pitfall 9: 把卡型能力判断做成静态文案承诺
**What goes wrong:** 应用根据 tech 名称或历史经验直接给出“支持写入/支持解锁/支持锁卡”提示，但真实标签能力与设备兼容并不一致。
**Prevention strategy:** 只把静态能力判断当“可能支持”，最终能力以真实探测、认证结果、回读结果为准；在 UI 上明确“可尝试”与“已确认支持”的区别。
**Warning signs:** 首页或结果页出现绝对化文案，如“该卡支持解锁”；不同设备对同一卡表现不一致。
**Better phase to handle:** Phase 1（读卡结果与操作前确认页）

### Pitfall 10: 真机测试被“模拟成功按钮”替代
**What goes wrong:** 团队用 demo 按钮、演示数据验证 UI，误以为高风险流程已稳定，导致生命周期、TagLost、重复贴卡、弱场景全未覆盖。
**Prevention strategy:** 为每个高风险流程维护真机测试矩阵：设备型号、卡型、贴卡姿势、重复触发、切后台、断电/锁屏、权限切换；把 demo 测试与真机测试分开记录。
**Warning signs:** 发布前只跑 `./gradlew test` 和人工点击模拟按钮；没有任何真实设备执行记录。
**Better phase to handle:** Phase 0 持续到每个后续 phase（作为门禁，不是尾声补救）

### Pitfall 11: 把“清理缓存”设计成危险的维护入口
**What goes wrong:** 设置页为了简洁把最近读卡、草稿、审计日志、会话痕迹都收进一个“清理缓存”，用户误点后删除关键追溯数据。
**Prevention strategy:** 区分可丢失缓存与合规数据；高风险数据清理需要单独入口、说明和权限；支持导出后再删。
**Warning signs:** 一个按钮同时影响日志、最近记录、模板或会话；按钮文案模糊。
**Better phase to handle:** Phase 2（设置页与运维辅助页优化）

## Minor Pitfalls

### Pitfall 12: 结果反馈过度追求一致，忽略高风险语义差异
**What goes wrong:** 为了统一视觉语言，把读卡失败、写卡失败、锁卡失败、未实现、未验证都渲染成同一类错误卡片。
**Prevention strategy:** 定义按风险分级的反馈模板；高风险流程必须区分“未执行”“执行失败”“执行成功但未验证”“不支持”。
**Warning signs:** 不同页面只换图标和标题，恢复动作完全一样。
**Better phase to handle:** Phase 1（统一状态与文案系统时）

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| 首页 / 导航重组 | 为了减少层级暴露高风险入口，削弱角色边界 | 先固化权限矩阵与入口策略，再做信息架构 |
| 读卡流程优化 | 自动扫描与状态跳转过快，用户不知道当前读到的是哪张卡 | 保留显式开始/停止与结果确认，记录最近一次 Tag session |
| 写卡流程优化 | 写入、回读、失败恢复混成一个结果，诱发重复写卡 | 拆分写入态与验证态，支持重新贴卡验证 |
| 锁卡页优化 | 只美化风险提示，不补执行前授权与执行后验证 | 强制二次确认 + 统一授权 + 明确验证结果 |
| 解锁页优化 | 对 demo 骨架做视觉包装，误导为真实能力 | 明示 `DEMO_ONLY/NOT_IMPLEMENTED`，禁用真实成功审计 |
| 日志页优化 | 先做筛选和视觉，不补字段模型 | 先补 operator、role snapshot、真实性、阶段字段 |
| 设置页优化 | 把审计清理、会话清理、缓存清理合并 | 拆分数据类型与权限级别，增加确认与导出 |
| 组件库 / 视觉统一 | 先抽 UI 组件，后面再补状态机 | 先统一流程状态和错误模型，再抽视觉组件 |
| 测试补强 | 只补 ViewModel 单测，遗漏真机生命周期回归 | 单测 + 仪器测试 + 真机矩阵三层并行 |

## Most Important Guidance for Roadmap

1. **先收口真假能力、权限与审计，再谈视觉统一。** 否则 roadmap 会把“危险的假闭环”误判成“体验升级”。
2. **把 ReaderMode / Tag 会话控制列为前置基础设施。** 这是后续所有流程页优化的隐藏依赖。
3. **把高风险流程从“自动执行”改成“分阶段确认”。** NFC 工具场景里，少一步不一定更好，尤其对锁卡/解锁。
4. **把测试缺口当产品风险，不是工程卫生。** 没有真机与生命周期回归，任何 UI 改版都可能放大误操作。
5. **拒绝假重构。** 只有当权限、状态、审计、测试一起变得更清晰时，才算真正降低风险。

## Sources

- 项目上下文：`.planning/PROJECT.md`（HIGH）
- 代码库风险审计：`.planning/codebase/CONCERNS.md`（HIGH）
- 测试现状：`.planning/codebase/TESTING.md`（HIGH）
- 产品与能力边界：`README.md`（HIGH）
- Android Developers, **Advanced NFC overview**（官方文档，2024-01-03，说明 NFC 前台分发、TagTechnology 与生命周期要求）https://developer.android.google.cn/develop/connectivity/nfc/advanced-nfc?hl=en （HIGH）
- Android Developers, **`<application>` manifest element**（官方文档，说明 `allowBackup` 默认行为与备份风险）https://developer.android.google.cn/guide/topics/manifest/application-element?hl=en （HIGH）
