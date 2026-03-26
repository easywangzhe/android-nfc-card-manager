package com.opencode.nfccardmanager.feature.write

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencode.nfccardmanager.core.common.findActivity
import com.opencode.nfccardmanager.core.nfc.NdefWriter
import com.opencode.nfccardmanager.core.nfc.NfcSessionManager
import com.opencode.nfccardmanager.core.nfc.model.WriteCardRequest
import com.opencode.nfccardmanager.core.nfc.model.WriteCardResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteEditorScreen(
    onBack: () -> Unit,
    viewModel: WriteViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val nfcSessionManager = remember(activity) { activity?.let { NfcSessionManager(it) } }
    val writer = remember { NdefWriter() }
    val scope = rememberCoroutineScope()

    DisposableEffect(nfcSessionManager, uiState.stage, uiState.content) {
        val sessionManager = nfcSessionManager
        if (sessionManager == null || uiState.stage != WriteStage.WRITING) {
            onDispose { }
        } else {
            val callback = sessionManager.createReaderCallback { tag ->
                scope.launch {
                    val result = writer.writeText(tag, WriteCardRequest(uiState.content))
                    viewModel.onWriteResult(result)
                }
            }

            if (sessionManager.isNfcAvailable() && sessionManager.isNfcEnabled()) {
                sessionManager.startReaderMode(callback)
                    .onFailure {
                        viewModel.onError(it.message ?: "启动写卡扫描失败")
                    }
            } else {
                viewModel.onError("当前设备不支持 NFC 或 NFC 未开启")
            }

            onDispose {
                sessionManager.stopReaderMode()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.resetResult()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NDEF 写卡") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
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
                Text(text = uiState.message, style = MaterialTheme.typography.titleMedium)
            }

            item {
                Text(text = "模板选择", style = MaterialTheme.typography.titleLarge)
            }

            items(uiState.templates) { template ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "${template.name}（${template.version}）")
                        Text(text = template.description)
                        Text(text = "预设内容：${template.content}")
                        Button(
                            onClick = { viewModel.applyTemplate(template.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        ) {
                            Text(
                                if (uiState.selectedTemplateId == template.id) {
                                    "已应用该模板"
                                } else {
                                    "应用模板"
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = viewModel::onContentChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("写入文本") },
                    placeholder = { Text("例如：设备编号=EQ-001") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    minLines = 4,
                )
            }

            item {
                Button(
                    onClick = { viewModel.startWriting() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.content.isNotBlank(),
                ) {
                    Text("开始写卡")
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.onWriteResult(
                            WriteCardResult(
                                cardInfo = com.opencode.nfccardmanager.core.nfc.model.CardInfo(
                                    uid = "04A1B2C3D4",
                                    techType = com.opencode.nfccardmanager.core.nfc.model.TechType.NDEF,
                                    summary = "演示写卡目标",
                                ),
                                success = true,
                                message = "演示写卡成功",
                                payloadPreview = uiState.content.ifBlank { "演示内容" },
                                verified = true,
                                verificationMessage = "演示回读校验通过",
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("模拟写卡成功")
                }
            }

            item {
                uiState.result?.let { result ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = if (result.success) "写卡成功" else "写卡失败")
                            Text(text = "UID：${result.cardInfo.uid}")
                            Text(text = "卡类型：${result.cardInfo.techType.name}")
                            Text(text = "结果：${result.message}")
                            Text(text = "写入内容：${result.payloadPreview}")
                            Text(text = "回读校验：${if (result.verified) "通过" else "失败"}")
                            Text(text = "校验说明：${result.verificationMessage}")
                        }
                    }
                }
            }
        }
    }
}
