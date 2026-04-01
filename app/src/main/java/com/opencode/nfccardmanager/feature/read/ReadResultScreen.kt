package com.opencode.nfccardmanager.feature.read

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencode.nfccardmanager.core.nfc.model.NfcFlowStage
import com.opencode.nfccardmanager.core.nfc.model.buildReadNextStepGuidance
import com.opencode.nfccardmanager.core.nfc.model.presentation
import com.opencode.nfccardmanager.core.nfc.model.toCapabilityAuthenticity
import com.opencode.nfccardmanager.core.security.ProtectedAction
import com.opencode.nfccardmanager.ui.component.AppCard
import com.opencode.nfccardmanager.ui.component.AppTopBar
import com.opencode.nfccardmanager.ui.component.KeyValueRow
import com.opencode.nfccardmanager.ui.component.PrimaryActionButton
import com.opencode.nfccardmanager.ui.component.SecondaryActionButton
import com.opencode.nfccardmanager.ui.component.SectionTitle
import com.opencode.nfccardmanager.ui.component.StatusPill
import com.opencode.nfccardmanager.ui.component.StatusTone
import com.opencode.nfccardmanager.ui.component.appPagePadding
import com.opencode.nfccardmanager.ui.component.toStatusTone
import com.opencode.nfccardmanager.ui.test.AppTestTags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadResultScreen(
    uid: String,
    techType: String,
    summary: String,
    onBack: () -> Unit,
    onFormatCard: () -> Unit,
) {
    val latestResult by ReadResultStore.latestResult.collectAsStateWithLifecycle()
    val stagePresentation = NfcFlowStage.SUCCESS.presentation()
    val readGuidance = latestResult?.let(::buildReadNextStepGuidance)
    val readAuthenticity = ProtectedAction.READ.toCapabilityAuthenticity(latestResult?.capability).presentation()
    val writeAuthenticity = ProtectedAction.WRITE.toCapabilityAuthenticity(latestResult?.capability).presentation()
    val lockAuthenticity = ProtectedAction.LOCK.toCapabilityAuthenticity(latestResult?.capability).presentation()
    val unlockAuthenticity = ProtectedAction.UNLOCK.toCapabilityAuthenticity(latestResult?.capability).presentation()

    Scaffold(
        topBar = {
            AppTopBar(title = "读卡结果", onBack = onBack)
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.appPagePadding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AppTestTags.READ_RESULT_CARD),
                ) {
                    Text(text = "读卡结果", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = readGuidance?.conclusion ?: "等待展示完整读卡结果。",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusPill(
                            text = when (latestResult?.readStatus) {
                                "READ_SUCCESS" -> "读取成功"
                                "EMPTY_NDEF" -> "空标签"
                                "NON_NDEF" -> "非 NDEF"
                                "READ_ERROR" -> "读取异常"
                                else -> "待确认"
                            },
                            tone = when (latestResult?.readStatus) {
                                "READ_SUCCESS" -> StatusTone.SUCCESS
                                "READ_ERROR" -> StatusTone.ERROR
                                "NON_NDEF", "EMPTY_NDEF" -> StatusTone.WARNING
                                else -> StatusTone.INFO
                            }
                        )
                        StatusPill(
                            text = readAuthenticity.label,
                            tone = readAuthenticity.tone.toStatusTone(),
                            modifier = Modifier.testTag(AppTestTags.READ_AUTHENTICITY_BADGE),
                        )
                    }
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("共享状态与真实性")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyValueRow("共享阶段", stagePresentation.title)
                        Text(text = stagePresentation.detail)
                        KeyValueRow("读卡真实性", readAuthenticity.label)
                        Text(text = readAuthenticity.detail)
                    }
                }
            }

            item {
                SectionTitle("下一步操作")
            }

            item {
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AppTestTags.READ_RECOMMENDATION_CARD),
                ) {
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = readGuidance?.recommendedAction ?: "请先确认读卡结果后再继续操作。")
                        readGuidance?.let {
                            KeyValueRow("推荐 CTA", it.ctaLabel)
                            Text(text = "判断依据：${it.reasonSummary}")
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    PrimaryActionButton(
                        text = if (readGuidance?.ctaLabel == "先去格式化") "先去格式化" else "格式化卡",
                        onClick = onFormatCard,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(AppTestTags.READ_PRIMARY_CTA),
                    )
                    SecondaryActionButton(
                        text = if (readGuidance?.ctaLabel == "重新读卡") "重新读卡" else "返回重新读卡",
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(AppTestTags.READ_SECONDARY_CTA),
                    )
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("核心结果")
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    KeyValueRow("UID", uid)
                    KeyValueRow("卡类型", techType)
                    KeyValueRow("摘要", summary)
                    KeyValueRow("NDEF 标签", if (latestResult?.isNdefTag == true) "是" else "否")
                    KeyValueRow("NDEF 消息数", "${latestResult?.ndefMessageCount ?: 0}")
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("读卡判断")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        when (latestResult?.readStatus) {
                            null -> {
                                Text(text = "暂无完整读卡结果缓存")
                            }

                            "NON_NDEF" -> {
                                Text(text = "状态：非 NDEF 卡")
                                Text(text = latestResult?.readReason.orEmpty())
                            }

                            "READ_ERROR" -> {
                                Text(text = "状态：读取异常")
                                Text(text = latestResult?.readReason.orEmpty())
                            }

                            "EMPTY_NDEF" -> {
                                Text(text = "状态：空 NDEF 标签")
                                Text(text = latestResult?.readReason.orEmpty())
                            }

                            else -> {
                                Text(text = "状态：读取成功")
                                Text(text = latestResult?.readReason.orEmpty())
                            }
                        }
                    }
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("卡片能力")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KeyValueRow("可读", "${latestResult?.capability?.canRead ?: true}")
                        KeyValueRow("可写", "${latestResult?.capability?.canWrite ?: false}")
                        KeyValueRow("可锁", "${latestResult?.capability?.canLock ?: false}")
                        KeyValueRow("可解锁", "${latestResult?.capability?.canUnlock ?: false}")
                        StatusPill(text = "写卡：${writeAuthenticity.label}", tone = writeAuthenticity.tone.toStatusTone())
                        Text(text = writeAuthenticity.detail)
                        StatusPill(text = "锁卡：${lockAuthenticity.label}", tone = lockAuthenticity.tone.toStatusTone())
                        Text(text = lockAuthenticity.detail)
                        StatusPill(text = "解锁：${unlockAuthenticity.label}", tone = unlockAuthenticity.tone.toStatusTone())
                        Text(text = unlockAuthenticity.detail)
                    }
                }
            }

            item {
                SectionTitle("基础信息")
            }

            if (latestResult?.detailItems.isNullOrEmpty()) {
                item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "暂无更多基础信息")
                    }
                }
            } else {
                items(latestResult?.detailItems ?: emptyList()) { item ->
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        KeyValueRow(item.label, item.value)
                    }
                }
            }

            item {
                SectionTitle("NDEF 记录")
            }

            if (latestResult?.records.isNullOrEmpty()) {
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "未读取到 NDEF 记录")
                        Text(text = "可能原因：标签为空、不是 NDEF 标签，或当前标签未格式化为 NDEF。")
                    }
                }
            } else {
                items(latestResult?.records ?: emptyList()) { record ->
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "类型：${record.type}")
                        Text(text = "TNF：${record.tnf}")
                        Text(text = "内容预览：${record.payloadPreview}")
                    }
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("技术栈列表")
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        val techList = latestResult?.rawTechList.orEmpty()
                        if (techList.isEmpty()) {
                            Text(text = "暂无技术栈数据")
                        } else {
                            techList.forEach { tech ->
                                Text(text = tech)
                            }
                        }
                    }
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("调试信息")
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(text = latestResult?.debugMessage ?: "暂无调试信息")
                        Text(text = "读取状态：${latestResult?.readStatus ?: "UNKNOWN"}")
                    }
                }
            }
        }
    }
}
