---
phase: 02-首页与导航重构
plan: 01
subsystem: ui
tags: [compose, navigation, permissions, home, testing]
requires:
  - phase: 01-基础治理与边界收口
    provides: [路由权限兜底, 共享阶段语义, 真实性边界]
provides:
  - 角色感知的首页分组契约
  - 按角色裁剪的底部导航定义
  - 首页入口可见性回归测试
affects: [home, navigation, shell]
tech-stack:
  added: []
  patterns: [首页与底部导航共享入口契约, 基于 SecurityManager 的入口裁剪]
key-files:
  created:
    - app/src/main/java/com/opencode/nfccardmanager/feature/home/HomeShellContract.kt
    - app/src/test/java/com/opencode/nfccardmanager/feature/home/HomeShellContractTest.kt
  modified: []
key-decisions:
  - "首页分组与底部导航先收口到纯 Kotlin 契约，避免 HomeScreen 与 AppNavGraph 各写一套角色判断。"
  - "入口可见性统一复用 SecurityManager 权限函数，减少首页重构时的权限漂移风险。"
patterns-established:
  - "Pattern: buildHomeSections/buildBottomNavDestinations 作为首页与导航的单一入口来源"
  - "Pattern: 审计员仅暴露管理工具分组，不再渲染禁用的主任务或高风险入口"
requirements-completed: [SHELL-02]
duration: 4m
completed: 2026-03-31
---

# Phase 2 Plan 1: 角色感知入口契约 Summary

**首页三分区与底部导航的角色可见性已被收口为单一 Kotlin 契约，并通过四类角色测试锁定入口裁剪规则。**

## Performance

- **Duration:** 4m
- **Started:** 2026-03-31T08:31:00Z
- **Completed:** 2026-03-31T08:35:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- 新增 `HomeShellContract.kt`，统一定义主任务、高风险操作、管理工具三类首页分组及底部导航项。
- 使用 `SecurityManager` 既有权限函数驱动入口过滤，保证首页与导航不复制角色判断。
- 新增 `HomeShellContractTest.kt`，覆盖 OPERATOR、SUPERVISOR、ADMIN、AUDITOR 的首页与底部导航可见性回归。

## Task Commits

Each task was committed atomically:

1. **Task 1: 写首页/导航入口契约测试骨架** - `88e6d65` (test)
2. **Task 2: 实现角色感知的首页分组与底部导航契约** - `b3e319c` (feat)

**Plan metadata:** `pending`

## Files Created/Modified
- `app/src/main/java/com/opencode/nfccardmanager/feature/home/HomeShellContract.kt` - 提供首页分组、入口语义和底部导航的共享契约。
- `app/src/test/java/com/opencode/nfccardmanager/feature/home/HomeShellContractTest.kt` - 校验四类角色的首页分组和底部导航可见性。

## Decisions Made
- 先收口入口契约，再改 UI，避免 `HomeScreen` 和 `AppNavGraph` 在 Phase 2 内部出现两套来源。
- 模板入口仅管理员可见，审计员仅保留日志与设置，直接满足计划中的角色裁剪要求。

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- 首页和底部导航已有统一契约来源，02-02 可直接消费 `buildHomeSections` 与 `buildBottomNavDestinations` 落地 UI。
- 角色入口裁剪已具备回归测试，后续壳层重构可持续验证不越权显示入口。

## Known Stubs

None.

---
*Phase: 02-首页与导航重构*
*Completed: 2026-03-31*

## Self-Check: PASSED

- FOUND: `.planning/phases/02-首页与导航重构/02-01-SUMMARY.md`
- FOUND commit: `88e6d65`
- FOUND commit: `b3e319c`
