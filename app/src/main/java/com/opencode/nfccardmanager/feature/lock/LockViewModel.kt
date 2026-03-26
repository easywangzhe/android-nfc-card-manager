package com.opencode.nfccardmanager.feature.lock

import androidx.lifecycle.ViewModel
import com.opencode.nfccardmanager.core.database.AuditLogManager
import com.opencode.nfccardmanager.core.nfc.model.LockCardResult
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
                    else -> "锁卡会将标签设置为永久只读，通常不可逆。"
                },
            )
        }
    }
}
