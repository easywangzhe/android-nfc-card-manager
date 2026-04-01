---
phase: 1
slug: 基础治理与边界收口
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-03-31
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Gradle Android test tasks |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew test` |
| **Full suite command** | `./gradlew test assembleDebug` |
| **Estimated runtime** | ~30-60 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew test`
- **After every plan wave:** Run `./gradlew test assembleDebug`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 1-01-01 | 01 | 1 | SHELL-05, FLOW-03 | unit | `./gradlew test --tests "*NfcFlowContract*"` | ❌ W0 | ⬜ pending |
| 1-01-02 | 01 | 1 | FLOW-02 | unit | `./gradlew test --tests "*NfcSessionCoordinator*"` | ❌ W0 | ⬜ pending |
| 1-02-01 | 02 | 2 | FLOW-01, FLOW-02 | unit + build | `./gradlew test assembleDebug` | ✅ | ⬜ pending |
| 1-02-02 | 02 | 2 | FLOW-03 | build | `./gradlew assembleDebug` | ✅ | ⬜ pending |
| 1-03-01 | 03 | 3 | SHELL-03, SHELL-04 | unit + build | `./gradlew test assembleDebug` | ✅ | ⬜ pending |
| 1-03-02 | 03 | 3 | SHELL-05, FLOW-01, FLOW-03 | build | `./gradlew assembleDebug` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/opencode/nfccardmanager/core/nfc/NfcFlowContractTest.kt` — stage/authenticity mapping tests for SHELL-05 and FLOW-03
- [ ] `app/src/test/java/com/opencode/nfccardmanager/core/nfc/NfcSessionCoordinatorTest.kt` — single-session arbitration tests for FLOW-02
- [ ] No framework install needed — existing Gradle/JUnit setup is sufficient

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Read/write/format/lock/unlock pages do not begin scanning until user presses the explicit start CTA | FLOW-01 | ReaderMode interaction is bound to Android UI lifecycle and real NFC environment | Open each flow on device/emulator, confirm initial state is waiting/not scanning, then tap start and verify scanning state appears |
| Demo-only, supported, unverified, and unsupported capability/result states are visually distinguishable | SHELL-05 | Requires reviewing final UI wording/tone on multiple screens | Use built-in demo actions and real route states to confirm authenticity badges/text differ clearly |
| Returning between NFC pages still leaves only one active flow session | FLOW-02 | Needs navigation timing and screen lifecycle validation | Start one flow, navigate away/back/into another flow, confirm the original session is released or blocked and duplicate execution does not occur |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
