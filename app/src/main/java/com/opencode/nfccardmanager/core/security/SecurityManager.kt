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

    fun canViewAudit(role: UserRole): Boolean = true

    fun canViewAuditDetail(role: UserRole): Boolean = true

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
