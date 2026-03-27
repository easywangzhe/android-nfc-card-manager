package com.opencode.nfccardmanager.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import com.opencode.nfccardmanager.ui.component.AppCard
import com.opencode.nfccardmanager.ui.component.AppTopBar
import com.opencode.nfccardmanager.ui.component.PrimaryActionButton
import com.opencode.nfccardmanager.ui.component.SectionTitle
import com.opencode.nfccardmanager.ui.component.StatusPill
import com.opencode.nfccardmanager.ui.component.StatusTone
import com.opencode.nfccardmanager.ui.component.appPagePadding

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
    canRead: Boolean,
    canWrite: Boolean,
    canLock: Boolean,
    canUnlock: Boolean,
) {
    val actions = listOf(
        HomeAction("读卡", onReadClick, canRead),
        HomeAction("写卡", onWriteClick, canWrite),
        HomeAction("锁卡", onLockClick, canLock),
        HomeAction("解锁", onUnlockClick, canUnlock),
    )

    Scaffold(
        topBar = {
            AppTopBar(title = "NFC 卡片管理", actionText = "退出", onActionClick = onLogout)
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.appPagePadding(paddingValues),
        ) {
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
                    StatusPill("NFC 工具", StatusTone.INFO)
                    StatusPill("会话已登录", StatusTone.SUCCESS)
                }
            }

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

            SectionTitle("快捷操作")
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 20.dp),
            ) {
                gridItems(actions) { action ->
                    PrimaryActionButton(
                        text = action.title,
                        onClick = action.onClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = action.enabled,
                    )
                }
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle("操作提示")
                Text(
                    text = "建议先读卡识别卡片能力，再执行写卡、锁卡或解锁等后续操作。",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
