package com.opencode.nfccardmanager.core.database

import android.content.Context
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.TechType
import com.opencode.nfccardmanager.core.nfc.model.maskedUid

object AuditLogManager {
    private var repository: AuditLogRepository? = null

    fun init(context: Context) {
        if (repository == null) {
            repository = AuditLogRepository(context)
        }
    }

    fun save(
        operationType: String,
        cardUid: String,
        cardType: String,
        result: String,
        message: String,
        operatorId: String = "system",
    ) {
        repository?.save(
            AuditLogRecord(
                operationType = operationType,
                operatorId = operatorId,
                cardUidMasked = CardInfo(cardUid, TechType.UNKNOWN).maskedUid(),
                cardType = cardType,
                result = result,
                message = message,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    fun list(): List<AuditLogRecord> = repository?.list().orEmpty()

    fun findById(id: Long): AuditLogRecord? = repository?.findById(id)

    fun clearAll() {
        repository?.clearAll()
    }
}
