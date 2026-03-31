# Requirements: Android NFC Card Manager

**Defined:** 2026-03-31
**Core Value:** 让用户在执行 NFC 卡片关键操作时，始终能清楚理解当前状态、风险提示和下一步动作，从而更安全、更高效地完成任务。

## v1 Requirements

Requirements for this brownfield optimization round. Each requirement will map to exactly one roadmap phase.

### Shell & Access

- [ ] **SHELL-01**: User can distinguish primary tasks, high-risk tasks, and management tools from the home screen without opening each page.
- [ ] **SHELL-02**: User only sees home entry points that match their current role permissions.
- [x] **SHELL-03**: User cannot reach restricted pages through navigation or direct route access when their role lacks permission.
- [x] **SHELL-04**: User cannot execute restricted NFC operations even if they somehow enter a page without the required role.
- [x] **SHELL-05**: User can clearly tell when a capability is fully supported, unverified, demo-only, or not implemented.

### Flow Safety

- [x] **FLOW-01**: User must explicitly start NFC scanning before a read, write, format, lock, or unlock session begins.
- [x] **FLOW-02**: User can perform one NFC operation session at a time without duplicate callbacks or repeated execution.
- [x] **FLOW-03**: User can distinguish the current phase of an operation, including waiting, scanning, processing, success, and failure.
- [ ] **FLOW-04**: User can distinguish write success from read-back verification success after a write operation.
- [ ] **FLOW-05**: User sees the next recommended action after an operation succeeds, fails, or remains unverified.

### High-Risk Operations

- [ ] **RISK-01**: User sees a risk summary and required confirmations before lock, unlock, or format can start.
- [ ] **RISK-02**: User can see which lock methods are supported by the current product and which are not supported.
- [ ] **RISK-03**: User can see whether a locked card can be unlocked in the current scenario, and whether that outcome is real support or only process demonstration.
- [ ] **RISK-04**: User can see whether a format failure happened, the likely reason, and the safest next step.
- [ ] **RISK-05**: User cannot accidentally leave or re-trigger a high-risk operation while processing is in progress.
- [ ] **RISK-06**: User can distinguish operation result source, including confirmed execution, failed execution, unverified result, or demo-only result.
- [ ] **RISK-07**: User receives failure recovery guidance for high-risk operations based on the actual failure condition.

### Support Pages & Audit

- [ ] **SUPP-01**: User can scan template, audit, and settings pages with a consistent information hierarchy and visual structure.
- [ ] **SUPP-02**: User can read audit logs with enough context to understand who performed the action, under which role, in what stage, and with what authenticity status.
- [ ] **SUPP-03**: User sees sensitive card or operation details with role-appropriate masking or graded disclosure.
- [ ] **SUPP-04**: User can understand whether a cached result, audit record, or settings action affects safety, traceability, or only local convenience.

## v2 Requirements

Deferred until the core UX and risk boundaries are stable.

### Audit & Efficiency

- **V2-01**: User can export audit records as a read-only share package for external review.
- **V2-02**: User can mark templates as favorites, recent, or categorized for faster reuse.
- **V2-03**: User can resume the most recent task from a controlled quick-access entry.

### Device & Capability Enhancements

- **V2-04**: User can use adaptive tablet or landscape layouts where the target device fleet requires it.
- **V2-05**: User can see enhanced card capability guidance for partially supported or unsupported tags.

## Out of Scope

Explicitly excluded from this roadmap.

| Feature | Reason |
|---------|--------|
| Remote account system or backend sync | Conflicts with current offline-first brownfield scope |
| Full rewrite of NFC core execution layer | This round prioritizes UX clarity and risk control over replacing working low-level logic |
| Generic real unlock protocol integration | Current unlock remains partially demonstrative and should be clarified before expansion |
| Batch auto-write or continuous high-risk automation | Increases misoperation and audit risk |
| Large-scale architecture rewrite or framework replacement | Too disruptive for a brownfield optimization milestone |
| Visual effects-first redesign | Does not solve the user's main clarity and safety problems |

## Traceability

Which phases cover which requirements.

| Requirement | Phase | Status |
|-------------|-------|--------|
| SHELL-01 | Phase 2 | Pending |
| SHELL-02 | Phase 2 | Pending |
| SHELL-03 | Phase 1 | Complete |
| SHELL-04 | Phase 1 | Complete |
| SHELL-05 | Phase 1 | Complete |
| FLOW-01 | Phase 1 | Complete |
| FLOW-02 | Phase 1 | Complete |
| FLOW-03 | Phase 1 | Complete |
| FLOW-04 | Phase 3 | Pending |
| FLOW-05 | Phase 3 | Pending |
| RISK-01 | Phase 4 | Pending |
| RISK-02 | Phase 4 | Pending |
| RISK-03 | Phase 4 | Pending |
| RISK-04 | Phase 3 | Pending |
| RISK-05 | Phase 4 | Pending |
| RISK-06 | Phase 4 | Pending |
| RISK-07 | Phase 4 | Pending |
| SUPP-01 | Phase 5 | Pending |
| SUPP-02 | Phase 5 | Pending |
| SUPP-03 | Phase 4 | Pending |
| SUPP-04 | Phase 5 | Pending |

**Coverage:**
- v1 requirements: 21 total
- Mapped to phases: 21
- Unmapped: 0 ✓

---
*Requirements defined: 2026-03-31*
*Last updated: 2026-03-31 after initial definition*
