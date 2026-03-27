package com.opencode.nfccardmanager.feature.audit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencode.nfccardmanager.ui.component.AppCard
import com.opencode.nfccardmanager.ui.component.AppTopBar
import com.opencode.nfccardmanager.ui.component.KeyValueRow
import com.opencode.nfccardmanager.ui.component.SectionTitle
import com.opencode.nfccardmanager.ui.component.StatusPill
import com.opencode.nfccardmanager.ui.component.StatusTone
import com.opencode.nfccardmanager.ui.component.appPagePadding
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
            AppTopBar(title = "日志审计", onBack = onBack)
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.appPagePadding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("日志总览")
                    Text(
                        text = if (uiState.isLoading) "日志加载中..." else "共 ${uiState.filteredLogs.size} / ${uiState.logs.size} 条本地审计日志",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            item {
                SectionTitle("筛选")
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
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                            Text(text = "暂无匹配日志")
                            Text(text = "可尝试修改筛选条件，或先执行读卡、写卡、锁卡操作。")
                    }
                }
            } else {
                items(uiState.filteredLogs) { log ->
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLogClick(log.id) }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill(
                                text = log.result,
                                tone = if (log.result == "SUCCESS") StatusTone.SUCCESS else StatusTone.ERROR,
                            )
                            KeyValueRow("操作", log.operationType)
                            KeyValueRow("卡片", "${log.cardUidMasked} / ${log.cardType}")
                            KeyValueRow("操作者", log.operatorId)
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
