---
phase: 04-高风险流程强化
plan: 01
subsystem: core
tags: [nfc, risk, guidance, security, test]
requires:
  - phase: 03-高频流程澄清
    provides: [共享结果指导契约]
provides:
  - 锁卡与解锁共享高风险支持边界契约
  - 高风险结果来源与恢复建议映射
  - 角色分级敏感信息遮罩策略
affects: [lock, unlock, security]
tech-stack:
  added: []
  patterns: [高风险页面统一消费纯 Kotlin guidance 与遮罩策略]
key-files:
  created:
    - app/src/main/java/com/opencode/nfccardmanager/core/nfc/model/HighRiskFlowGuidance.kt
    - app/src/main/java/com/opencode/nfccardmanager/core/security/SensitiveDisplayPolicy.kt
    - app/src/test/java/com/opencode/nfccardmanager/core/nfc/HighRiskFlowGuidanceTest.kt
  modified: []
key-decisions:
  - "锁卡支持边界与解锁 demo-only 语义统一收口到 HighRiskFlowGuidance，页面不再自行拼接真假支持判断。"
  - "敏感 UID/凭据展示统一按角色分级：管理员全量、主管半遮罩、操作员/审计员强遮罩。"
requirements-completed: [RISK-02, RISK-03, RISK-06, RISK-07, SUPP-03]
duration: 6m
completed: 2026-03-31
---

# Phase 4 Plan 1: 高风险共享指导契约 Summary

**锁卡与解锁现在共享同一套高风险支持边界、结果来源和敏感信息遮罩规则。**

## Performance

- **Duration:** 6m
- **Completed:** 2026-03-31T09:15:53Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- 新增 `HighRiskFlowGuidance`，统一输出锁卡/解锁的支持摘要、结果来源、结论、恢复建议与 CTA。
- 新增 `SensitiveDisplayPolicy`，让高风险页面直接复用角色分级遮罩逻辑。
- `HighRiskFlowGuidanceTest` 锁定密码保护、永久只读、不支持、未验证与 demo-only 语义。

## Task Commits

1. **Task 1: 先锁定高风险指导契约与敏感信息遮罩测试** - `0af34be` (test)
2. **Task 2: 实现高风险共享指导契约与角色分级遮罩策略** - `1cfb9cf` (feat)

## Files Created/Modified
- `app/src/main/java/com/opencode/nfccardmanager/core/nfc/model/HighRiskFlowGuidance.kt` - 高风险共享支持/结果指导契约。
- `app/src/main/java/com/opencode/nfccardmanager/core/security/SensitiveDisplayPolicy.kt` - 角色分级遮罩策略。
- `app/src/test/java/com/opencode/nfccardmanager/core/nfc/HighRiskFlowGuidanceTest.kt` - 高风险指导语义回归测试。

## Decisions Made
- 解锁即使 `success=true` 也仍按 `demo-only` 输出，避免流程骨架被误读为真实解锁成功。
- 锁卡失败恢复建议按“不支持 / 认证条件 / 通用失败”分支输出，而不是统一写成重试。

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: `.planning/phases/04-高风险流程强化/04-01-SUMMARY.md`
- FOUND commit: `0af34be`
- FOUND commit: `1cfb9cf`
