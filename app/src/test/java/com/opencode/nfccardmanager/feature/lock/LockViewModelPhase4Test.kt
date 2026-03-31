package com.opencode.nfccardmanager.feature.lock

import com.opencode.nfccardmanager.core.nfc.model.CardCapability
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.LockCardResult
import com.opencode.nfccardmanager.core.nfc.model.LockMode
import com.opencode.nfccardmanager.core.nfc.model.ReadCardResult
import com.opencode.nfccardmanager.core.nfc.model.TechType
import com.opencode.nfccardmanager.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LockViewModelPhase4Test {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `risk confirmation incomplete keeps lock flow not ready`() {
        val viewModel = LockViewModel()

        viewModel.toggleRiskAcknowledged(true)
        viewModel.onConfirmTextChange("LOC")

        val uiState = viewModel.uiState.value
        assertEquals(LockStage.IDLE, uiState.stage)
        assertEquals("前置条件未满足，暂不能开始锁卡。", uiState.message)
        assertEquals(false, uiState.prerequisites.last().satisfied)
    }

    @Test
    fun `tag resolution exposes different support summaries for password and permanent modes`() {
        val viewModel = LockViewModel()

        viewModel.onTagResolved(sampleReadResult(LockMode.PASSWORD_PROTECTED, true))
        val passwordProtected = viewModel.uiState.value
        assertEquals("密码保护锁定已支持，可保留后续受控解锁路径。", passwordProtected.supportSummary?.supportedLabel)

        viewModel.onTagResolved(sampleReadResult(LockMode.READ_ONLY_PERMANENT, true))
        val permanent = viewModel.uiState.value
        assertEquals("当前仅支持永久只读锁定，执行后通常不可逆。", permanent.supportSummary?.supportedLabel)
        assertTrue(permanent.supportSummary?.unsupportedLabel?.contains("不支持密码保护") == true)
    }

    @Test
    fun `lock results distinguish confirmed unverified and failed recovery guidance`() {
        val viewModel = LockViewModel()

        viewModel.onLockResult(sampleResult(success = true, verified = true, message = "锁卡完成", verificationMessage = "已确认只读生效"))
        val confirmed = viewModel.uiState.value
        assertEquals(LockStage.SUCCESS, confirmed.stage)
        assertEquals("本次锁卡已确认执行。", confirmed.resultGuidance?.conclusion)

        viewModel.onLockResult(sampleResult(success = true, verified = false, message = "锁卡完成", verificationMessage = "未完成校验"))
        val unverified = viewModel.uiState.value
        assertEquals("请保持卡片在手边，再次读卡校验锁定状态后再离开。", unverified.resultGuidance?.recoveryAction)

        viewModel.onLockResult(sampleResult(success = false, verified = false, message = "当前卡片不支持锁卡", verificationMessage = "既不支持密码保护，也不支持永久只读锁定。"))
        val failed = viewModel.uiState.value
        assertEquals(LockStage.ERROR, failed.stage)
        assertEquals("请停止继续尝试该卡，改用支持锁卡的卡片。", failed.resultGuidance?.recoveryAction)
    }

    private fun sampleReadResult(lockMode: LockMode, canLock: Boolean) = ReadCardResult(
        cardInfo = sampleCardInfo(),
        capability = CardCapability(
            canRead = true,
            canWrite = true,
            canLock = canLock,
            lockMode = lockMode,
            canUnlock = lockMode == LockMode.PASSWORD_PROTECTED,
            requiresAuthForWrite = lockMode == LockMode.PASSWORD_PROTECTED,
        ),
        isNdefTag = true,
        readStatus = "READ_SUCCESS",
    )

    private fun sampleResult(
        success: Boolean,
        verified: Boolean,
        message: String,
        verificationMessage: String,
    ) = LockCardResult(
        cardInfo = sampleCardInfo(),
        success = success,
        message = message,
        lockMode = LockMode.PASSWORD_PROTECTED,
        irreversible = true,
        verified = verified,
        verificationMessage = verificationMessage,
    )

    private fun sampleCardInfo() = CardInfo(
        uid = "04A1B2C3D4",
        techType = TechType.ULTRALIGHT,
        summary = "测试锁卡",
    )
}
