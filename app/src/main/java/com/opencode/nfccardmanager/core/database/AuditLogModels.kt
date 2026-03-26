package com.opencode.nfccardmanager.core.database

data class AuditLogRecord(
    val id: Long = 0,
    val operationType: String,
    val operatorId: String,
    val cardUidMasked: String,
    val cardType: String,
    val result: String,
    val message: String,
    val createdAt: Long,
)
