package com.opencode.nfccardmanager.core.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_NAME = "audit_logs.db"
private const val DB_VERSION = 1
private const val TABLE_AUDIT = "audit_logs"

class AuditLogDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_AUDIT (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                operation_type TEXT NOT NULL,
                operator_id TEXT NOT NULL,
                card_uid_masked TEXT NOT NULL,
                card_type TEXT NOT NULL,
                result TEXT NOT NULL,
                message TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun insert(record: AuditLogRecord) {
        writableDatabase.insert(
            TABLE_AUDIT,
            null,
            ContentValues().apply {
                put("operation_type", record.operationType)
                put("operator_id", record.operatorId)
                put("card_uid_masked", record.cardUidMasked)
                put("card_type", record.cardType)
                put("result", record.result)
                put("message", record.message)
                put("created_at", record.createdAt)
            },
        )
    }

    fun queryAll(): List<AuditLogRecord> {
        val cursor = readableDatabase.query(
            TABLE_AUDIT,
            null,
            null,
            null,
            null,
            null,
            "created_at DESC",
        )

        return buildList {
            cursor.use {
                while (it.moveToNext()) {
                    add(
                        AuditLogRecord(
                            id = it.getLong(it.getColumnIndexOrThrow("id")),
                            operationType = it.getString(it.getColumnIndexOrThrow("operation_type")),
                            operatorId = it.getString(it.getColumnIndexOrThrow("operator_id")),
                            cardUidMasked = it.getString(it.getColumnIndexOrThrow("card_uid_masked")),
                            cardType = it.getString(it.getColumnIndexOrThrow("card_type")),
                            result = it.getString(it.getColumnIndexOrThrow("result")),
                            message = it.getString(it.getColumnIndexOrThrow("message")),
                            createdAt = it.getLong(it.getColumnIndexOrThrow("created_at")),
                        )
                    )
                }
            }
        }
    }

    fun queryById(id: Long): AuditLogRecord? {
        val cursor = readableDatabase.query(
            TABLE_AUDIT,
            null,
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
            "1",
        )

        return cursor.use {
            if (!it.moveToFirst()) return null
            AuditLogRecord(
                id = it.getLong(it.getColumnIndexOrThrow("id")),
                operationType = it.getString(it.getColumnIndexOrThrow("operation_type")),
                operatorId = it.getString(it.getColumnIndexOrThrow("operator_id")),
                cardUidMasked = it.getString(it.getColumnIndexOrThrow("card_uid_masked")),
                cardType = it.getString(it.getColumnIndexOrThrow("card_type")),
                result = it.getString(it.getColumnIndexOrThrow("result")),
                message = it.getString(it.getColumnIndexOrThrow("message")),
                createdAt = it.getLong(it.getColumnIndexOrThrow("created_at")),
            )
        }
    }

    fun clearAll() {
        writableDatabase.delete(TABLE_AUDIT, null, null)
    }
}
