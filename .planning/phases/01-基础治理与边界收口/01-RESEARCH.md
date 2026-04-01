# Phase 01: 基础治理与边界收口 - Research

**Researched:** 2026-03-31
**Status:** Complete

## Scope

Phase 1 needs to stabilize four cross-cutting concerns before broader UI rework:

1. 权限边界必须同时在导航层和执行层成立（SHELL-03, SHELL-04）
2. NFC 流程必须由用户显式开始，而不是页面进入即自动开扫（FLOW-01）
3. 任一时刻只能有一个受控 ReaderMode 会话，避免重复贴卡与重复执行（FLOW-02）
4. 页面必须明确表达阶段状态与能力真实性，尤其区分真实支持、未验证、demo-only、未实现（SHELL-05, FLOW-03）

## Current Code Findings

### 1) Route permission checks exist, but operation safety is not centralized

- `navigation/AppNavGraph.kt` already blocks restricted routes for scan/write/format/lock/unlock/template/audit pages.
- `SecurityManager.kt` exposes `canRead/canWrite/canLock/canUnlock/...` policies.
- But most feature screens still execute NFC logic directly after screen state changes; there is no shared execution gate object that every operation goes through.

Implication:
- Route protection partially satisfies `SHELL-03`.
- `SHELL-04` is still fragile because execution rules are duplicated per screen instead of enforced by a single operation/session policy.

### 2) Multiple NFC pages auto-start scanning on entry

Observed auto-start patterns:

- `feature/scan/ScanScreen.kt` starts `viewModel.startScan(...)` inside `DisposableEffect`
- `feature/write/WriteEditorScreen.kt` starts ReaderMode automatically when `uiState.stage == WRITING`
- `feature/format/FormatCardScreen.kt` calls `viewModel.start()` in `LaunchedEffect(Unit)` and auto-enters scanning
- `feature/lock/LockRiskScreen.kt` starts ReaderMode automatically when `stage == LOCKING`
- `feature/unlock/UnlockVerifyScreen.kt` starts ReaderMode automatically when `stage == SCANNING`

Implication:
- Read/format definitely violate the intended “explicit start” contract.
- Write/lock/unlock have partial gating, but once state flips they still rely on per-screen lifecycle side effects rather than a shared session controller.

### 3) No app-level single-session arbitration exists

- `core/nfc/NfcSessionManager.kt` is only an Activity wrapper around `enableReaderMode/disableReaderMode`.
- It validates adapter availability and activity usability, but it does **not** track whether another flow is already active.
- Each screen constructs its own `NfcSessionManager(activity)` and manages lifecycle locally.

Risk:
- Re-entry, recomposition, or quick navigation between NFC screens can produce overlapping “ready to scan” states.
- Flow-level dedupe is ad hoc (`ScanViewModel` only suppresses a repeated success for the same UID in one mode).

Recommendation:
- Add a shared session coordinator in `core/nfc/` as the single source of truth for active operation type, session token, and lifecycle.
- Screens should request permission to start/stop a session through this coordinator before enabling ReaderMode.

### 4) Stage vocabularies are inconsistent across flows

Examples:

- `ScanStage`: `IDLE`, `SCANNING`, `TAG_DETECTED`, `SUCCESS`, `ERROR`
- `WriteStage`: `IDLE`, `READY`, `WRITING`, `SUCCESS`, `ERROR`
- `FormatStage` / `LockStage` / `UnlockStage` use their own names and meanings

Implication:
- `FLOW-03` is only partially met. Internally there are stages, but the user-facing semantics are inconsistent.
- Phase 1 should not force a giant shared enum rewrite; instead define a shared display vocabulary (waiting / scanning / processing / success / failure) and map feature-specific stages onto it.

### 5) Authenticity / demo-state signaling is present but scattered and inconsistent

Evidence:

- `ScanViewModel.kt` demo path embeds `demo=true` only in debug text and uses “演示卡片” summary
- `WriteEditorScreen.kt` has “模拟写卡成功” with success-looking result fields
- `LockRiskScreen.kt` and `UnlockVerifyScreen.kt` both offer simulation buttons that can look like final success states
- `UnlockExecutor.kt` explicitly says unlock is still “流程演示”, but that truth is not normalized into a reusable authenticity model
- `CardCapabilityResolver.kt` currently describes technical capability, not authenticity status

