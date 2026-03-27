package com.opencode.nfccardmanager.feature.unlock

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencode.nfccardmanager.core.common.findActivity
import com.opencode.nfccardmanager.core.nfc.NfcSessionManager
import com.opencode.nfccardmanager.core.nfc.TagParser
import com.opencode.nfccardmanager.core.nfc.UnlockExecutor
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.LockMode
import com.opencode.nfccardmanager.core.nfc.model.TechType
import com.opencode.nfccardmanager.core.nfc.model.UnlockCardRequest
import com.opencode.nfccardmanager.core.nfc.model.UnlockCardResult
import com.opencode.nfccardmanager.ui.component.AppCard
import com.opencode.nfccardmanager.ui.component.KeyValueRow
import com.opencode.nfccardmanager.ui.component.PrimaryActionButton
import com.opencode.nfccardmanager.ui.component.SecondaryActionButton
import com.opencode.nfccardmanager.ui.component.SectionTitle
import com.opencode.nfccardmanager.ui.component.StatusPill
import com.opencode.nfccardmanager.ui.component.StatusTone
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

    DisposableEffect(nfcManager, uiState.stage) {
        val session = nfcManager
        if (session == null || uiState.stage != UnlockStage.SCANNING) {
            onDispose { }
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

            if (session.isNfcAvailable() && session.isNfcEnabled()) {
                session.startReaderMode(callback)
                    .onFailure {
                        viewModel.onError(it.message ?: "启动解锁扫描失败")
                    }
            } else {
                viewModel.onError("当前设备不支持 NFC 或 NFC 未开启")
            }

            onDispose {
                session.stopReaderMode()
            }
        }
    }

    LaunchedEffect(uiState.stage) {
        if (uiState.stage == UnlockStage.SCANNING) {
            delay(15000)
            if (viewModel.uiState.value.stage == UnlockStage.SCANNING) {
                viewModel.onError("15 秒内未检测到可识别卡片，请确认 NFC 已开启并将卡片贴近手机背部")
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("解锁") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
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
                SectionTitle("解锁能力边界")
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill("受控恢复操作", StatusTone.WARNING)
                    Text(text = "1. 本页面仅对密码保护型锁定有实际意义")
                    Text(text = "2. NDEF 永久只读锁定通常不可逆，不能通用解锁")
                    Text(text = "3. 当前已完成密码保护型解锁流程骨架，真实底层认证命令仍待接入")
                }
            }

            Text(text = uiState.message, style = MaterialTheme.typography.titleMedium)

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
            )

            OutlinedTextField(
                value = uiState.credential,
                onValueChange = viewModel::onCredentialChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("解锁凭据") },
                placeholder = { Text("演示密码 123456") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )

            PrimaryActionButton(
                text = "开始解锁（仅密码保护型）",
                onClick = viewModel::startUnlock,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.stage == UnlockStage.READY || uiState.stage == UnlockStage.SCANNING,
            )

            SecondaryActionButton(
                text = "模拟解锁成功",
                onClick = {
                    viewModel.onUnlockResult(
                        UnlockCardResult(
                            cardInfo = CardInfo(
                                uid = "04A1B2C3D4",
                                techType = TechType.ULTRALIGHT,
                                summary = "演示解锁目标",
                            ),
                            success = true,
                            message = "演示解锁流程成功",
                            verificationMessage = "演示模式：已通过凭据校验，后续可接真实解除写保护命令",
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            uiState.result?.let { result ->
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(if (result.success) "解锁结果" else "解锁失败")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = if (result.success) "解锁成功" else "解锁失败")
                        KeyValueRow("UID", result.cardInfo.uid)
                        KeyValueRow("卡类型", result.cardInfo.techType.name)
                        Text(text = "结果：${result.message}")
                        Text(text = "校验说明：${result.verificationMessage}")
                    }
                }
            }
        }
    }
}
