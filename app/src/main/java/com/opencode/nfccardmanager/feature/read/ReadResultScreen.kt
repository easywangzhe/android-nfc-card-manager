package com.opencode.nfccardmanager.feature.read

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadResultScreen(
    uid: String,
    techType: String,
    summary: String,
    onBack: () -> Unit,
) {
    val latestResult by ReadResultStore.latestResult.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("读卡结果") },
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
            item {
                Text(
                    text = "读卡结果",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "UID：$uid")
                        Text(text = "卡类型：$techType")
                        Text(text = "摘要：$summary")
                        Text(text = "NDEF 消息数：${latestResult?.ndefMessageCount ?: 0}")
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "卡片能力")
                        Text(text = "可读：${latestResult?.capability?.canRead ?: true}")
                        Text(text = "可写：${latestResult?.capability?.canWrite ?: false}")
                        Text(text = "可锁：${latestResult?.capability?.canLock ?: false}")
                        Text(text = "可解锁：${latestResult?.capability?.canUnlock ?: false}")
                    }
                }
            }

            item {
                Text(
                    text = "NDEF 记录",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            if (latestResult?.records.isNullOrEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "未读取到 NDEF 记录")
                            Text(text = "可能原因：标签为空、不是 NDEF 标签，或当前标签未格式化为 NDEF。")
                        }
                    }
                }
            } else {
                items(latestResult?.records ?: emptyList()) { record ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "类型：${record.type}")
                            Text(text = "TNF：${record.tnf}")
                            Text(text = "内容预览：${record.payloadPreview}")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "技术栈列表")
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
        }
    }
}
