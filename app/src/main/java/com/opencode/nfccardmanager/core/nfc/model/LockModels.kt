package com.opencode.nfccardmanager.core.nfc.model

data class LockCardResult(
    val cardInfo: CardInfo,
    val success: Boolean,
    val message: String,
    val irreversible: Boolean = true,
    val verified: Boolean = false,
    val verificationMessage: String = "未校验",
)
