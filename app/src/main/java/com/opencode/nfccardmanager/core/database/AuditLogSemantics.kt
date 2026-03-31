package com.opencode.nfccardmanager.core.database

enum class AuditOperatorRole(val storageValue: String, val label: String) {
    ADMIN("ADMIN", "管理员"),
    SUPERVISOR("SUPERVISOR", "主管"),
    OPERATOR("OPERATOR", "操作员"),
    AUDITOR("AUDITOR", "审计员"),
    LEGACY("LEGACY", "历史记录"),
}

enum class AuditFlowStage(val storageValue: String, val label: String) {
    REQUESTED("REQUESTED", "已发起"),
    EXECUTING("EXECUTING", "执行中"),
    COMPLETED("COMPLETED", "执行完成"),
    FAILED("FAILED", "执行失败"),
    UNMARKED("UNMARKED", "未标记"),
}

enum class AuditAuthenticity(val storageValue: String, val label: String) {
    VERIFIED("VERIFIED", "真实设备结果"),
    DEMO_ONLY("DEMO_ONLY", "Demo 流程"),
    ESTIMATED("ESTIMATED", "推断结果"),
    PENDING("PENDING", "待补充"),
}

enum class AuditImpactScope(val storageValue: String, val label: String) {
    SAFETY("SAFETY", "安全性"),
    TRACEABILITY("TRACEABILITY", "可追责性"),
    LOCAL_CONVENIENCE("LOCAL_CONVENIENCE", "本地便利性"),
    PENDING("PENDING", "待补充"),
}

data class AuditLogMetadata(
    val operatorRole: AuditOperatorRole?,
    val flowStage: AuditFlowStage?,
    val authenticity: AuditAuthenticity?,
    val impactScope: AuditImpactScope?,
) {
    fun resolved(): AuditLogResolvedMetadata {
        return AuditLogResolvedMetadata(
            operatorRole = operatorRole ?: AuditOperatorRole.LEGACY,
            flowStage = flowStage ?: AuditFlowStage.UNMARKED,
            authenticity = authenticity ?: AuditAuthenticity.PENDING,
            impactScope = impactScope ?: AuditImpactScope.PENDING,
        )
    }
}

data class AuditLogResolvedMetadata(
    val operatorRole: AuditOperatorRole,
    val flowStage: AuditFlowStage,
    val authenticity: AuditAuthenticity,
    val impactScope: AuditImpactScope,
)

internal fun parseAuditOperatorRole(value: String?): AuditOperatorRole? {
    return AuditOperatorRole.entries.firstOrNull { it.storageValue == value }
}

internal fun parseAuditFlowStage(value: String?): AuditFlowStage? {
    return AuditFlowStage.entries.firstOrNull { it.storageValue == value }
}

internal fun parseAuditAuthenticity(value: String?): AuditAuthenticity? {
    return AuditAuthenticity.entries.firstOrNull { it.storageValue == value }
}

internal fun parseAuditImpactScope(value: String?): AuditImpactScope? {
    return AuditImpactScope.entries.firstOrNull { it.storageValue == value }
}
