package com.opencode.nfccardmanager.feature.home

import com.opencode.nfccardmanager.core.security.SecurityManager
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
    return homeSectionDefinitions.mapNotNull { definition ->
        val visibleEntries = definition.entries.filter { entry -> entry.isVisible(role) }
            .map { entry -> entry.model }
        if (visibleEntries.isEmpty()) {
            null
        } else {
            HomeSection(
                kind = definition.kind,
                title = definition.title,
                description = definition.description,
                entries = visibleEntries,
            )
        }
    }
}

fun buildBottomNavDestinations(role: UserRole): List<BottomNavDestination> {
    return bottomNavDefinitions.filter { destination -> destination.isVisible(role) }
        .map { destination -> destination.model }
}

private data class HomeEntryDefinition(
    val model: HomeEntry,
    val isVisible: (UserRole) -> Boolean,
)

private data class HomeSectionDefinition(
    val kind: HomeSectionKind,
    val title: String,
    val description: String,
    val entries: List<HomeEntryDefinition>,
)

private data class BottomNavDefinition(
    val model: BottomNavDestination,
    val isVisible: (UserRole) -> Boolean,
)

private val homeSectionDefinitions = listOf(
    HomeSectionDefinition(
        kind = HomeSectionKind.PRIMARY,
        title = "主任务",
        description = "适合日常执行的读写操作入口。",
        entries = listOf(
            HomeEntryDefinition(
                model = HomeEntry(
                    title = "读卡",
                    description = "先识别卡片内容与能力，再决定后续动作。",
                    destination = HomeEntryDestination.READ,
                    riskLevel = HomeEntryRiskLevel.NORMAL,
                ),
                isVisible = SecurityManager::canRead,
            ),
            HomeEntryDefinition(
                model = HomeEntry(
                    title = "写卡",
                    description = "写入模板或业务内容前，先确认目标卡片状态。",
                    destination = HomeEntryDestination.WRITE,
                    riskLevel = HomeEntryRiskLevel.NORMAL,
                ),
                isVisible = SecurityManager::canWrite,
            ),
        ),
    ),
    HomeSectionDefinition(
        kind = HomeSectionKind.HIGH_RISK,
        title = "高风险操作",
        description = "操作成本高，请确认卡片状态与流程前置条件。",
        entries = listOf(
            HomeEntryDefinition(
                model = HomeEntry(
                    title = "锁卡",
                    description = "执行前请确认不可逆风险与恢复方案。",
                    destination = HomeEntryDestination.LOCK,
                    riskLevel = HomeEntryRiskLevel.WARNING,
                ),
                isVisible = SecurityManager::canLock,
            ),
            HomeEntryDefinition(
                model = HomeEntry(
                    title = "解锁",
                    description = "仅在受控场景下执行，注意区分真实支持与演示结果。",
                    destination = HomeEntryDestination.UNLOCK,
                    riskLevel = HomeEntryRiskLevel.WARNING,
                ),
                isVisible = SecurityManager::canUnlock,
            ),
        ),
    ),
    HomeSectionDefinition(
        kind = HomeSectionKind.MANAGEMENT,
        title = "管理工具",
        description = "查看记录、管理模板或处理本地设置。",
        entries = listOf(
            HomeEntryDefinition(
                model = HomeEntry(
                    title = "模板管理",
                    description = "维护可复用写卡模板，仅管理员可见。",
                    destination = HomeEntryDestination.TEMPLATE,
                    riskLevel = HomeEntryRiskLevel.NORMAL,
                ),
                isVisible = SecurityManager::canManageTemplate,
            ),
            HomeEntryDefinition(
                model = HomeEntry(
                    title = "审计日志",
                    description = "查看操作轨迹与执行结果。",
                    destination = HomeEntryDestination.AUDIT,
                    riskLevel = HomeEntryRiskLevel.NORMAL,
                ),
                isVisible = SecurityManager::canViewAudit,
            ),
            HomeEntryDefinition(
                model = HomeEntry(
                    title = "设置 / 账号",
                    description = "切换角色、退出登录与处理本地配置。",
                    destination = HomeEntryDestination.SETTINGS,
                    riskLevel = HomeEntryRiskLevel.NORMAL,
                ),
                isVisible = { true },
            ),
        ),
    ),
)

private val bottomNavDefinitions = listOf(
    BottomNavDefinition(
        model = BottomNavDestination(route = "home", label = "首页"),
        isVisible = { true },
    ),
    BottomNavDefinition(
        model = BottomNavDestination(route = "template", label = "模板"),
        isVisible = SecurityManager::canManageTemplate,
    ),
    BottomNavDefinition(
        model = BottomNavDestination(route = "audit", label = "日志"),
        isVisible = SecurityManager::canViewAudit,
    ),
    BottomNavDefinition(
        model = BottomNavDestination(route = "settings", label = "我的"),
        isVisible = { true },
    ),
)
