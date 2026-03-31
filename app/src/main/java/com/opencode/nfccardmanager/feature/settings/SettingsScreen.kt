package com.opencode.nfccardmanager.feature.settings

import android.nfc.NfcAdapter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.opencode.nfccardmanager.core.database.AuditLogManager
import com.opencode.nfccardmanager.core.security.SecurityManager
import com.opencode.nfccardmanager.core.security.UserSession
import com.opencode.nfccardmanager.feature.read.ReadResultStore
import com.opencode.nfccardmanager.feature.support.SupportImpact
import com.opencode.nfccardmanager.feature.support.SupportSection
import com.opencode.nfccardmanager.feature.support.supportPageSummary
import com.opencode.nfccardmanager.ui.component.AppCard
import com.opencode.nfccardmanager.ui.component.AppTopBar
import com.opencode.nfccardmanager.ui.component.DangerActionButton
import com.opencode.nfccardmanager.ui.component.PrimaryActionButton
import com.opencode.nfccardmanager.ui.component.SectionTitle
import com.opencode.nfccardmanager.ui.component.SupportImpactBadge
import com.opencode.nfccardmanager.ui.component.SupportPageSummaryCard
import com.opencode.nfccardmanager.ui.component.StatusPill
import com.opencode.nfccardmanager.ui.component.StatusTone
import com.opencode.nfccardmanager.ui.component.appPagePadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSession: UserSession,
    onBack: (() -> Unit)? = null,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val nfcAdapter = remember(context) { NfcAdapter.getDefaultAdapter(context) }
    val nfcStatus = remember(nfcAdapter) {
        when {
            nfcAdapter == null -> "当前设备不支持 NFC"
            nfcAdapter.isEnabled -> "NFC 已开启"
            else -> "NFC 未开启"
        }
    }
    val pageSummary = remember {
        supportPageSummary(
            title = "设置与本地状态",
            impact = SupportImpact.LOCAL_CONVENIENCE,
            summary = "本页用于说明账号、设备、本地缓存和追责记录边界；普通清理不会直接改变卡片业务状态。",
            sections = listOf(
                SupportSection("账号与设备", "查看当前登录角色与 NFC 状态"),
                SupportSection("本地清理", "区分缓存便利与追责记录"),
            ),
        )
    }
    var cacheMessage by remember { mutableStateOf("可分别清理最近读卡缓存或本地审计日志。") }

    Scaffold(
        topBar = {
            AppTopBar(title = "我的 / 设置", onBack = onBack)
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.appPagePadding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SupportPageSummaryCard(summary = pageSummary)
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("账号信息")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "显示名称：${currentSession.displayName}")
                        Text(text = "用户名：${currentSession.username}")
                        Text(text = "当前角色：${SecurityManager.roleLabel(currentSession.role)}")
                    }
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("NFC 状态")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(
                            nfcStatus,
                            when (nfcStatus) {
                                "NFC 已开启" -> StatusTone.SUCCESS
                                "NFC 未开启" -> StatusTone.WARNING
                                else -> StatusTone.ERROR
                            }
                        )
                        Text(text = nfcStatus)
                        Text(text = "如需修改，请前往系统设置打开或关闭 NFC。")
                    }
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("应用信息")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "应用名称：NFC 卡片管理")
                        Text(text = "版本：1.0.0")
                        Text(text = "环境：本地演示版")
                    }
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("本地清理与说明")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = cacheMessage)
                        Text(text = "1. 当前登录态已持久化保存")
                        Text(text = "2. 角色切换会影响首页与页面权限")
                        Text(text = "3. 退出登录后需要重新登录")
                    }
                }
            }

            item {
                PrimaryActionButton(
                    text = "清理最近读卡缓存",
                    onClick = {
                        ReadResultStore.clear()
                        cacheMessage = "最近读卡缓存已清理；这只影响本地查看便利，不影响审计追责记录。"
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("影响范围")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SupportImpactBadge(impact = SupportImpact.LOCAL_CONVENIENCE)
                        Text(text = "清理最近读卡缓存只影响本地查看与回显便利。")
                        SupportImpactBadge(impact = SupportImpact.TRACEABILITY)
                        Text(text = "清空本地审计日志会影响后续追责与复盘，请谨慎执行。")
                    }
                }
            }

            item {
                DangerActionButton(
                    text = "清空本地审计日志",
                    onClick = {
                        AuditLogManager.clearAll()
                        cacheMessage = "本地审计日志已清空；最近读卡缓存未受影响。"
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("关于")
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "本应用用于企业内部 NFC 卡片读写、锁卡、解锁与审计管理演示。")
                        Text(text = "当前版本已实现读卡、NDEF 写卡、锁卡、解锁骨架、模板管理、日志审计。")
                    }
                }
            }

            item {
                PrimaryActionButton(
                    text = "退出登录",
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
