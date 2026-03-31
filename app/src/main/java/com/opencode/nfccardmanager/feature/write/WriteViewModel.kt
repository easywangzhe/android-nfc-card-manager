package com.opencode.nfccardmanager.feature.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.nfccardmanager.core.database.AuditFlowStage
import com.opencode.nfccardmanager.core.database.AuditImpactScope
import com.opencode.nfccardmanager.core.database.AuditLogManager
import com.opencode.nfccardmanager.core.nfc.model.FlowNextStepGuidance
import com.opencode.nfccardmanager.core.nfc.model.ReadCardResult
import com.opencode.nfccardmanager.core.nfc.model.WriteCardResult
import com.opencode.nfccardmanager.core.nfc.model.buildWriteOutcomeGuidance
import com.opencode.nfccardmanager.core.security.SecurityManager
import com.opencode.nfccardmanager.feature.audit.mapUserRoleToAuditRole
import com.opencode.nfccardmanager.feature.audit.writeAuthenticity
import com.opencode.nfccardmanager.feature.template.LocalTemplateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WriteViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        WriteUiState(
            templates = LocalTemplateRepository.listTemplates(),
        )
    )
    val uiState: StateFlow<WriteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            LocalTemplateRepository.templates.collect { templates ->
                _uiState.update { state ->
                    state.copy(
                        templates = templates,
                        selectedTemplateId = state.selectedTemplateId?.takeIf { id -> templates.any { it.id == id } },
                    )
                }
            }
        }
    }

    fun applyTemplate(templateId: String) {
        val template = LocalTemplateRepository.findById(templateId) ?: return
        _uiState.update {
            it.copy(
                selectedTemplateId = template.id,
                content = template.content,
                stage = WriteStage.READY,
                message = "已应用模板：${template.name}，请确认内容后贴卡写入",
                result = null,
                resultGuidance = null,
                nextStepGuidance = null,
            )
        }
    }

    fun onContentChange(value: String) {
        _uiState.update {
            it.copy(
                content = value,
                selectedTemplateId = null,
                stage = if (value.isBlank()) WriteStage.IDLE else WriteStage.READY,
                message = if (value.isBlank()) {
                    "请输入待写入的文本内容，确认后点击开始写卡。"
                } else {
                    "内容已准备，请点击开始写卡后再贴近支持 NDEF 的标签。"
                },
                resultGuidance = null,
                nextStepGuidance = null,
            )
        }
    }

    fun startWriting() {
        if (_uiState.value.content.isBlank()) {
            onError("写入内容不能为空")
            return
        }
        _uiState.update {
            it.copy(
                stage = WriteStage.WRITING,
                message = "等待标签贴近，检测到标签后将自动写入",
                detectedUid = null,
                detectedTechType = null,
                detectedWritable = null,
                detectedNdefType = null,
                detectedCapacity = null,
                detectedRequiredBytes = null,
                detectedReadOnlyCapable = null,
                detectedCanProceed = null,
                detectedMessage = "",
                detectedTechList = emptyList(),
                lastErrorDetail = "",
                result = null,
                resultGuidance = null,
                nextStepGuidance = null,
            )
        }
    }

    fun retryWriting() {
        startWriting()
    }

    fun onTagDetected(
        readResult: ReadCardResult,
        supportsWrite: Boolean,
        canProceed: Boolean,
        precheckReason: String,
        requiredBytes: Int?,
    ) {
        val ndefType = readResult.detailItems.firstOrNull { it.label == "NDEF 类型" }?.value
        val capacity = readResult.detailItems
            .firstOrNull { it.label == "NDEF 容量" }
            ?.value
            ?.substringBefore(" ")
            ?.toIntOrNull()
        val readOnlyCapable = readResult.detailItems
            .firstOrNull { it.label == "可设只读" }
            ?.value
            ?.let { it == "是" }

        _uiState.update {
            it.copy(
                detectedUid = readResult.cardInfo.uid,
                detectedTechType = readResult.cardInfo.techType.name,
                detectedWritable = supportsWrite,
                detectedNdefType = ndefType,
                detectedCapacity = capacity,
                detectedRequiredBytes = requiredBytes,
                detectedReadOnlyCapable = readOnlyCapable,
                detectedCanProceed = canProceed,
                detectedTechList = readResult.rawTechList,
                detectedMessage = if (canProceed) {
                    "已检测到可尝试写入的标签，正在执行写卡。$precheckReason"
                } else {
                    precheckReason
                },
                lastErrorDetail = "",
            )
        }
    }

    fun onRawTagDetected(
        uid: String,
        techType: String,
        techList: List<String>,
        supportsWrite: Boolean,
        canProceed: Boolean,
        precheckReason: String,
        requiredBytes: Int?,
        capacityBytes: Int?,
        isWritable: Boolean?,
    ) {
        _uiState.update {
            it.copy(
                detectedUid = uid,
                detectedTechType = techType,
                detectedWritable = supportsWrite,
                detectedNdefType = if (techList.any { tech -> tech.endsWith("Ndef") }) "已识别 NDEF" else "未识别",
                detectedCapacity = capacityBytes,
                detectedRequiredBytes = requiredBytes,
                detectedReadOnlyCapable = null,
                detectedCanProceed = canProceed,
                detectedTechList = techList,
                detectedMessage = buildString {
                    append("已检测到标签。")
                    if (isWritable != null) {
                        append(" 当前可写：")
                        append(if (isWritable) "是" else "否")
                        append("。")
                    }
                    append(precheckReason)
                },
                lastErrorDetail = "",
            )
        }
    }

    fun onWriteResult(result: WriteCardResult) {
        val guidance = buildWriteOutcomeGuidance(result)
        AuditLogManager.save(
            operationType = "WRITE",
            cardUid = result.cardInfo.uid,
            cardType = result.cardInfo.techType.name,
            result = if (result.success) "SUCCESS" else "FAILED",
            message = "${result.message}；${result.verificationMessage}",
            operatorId = SecurityManager.currentSession.value?.username ?: "system",
            operatorRole = mapUserRoleToAuditRole(SecurityManager.currentRole.value.name),
            flowStage = if (result.success) AuditFlowStage.COMPLETED else AuditFlowStage.FAILED,
            authenticity = writeAuthenticity(result.success, result.verified),
            impactScope = AuditImpactScope.TRACEABILITY,
        )
        _uiState.update {
            it.copy(
                stage = if (result.success) WriteStage.SUCCESS else WriteStage.ERROR,
                message = result.message,
                result = result,
                resultGuidance = guidance,
                nextStepGuidance = guidance.nextStep,
            )
        }
    }

    fun onError(message: String) {
        AuditLogManager.save(
            operationType = "WRITE",
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
                stage = WriteStage.ERROR,
                message = message,
                lastErrorDetail = message,
                result = null,
                resultGuidance = null,
                nextStepGuidance = buildGenericErrorNextStep(message),
            )
        }
    }

    fun resetResult() {
        _uiState.update {
            it.copy(
                stage = if (it.content.isBlank()) WriteStage.IDLE else WriteStage.READY,
                message = if (it.content.isBlank()) "请输入待写入的文本内容，确认后点击开始写卡。" else "内容已准备，请点击开始写卡后再贴近支持 NDEF 的标签。",
                detectedUid = null,
                detectedTechType = null,
                detectedWritable = null,
                detectedNdefType = null,
                detectedCapacity = null,
                detectedRequiredBytes = null,
                detectedReadOnlyCapable = null,
                detectedCanProceed = null,
                detectedMessage = "",
                detectedTechList = emptyList(),
                lastErrorDetail = "",
                result = null,
                resultGuidance = null,
                nextStepGuidance = null,
            )
        }
    }

    private fun buildGenericErrorNextStep(message: String) = FlowNextStepGuidance(
        title = "推荐下一步",
        conclusion = message,
        reasonSummary = message,
        recommendedAction = "请先检查卡片稳定性与兼容性，再重新贴卡重试。",
        ctaLabel = "重新写卡",
    )
}
