# Codebase Concerns

**Analysis Date:** 2026-03-30

## Tech Debt

**[确认问题] 演示鉴权与角色控制混入正式主流程：**
- Issue: `SecurityManager` 使用硬编码账号与固定密码 `123456`，并允许任意已登录用户通过首页角色切换直接调用 `switchRole` 提升角色；权限控制停留在本地 UI / 路由层，未形成不可绕过的授权边界。
- Files: `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/security/SessionStore.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginViewModel.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/home/HomeScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`, `README.md`
- Impact: 任意登录用户都可切到 `ADMIN`/`SUPERVISOR` 看到高风险入口并执行锁卡/解锁；登录态与角色还会被持久化，导致设备重启后继续保留提升后的权限。
- Fix approach: 将演示账号与角色切换从生产主路径剥离；把权限判断与操作执行绑定到统一授权层；禁止普通页面直接切换角色；将 `SessionStore` 改为受保护存储并保存服务端/签名后的会话。

**[确认问题] 解锁/密码保护锁卡仍是流程骨架，但 UI 按成功路径呈现：**
- Issue: `PasswordProtectedLocker.lock` 未对标签执行任何 NFC 命令即返回成功；`UnlockExecutor.execute` 仅校验固定凭据后直接返回成功；README 与页面文案也声明为骨架实现。
- Files: `app/src/main/java/com/opencode/nfccardmanager/core/nfc/PasswordProtectedLocker.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/nfc/UnlockExecutor.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/unlock/UnlockVerifyScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/lock/LockRiskScreen.kt`, `README.md`
- Impact: 审计日志会记录“SUCCESS”，但物理卡状态未变化，容易让操作员误判卡已被保护/解锁。
- Fix approach: 将骨架能力明确标记为不可执行的 mock；未接入真实命令前返回 `FAILED`/`NOT_IMPLEMENTED`；把演示按钮与真实入口分开。

**[确认问题] 领域层存在空壳 UseCase，实际逻辑散落在 UI Screen 与工具类：**
- Issue: `ReadCardUseCase`、`WriteCardUseCase`、`LockCardUseCase`、`UnlockCardUseCase` 为空类，真正业务流程直接写在 Screen/ViewModel/NFC helper 中。
- Files: `app/src/main/java/com/opencode/nfccardmanager/domain/usecase/ReadCardUseCase.kt`, `app/src/main/java/com/opencode/nfccardmanager/domain/usecase/WriteCardUseCase.kt`, `app/src/main/java/com/opencode/nfccardmanager/domain/usecase/LockCardUseCase.kt`, `app/src/main/java/com/opencode/nfccardmanager/domain/usecase/UnlockCardUseCase.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/lock/LockRiskScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/unlock/UnlockVerifyScreen.kt`
- Impact: 业务复用、测试隔离与替换真实 NFC 实现的成本高；同类流程难以统一超时、审计、异常映射策略。
- Fix approach: 把 NFC 读/写/锁/解锁流程上收进 UseCase 或 coordinator 层，Screen 仅负责触发与渲染状态。

**[潜在风险] 大型 Compose 页面与 NFC 工具类承担过多职责：**
- Issue: 多个页面和核心 NFC 类体积偏大，既包含 UI，又包含 ReaderMode 生命周期、错误文案、审计触发、数据转换。
- Files: `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NdefWriter.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/read/ReadResultScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/lock/LockRiskScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/unlock/UnlockVerifyScreen.kt`
- Impact: 改动容易牵连 UI、硬件交互和审计逻辑；后续接入更多卡型时回归风险高。
- Fix approach: 拆分为“页面状态机 + ReaderMode 控制器 + NFC 执行器 + 审计适配器”。

## Known Bugs

**[确认问题] 任意登录用户可在首页切换到高权限角色：**
- Symptoms: 登录后首页“角色切换”直接展示所有 `UserRole.entries`，点击即调用 `SecurityManager.switchRole`。
- Files: `app/src/main/java/com/opencode/nfccardmanager/feature/home/HomeScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt`
- Trigger: 以 `operator` 或 `auditor` 登录，切换到 `ADMIN` 或 `SUPERVISOR` 后访问锁卡、解锁、模板等页面。
- Workaround: 无代码层缓解；只能依赖人为不使用角色切换控件。

