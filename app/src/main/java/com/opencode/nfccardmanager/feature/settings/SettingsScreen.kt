package com.opencode.nfccardmanager.feature.settings

import android.nfc.NfcAdapter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
    var cacheMessage by remember { mutableStateOf("可清理本地审计日志与最近读卡缓存") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("我的 / 设置") },
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "账号信息", style = MaterialTheme.typography.titleLarge)
                        Text(text = "显示名称：${currentSession.displayName}")
                        Text(text = "用户名：${currentSession.username}")
                        Text(text = "当前角色：${SecurityManager.roleLabel(currentSession.role)}")
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "NFC 状态", style = MaterialTheme.typography.titleLarge)
                        Text(text = nfcStatus)
                        Text(text = "如需修改，请前往系统设置打开或关闭 NFC。")
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "应用信息", style = MaterialTheme.typography.titleLarge)
                        Text(text = "应用名称：NFC 卡片管理")
                        Text(text = "版本：1.0.0")
                        Text(text = "环境：本地演示版")
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "缓存与说明", style = MaterialTheme.typography.titleLarge)
                        Text(text = cacheMessage)
                        Text(text = "1. 当前登录态已持久化保存")
                        Text(text = "2. 角色切换会影响首页与页面权限")
                        Text(text = "3. 退出登录后需要重新登录")
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        AuditLogManager.clearAll()
                        ReadResultStore.clear()
                        cacheMessage = "本地审计日志与最近读卡缓存已清理"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("清理缓存")
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "关于", style = MaterialTheme.typography.titleLarge)
                        Text(text = "本应用用于企业内部 NFC 卡片读写、锁卡、解锁与审计管理演示。")
                        Text(text = "当前版本已实现读卡、NDEF 写卡、锁卡、解锁骨架、模板管理、日志审计。")
                    }
                }
            }

            item {
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("退出登录")
                }
            }
        }
    }
}
