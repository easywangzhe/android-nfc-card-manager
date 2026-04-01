package com.opencode.nfccardmanager.feature.format

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencode.nfccardmanager.core.common.findActivity
import com.opencode.nfccardmanager.core.nfc.NdefFormatter
import com.opencode.nfccardmanager.core.nfc.NfcOperationType
import com.opencode.nfccardmanager.core.nfc.NfcSessionManager
import com.opencode.nfccardmanager.core.nfc.ReaderModeSession
import com.opencode.nfccardmanager.core.nfc.model.buildFormatNextStepGuidance
import com.opencode.nfccardmanager.core.nfc.model.presentation
import com.opencode.nfccardmanager.core.nfc.model.toCapabilityAuthenticity
import com.opencode.nfccardmanager.core.nfc.model.toNfcFlowStage
import com.opencode.nfccardmanager.core.security.ProtectedAction
import com.opencode.nfccardmanager.core.security.SecurityManager
import com.opencode.nfccardmanager.ui.component.AppCard
import com.opencode.nfccardmanager.ui.component.KeyValueRow
import com.opencode.nfccardmanager.ui.component.PrimaryActionButton
import com.opencode.nfccardmanager.ui.component.SectionTitle
import com.opencode.nfccardmanager.ui.component.StatusPill
import com.opencode.nfccardmanager.ui.component.toStatusTone
import com.opencode.nfccardmanager.ui.test.AppTestTags
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatCardScreen(
    onBack: () -> Unit,
    onGoWrite: () -> Unit,
    viewModel: FormatViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val nfcManager = remember(activity) { activity?.let { NfcSessionManager(it) } }
    val formatter = remember { NdefFormatter() }
    val scope = rememberCoroutineScope()
    var activeSession by remember { mutableStateOf<ReaderModeSession?>(null) }
    val stagePresentation = uiState.stage.toNfcFlowStage().presentation()
    val currentRole by SecurityManager.currentRole.collectAsStateWithLifecycle()
    val authenticityPresentation = ProtectedAction.FORMAT.toCapabilityAuthenticity().presentation()

    DisposableEffect(nfcManager) {
        onDispose {
            nfcManager?.releaseReaderMode(activeSession)
            activeSession = null
        }
    }

    LaunchedEffect(uiState.stage) {
        if (uiState.stage == FormatStage.SUCCESS || uiState.stage == FormatStage.ERROR || uiState.stage == FormatStage.IDLE) {
            nfcManager?.releaseReaderMode(activeSession)
            activeSession = null
        }
    }

    LaunchedEffect(uiState.stage, activeSession?.token) {
        val session = activeSession ?: return@LaunchedEffect
        if (uiState.stage == FormatStage.SCANNING) {
            delay(15000)
            if (activeSession?.token == session.token && viewModel.uiState.value.stage == FormatStage.SCANNING) {
                nfcManager?.releaseReaderMode(activeSession)
                activeSession = null
                viewModel.onError("15 秒内未检测到可格式化卡片，请确认 NFC 已开启并将卡片贴近手机背部")
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("格式化卡") },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            nfcManager?.releaseReaderMode(activeSession)
                            activeSession = null
                            onBack()
                        }
                    ) { Text("返回") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle("格式化说明")
                Text("该功能会尝试将支持 NdefFormatable 的卡片格式化为 NDEF。")
                Text("如果卡片本身已是 NDEF 且可写，会执行清空内容。")
                Text("格式化或清空成功后，可直接进入写卡流程。")
            }

            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AppTestTags.FORMAT_STATUS_CARD),
            ) {
                SectionTitle("当前状态")
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    KeyValueRow("共享阶段", stagePresentation.title)
                    KeyValueRow("会话占用", if (activeSession != null) "进行中" else "空闲")
                    StatusPill(text = authenticityPresentation.label, tone = authenticityPresentation.tone.toStatusTone())
                    Text(stagePresentation.detail)
                    Text(authenticityPresentation.detail)
                    Text(uiState.message)
                }
            }

            uiState.result?.let { result ->
                val guidance = uiState.resultGuidance ?: buildFormatNextStepGuidance(result)
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AppTestTags.FORMAT_RESULT_CARD),
                ) {
                    SectionTitle(if (result.success) "格式化成功" else "格式化失败")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KeyValueRow("UID", result.cardInfo.uid)
                        KeyValueRow("卡类型", result.cardInfo.techType.name)
                        KeyValueRow("状态", result.status)
                        Text("结果：${result.message}")
                        Column(modifier = Modifier.testTag(AppTestTags.FORMAT_WHAT_HAPPENED_SECTION)) {
                            SectionTitle("发生了什么")
                            Text(guidance.conclusion)
                        }
                        Column(modifier = Modifier.testTag(AppTestTags.FORMAT_WHY_SECTION)) {
                            SectionTitle("为什么")
                            Text(guidance.reasonSummary)
                        }
                        Column(modifier = Modifier.testTag(AppTestTags.FORMAT_NEXT_STEP_SECTION)) {
                            SectionTitle(guidance.title)
                            Text(guidance.recommendedAction)
                            KeyValueRow("建议 CTA", guidance.ctaLabel)
                        }
                    }
                }
            }

            if (uiState.result == null && uiState.resultGuidance != null && uiState.stage == FormatStage.ERROR) {
                val guidance = uiState.resultGuidance ?: return@Column
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AppTestTags.FORMAT_RESULT_CARD),
                ) {
                    SectionTitle("格式化失败")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("结果：${uiState.message}")
                        Column(modifier = Modifier.testTag(AppTestTags.FORMAT_WHAT_HAPPENED_SECTION)) {
                            SectionTitle("发生了什么")
                            Text(guidance.conclusion)
                        }
                        Column(modifier = Modifier.testTag(AppTestTags.FORMAT_WHY_SECTION)) {
                            SectionTitle("为什么")
                            Text(guidance.reasonSummary)
                        }
                        Column(modifier = Modifier.testTag(AppTestTags.FORMAT_NEXT_STEP_SECTION)) {
                            SectionTitle(guidance.title)
                            Text(guidance.recommendedAction)
                            KeyValueRow("建议 CTA", guidance.ctaLabel)
                        }
                    }
                }
            }

            PrimaryActionButton(
                text = if (uiState.stage == FormatStage.SCANNING) "等待贴卡中..." else "开始格式化",
                onClick = {
                    val permission = SecurityManager.ensureAccess(currentRole, ProtectedAction.FORMAT)
                    if (permission.isFailure) {
                        viewModel.onError(permission.exceptionOrNull()?.message ?: "当前角色无权格式化卡片")
                        return@PrimaryActionButton
                    }

                    val sessionManager = nfcManager
                    if (sessionManager == null) {
                        viewModel.onError("无法获取 Activity 上下文，暂时不能启动格式化扫描")
                    } else {
                        val callback = sessionManager.createReaderCallback { tag ->
                            scope.launch {
                                viewModel.onFormatResult(formatter.format(tag))
                            }
                        }

                        sessionManager.requestReaderMode(
                            owner = "format-screen",
                            operation = NfcOperationType.FORMAT,
                            callback = callback,
                        ).onSuccess { session ->
                            activeSession = session
                            viewModel.start()
                        }.onFailure {
                            viewModel.onError(it.message ?: "启动格式化扫描失败")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AppTestTags.FORMAT_START_BUTTON),
                enabled = uiState.stage != FormatStage.SCANNING && activeSession == null,
            )

            PrimaryActionButton(
                text = uiState.resultGuidance?.ctaLabel?.takeIf { uiState.result?.success == true } ?: "去写卡",
                onClick = {
                    nfcManager?.releaseReaderMode(activeSession)
                    activeSession = null
                    onGoWrite()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AppTestTags.FORMAT_GO_WRITE_BUTTON),
                enabled = uiState.result?.success == true,
            )
        }
    }
}