**[确认问题] 锁卡/解锁执行权限与权限矩阵文档不一致：**
- Symptoms: 文档要求高风险操作默认“主管发起/审批，管理员执行”，但代码允许 `SUPERVISOR` 直接执行锁卡和解锁。
- Files: `docs/权限矩阵文档.md`, `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt`, `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- Trigger: 主管账号登录后进入 `Routes.LOCK_RISK` 或 `Routes.UNLOCK_VERIFY`。
- Workaround: 仅靠流程约定限制主管不要执行。

**[确认问题] 审计日志操作者恒为默认值，无法追溯真实执行人：**
- Symptoms: `AuditLogManager.save` 默认 `operatorId = "system"`，调用处未传入当前会话用户。
- Files: `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogManager.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteViewModel.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/lock/LockViewModel.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/unlock/UnlockViewModel.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatViewModel.kt`
- Trigger: 任意操作完成后查看 `日志审计` 页面与日志详情页。
- Workaround: 无；当前所有记录都缺少真实责任人。

**[确认问题] 读卡结果页显示明文 UID，与文档“默认脱敏”要求冲突：**
- Symptoms: 扫描页与读卡结果页直接展示 `cardInfo.uid`，只有审计日志另行做了脱敏。
- Files: `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanViewModel.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/read/ReadResultScreen.kt`, `docs/权限矩阵文档.md`
- Trigger: 完成一次读卡后进入 `ReadResultScreen`。
- Workaround: 无；只能依赖使用者避免截图或传播页面内容。

**[潜在风险] NDEF 能力解析对真实卡型存在误判空间：**
- Symptoms: `CardCapabilityResolver` 仅依据 `TechType` 静态映射写/锁/解锁能力；`TagParser.resolveTechType` 也只按 tech 名称字符串优先级归类。
- Files: `app/src/main/java/com/opencode/nfccardmanager/core/nfc/CardCapabilityResolver.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt`
- Trigger: 遇到同时暴露多种 tech、但不支持对应认证/锁定命令的标签。
- Workaround: 先人工验证卡片规格；不要仅依据应用给出的“支持解锁/密码保护”提示执行高风险操作。

## Security Considerations

**[确认问题] 本地会话与角色信息以明文 SharedPreferences 保存：**
- Risk: 设备调试、root、备份恢复或本地取证可直接读取用户名、显示名、角色；与 `allowBackup="true"` 组合后会被系统备份通道带走。
- Files: `app/src/main/java/com/opencode/nfccardmanager/core/security/SessionStore.kt`, `app/src/main/AndroidManifest.xml`
- Current mitigation: 未检测到加密存储、备份排除或设备绑定。
- Recommendations: 使用 `EncryptedSharedPreferences`/Jetpack Security 或账号令牌方案；为敏感数据关闭备份或配置 backup rules。

**[确认问题] 演示凭据在代码与文档中明示：**
- Risk: 固定密码 `123456` 同时出现在登录和解锁路径；README 与 UI 占位文案也公开该凭据。
- Files: `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/nfc/UnlockExecutor.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/auth/LoginViewModel.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/unlock/UnlockVerifyScreen.kt`, `README.md`
- Current mitigation: 仅靠“演示版”文字提示。
- Recommendations: 删除硬编码凭据；将 demo 模式与真实构建变体隔离；避免在仓库文档中保留可执行凭据。

**[确认问题] NFC 敏感标识在多个页面明文展示且可被截图/旁观读取：**
- Risk: `UID`、技术栈、调试信息、回读内容、模板内容均明文显示，缺少权限差异化展示与防录屏措施。
- Files: `app/src/main/java/com/opencode/nfccardmanager/feature/read/ReadResultScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/audit/AuditLogDetailScreen.kt`
- Current mitigation: 审计列表中只对 UID 做部分脱敏。
- Recommendations: 对高敏字段按角色脱敏；为敏感页面启用 `FLAG_SECURE`；记录明文查看行为。

**[潜在风险] CI 权限偏大且自动发布使用可写仓库权限：**
- Risk: GitHub Actions 工作流声明 `contents: write`，并在每次 `master` push 后自动发布 `latest` 预发布包。
- Files: `.github/workflows/android-build.yml`
- Current mitigation: 未检测到环境保护、签名校验或分环境发布策略。
- Recommendations: 将构建与发布拆分；默认最小化权限；只在受控 tag/release 事件中发布。

## Performance Bottlenecks

**[潜在风险] 审计与模板数据库访问均为同步 SQLite 调用：**
- Problem: `insert/queryAll/queryById` 直接在调用线程执行；ViewModel 和页面加载日志时未统一切到 IO 线程。
- Files: `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogDbHelper.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogRepository.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/audit/AuditLogScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/template/TemplateDbHelper.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/template/LocalTemplateRepository.kt`
- Cause: 当前数据层未使用 Room/DAO/coroutine dispatcher，页面初始化即可能直接做本地 IO。
- Improvement path: 迁移到 Room 或至少在 repository 层统一切换到 `Dispatchers.IO`；增加分页/limit。

**[潜在风险] 审计日志无限增长且查询无分页：**
- Problem: `audit_logs` 表无容量上限、无归档、无筛选索引；`queryAll()` 每次全量加载并在 UI 层筛选。
- Files: `app/src/main/java/com/opencode/nfccardmanager/core/database/AuditLogDbHelper.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/audit/AuditLogScreen.kt`
- Cause: 当前实现将所有日志一次性读入内存。
- Improvement path: 增加索引、分页查询、保留策略与后台清理任务。

## Fragile Areas

**[确认问题] ReaderMode 回调缺少“单次执行/去抖/并发锁”，高风险页面容易重复执行：**
- Files: `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/write/WriteEditorScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/lock/LockRiskScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/unlock/UnlockVerifyScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatCardScreen.kt`
- Why fragile: ReaderMode 启动后直到 Compose 重新组合并触发 `onDispose` 才会关闭；同一标签在短时间内可能多次触发回调。写卡/锁卡/格式化/解锁流程都没有显式 in-flight 标记或 mutex。
- Safe modification: 在回调第一时间设置原子状态并立即关闭 ReaderMode；将真实 NFC 操作串行化；把“开始扫描”和“执行写入/锁定”拆成两个状态机阶段。
- Test coverage: `app/src/test` 与 `app/src/androidTest` 未检测到任何测试，缺少重复触发与生命周期回归用例。

**[确认问题] 页面进入即自动开始硬件操作，误触成本高：**
- Files: `app/src/main/java/com/opencode/nfccardmanager/feature/format/FormatCardScreen.kt`, `app/src/main/java/com/opencode/nfccardmanager/feature/scan/ScanScreen.kt`
- Why fragile: `FormatCardScreen` 在 `LaunchedEffect(Unit)` 里直接 `viewModel.start()`，一旦用户带卡进入页面就会立即尝试格式化；读卡页也会在进入页面后自动启动扫描。
- Safe modification: 改为显式“开始扫描/开始格式化”确认按钮，并在高风险页增加二次确认。
- Test coverage: 无自动化测试覆盖“页面首帧即贴卡”的用户路径。

**[潜在风险] 写后回读依赖同一个 Tag 对象，卡片轻微移动就可能出现误判：**
- Files: `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NdefWriter.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt`
- Why fragile: `verifyWrite()` 在写入后立刻复用当前 `Tag` 再读一次；NFC 标签稍有位移就会得到“校验失败”，但写入可能已成功。
- Safe modification: 将“写入成功”和“回读校验失败”拆分成更精确状态；允许用户二次贴卡完成独立校验。
- Test coverage: 未检测到围绕 Tag 丢失、重贴卡、弱场景的测试。

## Scaling Limits

**[潜在风险] 日志与模板方案仅适合单机演示规模：**
- Current capacity: 仅本地 SQLite + 内存 `StateFlow`；无多设备同步、无冲突控制、无远端审计汇聚。
- Limit: 当设备数量、模板数量、日志量增加时，权限范围、审计合规和数据一致性都会失效。
- Scaling path: 引入服务端身份、模板中心、远端审计上报与设备级数据同步策略。

## Dependencies at Risk

**[潜在风险] 仅依赖平台原生 NFC API，卡型兼容规则完全自维护：**
- Risk: `android.nfc.tech.*` 能力判断与错误处理都靠自定义代码，缺少更高层协议抽象与卡型兼容数据库。
- Impact: 新卡型接入或兼容性问题需要逐个设备/标签人工排查。
- Migration plan: 在保留原生 API 的前提下，增加协议分层与卡型适配表；必要时引入更强的设备兼容测试基线。

## Missing Critical Features

**[确认问题] 高风险操作缺少审批链、二次授权和真实执行隔离：**
- Problem: 当前锁卡/解锁只依赖本地页面确认与固定凭据，没有审批流、没有管理员二次确认、没有远端审计校验。
- Blocks: 无法达到 `docs/权限矩阵文档.md` 中描述的企业权限策略，也不适合真实生产卡片管理。

**[确认问题] 应用缺少真正可验证的环境/安全边界：**
- Problem: `AndroidManifest.xml` 中 `allowBackup="true"`，未检测到 `FLAG_SECURE`、root/调试态检测、会话超时、失败重试限制。
- Blocks: 真机投放时无法满足基础安全审计要求。

## Test Coverage Gaps

**[确认问题] NFC 读写锁解主路径没有自动化测试：**
- What's not tested: `TagParser`、`NdefWriter`、`NdefLocker`、`NdefFormatter`、`UnlockExecutor`、`NfcSessionManager` 的成功/失败/边界行为。
- Files: `app/src/main/java/com/opencode/nfccardmanager/core/nfc/TagParser.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NdefWriter.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NdefLocker.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NdefFormatter.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/nfc/UnlockExecutor.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/nfc/NfcSessionManager.kt`
- Risk: NFC 边界条件（空标签、TagLost、不可写、容量不足、ReaderMode 重入）变更后会无声回归。
- Priority: High

**[确认问题] 权限与会话安全没有测试：**
- What's not tested: 角色切换、未登录跳转、审计员限制、SharedPreferences 恢复逻辑、日志操作者归属。
- Files: `app/src/main/java/com/opencode/nfccardmanager/core/security/SecurityManager.kt`, `app/src/main/java/com/opencode/nfccardmanager/core/security/SessionStore.kt`, `app/src/main/java/com/opencode/nfccardmanager/navigation/AppNavGraph.kt`
- Risk: 权限回归会直接演变成越权执行。
- Priority: High

**[确认问题] 项目当前未检测到任何单元测试或仪器测试目录：**
- What's not tested: 整个应用。
- Files: `app/build.gradle.kts`, `app/src/main/java/com/opencode/nfccardmanager/`
- Risk: CI 中虽然执行 `./gradlew test`，但当前没有实际测试资产可阻止回归。
- Priority: High

---

*Concerns audit: 2026-03-30*
