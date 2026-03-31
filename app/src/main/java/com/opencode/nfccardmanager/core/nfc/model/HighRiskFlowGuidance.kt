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
    return when {
        capability == null -> HighRiskSupportSummary(
            title = "锁卡支持边界",
            supportedLabel = "需先识别卡片，才能确认支持密码保护还是永久只读。",
            unsupportedLabel = "识别前不要假定当前卡片一定支持锁卡。",
            authenticityLabel = "待识别",
        )

        !capability.canLock || capability.lockMode == LockMode.NONE -> HighRiskSupportSummary(
            title = "锁卡支持边界",
            supportedLabel = "当前场景不可执行锁卡。",
            unsupportedLabel = "当前卡片不支持锁卡。",
            authenticityLabel = "未支持",
        )

        capability.lockMode == LockMode.PASSWORD_PROTECTED -> HighRiskSupportSummary(
            title = "锁卡支持边界",
            supportedLabel = "密码保护锁定已支持，可保留后续受控解锁路径。",
            unsupportedLabel = "若认证失败或卡片策略变化，仍可能无法继续解锁。",
            authenticityLabel = "真实支持",
        )

        else -> HighRiskSupportSummary(
            title = "锁卡支持边界",
            supportedLabel = "当前仅支持永久只读锁定，执行后通常不可逆。",
            unsupportedLabel = "不支持密码保护解锁链路，请先确认不可逆风险。",
            authenticityLabel = "真实支持",
        )
    }
}

fun buildUnlockSupportSummary(capability: CardCapability?): HighRiskSupportSummary {
    return when {
        capability == null -> HighRiskSupportSummary(
            title = "解锁支持边界",
            supportedLabel = "需先识别卡片，才能确认是否属于密码保护型锁定。",
            unsupportedLabel = "识别前不要把本流程当成真实可解锁能力。",
            authenticityLabel = "仅演示",
        )

        !capability.canUnlock || capability.lockMode != LockMode.PASSWORD_PROTECTED -> HighRiskSupportSummary(
            title = "解锁支持边界",
            supportedLabel = "当前场景不可解锁。",
            unsupportedLabel = when (capability.lockMode) {
                LockMode.READ_ONLY_PERMANENT -> "永久只读通常不可逆，当前卡片不适用通用解锁流程。"
                else -> "当前卡片不支持解锁。"
            },
            authenticityLabel = "未支持",
        )

        else -> HighRiskSupportSummary(
            title = "解锁支持边界",
            supportedLabel = "当前卡片可进入密码保护型解锁流程演示，但尚未接入真实底层解除写保护命令。",
            unsupportedLabel = "即使流程演示成功，也不能视为真实解锁已完成。",
            authenticityLabel = "仅演示",
        )
    }
}

fun buildLockResultGuidance(result: LockCardResult?): HighRiskResultGuidance? {
    return result?.let { lockResult ->
        when {
            !lockResult.success -> HighRiskResultGuidance(
                source = HighRiskResultSource.FAILED,
                conclusion = when {
                    lockResult.message.contains("不支持") -> "当前卡片不具备锁卡条件，本次操作未执行。"
                    lockResult.verificationMessage.contains("识别") -> "锁卡前识别失败，本次操作未完成。"
                    else -> "本次锁卡执行失败。"
                },
                recoveryAction = when {
                    lockResult.message.contains("不支持") -> "请停止继续尝试该卡，改用支持锁卡的卡片。"
                    lockResult.verificationMessage.contains("认证") -> "请复核权限或认证条件，确认后再重试一次。"
                    else -> "请保持卡片稳定后重试；若仍失败，先保留现状并升级处理。"
                },
                ctaLabel = "返回风险检查",
            )

            lockResult.verified -> HighRiskResultGuidance(
                source = HighRiskResultSource.CONFIRMED_EXECUTED,
                conclusion = "本次锁卡已确认执行。",
                recoveryAction = if (lockResult.irreversible) {
                    "请立即记录锁定结果，并避免再尝试写入该卡。"
                } else {
                    "请记录当前密码保护状态，并妥善保存后续解锁凭据。"
                },
                ctaLabel = "记录结果",
            )

            else -> HighRiskResultGuidance(
                source = HighRiskResultSource.UNVERIFIED,
                conclusion = "锁卡流程已返回成功，但当前结果尚未完成校验。",
                recoveryAction = "请保持卡片在手边，再次读卡校验锁定状态后再离开。",
                ctaLabel = "去校验结果",
            )
        }
    }
}

fun buildUnlockResultGuidance(result: UnlockCardResult?): HighRiskResultGuidance? {
    return result?.let { unlockResult ->
        when {
            unlockResult.success -> HighRiskResultGuidance(
                source = HighRiskResultSource.DEMO_ONLY,
                conclusion = "当前仅完成流程演示，未接入真实底层命令，因此不能视为真实解锁成功。",
                recoveryAction = "若需真实恢复写入能力，请改用后续接入真实命令的版本或人工处置流程。",
                ctaLabel = "查看边界说明",
            )

            unlockResult.message.contains("凭据") -> HighRiskResultGuidance(
                source = HighRiskResultSource.FAILED,
                conclusion = "本次解锁因凭据校验失败而未执行后续流程。",
                recoveryAction = "请核对凭据来源后重试，不要连续多次盲试。",
                ctaLabel = "重新输入凭据",
            )

            unlockResult.message.contains("不支持") -> HighRiskResultGuidance(
                source = HighRiskResultSource.FAILED,
                conclusion = "当前卡片或锁定方式不支持通用解锁。",
                recoveryAction = "请停止继续尝试，保留现状并改走人工恢复或换卡流程。",
                ctaLabel = "返回边界说明",
            )

            else -> HighRiskResultGuidance(
                source = HighRiskResultSource.FAILED,
                conclusion = "本次解锁未完成。",
                recoveryAction = "请确认卡片、凭据和贴卡稳定性后再重试一次。",
                ctaLabel = "重试前检查",
            )
        }
    }
}
