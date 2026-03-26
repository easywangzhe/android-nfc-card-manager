package com.opencode.nfccardmanager.feature.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.nfccardmanager.core.database.AuditLogManager
import com.opencode.nfccardmanager.core.nfc.model.WriteCardResult
import com.opencode.nfccardmanager.feature.template.LocalTemplateRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WriteViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        WriteUiState(
            templates = LocalTemplateRepository.listTemplates(),
        )
    )
    val uiState: StateFlow<WriteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            LocalTemplateRepository.templates.collect { templates ->
                _uiState.update { state ->
                    state.copy(
                        templates = templates,
                        selectedTemplateId = state.selectedTemplateId?.takeIf { id -> templates.any { it.id == id } },
                    )
                }
            }
        }
    }

    fun applyTemplate(templateId: String) {
        val template = LocalTemplateRepository.findById(templateId) ?: return
        _uiState.update {
            it.copy(
                selectedTemplateId = template.id,
                content = template.content,
                stage = WriteStage.READY,
                message = "已应用模板：${template.name}，请确认内容后贴卡写入",
                result = null,
            )
        }
    }

    fun onContentChange(value: String) {
        _uiState.update {
            it.copy(
                content = value,
                selectedTemplateId = null,
                stage = if (value.isBlank()) WriteStage.IDLE else WriteStage.READY,
                message = if (value.isBlank()) {
                    "请输入待写入的文本内容，然后贴卡写入"
                } else {
                    "内容已准备，请将支持 NDEF 的标签贴近手机背部"
                },
            )
        }
    }

    fun startWriting() {
        if (_uiState.value.content.isBlank()) {
            onError("写入内容不能为空")
            return
        }
        _uiState.update {
            it.copy(stage = WriteStage.WRITING, message = "等待标签贴近，检测到标签后将自动写入")
        }
    }

    fun onWriteResult(result: WriteCardResult) {
        AuditLogManager.save(
            operationType = "WRITE",
            cardUid = result.cardInfo.uid,
            cardType = result.cardInfo.techType.name,
            result = if (result.success) "SUCCESS" else "FAILED",
            message = "${result.message}；${result.verificationMessage}",
        )
        _uiState.update {
            it.copy(
                stage = if (result.success) WriteStage.SUCCESS else WriteStage.ERROR,
                message = result.message,
                result = result,
            )
        }
    }

    fun onError(message: String) {
        AuditLogManager.save(
            operationType = "WRITE",
            cardUid = "UNKNOWN",
            cardType = "UNKNOWN",
            result = "FAILED",
            message = message,
        )
        _uiState.update {
            it.copy(stage = WriteStage.ERROR, message = message)
        }
    }

    fun resetResult() {
        _uiState.update {
            it.copy(
                stage = if (it.content.isBlank()) WriteStage.IDLE else WriteStage.READY,
                message = if (it.content.isBlank()) "请输入待写入的文本内容，然后贴卡写入" else "内容已准备，请将支持 NDEF 的标签贴近手机背部",
                result = null,
            )
        }
    }
}
