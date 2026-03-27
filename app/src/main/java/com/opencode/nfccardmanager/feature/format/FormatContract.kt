package com.opencode.nfccardmanager.feature.format

import com.opencode.nfccardmanager.core.nfc.model.FormatCardResult

enum class FormatStage {
    IDLE,
    SCANNING,
    SUCCESS,
    ERROR,
}

data class FormatUiState(
    val stage: FormatStage = FormatStage.IDLE,
    val message: String = "请将待格式化卡片贴近手机背部，系统将尝试格式化为 NDEF。",
    val result: FormatCardResult? = null,
)
