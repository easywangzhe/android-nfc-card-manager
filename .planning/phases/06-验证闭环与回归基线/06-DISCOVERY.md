# Phase 06 Discovery: 验证闭环与回归基线

## Discovery Level

Level 0（在现有 Compose + ViewModel + core 架构内补验证）+ 测试/验证契约补齐。

- 不引入后端或新框架
- 优先复用现有 JUnit4、AndroidX Test、Compose UI Test 依赖
- 不直接扩张业务能力，先把 Phase 1-5 已完成的权限、真实性、结果来源与审计语义锁进可重复验证基线

## Current Findings

1. 当前仓库已具备一批 Phase 1-5 新增的 JVM 单测，但 `app/src/androidTest` 仍为空；Compose UI Test 依赖虽然已在 `app/build.gradle.kts` 中配置，关键路径还没有进入仪器化/UI 回归。
2. `05-VERIFICATION.md` 已明确当前 closeout 主要是 roadmap / requirements / summaries 的交叉验证，不等同于一次新的真机 NFC UAT；这意味着文档层闭环已完成，但设备层验证基线仍未成型。
3. 代码中仍保留较多“模拟/仅演示”入口与文案；Phase 1-5 已收口真实性语义，但如果缺少 UI 层与审计层回归，后续最容易再次出现 demo-only、未验证、真实成功之间的表达漂移。
4. 现有测试主要集中在纯 Kotlin contract / ViewModel 层，尚未自动验证首页入口可见性、页面真实性标签、结果区结构、导航守卫与审计详情链路的一致性。
5. 真机 NFC 弱场景仍缺标准化验证清单，例如重复贴卡、ReaderMode 生命周期切换、写后回读失败、格式化失败恢复、高风险处理中保护与日志对应关系。

## Phase Thesis

这一阶段应优先“验证已完成能力是否稳定可信”，而不是继续增加新业务功能。目标是把自动化回归、真机检查清单和端到端语义一致性连成一个最小可信闭环。

## Candidate Scope

### Automation Baseline

- 登录后首页入口可见性与角色裁剪
- 路由守卫与高风险入口真实性标签
- 高频流程结果区的关键状态、结果来源与下一步动作
- 审计列表/详情中的角色、阶段、真实性来源一致性

### Real-device Verification Baseline

- 读卡：成功、空 NDEF、非 NDEF、读取失败
- 写卡：写入成功、回读校验失败、重新贴卡验证
- 格式化：支持、非支持、清空失败、格式化异常
- 锁卡/解锁：风险前置、处理中保护、demo-only 边界、失败恢复

### Explicit Non-goals

- 不接入新卡型、后端或远程账号体系
- 不做大规模架构重写或引入新的测试框架迁移
- 不把真机验证 phase 变成新的业务能力扩张 phase

## Planning Guidance

- 先定义“验证对象 / 观测信号 / 通过口径”，再写测试实现，避免先堆 UI test 再返工断言口径。
- 自动化优先级建议为：权限与真实性 > 高频结果语义 > 审计一致性 > 辅助页视觉稳定性。
- 优先补必要的 semantics / test hooks / 可观测文案，不为了测试而重写页面结构。
- 真机验证文档必须明确设备、卡型、前置条件、触发步骤、期望 UI、期望审计结果与异常恢复方式。
- 如果为测试可写性发现必须做大规模依赖注入或架构改造，应拆成后续 phase，不作为本 phase 前置条件。

## Candidate Plans

1. **06-01** — 验证口径与真机回归矩阵收口
2. **06-02** — 权限与高风险真实性自动化回归落地
3. **06-03** — 高频流程与审计端到端一致性回归
