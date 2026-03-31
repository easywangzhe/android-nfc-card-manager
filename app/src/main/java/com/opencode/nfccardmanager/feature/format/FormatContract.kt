package com.opencode.nfccardmanager.feature.format

import com.opencode.nfccardmanager.core.nfc.model.FlowNextStepGuidance
import com.opencode.nfccardmanager.core.nfc.model.FormatCardResult

enum class FormatStage {
    IDLE,
    SCANNING,
    SUCCESS,
    ERROR,
}

data class FormatUiState(
    val stage: FormatStage = FormatStage.IDLE,
    val message: String = "点击开始格式化后，再将待处理卡片贴近手机背部。",
    val result: FormatCardResult? = null,
    val resultGuidance: FlowNextStepGuidance? = null,
)
