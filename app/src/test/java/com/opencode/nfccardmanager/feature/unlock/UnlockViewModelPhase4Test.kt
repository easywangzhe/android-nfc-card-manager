package com.opencode.nfccardmanager.feature.unlock

import com.opencode.nfccardmanager.core.nfc.model.CardCapability
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.LockMode
import com.opencode.nfccardmanager.core.nfc.model.ReadCardResult
import com.opencode.nfccardmanager.core.nfc.model.TechType
import com.opencode.nfccardmanager.core.nfc.model.UnlockCardResult
import com.opencode.nfccardmanager.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UnlockViewModelPhase4Test {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `missing reason or credential keeps unlock flow blocked`() {
        val viewModel = UnlockViewModel()

        viewModel.onReasonChange("需要恢复写入")

        val uiState = viewModel.uiState.value
        assertEquals(UnlockStage.IDLE, uiState.stage)
        assertEquals("前置条件未满足，暂不能开始解锁。", uiState.message)
        assertEquals(false, uiState.prerequisites.last().satisfied)
    }

    @Test
    fun `unsupported cards explain irreversible boundary`() {
        val viewModel = UnlockViewModel()

        viewModel.onTagResolved(sampleReadResult(lockMode = LockMode.READ_ONLY_PERMANENT, canUnlock = false))

        val uiState = viewModel.uiState.value
        assertEquals("当前场景不可解锁。", uiState.supportSummary?.supportedLabel)
        assertTrue(uiState.supportSummary?.unsupportedLabel?.contains("永久只读") == true)
    }

    @Test
    fun `successful unlock result stays demo only and failures differ by cause`() {
        val viewModel = UnlockViewModel()

        viewModel.onUnlockResult(
            UnlockCardResult(
                cardInfo = sampleCardInfo(),
                success = true,
                message = "解锁流程骨架执行成功",
                verificationMessage = "当前版本仅完成密码保护型卡片的流程演示，未接入真实底层解除写保护命令",
            )
        )
        val demoOnly = viewModel.uiState.value
        assertEquals(UnlockStage.SUCCESS, demoOnly.stage)
        assertTrue(demoOnly.resultGuidance?.conclusion?.contains("仅完成流程演示") == true)

        viewModel.onUnlockResult(
            UnlockCardResult(
                cardInfo = sampleCardInfo(),
                success = false,
                message = "解锁凭据错误",
                verificationMessage = "未通过凭据校验，未执行后续解锁流程",
            )
        )
        val credentialFailed = viewModel.uiState.value
        assertEquals("请核对凭据来源后重试，不要连续多次盲试。", credentialFailed.resultGuidance?.recoveryAction)

        viewModel.onUnlockResult(
            UnlockCardResult(
                cardInfo = sampleCardInfo(),
                success = false,
                message = "当前卡片不支持通用解锁",
                verificationMessage = "若为 NDEF 永久只读锁定，则通常不可逆，无法解锁",
            )
        )
        val unsupported = viewModel.uiState.value
        assertEquals("请停止继续尝试，保留现状并改走人工恢复或换卡流程。", unsupported.resultGuidance?.recoveryAction)
    }

    private fun sampleReadResult(lockMode: LockMode, canUnlock: Boolean) = ReadCardResult(
        cardInfo = sampleCardInfo(),
        capability = CardCapability(
            canRead = true,
            canWrite = true,
            canLock = true,
            lockMode = lockMode,
            canUnlock = canUnlock,
            requiresAuthForWrite = lockMode == LockMode.PASSWORD_PROTECTED,
        ),
        isNdefTag = true,
        readStatus = "READ_SUCCESS",
    )

    private fun sampleCardInfo() = CardInfo(
        uid = "04A1B2C3D4",
        techType = TechType.ULTRALIGHT,
        summary = "测试解锁",
    )
}
