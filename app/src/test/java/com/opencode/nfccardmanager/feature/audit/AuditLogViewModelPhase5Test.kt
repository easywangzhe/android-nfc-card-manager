package com.opencode.nfccardmanager.feature.audit

import com.opencode.nfccardmanager.core.database.AuditAuthenticity
import com.opencode.nfccardmanager.core.database.AuditFlowStage
import com.opencode.nfccardmanager.core.database.AuditImpactScope
import com.opencode.nfccardmanager.core.database.AuditLogRecord
import com.opencode.nfccardmanager.core.database.AuditOperatorRole
import com.opencode.nfccardmanager.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuditLogViewModelPhase5Test {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `list projection exposes operator role stage authenticity and impact labels`() {
        val item = buildAuditLogListItem(sampleLog())
        val legacyItem = buildAuditLogListItem(sampleLog(id = 2, operatorRole = AuditOperatorRole.LEGACY, flowStage = AuditFlowStage.UNMARKED, authenticity = AuditAuthenticity.PENDING, impactScope = AuditImpactScope.PENDING))

        assertEquals("系统管理员", item.roleLabel)
        assertEquals("执行完成", item.stageLabel)
        assertEquals("真实设备结果", item.authenticityLabel)
        assertEquals("可追责性", item.impactLabel)
        assertEquals("历史记录", legacyItem.roleLabel)
        assertEquals("未标记", legacyItem.stageLabel)
        assertEquals("待补充", legacyItem.authenticityLabel)
    }

    @Test
    fun `detail projection splits who stage authenticity impact and event summary`() {
        val detail = buildAuditLogDetailPresentation(sampleLog())

        assertEquals("admin（系统管理员）", detail.whoSummary)
        assertEquals("执行完成", detail.stageLabel)
        assertEquals("真实设备结果", detail.authenticityLabel)
        assertEquals("可追责性", detail.impactLabel)
        assertTrue(detail.whatHappened.contains("WRITE"))
    }

    @Test
    fun `keyword and result filters still work with presentation layer`() {
        val viewModel = AuditLogViewModel()
        viewModel.updateLogsForTest(
            listOf(
                sampleLog(id = 1, operationType = "WRITE", result = "SUCCESS", message = "写卡成功"),
                sampleLog(id = 2, operationType = "LOCK", result = "FAILED", message = "锁卡失败"),
            )
        )

        viewModel.onResultFilterChange(AuditResultFilter.FAILED)
        assertEquals(1, viewModel.uiState.value.filteredLogs.size)
        assertEquals("LOCK", viewModel.uiState.value.filteredLogs.first().operationType)

        viewModel.onKeywordChange("写卡")
        viewModel.onResultFilterChange(AuditResultFilter.ALL)
        assertEquals(1, viewModel.uiState.value.filteredLogs.size)
        assertEquals("WRITE", viewModel.uiState.value.filteredLogs.first().operationType)
    }

    private fun sampleLog(
        id: Long = 1,
        operationType: String = "WRITE",
        result: String = "SUCCESS",
        message: String = "写卡成功，内容已回读校验",
        operatorRole: AuditOperatorRole = AuditOperatorRole.ADMIN,
        flowStage: AuditFlowStage = AuditFlowStage.COMPLETED,
        authenticity: AuditAuthenticity = AuditAuthenticity.VERIFIED,
        impactScope: AuditImpactScope = AuditImpactScope.TRACEABILITY,
    ) = AuditLogRecord(
        id = id,
        operationType = operationType,
        operatorId = "admin",
        operatorRole = operatorRole,
        cardUidMasked = "04****D4",
        cardType = "NDEF",
        flowStage = flowStage,
        result = result,
        authenticity = authenticity,
        impactScope = impactScope,
        message = message,
        createdAt = 1711860000000,
    )
}
