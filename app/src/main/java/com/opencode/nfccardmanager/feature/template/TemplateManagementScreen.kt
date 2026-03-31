package com.opencode.nfccardmanager.feature.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencode.nfccardmanager.ui.component.AppCard
import com.opencode.nfccardmanager.ui.component.AppTopBar
import com.opencode.nfccardmanager.ui.component.PrimaryActionButton
import com.opencode.nfccardmanager.ui.component.SecondaryActionButton
import com.opencode.nfccardmanager.ui.component.SectionTitle
import com.opencode.nfccardmanager.ui.component.SupportImpactBadge
import com.opencode.nfccardmanager.ui.component.SupportPageSummaryCard
import com.opencode.nfccardmanager.ui.component.StatusPill
import com.opencode.nfccardmanager.ui.component.StatusTone
import com.opencode.nfccardmanager.ui.component.appPagePadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateManagementScreen(
    onBack: (() -> Unit)? = null,
    viewModel: TemplateManagementViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(title = "模板管理", onBack = onBack)
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.appPagePadding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SupportPageSummaryCard(summary = uiState.pageSummary)
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("当前说明")
                    Text(text = uiState.message, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusPill(
                            text = if (uiState.editingTemplateId == null) "新增模式" else "编辑模式",
                            tone = if (uiState.editingTemplateId == null) StatusTone.INFO else StatusTone.WARNING,
                        )
                        SupportImpactBadge(impact = uiState.pageSummary.impact)
                    }
                }
            }

            item {
                SectionTitle("模板编辑")
            }

            item {
                OutlinedTextField(
                    value = uiState.nameInput,
                    onValueChange = viewModel::onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("模板名称") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.descriptionInput,
                    onValueChange = viewModel::onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("模板说明") },
                    minLines = 2,
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.contentInput,
                    onValueChange = viewModel::onContentChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("模板内容") },
                    minLines = 4,
                )
            }

            item {
                PrimaryActionButton(
                    text = if (uiState.editingTemplateId == null) "保存新增模板" else "保存模板修改",
                    onClick = viewModel::saveTemplate,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                SecondaryActionButton(
                    text = "清空表单",
                    onClick = viewModel::startCreate,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                SectionTitle("模板列表")
            }

            items(uiState.templates) { template ->
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = "${template.name}（${template.version}）")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill("本地模板", StatusTone.SUCCESS)
                            SupportImpactBadge(impact = uiState.pageSummary.impact)
                        }
                        Text(text = "说明：${template.description}")
                        Text(text = "内容：${template.content}")
                    PrimaryActionButton(
                            text = "编辑",
                            onClick = { viewModel.startEdit(template) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SecondaryActionButton(
                            text = "删除",
                            onClick = { viewModel.deleteTemplate(template.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
