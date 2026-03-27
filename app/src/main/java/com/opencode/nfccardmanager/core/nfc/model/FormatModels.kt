package com.opencode.nfccardmanager.core.nfc.model

data class FormatCardResult(
    val cardInfo: CardInfo,
    val success: Boolean,
    val message: String,
    val status: String,
    val reason: String,
)
