package com.opencode.nfccardmanager.feature.format

import androidx.lifecycle.ViewModel
import com.opencode.nfccardmanager.core.database.AuditFlowStage
import com.opencode.nfccardmanager.core.database.AuditImpactScope
import com.opencode.nfccardmanager.core.database.AuditLogManager
import com.opencode.nfccardmanager.core.nfc.model.FormatCardResult
import com.opencode.nfccardmanager.core.nfc.model.buildFormatNextStepGuidance
import com.opencode.nfccardmanager.core.security.SecurityManager
import com.opencode.nfccardmanager.feature.audit.formatAuthenticity
import com.opencode.nfccardmanager.feature.audit.mapUserRoleToAuditRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FormatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FormatUiState())
    val uiState: StateFlow<FormatUiState> = _uiState.asStateFlow()

    fun start() {
        _uiState.update {
            it.copy(
                stage = FormatStage.SCANNING,
                message = "等待贴卡，检测到支持的卡片后将尝试格式化为 NDEF。",
                result = null,
                resultGuidance = null,
            )
        }
    }

    fun onFormatResult(result: FormatCardResult) {
        val guidance = buildFormatNextStepGuidance(result)
        AuditLogManager.save(
            operationType = "FORMAT",
            cardUid = result.cardInfo.uid,
            cardType = result.cardInfo.techType.name,
            result = if (result.success) "SUCCESS" else "FAILED",
            message = "${result.message}；${result.reason}",
            operatorId = SecurityManager.currentSession.value?.username ?: "system",
            operatorRole = mapUserRoleToAuditRole(SecurityManager.currentRole.value.name),
            flowStage = if (result.success) AuditFlowStage.COMPLETED else AuditFlowStage.FAILED,
            authenticity = formatAuthenticity(result.success),
            impactScope = AuditImpactScope.TRACEABILITY,
        )
        _uiState.update {
            it.copy(
                stage = if (result.success) FormatStage.SUCCESS else FormatStage.ERROR,
                message = result.message,
                result = result,
                resultGuidance = guidance,
            )
        }
    }

    fun onError(message: String) {
        AuditLogManager.save(
            operationType = "FORMAT",
            cardUid = "UNKNOWN",
            cardType = "UNKNOWN",
            result = "FAILED",
            message = message,
            operatorId = SecurityManager.currentSession.value?.username ?: "system",
            operatorRole = mapUserRoleToAuditRole(SecurityManager.currentRole.value.name),
            flowStage = AuditFlowStage.FAILED,
            authenticity = com.opencode.nfccardmanager.core.database.AuditAuthenticity.PENDING,
            impactScope = AuditImpactScope.TRACEABILITY,
        )
        _uiState.update {
            it.copy(
                stage = FormatStage.ERROR,
                message = message,
                resultGuidance = com.opencode.nfccardmanager.core.nfc.model.FlowNextStepGuidance(
                    title = "推荐下一步",
                    conclusion = message,
                    reasonSummary = message,
                    recommendedAction = "请先保持卡片稳定后重新贴卡；若仍失败，暂停继续格式化并检查卡片兼容性。",
                    ctaLabel = "重新格式化",
                ),
            )
        }
    }
}
