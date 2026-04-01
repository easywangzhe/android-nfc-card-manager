package com.opencode.nfccardmanager.feature.unlock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencode.nfccardmanager.core.common.findActivity
import com.opencode.nfccardmanager.core.nfc.NfcOperationType
import com.opencode.nfccardmanager.core.nfc.NfcSessionManager
import com.opencode.nfccardmanager.core.nfc.ReaderModeSession
import com.opencode.nfccardmanager.core.nfc.TagParser
import com.opencode.nfccardmanager.core.nfc.UnlockExecutor
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.HighRiskResultSource
import com.opencode.nfccardmanager.core.nfc.model.LockMode
import com.opencode.nfccardmanager.core.nfc.model.TechType
import com.opencode.nfccardmanager.core.nfc.model.UnlockCardRequest
import com.opencode.nfccardmanager.core.nfc.model.UnlockCardResult
import com.opencode.nfccardmanager.core.nfc.model.presentation
import com.opencode.nfccardmanager.core.nfc.model.toCapabilityAuthenticity
import com.opencode.nfccardmanager.core.nfc.model.toNfcFlowStage
import com.opencode.nfccardmanager.core.security.ProtectedAction
import com.opencode.nfccardmanager.core.security.SecurityManager
import com.opencode.nfccardmanager.ui.component.AppCard
import com.opencode.nfccardmanager.ui.component.KeyValueRow
import com.opencode.nfccardmanager.ui.component.PrimaryActionButton
import com.opencode.nfccardmanager.ui.component.SecondaryActionButton
import com.opencode.nfccardmanager.ui.component.SectionTitle
import com.opencode.nfccardmanager.ui.component.StatusPill
import com.opencode.nfccardmanager.ui.component.StatusTone
import com.opencode.nfccardmanager.ui.component.toStatusTone
import com.opencode.nfccardmanager.ui.test.AppTestTags
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockVerifyScreen(
    onBack: () -> Unit,
    viewModel: UnlockViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val nfcManager = remember(activity) { activity?.let { NfcSessionManager(it) } }
    val tagParser = remember { TagParser() }
    val executor = remember { UnlockExecutor() }
    val scope = rememberCoroutineScope()
    var activeSession by remember { mutableStateOf<ReaderModeSession?>(null) }
    val stagePresentation = uiState.stage.toNfcFlowStage().presentation()
    val currentRole by SecurityManager.currentRole.collectAsStateWithLifecycle()
    val authenticityPresentation = ProtectedAction.UNLOCK.toCapabilityAuthenticity(uiState.capability).presentation()
    val isProcessing = uiState.stage == UnlockStage.SCANNING

    BackHandler(enabled = isProcessing) {}

    DisposableEffect(nfcManager) {
        onDispose {
            nfcManager?.releaseReaderMode(activeSession)
            activeSession = null
        }
    }

    LaunchedEffect(uiState.stage) {
        if (uiState.stage == UnlockStage.SUCCESS || uiState.stage == UnlockStage.ERROR || uiState.stage == UnlockStage.IDLE || uiState.stage == UnlockStage.READY) {
            nfcManager?.releaseReaderMode(activeSession)
            activeSession = null
        }
    }

    LaunchedEffect(uiState.stage, activeSession?.token) {
        val session = activeSession ?: return@LaunchedEffect
        if (uiState.stage == UnlockStage.SCANNING) {
            delay(15000)
            if (activeSession?.token == session.token && viewModel.uiState.value.stage == UnlockStage.SCANNING) {
                nfcManager?.releaseReaderMode(activeSession)
                activeSession = null
                viewModel.onError("15 秒内未检测到可识别卡片，请确认 NFC 已开启并将卡片贴近手机背部")
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("解锁") },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            if (isProcessing) return@TextButton
                            nfcManager?.releaseReaderMode(activeSession)
                            activeSession = null
                            onBack()
                        }
                        , enabled = !isProcessing
                    ) {
                        Text(if (isProcessing) "处理中" else "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AppTestTags.UNLOCK_BOUNDARY_CARD),
            ) {
                SectionTitle("解锁能力边界")
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill("受控恢复操作", StatusTone.WARNING)
                    Text(text = "1. 需先识别卡片边界，再判断是否可进入解锁流程。")
                    Text(text = "2. NDEF 永久只读通常不可逆，不能通用解锁。")
                    Text(text = "3. 当前密码保护型解锁仍是流程演示，未接入真实底层解除写保护命令。")
                    if (isProcessing) {
                        StatusPill(text = "处理中请勿离开或重复贴卡", tone = StatusTone.WARNING)
                    }
                }
            }

            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AppTestTags.UNLOCK_AUTHENTICITY_CARD),
            ) {
                SectionTitle("当前状态与真实性")
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    KeyValueRow("共享阶段", stagePresentation.title)
                    KeyValueRow("会话占用", if (activeSession != null) "进行中" else "空闲")
                    StatusPill(text = authenticityPresentation.label, tone = authenticityPresentation.tone.toStatusTone())
                    uiState.supportSummary?.let { summary ->
                        Text(text = summary.title, style = MaterialTheme.typography.titleMedium)
                        Text(text = "当前范围：${summary.supportedLabel}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "不支持场景：${summary.unsupportedLabel}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "结果来源边界：${summary.authenticityLabel}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(text = stagePresentation.detail, style = MaterialTheme.typography.bodyMedium)
                    Text(text = authenticityPresentation.detail, style = MaterialTheme.typography.bodyMedium)
                    Text(text = uiState.message, style = MaterialTheme.typography.titleMedium)
                }
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle("必需前置条件")
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.prerequisites.forEach { prerequisite ->
                        KeyValueRow(prerequisite.label, if (prerequisite.satisfied) "已满足" else "未满足")
                    }
                }
            }

            uiState.capability?.let { capability ->
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("卡片能力识别")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KeyValueRow("锁定类型", when (capability.lockMode) {
                            LockMode.PASSWORD_PROTECTED -> "密码保护"
                            LockMode.READ_ONLY_PERMANENT -> "永久只读"
                            LockMode.NONE -> "不支持锁定"
                        })
                        KeyValueRow("支持解锁", if (capability.canUnlock) "是" else "否")
                        KeyValueRow("写入需认证", if (capability.requiresAuthForWrite) "是" else "否")
                    }
                }
            }

            OutlinedTextField(
                value = uiState.reason,
                onValueChange = viewModel::onReasonChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("解锁理由") },
                placeholder = { Text("例如：标签信息需修正") },
                minLines = 2,
                enabled = !isProcessing,
            )

            OutlinedTextField(
                value = uiState.credential,
                onValueChange = viewModel::onCredentialChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("解锁凭据") },
                placeholder = { Text("演示密码 123456") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !isProcessing,
            )

            PrimaryActionButton(
                text = "开始解锁（仅密码保护型）",
                onClick = {
                    val permission = SecurityManager.ensureAccess(currentRole, ProtectedAction.UNLOCK)
                    if (permission.isFailure) {
                        viewModel.onError(permission.exceptionOrNull()?.message ?: "当前角色无权解锁")
                        return@PrimaryActionButton
                    }

                    val session = nfcManager
                    if (isProcessing) {
                        return@PrimaryActionButton
                    }

                    if (uiState.stage != UnlockStage.READY) {
                        viewModel.startUnlock()
                    } else if (session == null) {
                        viewModel.onError("无法获取 Activity 上下文，暂时不能启动解锁扫描")
                    } else {
                        val callback = session.createReaderCallback { tag ->
                            scope.launch {
                                runCatching { tagParser.parse(tag) }
                                    .onSuccess { readResult ->
                                        viewModel.onTagResolved(readResult)
                                        val result = executor.execute(
                                            readResult = readResult,
                                            request = UnlockCardRequest(
                                                reason = uiState.reason,
                                                credential = uiState.credential,
                                            ),
                                        )
                                        viewModel.onUnlockResult(result)
                                    }
                                    .onFailure {
                                        viewModel.onError("卡片识别失败，无法继续解锁流程")
                                    }
                            }
                        }

                        session.requestReaderMode(
                            owner = "unlock-screen",
                            operation = NfcOperationType.UNLOCK,
                            callback = callback,
                        ).onSuccess { readerSession ->
                            activeSession = readerSession
                            viewModel.startUnlock()
                        }.onFailure {
                            viewModel.onError(it.message ?: "启动解锁扫描失败")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AppTestTags.UNLOCK_START_BUTTON),
                enabled = uiState.stage == UnlockStage.READY && activeSession == null,
            )

            SecondaryActionButton(
                text = "模拟解锁成功（仅演示）",
                onClick = {
                    if (isProcessing) return@SecondaryActionButton
                    val permission = SecurityManager.ensureAccess(currentRole, ProtectedAction.UNLOCK)
                    if (permission.isFailure) {
                        viewModel.onError(permission.exceptionOrNull()?.message ?: "当前角色无权解锁")
                        return@SecondaryActionButton
                    }
                    viewModel.onUnlockResult(
                        UnlockCardResult(
                            cardInfo = CardInfo(
                                uid = "04A1B2C3D4",
                                techType = TechType.ULTRALIGHT,
                                summary = "演示解锁目标",
                            ),
                            success = true,
                            message = "演示解锁流程成功（仅演示）",
                            verificationMessage = "演示模式：已通过本地凭据校验，但未接入真实解除写保护命令",
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AppTestTags.UNLOCK_SIMULATE_SUCCESS_BUTTON),
                enabled = activeSession == null && !isProcessing,
            )

            uiState.result?.let { result ->
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AppTestTags.UNLOCK_RESULT_CARD),
                ) {
                    SectionTitle(if (result.success) "解锁结果" else "解锁失败")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.resultGuidance?.let { guidance ->
                            Column(modifier = Modifier.testTag(AppTestTags.UNLOCK_RESULT_SOURCE)) {
                                StatusPill(
                                    text = unlockResultSourceLabel(guidance.source),
                                    tone = when (guidance.source) {
                                        HighRiskResultSource.DEMO_ONLY -> StatusTone.INFO
                                        HighRiskResultSource.FAILED -> StatusTone.ERROR
                                        HighRiskResultSource.UNVERIFIED -> StatusTone.WARNING
                                        HighRiskResultSource.CONFIRMED_EXECUTED -> StatusTone.SUCCESS
                                    }
                                )
                                Text(text = "发生了什么：${guidance.conclusion}")
                                Text(text = "当前最安全下一步：${guidance.recoveryAction}")
                                Text(text = "建议动作：${guidance.ctaLabel}")
                            }
                        }
                        Text(text = if (result.success) "解锁成功" else "解锁失败")
                        uiState.maskedSensitiveFields.forEach { field ->
                            KeyValueRow(field.label, field.value)
                        }
                        KeyValueRow("卡类型", result.cardInfo.techType.name)
                        Text(text = "为什么：${result.message}")
                        Text(text = "校验说明：${result.verificationMessage}")
                        if (result.message.contains("仅演示") || result.verificationMessage.contains("演示模式")) {
                            Box(modifier = Modifier.testTag(AppTestTags.UNLOCK_DEMO_ONLY_BADGE)) {
                                StatusPill(text = "仅演示", tone = StatusTone.INFO)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun unlockResultSourceLabel(source: HighRiskResultSource): String {
    return when (source) {
        HighRiskResultSource.CONFIRMED_EXECUTED -> "已确认执行"
        HighRiskResultSource.FAILED -> "执行失败"
        HighRiskResultSource.UNVERIFIED -> "结果未验证"
        HighRiskResultSource.DEMO_ONLY -> "仅演示"
    }
}
