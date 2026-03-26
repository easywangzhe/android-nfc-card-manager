package com.opencode.nfccardmanager.feature.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateManagementScreen(
    onBack: (() -> Unit)? = null,
    viewModel: TemplateManagementViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("模板管理") },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) {
                            Text("返回")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(text = uiState.message, modifier = Modifier.fillMaxWidth())
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
                Button(
                    onClick = viewModel::saveTemplate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.editingTemplateId == null) "保存新增模板" else "保存模板修改")
                }
            }

            item {
                Button(
                    onClick = viewModel::startCreate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("清空表单")
                }
            }

            item {
                Text(text = "模板列表", modifier = Modifier.fillMaxWidth())
            }

            items(uiState.templates) { template ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = "${template.name}（${template.version}）")
                        Text(text = "说明：${template.description}")
                        Text(text = "内容：${template.content}")
                        Button(
                            onClick = { viewModel.startEdit(template) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("编辑")
                        }
                        Button(
                            onClick = { viewModel.deleteTemplate(template.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("删除")
                        }
                    }
                }
            }
        }
    }
}
