# Testing Patterns

**Analysis Date:** 2026-03-30

## Test Framework

**Runner:**
- 本地单元测试依赖 JUnit 4，声明于 `app/build.gradle.kts`：`testImplementation("junit:junit:4.13.2")`。
- Android 仪器测试依赖 `androidx.test.ext:junit:1.2.1`、`androidx.test.espresso:espresso-core:3.6.1`、`androidx.compose.ui:ui-test-junit4`，同样声明于 `app/build.gradle.kts`。
- Instrumentation runner 为 `androidx.test.runner.AndroidJUnitRunner`，配置位于 `app/build.gradle.kts`。

**Assertion Library:**
- 未检测到 Truth、AssertJ、Kotest 或 MockK；当前可确认的断言体系仅为 JUnit4 / AndroidX Test 默认能力，依据 `app/build.gradle.kts`。

**Run Commands:**
```bash
./gradlew test                 # 运行 JVM 单元测试
./gradlew connectedAndroidTest # 运行设备/模拟器测试（依赖 Android 环境，仓库未提供现成用例）
./gradlew assembleDebug        # 构建验证，见 `.github/workflows/android-build.yml`
```

## Test File Organization

**Location:**
- 标准测试目录预期应为 `app/src/test/` 与 `app/src/androidTest/`，因为 `app/build.gradle.kts` 已声明对应测试依赖。
- 实际扫描结果中，这两个目录下未发现任何 `.kt` 测试文件，说明当前仓库测试实现缺失。

**Naming:**
- 未检测到 `*Test.kt` 或 `*Spec.kt` 文件；当前无法归纳真实命名规范。

**Structure:**
```text
app/
├── src/main/java/...          # 实际业务代码
├── src/test/                  # 目录模式存在于 Gradle 约定中，但当前未发现测试文件
└── src/androidTest/           # 目录模式存在于 Gradle 约定中，但当前未发现测试文件
```

## Current State Summary

- 测试框架“已配置但未落地”：`app/build.gradle.kts` 声明了 JUnit、AndroidX Test、Espresso、Compose UI Test 依赖，但代码库中未发现任何测试源文件。
- CI 会执行 `./gradlew test`，见 `.github/workflows/android-build.yml` 第 42-46 行附近；由于没有测试文件，这一步更像是“空跑校验”。
- 发布流程 `.github/workflows/android-release.yml` 同样在构建前执行 `./gradlew test`，但没有覆盖 UI、NFC、数据库与权限流的真实回归风险。

## Existing Verification Practices

**CI / 自动验证：**
- `.github/workflows/android-build.yml` 在 push、pull_request、workflow_dispatch 时执行：
  - `./gradlew test`
  - `./gradlew assembleDebug`
- `.github/workflows/android-release.yml` 在 tag 发布时执行：
  - `./gradlew test`
  - `./gradlew assembleDebug`
- 当前没有发现单独的 lint、detekt、ktlint、截图测试或覆盖率上传步骤。

**应用内演示替代测试：**
- 多个页面内置“模拟成功 / 模拟异常 / 演示数据”按钮，用于人工验证流程，而非自动测试。
- `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt` 提供“模拟识别卡片”“模拟异常”“进入读卡结果演示”。
- `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt` 提供“模拟写卡成功”。
- `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt` 内置 `simulateReadCard()` 与 `demoCardInfo()` 支持演示路径。

## Test Structure

**Suite Organization:**
- Not detected。当前仓库没有任何真实测试套件。

**Recommended structure to follow current architecture:**
```kotlin
class ScanViewModelTest {
    @Test
    fun `startScan sets error state when NFC unavailable`() {
        val viewModel = ScanViewModel()

        viewModel.startScan(isNfcAvailable = false, isNfcEnabled = false)

        assertEquals(ScanStage.ERROR, viewModel.uiState.value.stage)
    }
}
```
- 上述结构是按现有 `ViewModel + StateFlow` 形态推导出的最贴近代码库风格的测试入口，对应源码在 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`。

**Patterns:**
- 现有代码最适合先覆盖纯 ViewModel 与纯 Kotlin 解析器：
  - `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginViewModel.kt`
  - `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`
  - `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`
  - `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt`
- Compose 页面大量依赖 `viewModel()`、`rememberCoroutineScope()`、`DisposableEffect` 与实际 NFC 环境，自动化成本高于 ViewModel 层，见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`、`app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`。

## Mocking

