package com.opencode.nfccardmanager.core.nfc

import com.opencode.nfccardmanager.core.nfc.model.CardCapability
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.HighRiskResultSource
import com.opencode.nfccardmanager.core.nfc.model.LockCardResult
import com.opencode.nfccardmanager.core.nfc.model.LockMode
import com.opencode.nfccardmanager.core.nfc.model.TechType
import com.opencode.nfccardmanager.core.nfc.model.UnlockCardResult
import com.opencode.nfccardmanager.core.nfc.model.buildLockResultGuidance
import com.opencode.nfccardmanager.core.nfc.model.buildLockSupportSummary
import com.opencode.nfccardmanager.core.nfc.model.buildUnlockResultGuidance
import com.opencode.nfccardmanager.core.nfc.model.buildUnlockSupportSummary
import com.opencode.nfccardmanager.core.security.UserRole
import com.opencode.nfccardmanager.core.security.maskRiskSensitiveValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighRiskFlowGuidanceTest {

    @Test
    fun `lock support summary distinguishes password protected permanent and unsupported`() {
        val passwordProtected = buildLockSupportSummary(sampleCapability(lockMode = LockMode.PASSWORD_PROTECTED, canLock = true))
        val permanent = buildLockSupportSummary(sampleCapability(lockMode = LockMode.READ_ONLY_PERMANENT, canLock = true))
        val unsupported = buildLockSupportSummary(sampleCapability(lockMode = LockMode.NONE, canLock = false))

        assertEquals("密码保护锁定已支持，可保留后续受控解锁路径。", passwordProtected.supportedLabel)
        assertEquals("真实支持", passwordProtected.authenticityLabel)
        assertEquals("当前仅支持永久只读锁定，执行后通常不可逆。", permanent.supportedLabel)
        assertTrue(permanent.unsupportedLabel.contains("不支持密码保护"))
        assertEquals("当前卡片不支持锁卡。", unsupported.unsupportedLabel)
        assertEquals("未支持", unsupported.authenticityLabel)
    }

    @Test
    fun `unlock support summary keeps can unlock card as demo only`() {
        val unsupported = buildUnlockSupportSummary(sampleCapability(lockMode = LockMode.READ_ONLY_PERMANENT, canUnlock = false))
        val demoOnly = buildUnlockSupportSummary(sampleCapability(lockMode = LockMode.PASSWORD_PROTECTED, canUnlock = true))

        assertEquals("当前场景不可解锁。", unsupported.supportedLabel)
        assertTrue(unsupported.unsupportedLabel.contains("永久只读"))
        assertEquals("仅演示", demoOnly.authenticityLabel)
        assertTrue(demoOnly.supportedLabel.contains("流程演示"))
    }

    @Test
    fun `result guidance distinguishes confirmed failed unverified and demo only`() {
        val confirmed = buildLockResultGuidance(
            LockCardResult(
                cardInfo = sampleCardInfo(),
                success = true,
                message = "锁卡完成",
                lockMode = LockMode.READ_ONLY_PERMANENT,
                verified = true,
                verificationMessage = "已确认只读生效",
            )
        )
        val unverified = buildLockResultGuidance(
            LockCardResult(
                cardInfo = sampleCardInfo(),
                success = true,
                message = "锁卡完成",
                lockMode = LockMode.PASSWORD_PROTECTED,
                verified = false,
                verificationMessage = "未完成校验",
            )
        )
        val failed = buildLockResultGuidance(
            LockCardResult(
                cardInfo = sampleCardInfo(),
                success = false,
                message = "当前卡片不支持锁卡",
                verificationMessage = "既不支持密码保护，也不支持永久只读锁定。",
            )
        )
        val demoOnly = buildUnlockResultGuidance(
            UnlockCardResult(
                cardInfo = sampleCardInfo(),
                success = true,
                message = "解锁流程骨架执行成功",
                verificationMessage = "当前版本仅完成密码保护型卡片的流程演示，未接入真实底层解除写保护命令",
            )
        )

        assertEquals(HighRiskResultSource.CONFIRMED_EXECUTED, confirmed?.source)
        assertEquals("本次锁卡已确认执行。", confirmed?.conclusion)
        assertEquals(HighRiskResultSource.UNVERIFIED, unverified?.source)
        assertTrue(unverified?.recoveryAction?.contains("再次读卡校验") == true)
        assertEquals(HighRiskResultSource.FAILED, failed?.source)
        assertEquals("请停止继续尝试该卡，改用支持锁卡的卡片。", failed?.recoveryAction)
        assertEquals(HighRiskResultSource.DEMO_ONLY, demoOnly?.source)
        assertTrue(demoOnly?.conclusion?.contains("仅完成流程演示") == true)
    }

    @Test
    fun `sensitive values are masked by role`() {
        val value = "04A1B2C3D4"

        assertEquals("04A1B2C3D4", maskRiskSensitiveValue(value, UserRole.ADMIN))
        assertEquals("04******D4", maskRiskSensitiveValue(value, UserRole.SUPERVISOR))
        assertEquals("已遮罩", maskRiskSensitiveValue(value, UserRole.OPERATOR))
        assertEquals("仅审计摘要", maskRiskSensitiveValue(value, UserRole.AUDITOR))
    }

    private fun sampleCapability(
        lockMode: LockMode,
        canLock: Boolean = true,
        canUnlock: Boolean = false,
    ) = CardCapability(
        canRead = true,
        canWrite = true,
        canLock = canLock,
        lockMode = lockMode,
        canUnlock = canUnlock,
        requiresAuthForWrite = lockMode == LockMode.PASSWORD_PROTECTED,
    )

    private fun sampleCardInfo() = CardInfo(
        uid = "04A1B2C3D4",
        techType = TechType.ULTRALIGHT,
        summary = "测试卡片",
    )
}
