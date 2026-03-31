package com.opencode.nfccardmanager.core.security

enum class SensitiveValueVisibility {
    FULL,
    PARTIAL,
    MASKED,
}

fun sensitiveValueVisibility(role: UserRole): SensitiveValueVisibility {
    return when (role) {
        UserRole.ADMIN -> SensitiveValueVisibility.FULL
        UserRole.SUPERVISOR -> SensitiveValueVisibility.PARTIAL
        UserRole.OPERATOR,
        UserRole.AUDITOR,
        -> SensitiveValueVisibility.MASKED
    }
}

fun maskRiskSensitiveValue(value: String, role: UserRole): String {
    if (value.isBlank()) return "已遮罩"
    return when (sensitiveValueVisibility(role)) {
        SensitiveValueVisibility.FULL -> value
        SensitiveValueVisibility.PARTIAL -> {
            if (value.length <= 4) "****" else value.take(2) + "******" + value.takeLast(2)
        }

        SensitiveValueVisibility.MASKED -> when (role) {
            UserRole.AUDITOR -> "仅审计摘要"
            else -> "已遮罩"
        }
    }
}
