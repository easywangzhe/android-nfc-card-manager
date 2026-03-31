package com.opencode.nfccardmanager.feature.lock

import androidx.lifecycle.ViewModel
import com.opencode.nfccardmanager.core.database.AuditFlowStage
import com.opencode.nfccardmanager.core.database.AuditImpactScope
import com.opencode.nfccardmanager.core.database.AuditLogManager
import com.opencode.nfccardmanager.core.nfc.model.CardCapability
import com.opencode.nfccardmanager.core.nfc.model.HighRiskSupportSummary
import com.opencode.nfccardmanager.core.nfc.model.LockCardResult
import com.opencode.nfccardmanager.core.nfc.model.LockMode
import com.opencode.nfccardmanager.core.nfc.model.ReadCardResult
import com.opencode.nfccardmanager.core.nfc.model.buildLockResultGuidance
import com.opencode.nfccardmanager.core.nfc.model.buildLockSupportSummary
import com.opencode.nfccardmanager.core.security.SecurityManager
import com.opencode.nfccardmanager.core.security.maskRiskSensitiveValue
import com.opencode.nfccardmanager.feature.audit.lockAuthenticity
import com.opencode.nfccardmanager.feature.audit.mapUserRoleToAuditRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LockViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    fun toggleRiskAcknowledged(value: Boolean) {
        _uiState.update { it.copy(riskAcknowledged = value) }
        updateStage()
    }

    fun onConfirmTextChange(value: String) {
        _uiState.update { it.copy(confirmText = value.uppercase(), result = null, resultGuidance = null) }
        updateStage()
    }

    fun startLocking() {
        if (!canStartLock()) {
            onError("请先勾选风险确认并输入 LOCK")
            return
        }
        _uiState.update {
            it.copy(
                stage = LockStage.LOCKING,
                message = "请将支持 NDEF 的标签贴近手机背部，执行永久只读锁定",
                result = null,
                resultGuidance = null,
            )
        }
    }

    fun onTagResolved(readResult: ReadCardResult) {
        val recommended = readResult.capability.lockMode
        val supportSummary = buildLockSupportSummary(readResult.capability)
        _uiState.update {
            it.copy(
                recommendedMode = recommended,
                supportSummary = supportSummary,
                modeHint = when (recommended) {
                    LockMode.PASSWORD_PROTECTED -> "检测到该卡支持密码保护，系统将优先采用可授权解锁的锁定方案。"
                    LockMode.READ_ONLY_PERMANENT -> "该卡不支持密码保护，将降级为永久只读锁定，请确认不可逆风险。"
                    else -> "当前卡片不支持锁卡。"
                },
                message = when (recommended) {
                    LockMode.PASSWORD_PROTECTED -> "已识别支持密码保护的卡片，继续后将优先执行密码保护型锁卡。"
                    LockMode.READ_ONLY_PERMANENT -> "已识别仅支持永久只读的卡片，继续后将执行不可逆锁卡。"
                    else -> "当前卡片不支持锁卡。"
                },
                maskedSensitiveFields = buildMaskedFields(readResult.cardInfo.uid, recommended),
            )
        }
    }

    fun onLockResult(result: LockCardResult) {
        AuditLogManager.save(
            operationType = "LOCK",
            cardUid = result.cardInfo.uid,
            cardType = result.cardInfo.techType.name,
            result = if (result.success) "SUCCESS" else "FAILED",
            message = "${result.message}；${result.verificationMessage}",
            operatorId = SecurityManager.currentSession.value?.username ?: "system",
            operatorRole = mapUserRoleToAuditRole(SecurityManager.currentRole.value.name),
            flowStage = if (result.success) AuditFlowStage.COMPLETED else AuditFlowStage.FAILED,
            authenticity = lockAuthenticity(result.success, result.verified),
            impactScope = AuditImpactScope.TRACEABILITY,
        )
        _uiState.update {
            it.copy(
                stage = if (result.success) LockStage.SUCCESS else LockStage.ERROR,
                message = result.message,
                result = result,
                resultGuidance = buildLockResultGuidance(result),
                supportSummary = buildLockSupportSummary(
                    CardCapability(
                        canRead = true,
                        canWrite = true,
                        canLock = result.lockMode != LockMode.NONE,
                        lockMode = result.lockMode,
                        canUnlock = result.lockMode == LockMode.PASSWORD_PROTECTED,
                        requiresAuthForWrite = result.lockMode == LockMode.PASSWORD_PROTECTED,
                    )
                ),
                maskedSensitiveFields = buildMaskedFields(result.cardInfo.uid, result.lockMode),
            )
        }
    }

    fun onError(message: String) {
        AuditLogManager.save(
            operationType = "LOCK",
            cardUid = "UNKNOWN",
            cardType = "UNKNOWN",
            result = "FAILED",
            message = message,
            operatorId = SecurityManager.currentSession.value?.username ?: "system",
            operatorRole = mapUserRoleToAuditRole(SecurityManager.currentRole.value.name),
            flowStage = AuditFlowStage.FAILED,
            authenticity = com.opencode.nfccardmanager.core.database.AuditAuthenticity.PENDING,
            impactScope = AuditImpactScope.TRACEABILITY,
        )
        _uiState.update {
            it.copy(stage = LockStage.ERROR, message = message, resultGuidance = null)
        }
    }

    private fun canStartLock(): Boolean {
        val state = _uiState.value
        return state.riskAcknowledged && state.confirmText == "LOCK"
    }

    private fun updateStage() {
        val state = _uiState.value
        val supportSummary = state.supportSummary ?: buildLockSupportSummary(null)
        _uiState.update {
            it.copy(
                stage = if (canStartLock()) LockStage.READY else LockStage.IDLE,
                message = when {
                    canStartLock() -> "确认条件已满足，点击开始锁卡后贴卡执行"
                    else -> "前置条件未满足，暂不能开始锁卡。"
                },
                supportSummary = supportSummary,
                prerequisites = buildPrerequisites(state.riskAcknowledged, state.confirmText),
            )
        }
    }

    private fun buildPrerequisites(riskAcknowledged: Boolean, confirmText: String): List<RiskPrerequisite> {
        return listOf(
            RiskPrerequisite(label = "已勾选不可逆风险确认", satisfied = riskAcknowledged),
            RiskPrerequisite(label = "已输入确认词 LOCK", satisfied = confirmText == "LOCK"),
        )
    }

    private fun buildMaskedFields(uid: String, lockMode: LockMode): List<MaskedRiskField> {
        val role = SecurityManager.currentRole.value
        return listOf(
            MaskedRiskField(label = "UID", value = maskRiskSensitiveValue(uid, role)),
            MaskedRiskField(
                label = "锁定方式摘要",
                value = when (lockMode) {
                    LockMode.PASSWORD_PROTECTED -> "密码保护"
                    LockMode.READ_ONLY_PERMANENT -> "永久只读"
                    LockMode.NONE -> "不支持"
                }
            ),
        )
    }
}
