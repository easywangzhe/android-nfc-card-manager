package com.opencode.nfccardmanager.feature.scan

import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencode.nfccardmanager.core.common.findActivity
import com.opencode.nfccardmanager.core.nfc.NfcOperationType
import com.opencode.nfccardmanager.core.nfc.NfcSessionManager
import com.opencode.nfccardmanager.core.nfc.ReaderModeSession
import com.opencode.nfccardmanager.core.nfc.TagParser
import com.opencode.nfccardmanager.core.nfc.model.presentation
import com.opencode.nfccardmanager.core.nfc.model.maskedUid
import com.opencode.nfccardmanager.core.nfc.model.toNfcFlowStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    mode: ScanMode,
    onBack: () -> Unit,
    onReadResult: (uid: String, techType: String, summary: String) -> Unit,
    viewModel: ScanViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val tagParser = remember { TagParser() }
    var sessionRestartToken by remember { mutableIntStateOf(0) }
    var activeSession by remember { mutableStateOf<ReaderModeSession?>(null) }
    val stagePresentation = uiState.stage.toNfcFlowStage().presentation()
    val nfcSessionManager = remember(activity) {
        activity?.let { NfcSessionManager(it) }
    }

    LaunchedEffect(mode) {
        viewModel.init(mode)
    }

    DisposableEffect(nfcSessionManager) {
        val sessionManager = nfcSessionManager
        onDispose {
            sessionManager?.releaseReaderMode(activeSession)
            activeSession = null
        }
    }

    LaunchedEffect(uiState.stage) {
        if (uiState.stage == ScanStage.SUCCESS || uiState.stage == ScanStage.ERROR || uiState.stage == ScanStage.IDLE) {
            nfcSessionManager?.releaseReaderMode(activeSession)
            activeSession = null
        }
    }

    LaunchedEffect(uiState.stage, activeSession?.token, sessionRestartToken) {
        val session = activeSession ?: return@LaunchedEffect
        if (uiState.stage == ScanStage.SCANNING) {
            delay(15000)
            if (activeSession?.token == session.token && viewModel.uiState.value.stage == ScanStage.SCANNING) {
                nfcSessionManager?.releaseReaderMode(activeSession)
                activeSession = null
                viewModel.onError("15 秒内未检测到卡片，请确认 NFC 已开启并将卡片贴近手机背部")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ScanUiEffect.NavigateToReadResult -> {
                    onReadResult(
                        effect.result.cardInfo.uid,
                        effect.result.cardInfo.techType.name,
                        effect.result.cardInfo.summary.orEmpty(),
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(titleForMode(mode)) },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            nfcSessionManager?.releaseReaderMode(activeSession)
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
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.titleMedium,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "共享阶段：${stagePresentation.title}")
                    Text(text = "阶段说明：${stagePresentation.detail}")
                    Text(text = "会话占用：${if (activeSession != null) "进行中" else "空闲"}")
                    Text(text = "NFC 可用：${uiState.isNfcAvailable}")
                    Text(text = "NFC 已开启：${uiState.isNfcEnabled}")
                    uiState.cardInfo?.let { cardInfo ->
                        Text(text = "UID：${cardInfo.maskedUid()}")
                        Text(text = "卡类型：${cardInfo.techType.name}")
                        Text(text = "摘要：${cardInfo.summary.orEmpty()}")
                    }
                }
            }

            Button(
                onClick = {
                    val sessionManager = nfcSessionManager
                    if (sessionManager == null) {
                        viewModel.onError("无法获取 Activity 上下文，暂时不能启动 NFC 扫描")
                    } else {
                        nfcSessionManager.releaseReaderMode(activeSession)
                        activeSession = null

                        val callback = sessionManager.createReaderCallback { tag ->
                            scope.launch {
                                runCatching { tagParser.parse(tag) }
                                    .onSuccess { result ->
                                        viewModel.onTagDiscovered(result)
                                    }
                                    .onFailure {
                                        viewModel.onError("卡片解析失败，请重试")
                                    }
                            }
                        }

                        sessionManager.requestReaderMode(
                            owner = "scan-screen-${mode.name.lowercase()}",
                            operation = when (mode) {
                                ScanMode.READ -> NfcOperationType.READ
                                ScanMode.WRITE -> NfcOperationType.WRITE
                                ScanMode.LOCK -> NfcOperationType.LOCK
                                ScanMode.UNLOCK -> NfcOperationType.UNLOCK
                            },
                            callback = callback,
                        ).onSuccess { session ->
                            activeSession = session
                            sessionRestartToken += 1
                            viewModel.startScan(sessionManager.isNfcAvailable(), sessionManager.isNfcEnabled())
                        }.onFailure {
                            viewModel.onError(it.message ?: "启动 NFC 扫描失败")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = activeSession == null && uiState.stage != ScanStage.SCANNING,
            ) {
                Text("开始扫描")
            }

            Button(
                onClick = { viewModel.simulateReadCard() },
                modifier = Modifier.fillMaxWidth(),
                enabled = activeSession == null,
            ) {
                Text("模拟识别卡片")
            }

            Button(
                onClick = {
                    viewModel.onError("演示异常：检测到多卡干扰，请只保留一张卡片")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = activeSession == null,
            ) {
                Text("模拟异常")
            }

            Button(
                onClick = {
                    nfcSessionManager?.releaseReaderMode(activeSession)
                    activeSession = null
                    viewModel.reset()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("重置状态")
            }

            if (mode == ScanMode.READ) {
                Button(
                    onClick = {
                        val demoCard = viewModel.demoCardInfo()
                        onReadResult(demoCard.uid, demoCard.techType.name, demoCard.summary.orEmpty())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = activeSession == null,
                ) {
                    Text("进入读卡结果演示")
                }
            }
        }
    }
}

private fun titleForMode(mode: ScanMode): String = when (mode) {
    ScanMode.READ -> "读卡 - 扫描卡片"
    ScanMode.WRITE -> "写卡 - 扫描卡片"
    ScanMode.LOCK -> "锁卡 - 扫描卡片"
    ScanMode.UNLOCK -> "解锁 - 扫描卡片"
}
