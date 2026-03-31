---
phase: 03-高频流程澄清
plan: 03
subsystem: ui
tags: [compose, read, format, guidance, viewmodel]
requires:
  - phase: 03-高频流程澄清
    provides: [共享高频流程指导契约]
provides:
  - 格式化页失败诊断与安全动作表达
  - 读卡结果页按 readStatus 渲染推荐下一步
  - 格式化结果 ViewModel guidance 映射测试
affects: [format, read, phase-4]
tech-stack:
  added: []
  patterns: [读卡与格式化页面统一消费共享 guidance 契约]
key-files:
  created: []
  modified:
    - app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatContract.kt
    - app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatViewModel.kt
    - app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatCardScreen.kt
    - app/src/main/java/com/opencode/nfccardmanager/feature/read/ReadResultScreen.kt
    - app/src/test/java/com/opencode/nfccardmanager/feature/format/FormatViewModelPhase3Test.kt
key-decisions:
  - "格式化失败页统一按发生了什么 / 为什么 / 当前最安全下一步表达，不再只重复 reason 原文。"
  - "读卡结果页不新增路由，只通过 guidance 文案与按钮文案顺序来前移推荐动作。"
patterns-established:
  - "Pattern: 格式化失败态必须显式说明失败已发生，不能使用类似成功后的含混文案"
  - "Pattern: 读卡结果按钮文案与 guidance CTA 保持一致"
requirements-completed: [FLOW-05, RISK-04]
duration: 12m
completed: 2026-03-31
---

# Phase 3 Plan 3: 读卡与格式化结果指导 Summary

**格式化页已补齐失败诊断和安全动作，读卡结果页也会按真实 `readStatus` 给出明确下一步建议。**

## Performance

- **Duration:** 12m
- **Started:** 2026-03-31T09:43:00Z
- **Completed:** 2026-03-31T09:55:00Z
- **Tasks:** 3
- **Files modified:** 5

## Accomplishments
- `FormatViewModel` 与 `FormatCardScreen` 已消费共享 guidance，能够稳定表达成功去写卡和失败安全动作。
- `ReadResultScreen` 现按 `READ_SUCCESS`、`EMPTY_NDEF`、`NON_NDEF`、`READ_ERROR` 渲染不同推荐动作说明与按钮文案。
- `FormatViewModelPhase3Test` 锁定格式化成功、卡片不支持、清空异常、格式化异常四类关键语义。

## Task Commits

1. **Task 1: 先锁定格式化结果指导的 ViewModel 测试** - `5aef5c0` (test)
2. **Task 2: 在格式化状态层与页面接入失败诊断和安全动作** - `c578b8b` (feat)
3. **Task 3: 让读卡结果页按读卡状态给出明确下一步建议** - `935a474` (feat)

**Plan metadata:** `pending`

## Files Created/Modified
- `app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatContract.kt` - 增加格式化 guidance 状态字段。
- `app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatViewModel.kt` - 映射格式化结果到共享 guidance。
- `app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatCardScreen.kt` - 呈现失败结论、原因和最安全下一步。
- `app/src/main/java/com/opencode/nfccardmanager/feature/read/ReadResultScreen.kt` - 按读卡状态显示不同建议动作与 CTA。
- `app/src/test/java/com/opencode/nfccardmanager/feature/format/FormatViewModelPhase3Test.kt` - 验证格式化 guidance 语义。

## Decisions Made
- 格式化结果页要先明确“失败已发生”，再给出原因和安全动作，避免用户误以为卡片已经被清空。
- 读卡结果页沿用现有按钮边界，不新增路由，只让 guidance 文案和按钮 CTA 与推荐动作一致。

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Phase 3 三个高频流程都已共享同一套结果指导语义，Phase 4 可延续到锁卡/解锁等高风险流程。
- 读卡与格式化页结果表达已经具备“结果、原因、建议动作”结构，可直接作为高风险页强化模板。

## Known Stubs

None.

---
*Phase: 03-高频流程澄清*
*Completed: 2026-03-31*

## Self-Check: PASSED

- FOUND: `.planning/phases/03-高频流程澄清/03-03-SUMMARY.md`
- FOUND commit: `5aef5c0`
- FOUND commit: `c578b8b`
- FOUND commit: `935a474`
