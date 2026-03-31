package com.opencode.nfccardmanager.feature.support

import com.opencode.nfccardmanager.ui.component.StatusTone

data class SupportPageSummary(
    val title: String,
    val summary: String,
    val impact: SupportImpact,
    val sections: List<SupportSection> = emptyList(),
)

data class SupportSection(
    val title: String,
    val description: String,
)

enum class SupportImpact(
    val label: String,
    val tone: StatusTone,
) {
    SAFETY(
        label = "安全性",
        tone = StatusTone.WARNING,
    ),
    TRACEABILITY(
        label = "可追责性",
        tone = StatusTone.INFO,
    ),
    LOCAL_CONVENIENCE(
        label = "本地便利性",
        tone = StatusTone.SUCCESS,
    ),
}

fun supportPageSummary(
    title: String,
    impact: SupportImpact,
    summary: String = "此页仅用于说明或管理本地辅助信息，不会直接改变卡片业务状态。",
    sections: List<SupportSection> = emptyList(),
): SupportPageSummary {
    return SupportPageSummary(
        title = title,
        summary = summary,
        impact = impact,
        sections = sections,
    )
}
