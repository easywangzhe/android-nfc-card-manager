package com.opencode.nfccardmanager.feature.support

import com.opencode.nfccardmanager.ui.component.StatusTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportPageContractTest {

    @Test
    fun `summary exposes shared title summary impact and sections`() {
        val summary = SupportPageSummary(
            title = "模板管理",
            summary = "模板只影响后续写卡复用效率",
            impact = SupportImpact.LOCAL_CONVENIENCE,
            sections = listOf(
                SupportSection(
                    title = "模板编辑",
                    description = "可新增或更新本地模板",
                ),
            ),
        )

        assertEquals("模板管理", summary.title)
        assertEquals("模板只影响后续写卡复用效率", summary.summary)
        assertEquals(SupportImpact.LOCAL_CONVENIENCE, summary.impact)
        assertEquals(1, summary.sections.size)
        assertEquals("模板编辑", summary.sections.first().title)
    }

    @Test
    fun `impact maps to stable Chinese labels and tones`() {
        assertEquals("安全性", SupportImpact.SAFETY.label)
        assertEquals(StatusTone.WARNING, SupportImpact.SAFETY.tone)

        assertEquals("可追责性", SupportImpact.TRACEABILITY.label)
        assertEquals(StatusTone.INFO, SupportImpact.TRACEABILITY.tone)

        assertEquals("本地便利性", SupportImpact.LOCAL_CONVENIENCE.label)
        assertEquals(StatusTone.SUCCESS, SupportImpact.LOCAL_CONVENIENCE.tone)
    }

    @Test
    fun `default summary copy does not imply business state changed`() {
        val summary = supportPageSummary(
            title = "设置",
            impact = SupportImpact.LOCAL_CONVENIENCE,
        )

        assertTrue(summary.summary.contains("不会直接改变卡片业务状态"))
        assertEquals(SupportImpact.LOCAL_CONVENIENCE, summary.impact)
        assertTrue(summary.sections.isEmpty())
    }
}
