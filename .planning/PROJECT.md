# Android NFC Card Manager

## What This Is

这是一个面向 NFC 卡片操作场景的 Android 本地应用，当前已具备登录、角色权限、读卡、写卡、锁卡、解锁、模板管理、日志审计和设置等基础能力。本轮项目初始化聚焦在现有产品上继续演进，目标不是重写底层能力，而是在保留既有 NFC 业务流程的前提下，系统性优化 UI 体验、信息层级和关键状态反馈。

## Core Value

让用户在执行 NFC 卡片关键操作时，始终能清楚理解当前状态、风险提示和下一步动作，从而更安全、更高效地完成任务。

## Requirements

### Validated

- ✓ 用户可以通过本地演示账号登录并恢复登录态 — existing
- ✓ 用户可以按角色访问不同功能入口，并在无权限时看到拒绝访问页 — existing
- ✓ 用户可以完成 NFC 读卡并查看读卡结果详情 — existing
- ✓ 用户可以基于手工输入或模板内容执行 NDEF 文本写卡并进行回读校验 — existing
- ✓ 用户可以执行带风险确认的锁卡流程，并记录结果 — existing
- ✓ 用户可以进入解锁流程并提交解锁理由与凭据完成流程演示 — existing
- ✓ 用户可以管理本地写卡模板，且模板在应用重启后保留 — existing
- ✓ 用户可以查看和筛选本地审计日志 — existing
- ✓ 用户可以在设置页查看账号、角色、NFC 状态和执行缓存清理 — existing

### Active

- [ ] 建立关键 NFC 路径的验证闭环与回归基线，覆盖权限、真实性、结果来源与审计语义的一致性验证
- [ ] 补齐 JVM 单测之外的关键路径自动化验证，优先覆盖登录/首页权限、高风险 demo-only 边界与高频结果页
- [ ] 沉淀可重复执行的真机 NFC 验证矩阵，避免后续 phase 只依赖模拟按钮、文档 closeout 或纯逻辑测试

### Out of Scope

- 通用后端服务化改造 — 当前项目以本地演示型实现为基础，本轮优先解决客户端体验问题
- 新增远程账号体系或真实在线认证 — 现阶段重点不是身份系统扩展，而是已有流程的可用性提升
- 完整重写 NFC 底层执行逻辑 — 本轮优化以界面、交互和状态表达为主，不主动替换已工作的核心能力
- 通用真实底层解锁协议接入 — 当前解锁仍是流程骨架，本轮先优化体验与边界表达

## Context

当前代码库是单模块 Android 应用，基于 Kotlin、Jetpack Compose、Material 3 与 Android NFC API 实现，主入口位于 `app/src/main/java/com/opencode/nfccardmanager/MainActivity.kt`，导航由 `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt` 统一管理。现有业务能力已覆盖首页、登录、扫描、读卡结果、写卡、格式化、锁卡、解锁、模板、日志与设置页面，且大多数页面采用 `Screen + ViewModel + Contract` 组织方式。

从现状看，产品能力已基本齐备，但页面信息组织与状态表达仍有较大提升空间，尤其在首页入口组织、流程阶段提示、风险操作确认和结果反馈方面。用户当前明确希望先以 UI 体验为主线，覆盖首页导航、读写流程页、锁解锁页和管理辅助页，重点改善“信息层级乱”和“状态不够清晰”这两类问题。

## Constraints

- **Tech stack**: 保持 Android + Kotlin + Jetpack Compose 现有技术栈 — 避免为了 UI 优化引入不必要的重型重构
- **Brownfield**: 必须在已有业务流程上渐进优化 — 当前读卡、写卡、模板、日志等能力已存在，不能破坏现有功能
- **NFC safety**: 锁卡、解锁等高风险流程必须保留清晰风险提示与确认机制 — 这些操作具有不可逆或高误操作成本
- **Role compatibility**: 现有角色权限边界需要继续成立 — 首页入口、路由与页面呈现不能绕过已有权限判断
- **Offline-first**: 当前主要是本地演示型应用 — 设计和需求不应默认依赖后端服务或在线能力

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 本轮以 UI 体验优化为主，而不是新增大块底层功能 | 用户当前最在意的是信息层级与状态表达问题 | Phase 1-5 已完成 |
| 优先覆盖首页导航、读写流程页、锁解锁页和管理辅助页 | 这些页面已经被用户明确指定为第一阶段范围 | Phase 1-5 已完成 |
| 状态清晰度优先于纯视觉美化 | 用户希望减少误操作，核心是让风险、阶段和结果更明确 | Phase 1-5 已完成 |
| v1 体验收口后，下一轮优先补验证闭环，而不是继续扩张业务功能 | 当前主要缺口已从“页面表达不清”转为“缺少可重复验证与端到端回归基线” | Proposed for post-v1 Phase 6 |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-01 after post-v1 phase seed update*