**Framework:**
- 未检测到 Mockito、MockK、MockWebServer 或其他 mock 框架；当前没有真实 mock 使用示例。

**Patterns:**
```kotlin
// 当前代码库未检测到 mock 模式；依赖创建主要是直接实例化：
val tagParser = TagParser()
val writer = NdefWriter()
val viewModel: ScanViewModel = viewModel()
```
- 直接实例化示例见 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt` 与 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`。

**What to Mock:**
- 新增自动测试时，优先替换以下边界依赖：
  - `NfcSessionManager`，源码位于 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`
  - `TagParser`，源码位于 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt`
  - `NdefWriter`，源码位于 `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NdefWriter.kt`
  - 全局单例 `SecurityManager`、`AuditLogManager`、`LocalTemplateRepository`

**What NOT to Mock:**
- `UiState` 数据类与纯 `copy(...)` 逻辑无需 mock，如 `ScanUiState` 位于 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanContract.kt`。
- 纯字符串映射与状态切换可直接断言最终 `StateFlow` 值，不必引入复杂替身。

## Fixtures and Factories

**Test Data:**
```kotlin
// 现有仓库中的内置演示数据可直接抽成测试 fixture
val demo = viewModel.demoCardInfo()
// 来源：`app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`
```

**Location:**
- 未检测到测试 fixtures/factories 目录。
- 当前最接近 fixture 的现成数据来自：
  - `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt` 的 `demoCardInfo()`
  - `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt` 中构造的演示 `WriteCardResult`

## Coverage

**Requirements:**
- None enforced。未检测到 JaCoCo、Kover、Codecov 或覆盖率门禁配置。

**View Coverage:**
```bash
./gradlew test   # 当前仓库未配置覆盖率任务
```

## Test Types

**Unit Tests:**
- 框架已声明，文件未落地。
- 最适合补充的单元测试对象：
  - `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginViewModel.kt`
  - `app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateManagementViewModel.kt`
  - `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`
  - `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt`

**Integration Tests:**
- 未检测到集成测试。
- 需要集成验证的高风险路径包括：
  - NFC reader mode 启停：`app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`
  - 审计日志写库：`app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogManager.kt`
  - 模板持久化：`app/src/main/java/com/opencode/nfccardmanager/feature/template/LocalTemplateRepository.kt`

**E2E Tests:**
- 框架依赖层面可做 Android instrumentation + Compose UI test，但当前未使用。
- 未检测到 Macrobenchmark、Firebase Test Lab、UI Automator 或截图回归测试。

## Common Patterns

**Async Testing:**
```kotlin
// 现有异步代码主要围绕 StateFlow / SharedFlow / coroutine scope
viewModel.startWriting()
assertEquals(WriteStage.WRITING, viewModel.uiState.value.stage)
```
- 此类断言模式适用于 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`。

**Error Testing:**
```kotlin
viewModel.onError("写入内容不能为空")
assertEquals(WriteStage.ERROR, viewModel.uiState.value.stage)
assertEquals("写入内容不能为空", viewModel.uiState.value.message)
```
- 对应源码位于 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`。

## Evidence of Testing Gaps

- `app/src/test/**/*.kt` 扫描结果为空。
- `app/src/androidTest/**/*.kt` 扫描结果为空。
- 未检测到 `jest.config.*`、`vitest.config.*`、`mockk`、`Mockito`、`Jacoco`、`Kover` 等测试或覆盖率配置。
- `.github/workflows/android-build.yml` 与 `.github/workflows/android-release.yml` 都运行 `./gradlew test`，但没有任何测试文件支撑真实断言。
- 复杂流程依赖人工点击演示按钮验证，尤其是 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt` 与 `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`。

## Prescriptive Guidance For Future Tests

- 新增测试时，优先在 `app/src/test/java/com/opencode/nfccardmanager/feature/...` 下与源码包结构镜像放置。
- 先测 ViewModel 和纯 Kotlin 组件，再补 Compose UI test；不要一开始就直接测真实 NFC 硬件交互。
- 对全局 `object` 依赖较强的模块，先通过构造函数参数或接口抽象拆出可替换依赖，再写测试；当前这一约束在 `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt` 已有轻量先例。
- 若新增 CI 门禁，应在 `.github/workflows/android-build.yml` 中追加 lint 或覆盖率任务，而不是只保留空跑 `./gradlew test`。

---

*Testing analysis: 2026-03-30*
