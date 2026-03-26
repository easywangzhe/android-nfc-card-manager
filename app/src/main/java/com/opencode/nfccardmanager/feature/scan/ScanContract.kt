package com.opencode.nfccardmanager.feature.scan

import com.opencode.nfccardmanager.core.nfc.model.CardCapability
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.ReadCardResult

enum class ScanMode {
    READ,
    WRITE,
    LOCK,
    UNLOCK,
}

enum class ScanStage {
    IDLE,
    SCANNING,
    TAG_DETECTED,
    SUCCESS,
    ERROR,
}

data class ScanUiState(
    val mode: ScanMode = ScanMode.READ,
    val stage: ScanStage = ScanStage.IDLE,
    val isNfcAvailable: Boolean = true,
    val isNfcEnabled: Boolean = true,
    val cardInfo: CardInfo? = null,
    val capability: CardCapability? = null,
    val message: String = "请将卡片贴近手机背部 NFC 区域",
)

sealed interface ScanUiEffect {
    data class NavigateToReadResult(val result: ReadCardResult) : ScanUiEffect
}
