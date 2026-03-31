package com.opencode.nfccardmanager.core.nfc

import com.opencode.nfccardmanager.core.nfc.model.CardCapability
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.FormatCardResult
import com.opencode.nfccardmanager.core.nfc.model.LockMode
import com.opencode.nfccardmanager.core.nfc.model.ReadCardResult
import com.opencode.nfccardmanager.core.nfc.model.TechType
import com.opencode.nfccardmanager.core.nfc.model.WriteCardResult
import com.opencode.nfccardmanager.core.nfc.model.buildFormatNextStepGuidance
import com.opencode.nfccardmanager.core.nfc.model.buildReadNextStepGuidance
import com.opencode.nfccardmanager.core.nfc.model.buildWriteOutcomeGuidance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighFrequencyFlowGuidanceTest {

    @Test
    fun `write success splits execution and verification guidance`() {
        val guidance = buildWriteOutcomeGuidance(
            WriteCardResult(
                cardInfo = sampleCardInfo(),
                success = true,
                message = "写入成功",
                payloadPreview = "设备编号=EQ-001",
                writeStatus = "WRITE_SUCCESS",
                writeReason = "标签支持 NDEF 写入",
                verified = true,
                verificationMessage = "已回读校验且内容一致",
            )
        )

        assertEquals("写入执行成功", guidance.executionTitle)
        assertEquals("写入命令已执行，当前卡片内容已更新。", guidance.executionConclusion)
        assertEquals("回读校验通过", guidance.verificationTitle)
        assertEquals("已回读校验且内容一致", guidance.verificationConclusion)
        assertEquals("推荐下一步", guidance.nextStep.title)
        assertEquals("可以继续写下一张卡，或返回当前业务流程继续处理。", guidance.nextStep.recommendedAction)
        assertEquals("继续处理", guidance.nextStep.ctaLabel)
    }

    @Test
    fun `verify failed keeps executed write separate from failed verification`() {
        val guidance = buildWriteOutcomeGuidance(
            WriteCardResult(
                cardInfo = sampleCardInfo(),
                success = false,
                message = "写入后校验失败",
                payloadPreview = "设备编号=EQ-002",
                writeStatus = "VERIFY_FAILED",
                writeReason = "写入命令已返回成功",
                verified = false,
                verificationMessage = "回读内容与目标内容不一致",
            )
        )

        assertEquals("写入执行成功", guidance.executionTitle)
        assertEquals("写入命令已执行，但当前不能把结果当成最终成功。", guidance.executionConclusion)
        assertEquals("回读校验失败", guidance.verificationTitle)
        assertEquals("回读内容与目标内容不一致", guidance.verificationConclusion)
        assertEquals("请保持卡片稳定后重新贴卡复核，必要时再重试写卡。", guidance.nextStep.recommendedAction)
        assertEquals("重新复核", guidance.nextStep.ctaLabel)
    }

    @Test
    fun `read statuses return distinct next step guidance`() {
        val readError = buildReadNextStepGuidance(sampleReadResult("READ_ERROR", "读取中断"))
        val emptyNdef = buildReadNextStepGuidance(sampleReadResult("EMPTY_NDEF", "未发现 NDEF 记录"))
        val nonNdef = buildReadNextStepGuidance(sampleReadResult("NON_NDEF", "当前不是 NDEF 标签"))

        assertEquals("请先重新贴卡重试，仍失败再检查 NFC 开关与卡片稳定性。", readError.recommendedAction)
        assertEquals("重新读卡", readError.ctaLabel)

        assertEquals("当前是空 NDEF 标签，可继续去写卡；若需先确认清空状态，也可先执行格式化。", emptyNdef.recommendedAction)
        assertEquals("去写卡", emptyNdef.ctaLabel)

        assertEquals("当前不是可直接读取的 NDEF 标签，不建议直接继续写卡；请先确认卡片类型或先尝试格式化。", nonNdef.recommendedAction)
        assertEquals("先去格式化", nonNdef.ctaLabel)
    }

    @Test
    fun `format failures map to safe action guidance`() {
        val unsupported = buildFormatNextStepGuidance(sampleFormatResult("UNSUPPORTED_TAG", "卡片不支持 NDEF 格式化"))
        val clearError = buildFormatNextStepGuidance(sampleFormatResult("CLEAR_ERROR", "清空旧内容失败"))
        val formatError = buildFormatNextStepGuidance(sampleFormatResult("FORMAT_ERROR", "格式化过程中异常中断"))

        assertEquals("已确认当前卡片不支持 NDEF 格式化。", unsupported.conclusion)
        assertEquals("停止继续格式化或写卡，改用支持 NDEF 的卡片。", unsupported.recommendedAction)
        assertEquals("更换卡片", unsupported.ctaLabel)

        assertTrue(clearError.reasonSummary.contains("清空"))
        assertEquals("清空失败已发生，请保持卡片稳定后重试，不要直接假定卡片已清空。", clearError.recommendedAction)

        assertEquals("格式化过程中发生异常，当前不能假定卡片已经可安全写入。", formatError.conclusion)
        assertEquals("请先重试一次；若仍失败，保留现状并检查卡片兼容性。", formatError.recommendedAction)
    }

    private fun sampleCardInfo() = CardInfo(
        uid = "04A1B2C3D4",
        techType = TechType.NDEF,
        summary = "测试卡",
    )

    private fun sampleCapability() = CardCapability(
        canRead = true,
        canWrite = true,
        canLock = true,
        lockMode = LockMode.READ_ONLY_PERMANENT,
        canUnlock = false,
        requiresAuthForWrite = false,
    )

    private fun sampleReadResult(status: String, reason: String) = ReadCardResult(
        cardInfo = sampleCardInfo(),
        capability = sampleCapability(),
        readStatus = status,
        readReason = reason,
    )

    private fun sampleFormatResult(status: String, reason: String) = FormatCardResult(
        cardInfo = sampleCardInfo(),
        success = false,
        message = reason,
        status = status,
        reason = reason,
    )
}
