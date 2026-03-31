package com.opencode.nfccardmanager.core.nfc

enum class NfcOperationType {
    READ,
    WRITE,
    FORMAT,
    LOCK,
    UNLOCK,
}

data class NfcActiveSession(
    val token: String,
    val owner: String,
    val operation: NfcOperationType,
    val startedAtMillis: Long,
)

sealed interface NfcSessionStartResult {
    data class Granted(val session: NfcActiveSession) : NfcSessionStartResult

    data class Rejected(val activeSession: NfcActiveSession) : NfcSessionStartResult
}

object NfcSessionCoordinator {
    private val lock = Any()
    private var sessionCounter = 0L
    private var currentSession: NfcActiveSession? = null

    fun requestStart(
        owner: String,
        operation: NfcOperationType,
        nowMillis: Long = System.currentTimeMillis(),
    ): NfcSessionStartResult {
        synchronized(lock) {
            currentSession?.let { activeSession ->
                return NfcSessionStartResult.Rejected(activeSession)
            }

            sessionCounter += 1
            val session = NfcActiveSession(
                token = buildToken(operation = operation, sequence = sessionCounter),
                owner = owner,
                operation = operation,
                startedAtMillis = nowMillis,
            )
            currentSession = session
            return NfcSessionStartResult.Granted(session)
        }
    }

    fun release(token: String, owner: String): Boolean {
        synchronized(lock) {
            val activeSession = currentSession ?: return false
            if (activeSession.token != token || activeSession.owner != owner) {
                return false
            }

            currentSession = null
            return true
        }
    }

    fun isOwner(token: String, owner: String): Boolean {
        synchronized(lock) {
            val activeSession = currentSession ?: return false
            return activeSession.token == token && activeSession.owner == owner
        }
    }

    fun activeSession(): NfcActiveSession? {
        synchronized(lock) {
            return currentSession
        }
    }

    internal fun resetForTest() {
        synchronized(lock) {
            currentSession = null
            sessionCounter = 0L
        }
    }

    private fun buildToken(operation: NfcOperationType, sequence: Long): String {
        return "${operation.name.lowercase()}-$sequence"
    }
}
