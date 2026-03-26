package com.opencode.nfccardmanager.core.nfc.model

data class WriteCardRequest(
    val text: String,
)

data class WriteCardResult(
    val cardInfo: CardInfo,
    val success: Boolean,
    val message: String,
    val payloadPreview: String,
    val verified: Boolean = false,
    val verificationMessage: String = "未校验",
)
