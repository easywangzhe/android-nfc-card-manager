package com.opencode.nfccardmanager.feature.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.nfccardmanager.feature.support.SupportImpact
import com.opencode.nfccardmanager.feature.support.SupportPageSummary
import com.opencode.nfccardmanager.feature.support.SupportSection
import com.opencode.nfccardmanager.feature.support.supportPageSummary
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
    val message: String = "可新增、编辑、删除本地模板；这些改动只影响后续写卡复用效率。",
    val pageSummary: SupportPageSummary = templatePageSummary(),
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
                message = "请输入本地模板信息后点击保存新增；不会直接修改当前卡片。",
                pageSummary = templatePageSummary(),
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
                message = "已进入本地模板编辑模式：${template.name}",
                pageSummary = templatePageSummary(
                    summary = "模板仅用于本地复用与后续写卡准备，不会直接修改当前卡片。",
                ),
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
                    message = "本地模板新增成功，可继续用于后续写卡复用。",
                    nameInput = "",
                    descriptionInput = "",
                    contentInput = "",
                    pageSummary = templatePageSummary(),
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
                    message = "本地模板更新成功，不会直接修改当前卡片。",
                    nameInput = "",
                    descriptionInput = "",
                    contentInput = "",
                    pageSummary = templatePageSummary(),
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
                    message = "本地模板已删除，已移除对应复用入口。",
                    pageSummary = templatePageSummary(),
                )
            } else {
                it.copy(
                    message = "本地模板已删除，已移除对应复用入口。",
                    pageSummary = templatePageSummary(),
                )
            }
        }
    }
}

private fun templatePageSummary(
    summary: String = "模板只影响后续写卡的本地复用效率，不会直接修改当前卡片。",
): SupportPageSummary {
    return supportPageSummary(
        title = "模板工作台",
        impact = SupportImpact.LOCAL_CONVENIENCE,
        summary = summary,
        sections = listOf(
            SupportSection(
                title = "模板编辑",
                description = "维护本地复用内容",
            ),
            SupportSection(
                title = "模板列表",
                description = "查看版本与用途说明",
            ),
        ),
    )
}
