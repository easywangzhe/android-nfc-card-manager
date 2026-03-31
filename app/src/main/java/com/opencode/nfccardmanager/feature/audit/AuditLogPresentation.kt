package com.opencode.nfccardmanager.feature.audit

import com.opencode.nfccardmanager.core.database.AuditAuthenticity
import com.opencode.nfccardmanager.core.database.AuditFlowStage
import com.opencode.nfccardmanager.core.database.AuditImpactScope
import com.opencode.nfccardmanager.core.database.AuditLogRecord
import com.opencode.nfccardmanager.core.database.AuditOperatorRole
import com.opencode.nfccardmanager.feature.support.SupportImpact
import com.opencode.nfccardmanager.feature.support.SupportPageSummary
import com.opencode.nfccardmanager.feature.support.SupportSection
import com.opencode.nfccardmanager.feature.support.supportPageSummary
import com.opencode.nfccardmanager.ui.component.StatusTone
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AuditLogListItemPresentation(
    val id: Long,
    val operationType: String,
    val result: String,
    val operatorSummary: String,
    val roleLabel: String,
    val stageLabel: String,
    val authenticityLabel: String,
    val impactLabel: String,
    val message: String,
    val cardSummary: String,
    val timestampLabel: String,
    val resultTone: StatusTone,
)

data class AuditLogDetailPresentation(
    val title: String,
    val whoSummary: String,
    val stageLabel: String,
    val authenticityLabel: String,
    val impactLabel: String,
    val whatHappened: String,
    val message: String,
    val resultLabel: String,
    val cardSummary: String,
    val timestampLabel: String,
    val resultTone: StatusTone,
)

fun buildAuditOverviewSummary(): SupportPageSummary {
    return supportPageSummary(
        title = "日志总览",
        impact = SupportImpact.TRACEABILITY,
        summary = "本页用于查看本地追责记录与执行边界，不会直接改变卡片业务状态。",
        sections = listOf(
            SupportSection("筛选", "按操作、结果和关键词定位记录"),
            SupportSection("详情", "查看角色、阶段、真实性和影响范围"),
        ),
    )
}

fun buildAuditLogListItem(record: AuditLogRecord): AuditLogListItemPresentation {
    return AuditLogListItemPresentation(
        id = record.id,
        operationType = record.operationType,
        result = record.result,
        operatorSummary = record.operatorId,
        roleLabel = record.operatorRole.toDisplayLabel(),
        stageLabel = record.flowStage.label,
        authenticityLabel = record.authenticity.label,
        impactLabel = record.impactScope.label,
        message = record.message,
        cardSummary = "${record.cardUidMasked} / ${record.cardType}",
        timestampLabel = record.createdAt.toDisplayTime(),
        resultTone = if (record.result == "SUCCESS") StatusTone.SUCCESS else StatusTone.ERROR,
    )
}

fun buildAuditLogDetailPresentation(record: AuditLogRecord): AuditLogDetailPresentation {
    return AuditLogDetailPresentation(
        title = "审计日志 #${record.id}",
        whoSummary = "${record.operatorId}（${record.operatorRole.toDisplayLabel()}）",
        stageLabel = record.flowStage.label,
        authenticityLabel = record.authenticity.label,
        impactLabel = record.impactScope.label,
        whatHappened = "${record.operationType}：${record.result}",
        message = record.message,
        resultLabel = record.result,
        cardSummary = "${record.cardUidMasked} / ${record.cardType}",
        timestampLabel = record.createdAt.toDisplayTime(),
        resultTone = if (record.result == "SUCCESS") StatusTone.SUCCESS else StatusTone.ERROR,
    )
}

fun mapUserRoleToAuditRole(roleName: String): AuditOperatorRole {
    return when (roleName) {
        "ADMIN" -> AuditOperatorRole.ADMIN
        "SUPERVISOR" -> AuditOperatorRole.SUPERVISOR
        "OPERATOR" -> AuditOperatorRole.OPERATOR
        "AUDITOR" -> AuditOperatorRole.AUDITOR
        else -> AuditOperatorRole.LEGACY
    }
}

fun writeAuthenticity(success: Boolean, verified: Boolean): AuditAuthenticity {
    return when {
        verified -> AuditAuthenticity.VERIFIED
        success -> AuditAuthenticity.ESTIMATED
        else -> AuditAuthenticity.VERIFIED
    }
}

fun lockAuthenticity(success: Boolean, verified: Boolean): AuditAuthenticity {
    return when {
        verified -> AuditAuthenticity.VERIFIED
        success -> AuditAuthenticity.ESTIMATED
        else -> AuditAuthenticity.VERIFIED
    }
}

fun unlockAuthenticity(success: Boolean): AuditAuthenticity {
    return if (success) AuditAuthenticity.DEMO_ONLY else AuditAuthenticity.VERIFIED
}

fun formatAuthenticity(success: Boolean): AuditAuthenticity {
    return if (success) AuditAuthenticity.VERIFIED else AuditAuthenticity.VERIFIED
}

fun stageForSuccess(success: Boolean): AuditFlowStage {
    return if (success) AuditFlowStage.COMPLETED else AuditFlowStage.FAILED
}

fun defaultPendingAuditMetadata() = AuditMetadataDefaults(
    operatorRole = AuditOperatorRole.LEGACY,
    flowStage = AuditFlowStage.UNMARKED,
    authenticity = AuditAuthenticity.PENDING,
    impactScope = AuditImpactScope.TRACEABILITY,
)

data class AuditMetadataDefaults(
    val operatorRole: AuditOperatorRole,
    val flowStage: AuditFlowStage,
    val authenticity: AuditAuthenticity,
    val impactScope: AuditImpactScope,
)

internal fun Long.toDisplayTime(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(this))
}

private fun AuditOperatorRole.toDisplayLabel(): String {
    return when (this) {
        AuditOperatorRole.ADMIN -> "系统管理员"
        AuditOperatorRole.SUPERVISOR -> "主管/审核人"
        AuditOperatorRole.OPERATOR -> "普通操作员"
        AuditOperatorRole.AUDITOR -> "审计员"
        AuditOperatorRole.LEGACY -> label
    }
}
