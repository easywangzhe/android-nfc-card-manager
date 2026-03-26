package com.opencode.nfccardmanager.core.security

import android.content.Context

private const val SESSION_PREFS = "user_session_prefs"
private const val KEY_USERNAME = "username"
private const val KEY_DISPLAY_NAME = "display_name"
private const val KEY_ROLE = "role"

class SessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)

    fun save(session: UserSession) {
        prefs.edit()
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_DISPLAY_NAME, session.displayName)
            .putString(KEY_ROLE, session.role.name)
            .apply()
    }

    fun load(): UserSession? {
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val displayName = prefs.getString(KEY_DISPLAY_NAME, null) ?: return null
        val roleName = prefs.getString(KEY_ROLE, null) ?: return null
        val role = runCatching { UserRole.valueOf(roleName) }.getOrNull() ?: return null
        return UserSession(
            username = username,
            displayName = displayName,
            role = role,
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
