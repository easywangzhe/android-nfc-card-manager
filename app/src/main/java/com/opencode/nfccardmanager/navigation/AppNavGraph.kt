package com.opencode.nfccardmanager.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencode.nfccardmanager.core.security.SecurityManager
import com.opencode.nfccardmanager.feature.auth.LoginScreen
import com.opencode.nfccardmanager.feature.audit.AuditLogDetailScreen
import com.opencode.nfccardmanager.feature.audit.AuditLogScreen
import com.opencode.nfccardmanager.feature.common.PermissionDeniedScreen
import com.opencode.nfccardmanager.feature.format.FormatCardScreen
import com.opencode.nfccardmanager.feature.home.HomeScreen
import com.opencode.nfccardmanager.feature.lock.LockRiskScreen
import com.opencode.nfccardmanager.feature.read.ReadResultScreen
import com.opencode.nfccardmanager.feature.scan.ScanMode
import com.opencode.nfccardmanager.feature.scan.ScanScreen
import com.opencode.nfccardmanager.feature.settings.SettingsScreen
import com.opencode.nfccardmanager.feature.template.TemplateManagementScreen
import com.opencode.nfccardmanager.feature.unlock.UnlockVerifyScreen
import com.opencode.nfccardmanager.feature.write.WriteEditorScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SCAN = "scan/{mode}"
    const val READ_RESULT = "read_result/{uid}/{techType}?summary={summary}"
    const val WRITE_EDITOR = "write_editor"
    const val FORMAT_CARD = "format_card"
    const val LOCK_RISK = "lock_risk"
    const val UNLOCK_VERIFY = "unlock_verify"
    const val TEMPLATE = "template"
    const val SETTINGS = "settings"
    const val AUDIT = "audit"
    const val AUDIT_DETAIL = "audit_detail/{logId}"

    fun scan(mode: ScanMode): String = "scan/${mode.name}"

    fun readResult(uid: String, techType: String, summary: String): String {
        return "read_result/$uid/$techType?summary=${Uri.encode(summary)}"
    }

    fun auditDetail(logId: Long): String = "audit_detail/$logId"
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val currentRole by SecurityManager.currentRole.collectAsStateWithLifecycle()
    val currentSession by SecurityManager.currentSession.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val bottomNavItems = listOf(
        BottomNavItem(Routes.HOME, "首页", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem(Routes.TEMPLATE, "模板", Icons.Filled.Widgets, Icons.Outlined.Widgets),
        BottomNavItem(Routes.AUDIT, "日志", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
        BottomNavItem(Routes.SETTINGS, "我的", Icons.Filled.Person, Icons.Outlined.Person),
    )
    val showBottomBar = currentSession != null && currentRoute in bottomNavItems.map { it.route }

    LaunchedEffect(currentSession) {
        if (currentSession == null) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (SecurityManager.isLoggedIn()) Routes.HOME else Routes.LOGIN,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                val session = currentSession
                if (session == null) {
                    PermissionDeniedScreen(
                        title = "未登录",
                        description = "请先登录后再访问首页。",
                        onBack = { navController.navigate(Routes.LOGIN) },
                    )
                    return@composable
                }
                HomeScreen(
                    currentSession = session,
                    currentRole = currentRole,
                    roleLabel = SecurityManager::roleLabel,
                    onRoleChange = SecurityManager::switchRole,
                    onLogout = {
                        SecurityManager.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onReadClick = { navController.navigate(Routes.scan(ScanMode.READ)) },
                    onWriteClick = { navController.navigate(Routes.WRITE_EDITOR) },
                    onLockClick = { navController.navigate(Routes.LOCK_RISK) },
                    onUnlockClick = { navController.navigate(Routes.UNLOCK_VERIFY) },
                    canRead = SecurityManager.canRead(currentRole),
                    canWrite = SecurityManager.canWrite(currentRole),
                    canLock = SecurityManager.canLock(currentRole),
                    canUnlock = SecurityManager.canUnlock(currentRole),
                )
            }
            composable(
                route = Routes.SCAN,
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode")
                    ?.let { ScanMode.valueOf(it) }
                    ?: ScanMode.READ

                val allowed = when (mode) {
                    ScanMode.READ -> SecurityManager.canRead(currentRole)
                    ScanMode.WRITE -> SecurityManager.canWrite(currentRole)
                    ScanMode.LOCK -> SecurityManager.canLock(currentRole)
                    ScanMode.UNLOCK -> SecurityManager.canUnlock(currentRole)
                }

                if (!allowed) {
                    PermissionDeniedScreen(
                        description = "当前角色 ${SecurityManager.roleLabel(currentRole)} 无权访问该操作页面。",
                        onBack = { navController.popBackStack() },
                    )
                } else {
                    ScanScreen(
                        mode = mode,
                        onBack = { navController.popBackStack() },
                        onReadResult = { uid, techType, summary ->
                            navController.navigate(Routes.readResult(uid, techType, summary))
                        },
                    )
                }
            }

            composable(
                route = Routes.READ_RESULT,
                arguments = listOf(
                    navArgument("uid") { type = NavType.StringType },
                    navArgument("techType") { type = NavType.StringType },
                    navArgument("summary") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { backStackEntry ->
                ReadResultScreen(
                    uid = backStackEntry.arguments?.getString("uid").orEmpty(),
                    techType = backStackEntry.arguments?.getString("techType").orEmpty(),
                    summary = backStackEntry.arguments?.getString("summary").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onFormatCard = { navController.navigate(Routes.FORMAT_CARD) },
                )
            }

            composable(Routes.FORMAT_CARD) {
                if (!SecurityManager.canWrite(currentRole)) {
                    PermissionDeniedScreen(
                        description = "当前角色 ${SecurityManager.roleLabel(currentRole)} 无权格式化卡片。",
                        onBack = { navController.popBackStack() },
                    )
                } else {
                    FormatCardScreen(
                        onBack = { navController.popBackStack() },
                        onGoWrite = { navController.navigate(Routes.WRITE_EDITOR) },
                    )
                }
            }

            composable(Routes.WRITE_EDITOR) {
                if (!SecurityManager.canWrite(currentRole)) {
                    PermissionDeniedScreen(
                        description = "当前角色 ${SecurityManager.roleLabel(currentRole)} 无权写卡。",
                        onBack = { navController.popBackStack() },
                    )
                } else {
                    WriteEditorScreen(onBack = { navController.popBackStack() })
                }
            }

            composable(Routes.LOCK_RISK) {
                if (!SecurityManager.canLock(currentRole)) {
                    PermissionDeniedScreen(
                        description = "当前角色 ${SecurityManager.roleLabel(currentRole)} 无权锁卡。",
                        onBack = { navController.popBackStack() },
                    )
                } else {
                    LockRiskScreen(onBack = { navController.popBackStack() })
                }
            }

            composable(Routes.UNLOCK_VERIFY) {
                if (!SecurityManager.canUnlock(currentRole)) {
                    PermissionDeniedScreen(
                        description = "当前角色 ${SecurityManager.roleLabel(currentRole)} 无权解锁。",
                        onBack = { navController.popBackStack() },
                    )
                } else {
                    UnlockVerifyScreen(onBack = { navController.popBackStack() })
                }
            }

            composable(Routes.TEMPLATE) {
                if (!SecurityManager.canManageTemplate(currentRole)) {
                    PermissionDeniedScreen(
                        description = "当前角色 ${SecurityManager.roleLabel(currentRole)} 无权管理模板。",
                        onBack = { navController.popBackStack() },
                    )
                } else {
                    TemplateManagementScreen()
                }
            }

            composable(Routes.SETTINGS) {
                val session = currentSession
                if (session == null) {
                    PermissionDeniedScreen(
                        title = "未登录",
                        description = "请先登录后再访问设置页。",
                        onBack = { navController.navigate(Routes.LOGIN) },
                    )
                } else {
                    SettingsScreen(
                        currentSession = session,
                        onBack = null,
                        onLogout = {
                            SecurityManager.logout()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                    )
                }
            }

            composable(Routes.AUDIT) {
                if (!SecurityManager.canViewAudit(currentRole)) {
                    PermissionDeniedScreen(
                        description = "当前角色 ${SecurityManager.roleLabel(currentRole)} 无权查看审计日志。",
                        onBack = { navController.popBackStack() },
                    )
                } else {
                    AuditLogScreen(
                        onBack = null,
                        onLogClick = { logId -> navController.navigate(Routes.auditDetail(logId)) },
                    )
                }
            }

            composable(
                route = Routes.AUDIT_DETAIL,
                arguments = listOf(navArgument("logId") { type = NavType.LongType }),
            ) { backStackEntry ->
                if (!SecurityManager.canViewAuditDetail(currentRole)) {
                    PermissionDeniedScreen(
                        description = "当前角色 ${SecurityManager.roleLabel(currentRole)} 无权查看日志详情。",
                        onBack = { navController.popBackStack() },
                    )
                } else {
                    AuditLogDetailScreen(
                        logId = backStackEntry.arguments?.getLong("logId") ?: 0L,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
