package com.opencode.nfccardmanager.feature.write

import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.TechType
import com.opencode.nfccardmanager.core.nfc.model.WriteCardResult
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import com.opencode.nfccardmanager.testutil.MainDispatcherRule

class WriteViewModelPhase3Test {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `on write result exposes execution verification and next step for write success`() {
        val viewModel = WriteViewModel()

        viewModel.onWriteResult(
            sampleWriteResult(
                success = true,
                status = "WRITE_SUCCESS",
                message = "写入成功",
                reason = "标签支持 NDEF 写入",
                verified = true,
                verificationMessage = "已回读校验且内容一致",
            )
        )

        val uiState = viewModel.uiState.value
        assertEquals(WriteStage.SUCCESS, uiState.stage)
        assertEquals("写入执行成功", uiState.resultGuidance?.executionTitle)
        assertEquals("回读校验通过", uiState.resultGuidance?.verificationTitle)
        assertEquals("可以继续写下一张卡，或返回当前业务流程继续处理。", uiState.nextStepGuidance?.recommendedAction)
    }

    @Test
    fun `verify failed keeps write executed but asks for recheck`() {
        val viewModel = WriteViewModel()

        viewModel.onWriteResult(
            sampleWriteResult(
                success = false,
                status = "VERIFY_FAILED",
                message = "写入后校验失败",
                reason = "写入命令已返回成功",
                verified = false,
                verificationMessage = "回读内容与目标内容不一致",
            )
        )

        val uiState = viewModel.uiState.value
        assertEquals(WriteStage.ERROR, uiState.stage)
        assertEquals("写入执行成功", uiState.resultGuidance?.executionTitle)
        assertEquals("回读校验失败", uiState.resultGuidance?.verificationTitle)
        assertEquals("请保持卡片稳定后重新贴卡复核，必要时再重试写卡。", uiState.nextStepGuidance?.recommendedAction)
        assertEquals("重新复核", uiState.nextStepGuidance?.ctaLabel)
    }

    @Test
    fun `write errors recommend checking card stability or compatibility`() {
        val viewModel = WriteViewModel()

        viewModel.onWriteResult(
            sampleWriteResult(
                success = false,
                status = "WRITE_ERROR",
                message = "写入异常",
                reason = "标签连接中断",
                verified = false,
                verificationMessage = "未执行校验",
            )
        )

        val resultErrorState = viewModel.uiState.value
        assertEquals("请先检查卡片稳定性与兼容性，再重新贴卡重试。", resultErrorState.nextStepGuidance?.recommendedAction)

        viewModel.onError("手动启动失败")

        val directErrorState = viewModel.uiState.value
        assertEquals(WriteStage.ERROR, directErrorState.stage)
        assertEquals("请先检查卡片稳定性与兼容性，再重新贴卡重试。", directErrorState.nextStepGuidance?.recommendedAction)
        assertEquals("重新写卡", directErrorState.nextStepGuidance?.ctaLabel)
    }

    private fun sampleWriteResult(
        success: Boolean,
        status: String,
        message: String,
        reason: String,
        verified: Boolean,
        verificationMessage: String,
    ) = WriteCardResult(
        cardInfo = CardInfo(
            uid = "04A1B2C3D4",
            techType = TechType.NDEF,
            summary = "测试卡",
        ),
        success = success,
        message = message,
        payloadPreview = "设备编号=EQ-001",
        writeStatus = status,
        writeReason = reason,
        verified = verified,
        verificationMessage = verificationMessage,
    )
}
