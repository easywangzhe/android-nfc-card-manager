package com.opencode.nfccardmanager.feature.audit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogDetailScreen(
    logId: Long,
    onBack: () -> Unit,
    viewModel: AuditLogDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(logId) {
        viewModel.load(logId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("日志详情") },
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
            val log = uiState.log
            if (log == null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = if (uiState.isLoading) "日志加载中..." else "未找到对应日志")
                        }
                    }
                }
            } else {
                item {
                    Text(text = "审计日志 #${log.id}", style = MaterialTheme.typography.headlineSmall)
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "操作类型：${log.operationType}")
                            Text(text = "执行结果：${log.result}")
                            Text(text = "卡片 UID：${log.cardUidMasked}")
                            Text(text = "卡片类型：${log.cardType}")
                            Text(text = "操作者：${log.operatorId}")
                            Text(text = "时间：${log.createdAt.toDisplayTime()}")
                        }
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "执行说明", style = MaterialTheme.typography.titleLarge)
                            Text(text = log.message)
                        }
                    }
                }
            }
        }
    }
}
