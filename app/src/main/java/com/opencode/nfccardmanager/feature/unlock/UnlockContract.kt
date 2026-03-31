package com.opencode.nfccardmanager.feature.unlock

import com.opencode.nfccardmanager.core.nfc.model.CardCapability
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.HighRiskResultGuidance
import com.opencode.nfccardmanager.core.nfc.model.HighRiskSupportSummary
import com.opencode.nfccardmanager.core.nfc.model.UnlockCardResult

data class UnlockPrerequisite(
    val label: String,
    val satisfied: Boolean,
)

data class UnlockMaskedField(
    val label: String,
    val value: String,
)

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
    val supportSummary: HighRiskSupportSummary? = null,
    val resultGuidance: HighRiskResultGuidance? = null,
    val prerequisites: List<UnlockPrerequisite> = listOf(
        UnlockPrerequisite(label = "已填写解锁理由", satisfied = false),
        UnlockPrerequisite(label = "已填写解锁凭据", satisfied = false),
    ),
    val maskedSensitiveFields: List<UnlockMaskedField> = emptyList(),
)
