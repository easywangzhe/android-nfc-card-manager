package com.opencode.nfccardmanager.feature.lock

import androidx.lifecycle.ViewModel
import com.opencode.nfccardmanager.core.database.AuditLogManager
import com.opencode.nfccardmanager.core.nfc.model.LockCardResult
import com.opencode.nfccardmanager.core.nfc.model.LockMode
import com.opencode.nfccardmanager.core.nfc.model.ReadCardResult
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
        _uiState.update { it.copy(confirmText = value.uppercase(), result = null) }
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
            )
        }
    }

    fun onTagResolved(readResult: ReadCardResult) {
        val recommended = readResult.capability.lockMode
        _uiState.update {
            it.copy(
                recommendedMode = recommended,
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
        )
        _uiState.update {
            it.copy(
                stage = if (result.success) LockStage.SUCCESS else LockStage.ERROR,
                message = result.message,
                result = result,
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
        )
        _uiState.update {
            it.copy(stage = LockStage.ERROR, message = message)
        }
    }

    private fun canStartLock(): Boolean {
        val state = _uiState.value
        return state.riskAcknowledged && state.confirmText == "LOCK"
    }

    private fun updateStage() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                stage = if (canStartLock()) LockStage.READY else LockStage.IDLE,
                message = when {
                    canStartLock() -> "确认条件已满足，点击开始锁卡后贴卡执行"
                    state.confirmText.isNotBlank() && state.confirmText != "LOCK" -> "确认词必须为 LOCK"
                    else -> "锁卡前会先识别卡片能力，并优先使用可解锁的密码保护方案。"
                },
            )
        }
    }
}
