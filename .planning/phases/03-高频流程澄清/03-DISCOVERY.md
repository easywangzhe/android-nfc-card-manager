# Phase 03 Discovery: 高频流程澄清

## Discovery Level

Level 0（沿用既有 Compose + ViewModel + core model 模式）+ UI 设计契约补齐。

- 不引入新依赖
- 延续 Phase 1 的共享阶段/真实性语义与 Phase 2 的信息层级整理方式
- 聚焦既有读卡、写卡、格式化页面的结果拆分与下一步指引，不重写 NFC 底层执行器

## Current Findings

1. `WriteEditorScreen.kt` 目前在结果区同时展示 `message`、`writeReason`、`verified`，但“写入命令执行成功”和“回读校验成功”仍被压扁成一个成功/失败结论，不满足 `FLOW-04`。
2. `ReadResultScreen.kt` 的“下一步操作”当前固定只有“格式化卡 / 重新读卡”，不同 `readStatus` 只体现在说明文案里，缺少稳定的推荐动作契约，不满足 `FLOW-05`。
3. `FormatCardScreen.kt` 已有 `status` 与 `reason`，但失败时只原样显示结果，没有把 `UNSUPPORTED_TAG`、`CLEAR_ERROR`、`FORMAT_ERROR` 收口为“发生了什么 / 为什么 / 当前最安全下一步”三段表达，不满足 `RISK-04`。
4. `NdefWriter.kt` 与 `NdefFormatter.kt` 已提供足够稳定的状态码和原因文案，适合先抽出纯 Kotlin 的指导契约，再分别接入写卡、读卡结果、格式化页面。

## UI Design Contract

### 高频结果卡片层级

所有 Phase 3 结果展示统一遵循以下顺序：

1. **当前阶段 / 真实性摘要**（沿用 Phase 1 共享语义）
2. **结果判定**（明确本次执行结果）
3. **原因与差异**（为什么成功/失败/未验证）
4. **推荐下一步**（当前最安全、最直接的后续动作）

### 写卡结果契约

写卡页必须把以下两个结论分开显示：

1. **写入执行结果**
   - 写入命令已执行
   - 写入命令未执行/异常
2. **回读校验结果**
   - 回读校验通过
   - 回读校验失败
   - 未执行回读校验

规则：

- `WRITE_SUCCESS` = 写入执行成功 + 回读校验通过
- `VERIFY_FAILED` = 写入执行成功 + 回读校验失败（不能降级成“纯写入失败”）
- `WRITE_ERROR` / `FORMAT_ERROR` / `UNSUPPORTED_TAG` = 写入执行未完成，回读校验视为未执行
- 结果区必须单独给出下一步建议，例如“重新贴卡重试”“先格式化后再写”“停止继续写入并更换卡片”

### 读卡结果推荐动作契约

| `readStatus` | 用户结论 | 推荐下一步 |
|-------------|----------|------------|
| `READ_SUCCESS` | 内容已读出 | 根据能力继续查看卡片信息；如需清空或重置再去格式化 |
| `EMPTY_NDEF` | 空 NDEF 标签 | 可继续写入业务内容；若需清空确认可先格式化 |
| `NON_NDEF` | 当前不是可直接读取的 NDEF 标签 | 不要直接继续写卡，先确认卡片类型或改走格式化 |
| `READ_ERROR` | 读取过程失败 | 先重新贴卡重试，仍失败再检查 NFC 与卡片稳定性 |

### 格式化失败与安全后续契约

| `status` | 失败结论 | 当前最安全下一步 |
|----------|----------|------------------|
| `UNSUPPORTED_TAG` | 卡片不支持 NDEF 格式化 | 停止继续格式化/写卡，改用支持 NDEF 的卡片 |
| `CLEAR_ERROR` | 清空既有 NDEF 内容失败 | 保持卡片稳定重试；不要直接假定卡片已清空 |
| `FORMAT_ERROR` | 格式化过程中发生异常 | 先重试一次；若仍失败，保留现状并检查卡片兼容性 |

成功态：

- `CLEARED_NDEF` / `FORMAT_SUCCESS` 都应明确提示“可以继续去写卡”

## Planning Guidance

- 先产出纯 Kotlin 共享指导契约与单元测试，再分别接入写卡页、格式化页和读卡结果页，避免三处重复维护状态判断。
- 写卡页保持 `NdefWriter` 的低层 truth source，不在 UI 层重写状态码；只新增“执行结果 / 校验结果 / 下一步建议”的展示语义。
- 格式化页必须直接消费 `FormatCardResult.status` 与 `reason`，而不是重新猜测失败原因。
- 读卡结果页不强制新增导航入口；本阶段的“下一步”可以是明确文案 + 已有按钮的重命名/重排序。
- 自动验证以 JVM 单元测试 + `assembleDebug` 为主，不引入重型 Compose UI 测试。