Implication:
- `SHELL-05` needs a shared authenticity/status model used by all high-signal result cards and capability summaries.

Recommendation:
- Introduce a small shared UI/domain model such as:
  - `SUPPORTED_REAL`
  - `SUPPORTED_UNVERIFIED`
  - `DEMO_ONLY`
  - `NOT_SUPPORTED`
- Bind every relevant CTA/result/status card to that model instead of free-text-only explanations.

## Brownfield Strategy

### Recommended implementation order

1. **Foundation first**: add shared operation/authenticity contracts and session coordinator
2. **Execution gating second**: make all NFC entry points request explicit start + single active session
3. **UI normalization last**: update route/screen state text and status cards to consume the shared contracts

This minimizes churn because later phases can reuse the contracts instead of redefining behavior per screen.

### Files likely to matter in planning

- `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt`
- `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`
- `app/src/main/java/com/opencode/nfccardmanager/core/nfc/CardCapabilityResolver.kt`
- `app/src/main/java/com/opencode/nfccardmanager/core/nfc/model/CardModels.kt`
- `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanContract.kt`
- `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`
- `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteContract.kt`
- `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`
- `app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatViewModel.kt`
- `app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatCardScreen.kt`
- `app/src/main/java/com/opencode/nfccardmanager/feature/lock/LockContract.kt`
- `app/src/main/java/com/opencode/nfccardmanager/feature/lock/LockRiskScreen.kt`
- `app/src/main/java/com/opencode/nfccardmanager/feature/unlock/UnlockContract.kt`
- `app/src/main/java/com/opencode/nfccardmanager/feature/unlock/UnlockVerifyScreen.kt`

## Planning Guidance

### Do

- Reuse existing `Screen + ViewModel + Contract` pattern
- Keep NFC low-level executors (`TagParser`, `NdefWriter`, `NdefFormatter`, `NdefLocker`, `UnlockExecutor`) intact unless needed for session/authenticity metadata
- Add shared contracts in `core/nfc/` or `core/nfc/model/` and let screens consume them
- Prefer small ViewModel/state refactors over navigation rewrites
- Add minimal JVM tests around new shared coordinator / mapping logic because current repo has no real tests

### Avoid

- Do not introduce Hilt/Koin or major architecture replacement
- Do not rewrite all flows into `domain/usecase` during this phase
- Do not claim real unlock support; preserve demo boundary explicitly
- Do not leave authenticity wording as per-screen ad hoc text

## Common Pitfalls

1. **Only fixing route guards** — this misses `SHELL-04`; execution guards still need a shared backstop.
2. **Only adding a “开始扫描” button visually** — if `DisposableEffect` still auto-starts session on entry, requirement still fails.
3. **Adding a global lock without UI state mapping** — user still cannot tell whether the flow is waiting, scanning, processing, success, or failure.
4. **Treating demo buttons as harmless dev tools** — they currently produce success-like outcomes and must be visibly classified.
5. **Testing only build success** — Phase 1 changes affect state transitions; at least one small unit-test scaffold should be planned.

## Validation Architecture

Phase 1 should validate three layers:

1. **Contract mapping tests**
   - shared stage mapping returns the expected user-facing phase labels
   - authenticity mapping returns the expected label/tone for supported/unverified/demo/not-supported

2. **Session arbitration tests**
   - starting a second NFC operation while one is active is rejected
   - stopping a non-owner or stale session token does not clear the current active session
   - explicit start is required before a screen can claim scanning state

3. **Build/regression checks**
   - `./gradlew test`
   - `./gradlew assembleDebug`

Because the repository currently lacks tests, plans should include Wave 0 test scaffolding wherever a task introduces new shared logic.

## Research Conclusion

Phase 1 is best planned as three focused execution slices:

1. shared governance contracts (permission/authenticity/session/stage vocabulary)
2. NFC flow session gating across read/write/format/lock/unlock
3. route + screen UI normalization to show explicit start, consistent phases, and authenticity truth

This gives later phases a stable shell instead of repeating safety/status fixes in each UI rewrite.
