package com.opencode.nfccardmanager.feature.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TemplateManagementUiState(
    val templates: List<WriteTemplate> = emptyList(),
    val editingTemplateId: String? = null,
    val nameInput: String = "",
    val descriptionInput: String = "",
    val contentInput: String = "",
    val message: String = "可新增、编辑、删除本地模板。",
)

class TemplateManagementViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        TemplateManagementUiState(
            templates = LocalTemplateRepository.listTemplates(),
        )
    )
    val uiState: StateFlow<TemplateManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            LocalTemplateRepository.templates.collect { templates ->
                _uiState.update { it.copy(templates = templates) }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(nameInput = value) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(descriptionInput = value) }
    }

    fun onContentChange(value: String) {
        _uiState.update { it.copy(contentInput = value) }
    }

    fun startCreate() {
        _uiState.update {
            it.copy(
                editingTemplateId = null,
                nameInput = "",
                descriptionInput = "",
                contentInput = "",
                message = "请输入模板信息后点击保存新增。",
            )
        }
    }

    fun startEdit(template: WriteTemplate) {
        _uiState.update {
            it.copy(
                editingTemplateId = template.id,
                nameInput = template.name,
                descriptionInput = template.description,
                contentInput = template.content,
                message = "已进入编辑模式：${template.name}",
            )
        }
    }

    fun saveTemplate() {
        val state = _uiState.value
        if (state.nameInput.isBlank() || state.contentInput.isBlank()) {
            _uiState.update { it.copy(message = "模板名称和内容不能为空") }
            return
        }

        val editingId = state.editingTemplateId
        if (editingId == null) {
            LocalTemplateRepository.addTemplate(
                name = state.nameInput,
                description = state.descriptionInput,
                content = state.contentInput,
            )
            _uiState.update {
                it.copy(
                    message = "模板新增成功",
                    nameInput = "",
                    descriptionInput = "",
                    contentInput = "",
                )
            }
        } else {
            LocalTemplateRepository.updateTemplate(
                id = editingId,
                name = state.nameInput,
                description = state.descriptionInput,
                content = state.contentInput,
            )
            _uiState.update {
                it.copy(
                    editingTemplateId = null,
                    message = "模板更新成功",
                    nameInput = "",
                    descriptionInput = "",
                    contentInput = "",
                )
            }
        }
    }

    fun deleteTemplate(id: String) {
        LocalTemplateRepository.deleteTemplate(id)
        _uiState.update {
            if (it.editingTemplateId == id) {
                it.copy(
                    editingTemplateId = null,
                    nameInput = "",
                    descriptionInput = "",
                    contentInput = "",
                    message = "模板已删除",
                )
            } else {
                it.copy(message = "模板已删除")
            }
        }
    }
}
