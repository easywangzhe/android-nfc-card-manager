package com.opencode.nfccardmanager.feature.audit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(
    onBack: (() -> Unit)? = null,
    onLogClick: (Long) -> Unit,
    viewModel: AuditLogViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadLogs()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("日志审计") },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) {
                            Text("返回")
                        }
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
                Text(
                    text = if (uiState.isLoading) "日志加载中..." else "共 ${uiState.filteredLogs.size} / ${uiState.logs.size} 条本地审计日志",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.keyword,
                    onValueChange = viewModel::onKeywordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("关键词筛选") },
                    placeholder = { Text("操作类型 / UID / 卡类型 / 说明") },
                    singleLine = true,
                )
            }

            item {
                Text(text = "操作类型", style = MaterialTheme.typography.titleMedium)
            }

            item {
                val operations = listOf("ALL", "READ", "WRITE", "LOCK")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(operations) { operation ->
                        FilterChip(
                            selected = uiState.operationFilter == operation,
                            onClick = { viewModel.onOperationFilterChange(operation) },
                            label = { Text(if (operation == "ALL") "全部" else operation) },
                        )
                    }
                }
            }

            item {
                Text(text = "结果状态", style = MaterialTheme.typography.titleMedium)
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AuditResultFilter.entries.toList()) { filter ->
                        FilterChip(
                            selected = uiState.resultFilter == filter,
                            onClick = { viewModel.onResultFilterChange(filter) },
                            label = {
                                Text(
                                    when (filter) {
                                        AuditResultFilter.ALL -> "全部"
                                        AuditResultFilter.SUCCESS -> "成功"
                                        AuditResultFilter.FAILED -> "失败"
                                    }
                                )
                            },
                        )
                    }
                }
            }

            item {
                TextButton(onClick = viewModel::resetFilters) {
                    Text("重置筛选")
                }
            }

            if (uiState.filteredLogs.isEmpty() && !uiState.isLoading) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "暂无匹配日志")
                            Text(text = "可尝试修改筛选条件，或先执行读卡、写卡、锁卡操作。")
                        }
                    }
                }
            } else {
                items(uiState.filteredLogs) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLogClick(log.id) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "操作：${log.operationType}")
                            Text(text = "结果：${log.result}")
                            Text(text = "卡片：${log.cardUidMasked} / ${log.cardType}")
                            Text(text = "操作者：${log.operatorId}")
                            Text(text = "说明：${log.message}")
                            Text(text = "时间：${log.createdAt.toDisplayTime()}")
                        }
                    }
                }
            }
        }
    }
}

internal fun Long.toDisplayTime(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(this))
}
