package com.opencode.nfccardmanager.feature.format

import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.FormatCardResult
import com.opencode.nfccardmanager.core.nfc.model.TechType
import com.opencode.nfccardmanager.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FormatViewModelPhase3Test {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `format success recommends going to write flow`() {
        val viewModel = FormatViewModel()

        viewModel.onFormatResult(sampleResult(true, "FORMAT_SUCCESS", "格式化成功", "已完成格式化"))

        val uiState = viewModel.uiState.value
        assertEquals(FormatStage.SUCCESS, uiState.stage)
        assertEquals("当前卡片已准备好，可继续去写卡。", uiState.resultGuidance?.recommendedAction)
        assertEquals("去写卡", uiState.resultGuidance?.ctaLabel)
    }

    @Test
    fun `unsupported tag asks user to stop and replace card`() {
        val viewModel = FormatViewModel()

        viewModel.onFormatResult(sampleResult(false, "UNSUPPORTED_TAG", "不支持格式化", "卡片不支持 NDEF 格式化"))

        val uiState = viewModel.uiState.value
        assertEquals(FormatStage.ERROR, uiState.stage)
        assertEquals("已确认当前卡片不支持 NDEF 格式化。", uiState.resultGuidance?.conclusion)
        assertEquals("停止继续格式化或写卡，改用支持 NDEF 的卡片。", uiState.resultGuidance?.recommendedAction)
    }

    @Test
    fun `clear and format errors keep failure explicit and ask for safe retry`() {
        val viewModel = FormatViewModel()

        viewModel.onFormatResult(sampleResult(false, "CLEAR_ERROR", "清空失败", "清空旧内容失败"))
        val clearError = viewModel.uiState.value
        assertEquals("清空失败已发生，请保持卡片稳定后重试，不要直接假定卡片已清空。", clearError.resultGuidance?.recommendedAction)

        viewModel.onFormatResult(sampleResult(false, "FORMAT_ERROR", "格式化异常", "格式化过程中异常中断"))
        val formatError = viewModel.uiState.value
        assertEquals("格式化过程中发生异常，当前不能假定卡片已经可安全写入。", formatError.resultGuidance?.conclusion)
        assertEquals("请先重试一次；若仍失败，保留现状并检查卡片兼容性。", formatError.resultGuidance?.recommendedAction)
    }

    private fun sampleResult(success: Boolean, status: String, message: String, reason: String) = FormatCardResult(
        cardInfo = CardInfo(
            uid = "04A1B2C3D4",
            techType = TechType.NDEF,
            summary = "测试卡",
        ),
        success = success,
        message = message,
        status = status,
        reason = reason,
    )
}
