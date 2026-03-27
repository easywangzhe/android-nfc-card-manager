package com.opencode.nfccardmanager.core.nfc.model

data class WriteCardRequest(
    val text: String,
)

data class WriteCardResult(
    val cardInfo: CardInfo,
    val success: Boolean,
    val message: String,
    val payloadPreview: String,
    val writeStatus: String = "UNKNOWN",
    val writeReason: String = "",
    val verified: Boolean = false,
    val verificationMessage: String = "未校验",
)

fun String.toWriteStatusLabel(): String = when (this) {
    "WRITE_SUCCESS" -> "写入成功"
    "VERIFY_FAILED" -> "回读校验失败"
    "UNSUPPORTED_TAG" -> "标签不支持写入"
    "WRITE_ERROR" -> "写入异常"
    "FORMAT_ERROR" -> "格式化写入失败"
    else -> this
}
