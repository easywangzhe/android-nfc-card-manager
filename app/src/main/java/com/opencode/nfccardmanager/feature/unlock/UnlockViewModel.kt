package com.opencode.nfccardmanager.feature.unlock

import androidx.lifecycle.ViewModel
import com.opencode.nfccardmanager.core.database.AuditLogManager
import com.opencode.nfccardmanager.core.nfc.model.ReadCardResult
import com.opencode.nfccardmanager.core.nfc.model.UnlockCardResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UnlockViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    fun onReasonChange(value: String) {
        _uiState.update {
            it.copy(
                reason = value,
                stage = if (canStart(value, it.credential)) UnlockStage.READY else UnlockStage.IDLE,
                result = null,
            )
        }
    }

    fun onCredentialChange(value: String) {
        _uiState.update {
            it.copy(
                credential = value,
                stage = if (canStart(it.reason, value)) UnlockStage.READY else UnlockStage.IDLE,
                result = null,
            )
        }
    }

    fun startUnlock() {
        val state = _uiState.value
        if (!canStart(state.reason, state.credential)) {
            onError("请先填写解锁理由和凭据")
            return
        }
        _uiState.update {
            it.copy(
                stage = UnlockStage.SCANNING,
                message = "请将待解锁卡片贴近手机背部，系统将先识别卡型和解锁能力",
                result = null,
            )
        }
    }

    fun onTagResolved(readResult: ReadCardResult) {
        _uiState.update {
            it.copy(
                cardInfo = readResult.cardInfo,
                capability = readResult.capability,
                message = when {
                    readResult.capability.lockMode == com.opencode.nfccardmanager.core.nfc.model.LockMode.PASSWORD_PROTECTED -> {
                        "已识别 ${readResult.cardInfo.techType.name}，该卡支持密码保护型解锁流程。"
                    }
                    readResult.capability.lockMode == com.opencode.nfccardmanager.core.nfc.model.LockMode.READ_ONLY_PERMANENT -> {
                        "已识别 ${readResult.cardInfo.techType.name}，该卡更可能属于永久只读锁定，通常不可解锁。"
                    }
                    else -> {
                        "已识别 ${readResult.cardInfo.techType.name}，当前卡片不支持解锁。"
                    }
                },
            )
        }
    }

    fun onUnlockResult(result: UnlockCardResult) {
        AuditLogManager.save(
            operationType = "UNLOCK",
            cardUid = result.cardInfo.uid,
            cardType = result.cardInfo.techType.name,
            result = if (result.success) "SUCCESS" else "FAILED",
            message = "${result.message}；${result.verificationMessage}",
        )
        _uiState.update {
            it.copy(
                stage = if (result.success) UnlockStage.SUCCESS else UnlockStage.ERROR,
                message = result.message,
                result = result,
            )
        }
    }

    fun onError(message: String) {
        AuditLogManager.save(
            operationType = "UNLOCK",
            cardUid = "UNKNOWN",
            cardType = "UNKNOWN",
            result = "FAILED",
            message = message,
        )
        _uiState.update {
            it.copy(stage = UnlockStage.ERROR, message = message)
        }
    }

    private fun canStart(reason: String, credential: String): Boolean {
        return reason.isNotBlank() && credential.isNotBlank()
    }
}
