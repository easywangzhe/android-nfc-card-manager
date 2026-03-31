package com.opencode.nfccardmanager.feature.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencode.nfccardmanager.core.common.findActivity
import com.opencode.nfccardmanager.core.nfc.NdefLocker
import com.opencode.nfccardmanager.core.nfc.NfcOperationType
import com.opencode.nfccardmanager.core.nfc.NfcSessionManager
import com.opencode.nfccardmanager.core.nfc.PasswordProtectedLocker
import com.opencode.nfccardmanager.core.nfc.ReaderModeSession
import com.opencode.nfccardmanager.core.nfc.TagParser
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.LockCardResult
import com.opencode.nfccardmanager.core.nfc.model.LockMode
import com.opencode.nfccardmanager.core.nfc.model.TechType
import com.opencode.nfccardmanager.core.nfc.model.presentation
import com.opencode.nfccardmanager.core.nfc.model.toNfcFlowStage
import com.opencode.nfccardmanager.ui.component.AppCard
import com.opencode.nfccardmanager.ui.component.DangerActionButton
import com.opencode.nfccardmanager.ui.component.KeyValueRow
import com.opencode.nfccardmanager.ui.component.SecondaryActionButton
import com.opencode.nfccardmanager.ui.component.SectionTitle
import com.opencode.nfccardmanager.ui.component.StatusPill
import com.opencode.nfccardmanager.ui.component.StatusTone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockRiskScreen(
    onBack: () -> Unit,
    viewModel: LockViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val sessionManager = remember(activity) { activity?.let { NfcSessionManager(it) } }
    val locker = remember { NdefLocker() }
    val passwordLocker = remember { PasswordProtectedLocker() }
    val tagParser = remember { TagParser() }
    val scope = rememberCoroutineScope()
    var activeSession by remember { mutableStateOf<ReaderModeSession?>(null) }
    val stagePresentation = uiState.stage.toNfcFlowStage().presentation()

    DisposableEffect(sessionManager) {
        onDispose {
            sessionManager?.releaseReaderMode(activeSession)
            activeSession = null
        }
    }

    LaunchedEffect(uiState.stage) {
        if (uiState.stage == LockStage.SUCCESS || uiState.stage == LockStage.ERROR || uiState.stage == LockStage.IDLE || uiState.stage == LockStage.READY) {
            sessionManager?.releaseReaderMode(activeSession)
            activeSession = null
        }
    }

    LaunchedEffect(uiState.stage, activeSession?.token) {
        val session = activeSession ?: return@LaunchedEffect
        if (uiState.stage == LockStage.LOCKING) {
            delay(15000)
            if (activeSession?.token == session.token && viewModel.uiState.value.stage == LockStage.LOCKING) {
                sessionManager?.releaseReaderMode(activeSession)
                activeSession = null
                viewModel.onError("15 秒内未检测到可锁定卡片，请确认 NFC 已开启并将卡片贴近手机背部")
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("锁卡（永久只读）") },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            sessionManager?.releaseReaderMode(activeSession)
                            activeSession = null
                            onBack()
                        }
                    ) {
                        Text("返回")
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
            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle("高风险提示")
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    StatusPill("高风险操作", StatusTone.ERROR)
                    Text(text = "1. 系统会先识别卡片能力，优先采用密码保护方案")
                    Text(text = "2. 若卡片不支持密码保护，则降级为永久只读")
                    Text(text = "3. 永久只读通常不可解锁，请确认风险")
                }
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle("锁卡策略")
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyValueRow("共享阶段", stagePresentation.title)
                    KeyValueRow("会话占用", if (activeSession != null) "进行中" else "空闲")
                    KeyValueRow(
                        "推荐方式",
                        when (uiState.recommendedMode) {
                            LockMode.PASSWORD_PROTECTED -> "密码保护"
                            LockMode.READ_ONLY_PERMANENT -> "永久只读"
                            else -> "待识别"
                        }
                    )
                    Text(text = stagePresentation.detail, style = MaterialTheme.typography.bodyMedium)
                    Text(text = uiState.message, style = MaterialTheme.typography.bodyLarge)
                    Text(text = uiState.modeHint, style = MaterialTheme.typography.bodyMedium)
                }
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle("风险确认")
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Checkbox(
                        checked = uiState.riskAcknowledged,
                        onCheckedChange = viewModel::toggleRiskAcknowledged,
                    )
                    Text(text = "我已知晓该操作通常不可逆，并愿意继续")
                }
            }

            OutlinedTextField(
                value = uiState.confirmText,
                onValueChange = viewModel::onConfirmTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("请输入确认词 LOCK") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                singleLine = true,
            )

            DangerActionButton(
                text = "确认锁卡（高风险）",
                onClick = {
                    val nfcManager = sessionManager
                    if (uiState.stage != LockStage.READY) {
                        viewModel.startLocking()
                    } else if (nfcManager == null) {
                        viewModel.onError("无法获取 Activity 上下文，暂时不能启动锁卡扫描")
                    } else {
                        val callback = nfcManager.createReaderCallback { tag ->
                            scope.launch {
                                val readResult = runCatching { tagParser.parse(tag) }.getOrNull()
                                if (readResult == null) {
                                    viewModel.onError("锁卡前无法识别卡片能力，请重试")
                                    return@launch
                                }
                                viewModel.onTagResolved(readResult)
                                val result = when (readResult.capability.lockMode) {
                                    LockMode.PASSWORD_PROTECTED -> passwordLocker.lock(readResult)
                                    LockMode.READ_ONLY_PERMANENT -> locker.makeReadOnly(tag)
                                    else -> LockCardResult(
                                        cardInfo = readResult.cardInfo,
                                        success = false,
                                        message = "当前卡片不支持锁卡",
                                        lockMode = LockMode.NONE,
                                        irreversible = false,
                                        verified = false,
                                        verificationMessage = "既不支持密码保护，也不支持永久只读锁定。",
                                    )
                                }
                                viewModel.onLockResult(result)
                            }
                        }

                        nfcManager.requestReaderMode(
                            owner = "lock-screen",
                            operation = NfcOperationType.LOCK,
                            callback = callback,
                        ).onSuccess { session ->
                            activeSession = session
                            viewModel.startLocking()
                        }.onFailure {
                            viewModel.onError(it.message ?: "启动锁卡扫描失败")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.stage == LockStage.READY && activeSession == null,
            )

            SecondaryActionButton(
                text = "模拟锁卡成功",
                onClick = {
                    viewModel.onLockResult(
                        LockCardResult(
                            cardInfo = CardInfo(
                                uid = "04A1B2C3D4",
                                techType = TechType.ULTRALIGHT,
                                summary = "演示锁卡目标",
                            ),
                            success = true,
                            message = "演示锁卡成功，已优先采用密码保护方案",
                            lockMode = LockMode.PASSWORD_PROTECTED,
                            irreversible = false,
                            verified = true,
                            verificationMessage = "演示校验通过：后续可通过凭据解锁",
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = activeSession == null,
            )

            uiState.result?.let { result ->
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(if (result.success) "锁卡结果" else "锁卡失败")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = if (result.success) "锁卡成功" else "锁卡失败")
                        KeyValueRow("UID", result.cardInfo.uid)
                        KeyValueRow("卡类型", result.cardInfo.techType.name)
                        KeyValueRow("锁定方式", when (result.lockMode) { LockMode.PASSWORD_PROTECTED -> "密码保护"; LockMode.READ_ONLY_PERMANENT -> "永久只读"; else -> "不支持" })
                        Text(text = "结果：${result.message}")
                        KeyValueRow("不可逆", if (result.irreversible) "是" else "否")
                        KeyValueRow("只读校验", if (result.verified) "通过" else "失败")
                        Text(text = "校验说明：${result.verificationMessage}")
                    }
                }
            }
        }
    }
}
