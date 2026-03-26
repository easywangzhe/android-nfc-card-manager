package com.opencode.nfccardmanager.core.nfc.model

data class UnlockCardRequest(
    val reason: String,
    val credential: String,
)

data class UnlockCardResult(
    val cardInfo: CardInfo,
    val success: Boolean,
    val message: String,
    val verificationMessage: String,
)
