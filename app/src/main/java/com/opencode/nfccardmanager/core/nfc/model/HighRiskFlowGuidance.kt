package com.opencode.nfccardmanager.core.nfc.model

data class HighRiskSupportSummary(
    val title: String,
    val supportedLabel: String,
    val unsupportedLabel: String,
    val authenticityLabel: String,
)

enum class HighRiskResultSource {
    CONFIRMED_EXECUTED,
    FAILED,
    UNVERIFIED,
    DEMO_ONLY,
}

data class HighRiskResultGuidance(
    val source: HighRiskResultSource,
    val conclusion: String,
    val recoveryAction: String,
    val ctaLabel: String,
)

fun buildLockSupportSummary(capability: CardCapability?): HighRiskSupportSummary {
    return HighRiskSupportSummary(
        title = "TODO",
        supportedLabel = "TODO",
        unsupportedLabel = "TODO",
        authenticityLabel = "TODO",
    )
}

fun buildUnlockSupportSummary(capability: CardCapability?): HighRiskSupportSummary {
    return HighRiskSupportSummary(
        title = "TODO",
        supportedLabel = "TODO",
        unsupportedLabel = "TODO",
        authenticityLabel = "TODO",
    )
}

fun buildLockResultGuidance(result: LockCardResult?): HighRiskResultGuidance? {
    return result?.let {
        HighRiskResultGuidance(
            source = HighRiskResultSource.FAILED,
            conclusion = "TODO",
            recoveryAction = "TODO",
            ctaLabel = "TODO",
        )
    }
}

fun buildUnlockResultGuidance(result: UnlockCardResult?): HighRiskResultGuidance? {
    return result?.let {
        HighRiskResultGuidance(
            source = HighRiskResultSource.FAILED,
            conclusion = "TODO",
            recoveryAction = "TODO",
            ctaLabel = "TODO",
        )
    }
}
