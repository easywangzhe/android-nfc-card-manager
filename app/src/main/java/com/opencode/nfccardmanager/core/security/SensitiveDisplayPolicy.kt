package com.opencode.nfccardmanager.core.security

enum class SensitiveValueVisibility {
    FULL,
    PARTIAL,
    MASKED,
}

fun maskRiskSensitiveValue(value: String, role: UserRole): String {
    return when (role) {
        UserRole.ADMIN -> value
        UserRole.SUPERVISOR -> "****"
        UserRole.OPERATOR -> "****"
        UserRole.AUDITOR -> "****"
    }
}
