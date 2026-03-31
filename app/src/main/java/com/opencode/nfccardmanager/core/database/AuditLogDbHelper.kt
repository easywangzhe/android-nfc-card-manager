package com.opencode.nfccardmanager.core.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_NAME = "audit_logs.db"
private const val DB_VERSION = 2
private const val TABLE_AUDIT = "audit_logs"

private const val COLUMN_OPERATOR_ROLE = "operator_role"
private const val COLUMN_FLOW_STAGE = "flow_stage"
private const val COLUMN_AUTHENTICITY = "authenticity"
private const val COLUMN_IMPACT_SCOPE = "impact_scope"

class AuditLogDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_AUDIT (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                operation_type TEXT NOT NULL,
                operator_id TEXT NOT NULL,
                $COLUMN_OPERATOR_ROLE TEXT NOT NULL DEFAULT '${AuditOperatorRole.LEGACY.storageValue}',
                card_uid_masked TEXT NOT NULL,
                card_type TEXT NOT NULL,
                $COLUMN_FLOW_STAGE TEXT NOT NULL DEFAULT '${AuditFlowStage.UNMARKED.storageValue}',
                result TEXT NOT NULL,
                $COLUMN_AUTHENTICITY TEXT NOT NULL DEFAULT '${AuditAuthenticity.PENDING.storageValue}',
                $COLUMN_IMPACT_SCOPE TEXT NOT NULL DEFAULT '${AuditImpactScope.PENDING.storageValue}',
                message TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE $TABLE_AUDIT ADD COLUMN $COLUMN_OPERATOR_ROLE TEXT NOT NULL DEFAULT '${AuditOperatorRole.LEGACY.storageValue}'"
            )
            db.execSQL(
                "ALTER TABLE $TABLE_AUDIT ADD COLUMN $COLUMN_FLOW_STAGE TEXT NOT NULL DEFAULT '${AuditFlowStage.UNMARKED.storageValue}'"
            )
            db.execSQL(
                "ALTER TABLE $TABLE_AUDIT ADD COLUMN $COLUMN_AUTHENTICITY TEXT NOT NULL DEFAULT '${AuditAuthenticity.PENDING.storageValue}'"
            )
            db.execSQL(
                "ALTER TABLE $TABLE_AUDIT ADD COLUMN $COLUMN_IMPACT_SCOPE TEXT NOT NULL DEFAULT '${AuditImpactScope.PENDING.storageValue}'"
            )
        }
    }

    fun insert(record: AuditLogRecord) {
        writableDatabase.insert(
            TABLE_AUDIT,
            null,
            ContentValues().apply {
                put("operation_type", record.operationType)
                put("operator_id", record.operatorId)
                put(COLUMN_OPERATOR_ROLE, record.operatorRole.storageValue)
                put("card_uid_masked", record.cardUidMasked)
                put("card_type", record.cardType)
                put(COLUMN_FLOW_STAGE, record.flowStage.storageValue)
                put("result", record.result)
                put(COLUMN_AUTHENTICITY, record.authenticity.storageValue)
                put(COLUMN_IMPACT_SCOPE, record.impactScope.storageValue)
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
                            operatorRole = parseAuditOperatorRole(
                                it.getString(it.getColumnIndexOrThrow(COLUMN_OPERATOR_ROLE))
                            ) ?: AuditOperatorRole.LEGACY,
                            cardUidMasked = it.getString(it.getColumnIndexOrThrow("card_uid_masked")),
                            cardType = it.getString(it.getColumnIndexOrThrow("card_type")),
                            flowStage = parseAuditFlowStage(
                                it.getString(it.getColumnIndexOrThrow(COLUMN_FLOW_STAGE))
                            ) ?: AuditFlowStage.UNMARKED,
                            result = it.getString(it.getColumnIndexOrThrow("result")),
                            authenticity = parseAuditAuthenticity(
                                it.getString(it.getColumnIndexOrThrow(COLUMN_AUTHENTICITY))
                            ) ?: AuditAuthenticity.PENDING,
                            impactScope = parseAuditImpactScope(
                                it.getString(it.getColumnIndexOrThrow(COLUMN_IMPACT_SCOPE))
                            ) ?: AuditImpactScope.PENDING,
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
                operatorRole = parseAuditOperatorRole(
                    it.getString(it.getColumnIndexOrThrow(COLUMN_OPERATOR_ROLE))
                ) ?: AuditOperatorRole.LEGACY,
                cardUidMasked = it.getString(it.getColumnIndexOrThrow("card_uid_masked")),
                cardType = it.getString(it.getColumnIndexOrThrow("card_type")),
                flowStage = parseAuditFlowStage(
                    it.getString(it.getColumnIndexOrThrow(COLUMN_FLOW_STAGE))
                ) ?: AuditFlowStage.UNMARKED,
                result = it.getString(it.getColumnIndexOrThrow("result")),
                authenticity = parseAuditAuthenticity(
                    it.getString(it.getColumnIndexOrThrow(COLUMN_AUTHENTICITY))
                ) ?: AuditAuthenticity.PENDING,
                impactScope = parseAuditImpactScope(
                    it.getString(it.getColumnIndexOrThrow(COLUMN_IMPACT_SCOPE))
                ) ?: AuditImpactScope.PENDING,
                message = it.getString(it.getColumnIndexOrThrow("message")),
                createdAt = it.getLong(it.getColumnIndexOrThrow("created_at")),
            )
        }
    }

    fun clearAll() {
        writableDatabase.delete(TABLE_AUDIT, null, null)
    }
}
