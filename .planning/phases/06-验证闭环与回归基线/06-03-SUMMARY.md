---
phase: 06-验证闭环与回归基线
plan: 03
subsystem: testing
tags: [androidTest, flow-guidance, audit, compose]
requires:
  - phase: 06-验证闭环与回归基线
    provides: [共享验证口径、test tags 与账号夹具]
  - phase: 03-高频流程澄清
    provides: [读卡/写卡/格式化结果 guidance 语义]
  - phase: 05-辅助页统一收尾
    provides: [审计列表与详情语义映射]
provides:
  - 高频流程结果页设备级回归
  - 审计列表与详情一致性设备级回归
  - 错误态格式化 guidance 与懒加载列表滚动断言模式
affects: [flow-guidance, audit, read, write, format]
tech-stack:
  added: []
  patterns: [结果区分段 test tags, lazy list scroll-to-node 回归, 审计详情结构断言]
key-files:
  created:
    - app/src/androidTest/java/com/opencode/nfccardmanager/feature/verification/Phase6FlowAndAuditConsistencyTest.kt
  modified:
    - app/src/main/java/com/opencode/nfccardmanager/feature/read/ReadResultScreen.kt
    - app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt
    - app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatCardScreen.kt
    - app/src/main/java/com/opencode/nfccardmanager/feature/audit/AuditLogScreen.kt
    - app/src/main/java/com/opencode/nfccardmanager/feature/audit/AuditLogDetailScreen.kt
    - app/src/main/java/com/opencode/nfccardmanager/ui/component/AppUi.kt
    - app/src/main/java/com/opencode/nfccardmanager/ui/test/AppTestTags.kt
key-decisions:
  - "格式化失败 guidance 必须在 error-only 场景也能渲染，不允许丢失 what/why/next-step 结构。"
  - "设备回归对 LazyColumn 内容统一使用根节点 tag + scroll-to-node，不假设目标分区首屏已组合。"
patterns-established:
  - "Pattern: 高频结果页 test tags 按 execution / verification / next-step 语义分段"
  - "Pattern: 审计列表先用本地日志 ID 锁定目标，再滚动到列表项进入详情页"
requirements-completed: [V2-06, V2-07]
duration: 38m
completed: 2026-04-01
---

# Phase 6 Plan 3: 高频结果页与审计一致性 instrumentation 回归 Summary

**读卡、写卡、格式化和审计详情的 guidance/语义已经进入设备级自动回归，并补齐了格式化错误态与懒加载列表的真实页面可观测性。**

## Performance

- **Duration:** 38m
- **Started:** 2026-04-01T05:50:21Z
- **Completed:** 2026-04-01T06:27:42Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments
- 为读卡结果页、写卡三段式结果区、格式化 guidance 和审计列表/详情接入稳定 test tags。
- 新增 `Phase6FlowAndAuditConsistencyTest.kt`，覆盖读卡 recommendation、写卡三段式、格式化失败结构和审计详情字段一致性。
- 在设备测试中验证 UI guidance 与审计 who/stage/authenticity/impact/message 五类字段之间的端到端一致性。

## Task Commits

Each task was committed atomically:

1. **Task 1: 接入高频结果页与审计页的稳定 test tags** - `fa17dac` (feat)
2. **Task 2: 编写高频流程与审计一致性 instrumentation 回归** - `f33400c` (fix)

## Files Created/Modified
- `app/src/main/java/com/opencode/nfccardmanager/feature/read/ReadResultScreen.kt` - recommendation、真实性 badge 和 CTA 具备稳定设备观测节点。
- `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt` - 写卡执行结果、回读校验、推荐下一步三段式结构可被设备回归稳定断言。
- `app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatCardScreen.kt` - 格式化成功/失败 guidance 与 error-only 结构统一暴露 test tags。
- `app/src/main/java/com/opencode/nfccardmanager/feature/audit/AuditLogScreen.kt` - 审计列表根节点、筛选与列表项具备稳定滚动定位能力。
- `app/src/main/java/com/opencode/nfccardmanager/feature/audit/AuditLogDetailScreen.kt` - who / semantics / impact / message 四个详情分区具备稳定观测节点。
- `app/src/main/java/com/opencode/nfccardmanager/ui/component/AppUi.kt` - `StatusPill` 支持外部 modifier，方便在页面层挂接 test tags。
- `app/src/main/java/com/opencode/nfccardmanager/ui/test/AppTestTags.kt` - 新增 write root 等滚动和结果分区标签。
- `app/src/androidTest/java/com/opencode/nfccardmanager/feature/verification/Phase6FlowAndAuditConsistencyTest.kt` - 高频流程 guidance 与审计详情一致性的设备级回归。

## Decisions Made
- 格式化失败 guidance 不能依赖 `FormatCardResult` 一定存在；只要 ViewModel 已给出 error guidance，UI 就必须保留“发生了什么 / 为什么 / 推荐下一步”。
- 对 `LazyColumn` 的设备级断言统一基于根节点 tag + `performScrollToNode()`，避免把非首屏节点误判为不存在。

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] 为格式化 error-only 场景补齐结构化 guidance 渲染**
- **Found during:** Task 2（编写高频流程与审计一致性 instrumentation 回归）
- **Issue:** `FormatViewModel.onError()` 已产出 `resultGuidance`，但 `FormatCardScreen` 只在 `result != null` 时渲染结果卡片，导致失败设备回归和真实用户都看不到结构化失败 guidance。
- **Fix:** 在 `FormatStage.ERROR` 且存在 `resultGuidance` 时额外渲染失败结果卡，保留“发生了什么 / 为什么 / 推荐下一步”结构。
- **Files modified:** `app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatCardScreen.kt`
- **Verification:** `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.opencode.nfccardmanager.feature.verification.Phase6FlowAndAuditConsistencyTest`
- **Committed in:** `f33400c` (part of task commit)

**2. [Rule 3 - Blocking] 为写卡结果区与审计列表补齐滚动定位契约**
- **Found during:** Task 2（编写高频流程与审计一致性 instrumentation 回归）
- **Issue:** 写卡和审计列表都使用 `LazyColumn`，离屏节点不会默认进入组合，导致设备测试无法稳定定位写卡演示按钮、结果分区和目标日志项。
- **Fix:** 增加 `WRITE_ROOT` 等滚动根节点 tag，并在 instrumentation 中统一使用 `performScrollToNode()` 先把目标节点滚入视口后再断言/点击。
- **Files modified:** `app/src/main/java/com/opencode/nfccardmanager/ui/test/AppTestTags.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`, `app/src/androidTest/java/com/opencode/nfccardmanager/feature/verification/Phase6FlowAndAuditConsistencyTest.kt`
- **Verification:** `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.opencode.nfccardmanager.feature.verification.Phase6FlowAndAuditConsistencyTest`
- **Committed in:** `f33400c` (part of task commit)

---

**Total deviations:** 2 auto-fixed (1 missing critical, 1 blocking)
**Impact on plan:** 两项修复都直接提升了真实页面上的 guidance 可见性与设备回归稳定性，属于必要的验证闭环补齐。

## Issues Encountered
- 写卡结果区和审计列表依赖懒加载组合，设备回归必须先把目标节点滚入视口，才能得到稳定断言结果。

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Phase 6 的自动化与真机验证闭环已经成型，可直接作为后续 UI/流程调整的回归基线。
- 权限、真实性、高频 guidance 和审计语义都已具备设备级回归，不再只依赖 JVM 逻辑测试。

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: `.planning/phases/06-验证闭环与回归基线/06-03-SUMMARY.md`
- FOUND commit: `fa17dac`
- FOUND commit: `f33400c`
