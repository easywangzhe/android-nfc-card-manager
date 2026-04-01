---
phase: 06-验证闭环与回归基线
plan: 02
subsystem: testing
tags: [androidTest, permissions, navigation, authenticity]
requires:
  - phase: 06-验证闭环与回归基线
    provides: [共享 AppTestTags 与本地演示账号夹具]
  - phase: 02-首页与导航重构
    provides: [首页分组与角色入口裁剪]
  - phase: 04-高风险流程强化
    provides: [锁卡/解锁真实性与结果来源语义]
provides:
  - 登录、首页权限与路由守卫设备级回归
  - 锁卡/解锁高风险页面稳定 test tags
  - 解锁 demo-only 真实性边界自动化断言
affects: [06-03, permissions, lock, unlock, navigation]
tech-stack:
  added: []
  patterns: [首页滚动断言, 角色切换路由守卫回归, 高风险页滚动可达性]
key-files:
  created:
    - app/src/androidTest/java/com/opencode/nfccardmanager/feature/verification/Phase6PermissionAndAuthenticityTest.kt
  modified:
    - app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginScreen.kt
    - app/src/main/java/com/opencode/nfccardmanager/feature/home/HomeScreen.kt
    - app/src/main/java/com/opencode/nfccardmanager/feature/common/PermissionDeniedScreen.kt
    - app/src/main/java/com/opencode/nfccardmanager/feature/lock/LockRiskScreen.kt
    - app/src/main/java/com/opencode/nfccardmanager/feature/unlock/UnlockVerifyScreen.kt
    - app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt
key-decisions:
  - "Nav redirect 只有在 currentRoute 已存在时才执行，避免冷启动和设备回归时提前访问 graph。"
  - "高风险页必须支持纵向滚动，确保确认动作与 demo-only 结果在手机视口内可达。"
patterns-established:
  - "Pattern: 首页可见性测试先滚动 HOME_ROOT，再断言 section 和 entry tags"
  - "Pattern: 角色切换后的路由守卫直接通过 instrumentation 校验拒绝态回退"
requirements-completed: [V2-06]
duration: 52m
completed: 2026-04-01
---

# Phase 6 Plan 2: 登录/首页权限与高风险真实性 instrumentation 回归 Summary

**登录后角色入口裁剪、路由守卫和解锁 demo-only 边界已进入设备级自动回归，并修复了冷启动导航与高风险页可达性问题。**

## Performance

- **Duration:** 52m
- **Started:** 2026-04-01T05:34:30Z
- **Completed:** 2026-04-01T06:26:31Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- 给登录页、首页、拒绝态、锁卡页、解锁页接入稳定 test tags，形成权限和高风险真实性的设备观测层。
- 新增 `Phase6PermissionAndAuthenticityTest.kt`，覆盖四角色首页可见性、路由守卫回退与解锁 demo-only 边界。
- 在 API 35 模拟器上跑通 `connectedDebugAndroidTest`，把壳层权限与真实性回归从 JVM 级测试推进到真实页面链路。

## Task Commits

Each task was committed atomically:

1. **Task 1: 接入权限与高风险页的稳定 test tags** - `5fd05fa` (feat)
2. **Task 2: 编写权限与真实性 instrumentation 回归** - `010e3fd` (fix)

## Files Created/Modified
- `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginScreen.kt` - 登录输入框、按钮和错误提示增加稳定 tag。
- `app/src/main/java/com/opencode/nfccardmanager/feature/home/HomeScreen.kt` - 首页 section / entry 节点转为可滚动断言的稳定观测结构。
- `app/src/main/java/com/opencode/nfccardmanager/feature/common/PermissionDeniedScreen.kt` - 拒绝态根节点、标题、描述和返回按钮可被稳定定位。
- `app/src/main/java/com/opencode/nfccardmanager/feature/lock/LockRiskScreen.kt` - 风险摘要、真实性块和结果来源块支持 instrumentation 定位并在设备视口内可滚动访问。
- `app/src/main/java/com/opencode/nfccardmanager/feature/unlock/UnlockVerifyScreen.kt` - 解锁真实性、demo-only 结果和演示入口支持稳定断言并可滚动访问。
- `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt` - 冷启动登录重定向改为等待 route 初始化后再访问 graph。
- `app/src/androidTest/java/com/opencode/nfccardmanager/feature/verification/Phase6PermissionAndAuthenticityTest.kt` - 覆盖角色可见性、路由守卫与 demo-only 真实性的设备级回归。

## Decisions Made
- 设备级导航重定向必须等待 `currentRoute` 初始化，避免冷启动时提前读取 `navController.graph` 导致崩溃。
- 高风险页的确认按钮与演示结果区必须在真实手机视口中可达，否则即使语义正确也无法完成受控验证链路。

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] 修复 NavGraph 冷启动时提前访问 `navController.graph` 的崩溃**
- **Found during:** Task 2（编写权限与真实性 instrumentation 回归）
- **Issue:** 冷启动执行 connected tests 时，`AppNavGraph` 会在 NavHost 尚未完成 route 初始化前访问 `navController.graph.id`，导致设备测试和真实启动流程崩溃。
- **Fix:** 把登录重定向改为仅在 `currentRoute` 已存在且当前不在登录页时执行。
- **Files modified:** `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- **Verification:** `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.opencode.nfccardmanager.feature.verification.Phase6PermissionAndAuthenticityTest`
- **Committed in:** `010e3fd` (part of task commit)

**2. [Rule 2 - Missing Critical] 让锁卡/解锁页在设备视口内保持可滚动可达**
- **Found during:** Task 2（编写权限与真实性 instrumentation 回归）
- **Issue:** 高风险页在手机视口下可能把确认按钮、演示按钮和结果区压到首屏以下，既阻塞设备测试，也会影响真实用户完成验证。
- **Fix:** 为 `LockRiskScreen` 和 `UnlockVerifyScreen` 增加纵向滚动，并在 instrumentation 中使用 `performScrollTo()` 访问演示入口。
- **Files modified:** `app/src/main/java/com/opencode/nfccardmanager/feature/lock/LockRiskScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/unlock/UnlockVerifyScreen.kt`, `app/src/androidTest/java/com/opencode/nfccardmanager/feature/verification/Phase6PermissionAndAuthenticityTest.kt`
- **Verification:** `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.opencode.nfccardmanager.feature.verification.Phase6PermissionAndAuthenticityTest`
- **Committed in:** `010e3fd` (part of task commit)

---

**Total deviations:** 2 auto-fixed (1 bug, 1 missing critical)
**Impact on plan:** 两项修复都直接影响设备回归可执行性和真实页面稳定性，没有引入额外业务范围。

## Issues Encountered
- 本机最初没有可用设备，补齐本地 API 35 模拟器与系统镜像后，connected instrumentation 才能稳定执行。

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- 06-03 可以在同一套模拟器环境上继续验证高频结果页与审计一致性。
- 权限裁剪、路由守卫和 demo-only 边界已经有设备回归兜底，后续 UI 调整更不容易漂移。

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: `.planning/phases/06-验证闭环与回归基线/06-02-SUMMARY.md`
- FOUND commit: `5fd05fa`
- FOUND commit: `010e3fd`
