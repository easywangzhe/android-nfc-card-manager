---
phase: 03-高频流程澄清
plan: 01
subsystem: ui
tags: [kotlin, nfc, guidance, write, read, format]
requires:
  - phase: 01-基础治理与边界收口
    provides: [统一 NFC 阶段语义, 真实性表达, 既有 status truth source]
provides:
  - 写卡执行结果与回读校验结果共享指导契约
  - 读卡状态推荐动作共享映射
  - 格式化结果安全后续动作共享映射
affects: [write, read, format, phase-3]
tech-stack:
  added: []
  patterns: [页面通过纯 Kotlin 指导契约消费既有状态码]
key-files:
  created:
    - app/src/main/java/com/opencode/nfccardmanager/core/nfc/model/HighFrequencyFlowGuidance.kt
    - app/src/test/java/com/opencode/nfccardmanager/core/nfc/HighFrequencyFlowGuidanceTest.kt
  modified: []
key-decisions:
  - "写卡页的执行结果、校验结果与下一步建议先抽成纯 Kotlin 契约，再由各页面消费。"
  - "共享指导层只消费既有 status code，不改写底层 NdefWriter / NdefFormatter 的 truth source。"
patterns-established:
  - "Pattern: 高频结果页统一输出 title/conclusion/reasonSummary/recommendedAction/ctaLabel"
  - "Pattern: VERIFY_FAILED 视为写入已执行但校验未通过，而不是普通写入失败"
requirements-completed: [FLOW-05, RISK-04]
duration: 8m
completed: 2026-03-31
---

# Phase 3 Plan 1: 高频指导契约 Summary

**纯 Kotlin 高频流程指导契约已统一写卡执行/校验差异、读卡推荐动作和格式化安全后续动作。**

## Performance

- **Duration:** 8m
- **Started:** 2026-03-31T09:21:00Z
- **Completed:** 2026-03-31T09:29:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- 新增 `HighFrequencyFlowGuidance.kt`，统一输出写卡、读卡、格式化三类 UI 可直接消费的指导模型。
- 通过 `HighFrequencyFlowGuidanceTest` 锁定写卡执行/校验拆分、读卡推荐动作、格式化失败安全动作语义。

## Task Commits

1. **Task 1: 先写高频流程指导契约测试骨架** - `6434daa` (test)
2. **Task 2: 实现共享的结果拆分与下一步指导契约** - `455f8ee` (feat)

**Plan metadata:** `pending`

## Files Created/Modified
- `app/src/main/java/com/opencode/nfccardmanager/core/nfc/model/HighFrequencyFlowGuidance.kt` - 统一输出写卡、读卡、格式化结果指导。
- `app/src/test/java/com/opencode/nfccardmanager/core/nfc/HighFrequencyFlowGuidanceTest.kt` - 回归锁定高频流程指导语义。

## Decisions Made
- 先把结果差异收口到纯 Kotlin 契约，再让页面消费，避免三个页面各自硬编码 `when(status)` 文案。
- `VERIFY_FAILED` 单独建模为“写入执行成功但回读校验失败”，保证后续页面不会误写成普通写卡失败。

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- 写卡页、格式化页和读卡结果页可以直接消费共享指导契约，不再重复维护推荐动作文案。
- Phase 3 后续页面实现已拥有稳定 truth source，可继续做 UI 层状态拆分。

## Known Stubs

None.

---
*Phase: 03-高频流程澄清*
*Completed: 2026-03-31*

## Self-Check: PASSED

- FOUND: `.planning/phases/03-高频流程澄清/03-01-SUMMARY.md`
- FOUND commit: `6434daa`
- FOUND commit: `455f8ee`
