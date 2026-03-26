package com.opencode.nfccardmanager.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opencode.nfccardmanager.core.security.UserSession
import com.opencode.nfccardmanager.core.security.UserRole

private data class HomeAction(
    val title: String,
    val onClick: () -> Unit,
    val enabled: Boolean,
)

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
    canRead: Boolean,
    canWrite: Boolean,
    canLock: Boolean,
    canUnlock: Boolean,
    canManageTemplate: Boolean,
    canViewAudit: Boolean,
) {
    val actions = listOf(
        HomeAction("读卡", onReadClick, canRead),
        HomeAction("写卡", onWriteClick, canWrite),
        HomeAction("锁卡", onLockClick, canLock),
        HomeAction("解锁", onUnlockClick, canUnlock),
        HomeAction("模板管理", onTemplateClick, canManageTemplate),
        HomeAction("日志审计", onAuditClick, canViewAudit),
        HomeAction("我的/设置", onSettingsClick, true),
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NFC 卡片管理") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("退出")
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
                .padding(horizontal = 16.dp, vertical = 20.dp),
        ) {
            Text(
                text = "Android NFC 企业工具骨架工程",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "当前用户：${currentSession.displayName}（${currentSession.username}）",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
            Text(
                text = "当前角色：${roleLabel(currentRole)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 20.dp),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(UserRole.entries.toList()) { role ->
                    FilterChip(
                        selected = role == currentRole,
                        onClick = { onRoleChange(role) },
                        label = { Text(roleLabel(role)) },
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 20.dp),
            ) {
                gridItems(actions) { action ->
                    Button(
                        onClick = action.onClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = action.enabled,
                    ) {
                        Text(action.title)
                    }
                }
            }
        }
    }
}
