package com.opencode.nfccardmanager.feature.home

import com.opencode.nfccardmanager.core.security.UserRole

enum class HomeSectionKind {
    PRIMARY,
    HIGH_RISK,
    MANAGEMENT,
}

enum class HomeEntryRiskLevel {
    NORMAL,
    WARNING,
}

enum class HomeEntryDestination {
    READ,
    WRITE,
    LOCK,
    UNLOCK,
    TEMPLATE,
    AUDIT,
    SETTINGS,
}

data class HomeEntry(
    val title: String,
    val description: String,
    val destination: HomeEntryDestination,
    val riskLevel: HomeEntryRiskLevel,
)

data class HomeSection(
    val kind: HomeSectionKind,
    val title: String,
    val description: String,
    val entries: List<HomeEntry>,
)

data class BottomNavDestination(
    val route: String,
    val label: String,
)

fun buildHomeSections(role: UserRole): List<HomeSection> {
    return emptyList()
}

fun buildBottomNavDestinations(role: UserRole): List<BottomNavDestination> {
    return emptyList()
}
