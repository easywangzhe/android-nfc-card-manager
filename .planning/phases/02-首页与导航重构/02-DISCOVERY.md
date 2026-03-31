# Phase 02 Discovery: 首页与导航重构

## Discovery Level

Level 0（既有代码模式内规划）+ UI 设计契约补齐。

- 不引入新依赖
- 延续现有 Compose + `AppUi.kt` 组件模式
- 基于已完成 Phase 1 的权限兜底、真实性语义和单会话边界继续推进

## Current Findings

1. `HomeScreen.kt` 当前把读卡/写卡/锁卡/解锁放在同一组“四宫格快捷操作”里，用户无法在首页上区分主任务与高风险操作。
2. `AppNavGraph.kt` 当前底部导航固定显示“首页 / 模板 / 日志 / 我的”，没有按角色裁剪；例如非管理员仍会看到模板入口，只是在进入页面后被拒绝。
3. `SecurityManager.kt` 已提供 `canRead/canWrite/canLock/canUnlock/canManageTemplate/canViewAudit`，具备做统一入口可见性裁剪的现成权限源。
4. 现有 `AppUi.kt` 已有 `AppCard`、`StatusPill`、`PrimaryActionButton`、`DangerActionButton`，足够支撑本阶段信息层级重组，无需引入新的 UI 框架。

## UI Design Contract

### Home information hierarchy

首页固定按以下顺序展示：

1. **当前用户与角色摘要**
2. **主任务**
   - 读卡
   - 写卡
3. **高风险操作**
   - 锁卡
   - 解锁
4. **管理工具**
   - 模板管理
   - 审计日志
   - 设置 / 账号
5. **操作提示**

### Entry visibility contract

按当前 `SecurityManager` 权限规则裁剪入口：

| Role | 主任务 | 高风险 | 管理工具 |
|------|--------|--------|----------|
| OPERATOR | 读卡、写卡 | 无 | 日志、设置 |
| SUPERVISOR | 读卡、写卡 | 锁卡、解锁 | 日志、设置 |
| ADMIN | 读卡、写卡 | 锁卡、解锁 | 模板、日志、设置 |
| AUDITOR | 无 | 无 | 日志、设置 |

规则：

- 无权限入口不显示为可点击主入口。
- 高风险入口必须使用警示语义，不与主任务共用同一种 CTA 表达。
- 管理工具放在独立分组，不混入主任务区域。

### Bottom navigation contract

- 首页：所有登录角色可见
- 模板：仅管理员可见
- 日志：所有登录角色可见
- 我的：所有登录角色可见

目标不是新增导航层级，而是让底部导航只显示“当前角色真的能进入的一级页”。

## Planning Guidance

- 优先产出“角色 -> 首页分组/导航项”的共享契约，避免 `HomeScreen` 与 `AppNavGraph` 各自维护一套判断。
- 首页入口卡片需要携带：分组、标题、说明、风险级别、目标路由/回调、是否可见。
- 继续复用 Phase 1 的 `ProtectedAction` 与真实性边界；解锁仍保持 demo-only，不在本阶段改变底层能力含义。
- 自动化验证以 JVM 单元测试 + `assembleDebug` 为主；本阶段不新增强依赖的 Compose UI 自动化。
