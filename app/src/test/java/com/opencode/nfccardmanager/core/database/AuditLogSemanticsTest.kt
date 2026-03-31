package com.opencode.nfccardmanager.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditLogSemanticsTest {

    @Test
    fun `semantic enums expose stable Chinese labels`() {
        assertEquals("管理员", AuditOperatorRole.ADMIN.label)
        assertEquals("执行完成", AuditFlowStage.COMPLETED.label)
        assertEquals("Demo 流程", AuditAuthenticity.DEMO_ONLY.label)
        assertEquals("可追责性", AuditImpactScope.TRACEABILITY.label)
        assertEquals("本地便利性", AuditImpactScope.LOCAL_CONVENIENCE.label)
    }

    @Test
    fun `legacy metadata uses explicit fallback labels`() {
        val metadata = AuditLogMetadata(
            operatorRole = null,
            flowStage = null,
            authenticity = null,
            impactScope = null,
        ).resolved()

        assertEquals("历史记录", metadata.operatorRole.label)
        assertEquals("未标记", metadata.flowStage.label)
        assertEquals("待补充", metadata.authenticity.label)
        assertEquals("待补充", metadata.impactScope.label)
    }

    @Test
    fun `impact scope distinguishes traceability and local convenience`() {
        assertTrue(AuditImpactScope.TRACEABILITY.label.contains("追责"))
        assertTrue(AuditImpactScope.LOCAL_CONVENIENCE.label.contains("本地便利"))
    }
}
