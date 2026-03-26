package com.opencode.nfccardmanager.feature.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencode.nfccardmanager.core.common.findActivity
import com.opencode.nfccardmanager.core.nfc.NdefLocker
import com.opencode.nfccardmanager.core.nfc.NfcSessionManager
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.LockCardResult
import com.opencode.nfccardmanager.core.nfc.model.TechType
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
    val scope = rememberCoroutineScope()

    DisposableEffect(sessionManager, uiState.stage) {
        val nfcManager = sessionManager
        if (nfcManager == null || uiState.stage != LockStage.LOCKING) {
            onDispose { }
        } else {
            val callback = nfcManager.createReaderCallback { tag ->
                scope.launch {
                    viewModel.onLockResult(locker.makeReadOnly(tag))
                }
            }

            if (nfcManager.isNfcAvailable() && nfcManager.isNfcEnabled()) {
                nfcManager.startReaderMode(callback)
                    .onFailure {
                        viewModel.onError(it.message ?: "启动锁卡扫描失败")
                    }
            } else {
                viewModel.onError("当前设备不支持 NFC 或 NFC 未开启")
            }

            onDispose {
                nfcManager.stopReaderMode()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("锁卡（永久只读）") },
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "高风险提示", style = MaterialTheme.typography.titleLarge)
                    Text(text = "1. 当前仅接入 NDEF 永久只读锁卡")
                    Text(text = "2. 锁卡后通常不可解锁")
                    Text(text = "3. 锁卡前请确认标签数据已正确")
                }
            }

            Text(text = uiState.message, style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "风险确认")
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

            Button(
                onClick = { viewModel.startLocking() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.stage == LockStage.READY || uiState.stage == LockStage.LOCKING,
            ) {
                Text("开始锁卡")
            }

            Button(
                onClick = {
                    viewModel.onLockResult(
                        LockCardResult(
                            cardInfo = CardInfo(
                                uid = "04A1B2C3D4",
                                techType = TechType.NDEF,
                                summary = "演示锁卡目标",
                            ),
                            success = true,
                            message = "演示锁卡成功，标签已进入永久只读",
                            irreversible = true,
                            verified = true,
                            verificationMessage = "演示校验通过：标签不可写",
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("模拟锁卡成功")
            }

            uiState.result?.let { result ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = if (result.success) "锁卡成功" else "锁卡失败")
                        Text(text = "UID：${result.cardInfo.uid}")
                        Text(text = "卡类型：${result.cardInfo.techType.name}")
                        Text(text = "结果：${result.message}")
                        Text(text = "不可逆：${if (result.irreversible) "是" else "否"}")
                        Text(text = "只读校验：${if (result.verified) "通过" else "失败"}")
                        Text(text = "校验说明：${result.verificationMessage}")
                    }
                }
            }
        }
    }
}
