package com.opencode.nfccardmanager.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opencode.nfccardmanager.core.security.UserSession
import com.opencode.nfccardmanager.core.security.UserRole
import com.opencode.nfccardmanager.ui.component.HomeSectionCard
import com.opencode.nfccardmanager.ui.component.AppCard
import com.opencode.nfccardmanager.ui.component.AppTopBar
import com.opencode.nfccardmanager.ui.component.SectionTitle
import com.opencode.nfccardmanager.ui.component.StatusPill
import com.opencode.nfccardmanager.ui.component.StatusTone
import com.opencode.nfccardmanager.ui.component.appPagePadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentSession: UserSession,
    currentRole: UserRole,
    roleLabel: (UserRole) -> String,
    onRoleChange: (UserRole) -> Unit,
    onLogout: () -> Unit,
    onReadClick: () -> Unit,
    onWriteClick: () -> Unit,
    onLockClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onTemplateClick: () -> Unit,
    onAuditClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val sections = buildHomeSections(currentRole)

    Scaffold(
        topBar = {
            AppTopBar(title = "NFC 卡片管理", actionText = "退出", onActionClick = onLogout)
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.appPagePadding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "NFC 卡片管理", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "当前用户：${currentSession.displayName}（${currentSession.username}）",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = "当前角色：${roleLabel(currentRole)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill("首页壳层", StatusTone.INFO)
                        StatusPill("会话已登录", StatusTone.SUCCESS)
                    }
                }
            }

            item {
                Column {
                    SectionTitle("角色切换")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(UserRole.entries.toList()) { role ->
                            FilterChip(
                                selected = role == currentRole,
                                onClick = { onRoleChange(role) },
                                label = { Text(roleLabel(role)) },
                            )
                        }
                    }
                }
            }

            items(sections) { section ->
                HomeSectionCard(
                    section = section,
                    onEntryClick = { entry ->
                        when (entry.destination) {
                            HomeEntryDestination.READ -> onReadClick()
                            HomeEntryDestination.WRITE -> onWriteClick()
                            HomeEntryDestination.LOCK -> onLockClick()
                            HomeEntryDestination.UNLOCK -> onUnlockClick()
                            HomeEntryDestination.TEMPLATE -> onTemplateClick()
                            HomeEntryDestination.AUDIT -> onAuditClick()
                            HomeEntryDestination.SETTINGS -> onSettingsClick()
                        }
                    },
                )
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle("操作提示")
                    Text(
                        text = "建议先完成读卡识别，再进入写卡或高风险操作；管理工具分组用于查看记录、模板和账号设置。",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
