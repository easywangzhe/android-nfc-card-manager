package com.opencode.nfccardmanager.feature.audit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencode.nfccardmanager.ui.component.AppCard
import com.opencode.nfccardmanager.ui.component.AppTopBar
import com.opencode.nfccardmanager.ui.component.KeyValueRow
import com.opencode.nfccardmanager.ui.component.SectionTitle
import com.opencode.nfccardmanager.ui.component.SupportImpactBadge
import com.opencode.nfccardmanager.ui.component.StatusPill
import com.opencode.nfccardmanager.ui.component.appPagePadding
import com.opencode.nfccardmanager.ui.test.AppTestTags

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
            AppTopBar(title = "日志详情", onBack = onBack)
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.appPagePadding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val detail = uiState.presentation
            if (detail == null) {
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                            Text(text = if (uiState.isLoading) "日志加载中..." else "未找到对应日志")
                    }
                }
            } else {
                item {
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AppTestTags.AUDIT_DETAIL_RESULT_CARD),
                    ) {
                        SectionTitle(detail.title)
                        StatusPill(
                            text = detail.resultLabel,
                            tone = detail.resultTone,
                        )
                    }
                }
                item {
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AppTestTags.AUDIT_DETAIL_WHO_SECTION),
                    ) {
                        SectionTitle("谁执行了什么")
                        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            KeyValueRow("执行人", detail.whoSummary)
                            KeyValueRow("发生了什么", detail.whatHappened)
                            KeyValueRow("卡片", detail.cardSummary)
                            KeyValueRow("时间", detail.timestampLabel)
                        }
                    }
                }
                item {
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AppTestTags.AUDIT_DETAIL_SEMANTICS_SECTION),
                    ) {
                        SectionTitle("结果来源与真实性")
                        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            KeyValueRow("当前阶段", detail.stageLabel)
                            KeyValueRow("真实性", detail.authenticityLabel)
                            Text(text = "该真实性标签用于说明记录来源边界，不代表卡片状态已再次变化。")
                        }
                    }
                }
                item {
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AppTestTags.AUDIT_DETAIL_IMPACT_SECTION),
                    ) {
                        SectionTitle("影响范围")
                        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SupportImpactBadge(impact = com.opencode.nfccardmanager.feature.support.SupportImpact.TRACEABILITY)
                            Text(text = detail.impactLabel)
                        }
                    }
                }
                item {
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AppTestTags.AUDIT_DETAIL_MESSAGE_SECTION),
                    ) {
                        SectionTitle("说明与后续参考")
                        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = detail.message)
                        }
                    }
                }
            }
        }
    }
}
