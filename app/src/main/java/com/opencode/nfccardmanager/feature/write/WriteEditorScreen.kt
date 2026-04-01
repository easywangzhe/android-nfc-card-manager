package com.opencode.nfccardmanager.feature.write

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencode.nfccardmanager.core.common.findActivity
import com.opencode.nfccardmanager.core.nfc.NdefWriter
import com.opencode.nfccardmanager.core.nfc.NfcOperationType
import com.opencode.nfccardmanager.core.nfc.NfcSessionManager
import com.opencode.nfccardmanager.core.nfc.ReaderModeSession
import com.opencode.nfccardmanager.core.nfc.TagParser
import com.opencode.nfccardmanager.core.nfc.model.presentation
import com.opencode.nfccardmanager.core.nfc.model.toCapabilityAuthenticity
import com.opencode.nfccardmanager.core.nfc.model.WriteCardRequest
import com.opencode.nfccardmanager.core.nfc.model.WriteCardResult
import com.opencode.nfccardmanager.core.nfc.model.toNfcFlowStage
import com.opencode.nfccardmanager.core.nfc.model.toWriteStatusLabel
import com.opencode.nfccardmanager.core.security.ProtectedAction
import com.opencode.nfccardmanager.core.security.SecurityManager
import com.opencode.nfccardmanager.ui.component.AppCard
import com.opencode.nfccardmanager.ui.component.AppTopBar
import com.opencode.nfccardmanager.ui.component.KeyValueRow
import com.opencode.nfccardmanager.ui.component.PrimaryActionButton
import com.opencode.nfccardmanager.ui.component.SecondaryActionButton
import com.opencode.nfccardmanager.ui.component.SectionTitle
import com.opencode.nfccardmanager.ui.component.StatusPill
import com.opencode.nfccardmanager.ui.component.StatusTone
import com.opencode.nfccardmanager.ui.component.appPagePadding
import com.opencode.nfccardmanager.ui.component.toStatusTone
import com.opencode.nfccardmanager.ui.test.AppTestTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val tagParser = remember { TagParser() }
    val scope = rememberCoroutineScope()
    var activeSession by remember { mutableStateOf<ReaderModeSession?>(null) }
    val stagePresentation = uiState.stage.toNfcFlowStage().presentation()
    val currentRole by SecurityManager.currentRole.collectAsStateWithLifecycle()
    val authenticityPresentation = ProtectedAction.WRITE.toCapabilityAuthenticity().presentation()

    DisposableEffect(nfcSessionManager) {
        onDispose {
            nfcSessionManager?.releaseReaderMode(activeSession)
            activeSession = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.resetResult()
    }

    LaunchedEffect(uiState.stage) {
        if (uiState.stage == WriteStage.SUCCESS || uiState.stage == WriteStage.ERROR || uiState.stage == WriteStage.IDLE || uiState.stage == WriteStage.READY) {
            nfcSessionManager?.releaseReaderMode(activeSession)
            activeSession = null
        }
    }

    LaunchedEffect(uiState.stage, activeSession?.token) {
        val session = activeSession ?: return@LaunchedEffect
        if (uiState.stage == WriteStage.WRITING) {
            delay(15000)
            if (activeSession?.token == session.token && viewModel.uiState.value.stage == WriteStage.WRITING) {
                nfcSessionManager?.releaseReaderMode(activeSession)
                activeSession = null
                viewModel.onError("15 秒内未检测到可写标签，请确认 NFC 已开启且标签支持 NDEF 写入")
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "NDEF 写卡",
                onBack = {
                    nfcSessionManager?.releaseReaderMode(activeSession)
                    activeSession = null
                    onBack()
                }
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.appPagePadding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AppTestTags.WRITE_STAGE_CARD),
                ) {
                    Text(text = "NDEF 写卡", style = MaterialTheme.typography.headlineSmall)
                    Text(text = uiState.message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(
                            text = when (uiState.stage) {
                                WriteStage.SUCCESS -> "写卡成功"
                                WriteStage.ERROR -> "写卡异常"
                                WriteStage.WRITING -> "写卡中"
                                WriteStage.READY -> "待写入"
                                else -> "待填写"
                            },
                            tone = when (uiState.stage) {
                                WriteStage.SUCCESS -> StatusTone.SUCCESS
                                WriteStage.ERROR -> StatusTone.ERROR
                                WriteStage.WRITING -> StatusTone.INFO
                                else -> StatusTone.WARNING
                            }
                        )
                        KeyValueRow(
                            "当前步骤",
                            when (uiState.stage) {
                                WriteStage.IDLE -> "1 选择模板"
                                WriteStage.READY -> "2 准备写入"
                                WriteStage.WRITING -> "3 贴卡写入"
                                WriteStage.SUCCESS -> "4 校验完成"
                                WriteStage.ERROR -> "4 异常处理"
                            }
                        )
                        KeyValueRow("共享阶段", stagePresentation.title)
                        KeyValueRow("会话占用", if (activeSession != null) "进行中" else "空闲")
                        StatusPill(text = authenticityPresentation.label, tone = authenticityPresentation.tone.toStatusTone())
                        Text(text = stagePresentation.detail, style = MaterialTheme.typography.bodyMedium)
                        Text(text = authenticityPresentation.detail, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (uiState.stage == WriteStage.WRITING) {
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle("等待贴卡")
                        Text(
                            text = "请将支持 NDEF 的标签稳定贴近手机背部 NFC 区域，检测到卡片后会自动写入。",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("标签识别反馈")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KeyValueRow("UID", uiState.detectedUid ?: "尚未识别到标签")
                        KeyValueRow("卡类型", uiState.detectedTechType ?: "未知")
                        KeyValueRow(
                            "支持 NDEF 写入",
                            uiState.detectedWritable?.let { if (it) "是" else "否" } ?: "待检测"
                        )
                        KeyValueRow("NDEF 类型", uiState.detectedNdefType ?: "待检测")
                        KeyValueRow(
                            "NDEF 容量",
                            uiState.detectedCapacity?.let { "$it bytes" } ?: "待检测"
                        )
                        KeyValueRow(
                            "当前写入需容量",
                            uiState.detectedRequiredBytes?.let { "$it bytes" } ?: "待检测"
                        )
                        KeyValueRow(
                            "可设只读",
                            uiState.detectedReadOnlyCapable?.let { if (it) "是" else "否" } ?: "待检测"
                        )
                        KeyValueRow(
                            "允许继续写入",
                            uiState.detectedCanProceed?.let { if (it) "是" else "否" } ?: "待检测"
                        )

                        Text(
                            text = if (uiState.detectedMessage.isNotBlank()) uiState.detectedMessage else "等待贴卡后显示标签诊断结果。",
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        if (uiState.detectedTechList.isNotEmpty()) {
                            SectionTitle("原始技术栈")
                            uiState.detectedTechList.forEach { tech ->
                                Text(tech, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            if (uiState.lastErrorDetail.isNotBlank()) {
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle("最近错误")
                        Text(
                            text = uiState.lastErrorDetail,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            item {
                SectionTitle("模板选择")
            }

            items(uiState.templates) { template ->
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "${template.name}（${template.version}）")
                        StatusPill(
                            text = if (uiState.selectedTemplateId == template.id) "当前使用中" else "可选模板",
                            tone = if (uiState.selectedTemplateId == template.id) StatusTone.SUCCESS else StatusTone.INFO,
                        )
                        Text(text = template.description)
                        Text(text = "预设内容：${template.content}")
                        PrimaryActionButton(
                            text = if (uiState.selectedTemplateId == template.id) "已应用该模板" else "应用模板",
                            onClick = { viewModel.applyTemplate(template.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        )
                    }
                }
            }

            item {
                SectionTitle("写入内容")
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
                SectionTitle("下一步操作")
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryActionButton(
                        text = if (uiState.stage == WriteStage.WRITING) "等待贴卡中..." else "开始写卡",
                        onClick = {
                            val permission = SecurityManager.ensureAccess(currentRole, ProtectedAction.WRITE)
                            if (permission.isFailure) {
                                viewModel.onError(permission.exceptionOrNull()?.message ?: "当前角色无权写卡")
                                return@PrimaryActionButton
                            }

                            val sessionManager = nfcSessionManager
                            if (sessionManager == null) {
                                viewModel.onError("无法获取 Activity 上下文，暂时不能启动写卡扫描")
                            } else if (uiState.content.isBlank()) {
                                viewModel.onError("写入内容不能为空")
                            } else {
                                val callback = sessionManager.createReaderCallback { tag ->
                                    scope.launch {
                                        val uid = tag.id?.joinToString(separator = "") { byte -> "%02X".format(byte) } ?: "UNKNOWN"
                                        val techList = tag.techList.toList()
                                        val techType = when {
                                            techList.any { it.endsWith("Ndef") } -> "NDEF"
                                            techList.any { it.endsWith("MifareUltralight") } -> "ULTRALIGHT"
                                            techList.any { it.endsWith("MifareClassic") } -> "MIFARE_CLASSIC"
                                            techList.any { it.endsWith("IsoDep") } -> "ISO_DEP"
                                            techList.any { it.endsWith("NfcA") } -> "NFC_A"
                                            else -> "UNKNOWN"
                                        }
                                        val readResult = runCatching { tagParser.parse(tag) }.getOrNull()
                                        delay(300)
                                        val precheck = writer.precheck(tag, WriteCardRequest(uiState.content))
                                        viewModel.onRawTagDetected(
                                            uid = uid,
                                            techType = techType,
                                            techList = techList,
                                            supportsWrite = precheck.supportNdefWrite,
                                            canProceed = precheck.canProceed,
                                            precheckReason = precheck.reason,
                                            requiredBytes = precheck.requiredBytes,
                                            capacityBytes = precheck.capacityBytes,
                                            isWritable = precheck.isWritable,
                                        )
                                        readResult?.let {
                                            viewModel.onTagDetected(
                                                readResult = it,
                                                supportsWrite = precheck.supportNdefWrite,
                                                canProceed = precheck.canProceed,
                                                precheckReason = precheck.reason,
                                                requiredBytes = precheck.requiredBytes,
                                            )
                                        }
                                        if (!precheck.canProceed) {
                                            viewModel.onError(precheck.reason)
                                            return@launch
                                        }
                                        val result = withContext(Dispatchers.IO) {
                                            writer.writeText(tag, WriteCardRequest(uiState.content))
                                        }
                                        viewModel.onWriteResult(result)
                                    }
                                }

                                sessionManager.requestReaderMode(
                                    owner = "write-screen",
                                    operation = NfcOperationType.WRITE,
                                    callback = callback,
                                ).onSuccess { session ->
                                    activeSession = session
                                    viewModel.startWriting()
                                }.onFailure {
                                    viewModel.onError(it.message ?: "启动写卡扫描失败")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AppTestTags.WRITE_START_BUTTON),
                        enabled = uiState.content.isNotBlank() && uiState.stage != WriteStage.WRITING && activeSession == null,
                    )

                    if (uiState.stage == WriteStage.ERROR || uiState.detectedUid != null || uiState.lastErrorDetail.isNotBlank()) {
                        SecondaryActionButton(
                            text = "重新扫描 / 重试写卡",
                            onClick = {
                                nfcSessionManager?.releaseReaderMode(activeSession)
                                activeSession = null
                                viewModel.resetResult()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(AppTestTags.WRITE_RETRY_BUTTON),
                            enabled = uiState.content.isNotBlank() && uiState.stage != WriteStage.WRITING && activeSession == null,
                        )
                    }
                }
            }

            item {
                SecondaryActionButton(
                    text = "模拟写卡成功（仅演示）",
                    onClick = {
                        val permission = SecurityManager.ensureAccess(currentRole, ProtectedAction.WRITE)
                        if (permission.isFailure) {
                            viewModel.onError(permission.exceptionOrNull()?.message ?: "当前角色无权写卡")
                            return@SecondaryActionButton
                        }
                        viewModel.onWriteResult(
                            WriteCardResult(
                                cardInfo = com.opencode.nfccardmanager.core.nfc.model.CardInfo(
                                    uid = "04A1B2C3D4",
                                    techType = com.opencode.nfccardmanager.core.nfc.model.TechType.NDEF,
                                    summary = "演示写卡目标",
                                ),
                                success = true,
                                message = "演示写卡成功（仅演示）",
                                payloadPreview = uiState.content.ifBlank { "演示内容" },
                                writeStatus = "WRITE_SUCCESS",
                                writeReason = "演示模式：标签支持 NDEF 写入。",
                                verified = true,
                                verificationMessage = "演示模式：仅更新本地界面，未对真实卡片执行写入与回读",
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AppTestTags.WRITE_SIMULATE_SUCCESS_BUTTON),
                )
            }

            item {
                uiState.result?.let { result ->
                    val guidance = uiState.resultGuidance
                    val nextStep = uiState.nextStepGuidance
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle(if (result.success) "写卡结果" else "写卡异常")
                        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(
                                text = result.writeStatus.toWriteStatusLabel(),
                                tone = if (result.success) StatusTone.SUCCESS else StatusTone.ERROR,
                            )
                            KeyValueRow("UID", result.cardInfo.uid)
                            KeyValueRow("卡类型", result.cardInfo.techType.name)
                            Text(text = "结果：${result.message}")
                            Text(text = "写入内容：${result.payloadPreview}")

                            guidance?.let {
                                Column(modifier = Modifier.testTag(AppTestTags.WRITE_EXECUTION_SECTION)) {
                                    SectionTitle("写入执行结果")
                                    StatusPill(
                                        text = it.executionTitle,
                                        tone = if (it.executionTitle.contains("成功")) StatusTone.SUCCESS else StatusTone.ERROR,
                                    )
                                    Text(text = it.executionConclusion)
                                    Text(text = "执行说明：${result.writeReason}")
                                }

                                Column(modifier = Modifier.testTag(AppTestTags.WRITE_VERIFICATION_SECTION)) {
                                    SectionTitle("回读校验结果")
                                    StatusPill(
                                        text = it.verificationTitle,
                                        tone = if (it.verificationTitle.contains("通过")) StatusTone.SUCCESS else StatusTone.WARNING,
                                    )
                                    Text(text = it.verificationConclusion)
                                    Text(text = "校验细节：${result.verificationMessage}")
                                }
                            } ?: run {
                                Text(text = "失败/说明：${result.writeReason}")
                                KeyValueRow("回读校验", if (result.verified) "通过" else "失败")
                                Text(text = "校验说明：${result.verificationMessage}")
                            }

                            nextStep?.let {
                                Column(modifier = Modifier.testTag(AppTestTags.WRITE_NEXT_STEP_SECTION)) {
                                    SectionTitle(it.title)
                                    Text(text = it.conclusion)
                                    Text(text = "判断依据：${it.reasonSummary}")
                                    Text(text = "建议动作：${it.recommendedAction}")
                                    KeyValueRow("建议 CTA", it.ctaLabel)
                                }
                            }

                            if (result.message.contains("仅演示") || result.verificationMessage.contains("演示模式")) {
                                Box(modifier = Modifier.testTag(AppTestTags.WRITE_DEMO_ONLY_BADGE)) {
                                    StatusPill(text = "仅演示", tone = StatusTone.INFO)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
