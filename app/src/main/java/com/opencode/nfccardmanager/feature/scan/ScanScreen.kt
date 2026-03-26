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
import com.opencode.nfccardmanager.core.nfc.NfcSessionManager
import com.opencode.nfccardmanager.core.nfc.TagParser
import com.opencode.nfccardmanager.core.nfc.model.maskedUid
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
    val nfcSessionManager = remember(activity) {
        activity?.let { NfcSessionManager(it) }
    }

    LaunchedEffect(mode) {
        viewModel.init(mode)
    }

    DisposableEffect(mode, nfcSessionManager, sessionRestartToken) {
        val sessionManager = nfcSessionManager
        if (sessionManager == null) {
            viewModel.onError("无法获取 Activity 上下文，暂时不能启动 NFC 扫描")
            onDispose { }
        } else {
            val isNfcAvailable = sessionManager.isNfcAvailable()
            val isNfcEnabled = sessionManager.isNfcEnabled()
            viewModel.startScan(isNfcAvailable, isNfcEnabled)

            if (isNfcAvailable && isNfcEnabled) {
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
                sessionManager.startReaderMode(callback)
                    .onFailure {
                        viewModel.onError(it.message ?: "启动 NFC 扫描失败")
                    }
            }

            onDispose {
                sessionManager.stopReaderMode()
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
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.titleMedium,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "扫描状态：${uiState.stage.name}")
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
                    nfcSessionManager?.let {
                        viewModel.startScan(it.isNfcAvailable(), it.isNfcEnabled())
                        sessionRestartToken += 1
                    } ?: viewModel.onError("无法获取 Activity 上下文，暂时不能启动 NFC 扫描")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("开始扫描")
            }

            Button(
                onClick = { viewModel.simulateReadCard() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("模拟识别卡片")
            }

            Button(
                onClick = {
                    viewModel.onError("演示异常：检测到多卡干扰，请只保留一张卡片")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("模拟异常")
            }

            Button(
                onClick = { viewModel.reset() },
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
