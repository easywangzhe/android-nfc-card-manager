package com.opencode.nfccardmanager.core.security

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSession(
    val username: String,
    val displayName: String,
    val role: UserRole,
)

enum class UserRole {
    OPERATOR,
    SUPERVISOR,
    ADMIN,
    AUDITOR,
}

enum class ProtectedAction {
    READ,
    WRITE,
    FORMAT,
    LOCK,
    UNLOCK,
    TEMPLATE,
    AUDIT,
    AUDIT_DETAIL,
}

object SecurityManager {
    private val mockAccounts = listOf(
        UserSession(username = "operator", displayName = "一线操作员", role = UserRole.OPERATOR),
        UserSession(username = "supervisor", displayName = "审核主管", role = UserRole.SUPERVISOR),
        UserSession(username = "admin", displayName = "系统管理员", role = UserRole.ADMIN),
        UserSession(username = "auditor", displayName = "审计员", role = UserRole.AUDITOR),
    )
    private var sessionStore: SessionStore? = null
    private val _currentSession = MutableStateFlow<UserSession?>(null)
    val currentSession: StateFlow<UserSession?> = _currentSession.asStateFlow()

    private val _currentRole = MutableStateFlow(UserRole.ADMIN)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    fun init(context: Context) {
        if (sessionStore == null) {
            sessionStore = SessionStore(context)
            restoreSession()
        }
    }

    fun login(username: String, password: String): Result<UserSession> {
        val account = mockAccounts.firstOrNull { it.username == username.trim().lowercase() }
            ?: return Result.failure(IllegalArgumentException("账号不存在"))
        if (password != "123456") {
            return Result.failure(IllegalArgumentException("密码错误"))
        }
        _currentSession.value = account
        _currentRole.value = account.role
        sessionStore?.save(account)
        return Result.success(account)
    }

    fun logout() {
        _currentSession.value = null
        _currentRole.value = UserRole.ADMIN
        sessionStore?.clear()
    }

    fun isLoggedIn(): Boolean = _currentSession.value != null

    fun switchRole(role: UserRole) {
        val current = _currentSession.value ?: return
        _currentRole.value = role
        _currentSession.value = current.copy(role = role).also {
            sessionStore?.save(it)
        }
    }

    private fun restoreSession() {
        val stored = sessionStore?.load() ?: return
        _currentSession.value = stored
        _currentRole.value = stored.role
    }

    fun canRead(role: UserRole): Boolean = role != UserRole.AUDITOR

    fun canWrite(role: UserRole): Boolean = role != UserRole.AUDITOR

    fun canLock(role: UserRole): Boolean = role == UserRole.ADMIN || role == UserRole.SUPERVISOR

    fun canUnlock(role: UserRole): Boolean = role == UserRole.ADMIN || role == UserRole.SUPERVISOR

    fun canManageTemplate(role: UserRole): Boolean = role == UserRole.ADMIN

    fun canViewAudit(role: UserRole): Boolean = when (role) {
        UserRole.OPERATOR,
        UserRole.SUPERVISOR,
        UserRole.ADMIN,
        UserRole.AUDITOR,
        -> true
    }

    fun canViewAuditDetail(role: UserRole): Boolean = when (role) {
        UserRole.OPERATOR,
        UserRole.SUPERVISOR,
        UserRole.ADMIN,
        UserRole.AUDITOR,
        -> true
    }

    fun canAccess(role: UserRole, action: ProtectedAction): Boolean {
        return when (action) {
            ProtectedAction.READ -> canRead(role)
            ProtectedAction.WRITE -> canWrite(role)
            ProtectedAction.FORMAT -> canWrite(role)
            ProtectedAction.LOCK -> canLock(role)
            ProtectedAction.UNLOCK -> canUnlock(role)
            ProtectedAction.TEMPLATE -> canManageTemplate(role)
            ProtectedAction.AUDIT -> canViewAudit(role)
            ProtectedAction.AUDIT_DETAIL -> canViewAuditDetail(role)
        }
    }

    fun ensureAccess(role: UserRole, action: ProtectedAction): Result<Unit> {
        if (canAccess(role, action)) {
            return Result.success(Unit)
        }
        return Result.failure(IllegalStateException(accessDeniedMessage(role, action)))
    }

    fun accessDeniedMessage(role: UserRole, action: ProtectedAction): String {
        val roleLabel = roleLabel(role)
        val actionLabel = when (action) {
            ProtectedAction.READ -> "读卡"
            ProtectedAction.WRITE -> "写卡"
            ProtectedAction.FORMAT -> "格式化卡片"
            ProtectedAction.LOCK -> "锁卡"
            ProtectedAction.UNLOCK -> "解锁"
            ProtectedAction.TEMPLATE -> "管理模板"
            ProtectedAction.AUDIT -> "查看审计日志"
            ProtectedAction.AUDIT_DETAIL -> "查看日志详情"
        }
        return "当前角色 $roleLabel 无权执行${actionLabel}。"
    }

    fun roleLabel(role: UserRole): String = when (role) {
        UserRole.OPERATOR -> "普通操作员"
        UserRole.SUPERVISOR -> "主管/审核人"
        UserRole.ADMIN -> "系统管理员"
        UserRole.AUDITOR -> "审计员"
    }

    fun maskUid(uid: String): String {
        if (uid.length <= 4) return uid
        return uid.take(2) + "****" + uid.takeLast(2)
    }
}
