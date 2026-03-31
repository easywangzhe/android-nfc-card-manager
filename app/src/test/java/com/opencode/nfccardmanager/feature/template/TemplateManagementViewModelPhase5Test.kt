package com.opencode.nfccardmanager.feature.template

import com.opencode.nfccardmanager.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TemplateManagementViewModelPhase5Test {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `default state explains template is local convenience only`() {
        val viewModel = TemplateManagementViewModel()

        val uiState = viewModel.uiState.value
        assertEquals("本地便利性", uiState.pageSummary.impact.label)
        assertTrue(uiState.pageSummary.summary.contains("不会直接修改当前卡片"))
    }

    @Test
    fun `edit mode keeps local template boundary`() {
        val viewModel = TemplateManagementViewModel()
        val template = viewModel.uiState.value.templates.first()

        viewModel.startEdit(template)

        val uiState = viewModel.uiState.value
        assertTrue(uiState.message.contains("本地模板"))
        assertTrue(uiState.pageSummary.summary.contains("本地复用"))
    }

    @Test
    fun `save and delete feedback preserve impact semantics`() {
        val viewModel = TemplateManagementViewModel()
        viewModel.onNameChange("巡检模板")
        viewModel.onContentChange("设备编号=EQ-009")

        viewModel.saveTemplate()
        val afterSave = viewModel.uiState.value
        assertTrue(afterSave.message.contains("本地模板"))
        assertEquals("本地便利性", afterSave.pageSummary.impact.label)

        val savedTemplate = afterSave.templates.last()
        viewModel.deleteTemplate(savedTemplate.id)

        val afterDelete = viewModel.uiState.value
        assertTrue(afterDelete.message.contains("本地模板"))
        assertEquals("本地便利性", afterDelete.pageSummary.impact.label)
    }
}
