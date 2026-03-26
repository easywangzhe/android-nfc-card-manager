package com.opencode.nfccardmanager.core.database

import android.content.Context

class AuditLogRepository(context: Context) {
    private val dbHelper = AuditLogDbHelper(context.applicationContext)

    fun save(record: AuditLogRecord) {
        dbHelper.insert(record)
    }

    fun list(): List<AuditLogRecord> = dbHelper.queryAll()

    fun findById(id: Long): AuditLogRecord? = dbHelper.queryById(id)

    fun clearAll() {
        dbHelper.clearAll()
    }
}
