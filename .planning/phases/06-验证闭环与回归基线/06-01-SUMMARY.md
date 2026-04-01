---
phase: 06-验证闭环与回归基线
plan: 01
subsystem: testing
tags: [verification, androidTest, compose, nfc]
requires:
  - phase: 02-首页与导航重构
    provides: [首页三段式分组与角色可见性契约]
  - phase: 03-高频流程澄清
    provides: [高频结果 guidance 与推荐下一步结构]
  - phase: 04-高风险流程强化
    provides: [高风险真实性与结果来源语义]
  - phase: 05-辅助页统一收尾
    provides: [审计语义与影响范围表达]
provides:
  - Phase 6 验证对象、观测信号与通过口径基线
  - 读卡、写卡、格式化、锁卡、解锁五条主流程真机回归矩阵
  - 共享 AppTestTags 观测契约与本地演示账号夹具
affects: [06-02, 06-03, androidTest, real-device-regression]
tech-stack:
  added: []
  patterns: [验证对象-观测信号映射, AppTestTags 共享契约, 本地演示账号夹具]
key-files:
  created:
    - docs/testing/phase-06-verification-baseline.md
    - docs/testing/phase-06-real-device-matrix.md
    - app/src/main/java/com/opencode/nfccardmanager/ui/test/AppTestTags.kt
    - app/src/androidTest/java/com/opencode/nfccardmanager/testutil/Phase6TestFixtures.kt
  modified: []
key-decisions:
  - "Phase 6 自动化统一通过 AppTestTags 观测结构节点，而不是依赖页面 copy。"
  - "真机矩阵必须同时写出期望 UI 与期望审计字段，作为后续回归唯一口径。"
patterns-established:
  - "Pattern: 先定义验证对象 / 观测信号 / 通过口径，再落 instrumentation"
  - "Pattern: androidTest 统一复用本地演示账号夹具，不新增测试专用账号"
requirements-completed: [V2-08]
duration: 12m
completed: 2026-04-01
---

# Phase 6 Plan 1: 收口验证口径、真机回归矩阵与共享测试契约 Summary

**Phase 6 的验证语言已统一收口为验证基线文档、真机矩阵、稳定 test tags 命名和可复用的本地演示账号夹具。**

## Performance

- **Duration:** 12m
- **Started:** 2026-04-01T05:22:31Z
- **Completed:** 2026-04-01T05:34:29Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- 新增 `phase-06-verification-baseline.md`，把登录/首页权限、高风险真实性、路由守卫、高频结果页和审计语义统一成单一验证口径。
- 新增 `phase-06-real-device-matrix.md`，为读卡、写卡、格式化、锁卡、解锁五条主流程写明期望 UI、期望审计与失败恢复动作。
- 新增 `AppTestTags.kt` 与 `Phase6TestFixtures.kt`，为 06-02/06-03 的 instrumentation 直接提供稳定结构节点和本地演示账号夹具。

## Task Commits

Each task was committed atomically:

1. **Task 1: 收口 Phase 6 验证基线与真机回归矩阵** - `2e7acd7` (chore)
2. **Task 2: 定义共享 UI 观测契约与测试账号夹具** - `d4ed4d1` (chore)

## Files Created/Modified
- `docs/testing/phase-06-verification-baseline.md` - 定义验证对象、观测信号、自动化覆盖方式与真机补充方式。
- `docs/testing/phase-06-real-device-matrix.md` - 收口五条主流程的真机回归矩阵与审计核对字段。
- `app/src/main/java/com/opencode/nfccardmanager/ui/test/AppTestTags.kt` - 提供登录、首页、高风险、高频结果页和审计页共享 test tags。
- `app/src/androidTest/java/com/opencode/nfccardmanager/testutil/Phase6TestFixtures.kt` - 固化 operator / supervisor / admin / auditor 本地演示账号夹具与登录辅助方法。

## Decisions Made
- 使用 `AppTestTags` 作为 Phase 6 自动化的主观测契约，避免测试断言继续绑定页面 copy 细节。
- 真机矩阵把“期望 UI + 期望审计字段 + 失败恢复动作”并列记录，后续回归不再只靠 closeout 文档描述。

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] 移除夹具里非必要的 `assertExists` 依赖以恢复编译**
- **Found during:** Task 2（定义共享 UI 观测契约与测试账号夹具）
- **Issue:** `Phase6TestFixtures.kt` 初版引用的 `assertExists` 在当前 androidTest 依赖解析下未成功编译，阻塞 `assembleDebugAndroidTest`。
- **Fix:** 保留稳定 tag 驱动的 `performTextClearance` / `performTextInput` / `performClick` 流程，移除对非必要内联断言的依赖。
- **Files modified:** `app/src/androidTest/java/com/opencode/nfccardmanager/testutil/Phase6TestFixtures.kt`
- **Verification:** `./gradlew assembleDebug assembleDebugAndroidTest`
- **Committed in:** `d4ed4d1` (part of task commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** 仅是测试夹具编译兼容性修复，没有改变计划范围或验证口径。

## Issues Encountered
- 首次 androidTest 构建因共享夹具引用的断言 API 解析失败而中断，精简为当前依赖稳定支持的 API 后恢复通过。

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- 06-02/06-03 可直接消费 `AppTestTags` 和 `Phase6TestFixtures` 开始写 instrumentation。
- 真机验证矩阵已形成单一口径，后续人工回归可直接按文档执行。

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: `.planning/phases/06-验证闭环与回归基线/06-01-SUMMARY.md`
- FOUND commit: `2e7acd7`
- FOUND commit: `d4ed4d1`
