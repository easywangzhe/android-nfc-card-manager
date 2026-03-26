package com.opencode.nfccardmanager.feature.write

import com.opencode.nfccardmanager.core.nfc.model.WriteCardResult
import com.opencode.nfccardmanager.feature.template.WriteTemplate

enum class WriteStage {
    IDLE,
    READY,
    WRITING,
    SUCCESS,
    ERROR,
}

data class WriteUiState(
    val content: String = "",
    val templates: List<WriteTemplate> = emptyList(),
    val selectedTemplateId: String? = null,
    val stage: WriteStage = WriteStage.IDLE,
    val message: String = "请输入待写入的文本内容，然后贴卡写入",
    val result: WriteCardResult? = null,
)
