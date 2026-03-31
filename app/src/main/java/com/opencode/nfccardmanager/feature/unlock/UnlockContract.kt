package com.opencode.nfccardmanager.feature.unlock

import com.opencode.nfccardmanager.core.nfc.model.CardCapability
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.UnlockCardResult

enum class UnlockStage {
    IDLE,
    READY,
    SCANNING,
    SUCCESS,
    ERROR,
}

data class UnlockUiState(
    val reason: String = "",
    val credential: String = "",
    val message: String = "请输入解锁理由和凭据，确认后点击开始解锁。",
    val stage: UnlockStage = UnlockStage.IDLE,
    val cardInfo: CardInfo? = null,
    val capability: CardCapability? = null,
    val result: UnlockCardResult? = null,
)
