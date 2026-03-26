package com.opencode.nfccardmanager.feature.lock

import com.opencode.nfccardmanager.core.nfc.model.LockCardResult

enum class LockStage {
    IDLE,
    READY,
    LOCKING,
    SUCCESS,
    ERROR,
}

data class LockUiState(
    val riskAcknowledged: Boolean = false,
    val confirmText: String = "",
    val stage: LockStage = LockStage.IDLE,
    val message: String = "锁卡会将标签设置为永久只读，通常不可逆。",
    val result: LockCardResult? = null,
)
