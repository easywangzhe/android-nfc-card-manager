package com.opencode.nfccardmanager.core.database

data class AuditLogRecord(
    val id: Long = 0,
    val operationType: String,
    val operatorId: String,
    val operatorRole: AuditOperatorRole = AuditOperatorRole.LEGACY,
    val cardUidMasked: String,
    val cardType: String,
    val flowStage: AuditFlowStage = AuditFlowStage.UNMARKED,
    val result: String,
    val authenticity: AuditAuthenticity = AuditAuthenticity.PENDING,
    val impactScope: AuditImpactScope = AuditImpactScope.PENDING,
    val message: String,
    val createdAt: Long,
)
