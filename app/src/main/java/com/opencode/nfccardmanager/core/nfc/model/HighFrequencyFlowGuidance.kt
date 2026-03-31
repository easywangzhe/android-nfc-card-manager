package com.opencode.nfccardmanager.core.nfc.model

data class FlowNextStepGuidance(
    val title: String,
    val conclusion: String,
    val reasonSummary: String,
    val recommendedAction: String,
    val ctaLabel: String,
)

data class WriteOutcomeGuidance(
    val executionTitle: String,
    val executionConclusion: String,
    val verificationTitle: String,
    val verificationConclusion: String,
    val nextStep: FlowNextStepGuidance,
)

fun buildWriteOutcomeGuidance(result: WriteCardResult): WriteOutcomeGuidance = WriteOutcomeGuidance(
    executionTitle = when (result.writeStatus) {
        "WRITE_SUCCESS", "VERIFY_FAILED" -> "写入执行成功"
        "UNSUPPORTED_TAG" -> "写入未执行"
        "FORMAT_ERROR" -> "写入未完成"
        "WRITE_ERROR" -> "写入执行失败"
        else -> if (result.success) "写入执行成功" else "写入状态待确认"
    },
    executionConclusion = when (result.writeStatus) {
        "WRITE_SUCCESS" -> "写入命令已执行，当前卡片内容已更新。"
        "VERIFY_FAILED" -> "写入命令已执行，但当前不能把结果当成最终成功。"
        "UNSUPPORTED_TAG" -> "当前卡片不满足写入前提，写入命令尚未执行。"
        "FORMAT_ERROR" -> "写入前置条件未满足，本次未完成有效写入。"
        "WRITE_ERROR" -> "写入过程中发生异常，本次没有得到可确认的写入结果。"
        else -> result.message
    },
    verificationTitle = when (result.writeStatus) {
        "WRITE_SUCCESS" -> "回读校验通过"
        "VERIFY_FAILED" -> "回读校验失败"
        else -> if (result.verified) "回读校验通过" else "未执行回读校验"
    },
    verificationConclusion = when (result.writeStatus) {
        "UNSUPPORTED_TAG", "FORMAT_ERROR", "WRITE_ERROR" -> "由于写入未成功完成，当前没有可确认的回读校验结果。"
        else -> result.verificationMessage
    },
    nextStep = when (result.writeStatus) {
        "WRITE_SUCCESS" -> FlowNextStepGuidance(
            title = "推荐下一步",
            conclusion = "本次写卡与回读校验都已完成。",
            reasonSummary = result.writeReason,
            recommendedAction = "可以继续写下一张卡，或返回当前业务流程继续处理。",
            ctaLabel = "继续处理",
        )

        "VERIFY_FAILED" -> FlowNextStepGuidance(
            title = "推荐下一步",
            conclusion = "写入已执行，但还不能把当前结果当成最终成功。",
            reasonSummary = result.verificationMessage,
            recommendedAction = "请保持卡片稳定后重新贴卡复核，必要时再重试写卡。",
            ctaLabel = "重新复核",
        )

        "UNSUPPORTED_TAG" -> FlowNextStepGuidance(
            title = "推荐下一步",
            conclusion = "当前卡片不支持直接写入。",
            reasonSummary = result.writeReason,
            recommendedAction = "停止继续写入，先更换支持 NDEF 的卡片。",
            ctaLabel = "更换卡片",
        )

        "FORMAT_ERROR" -> FlowNextStepGuidance(
            title = "推荐下一步",
            conclusion = "当前卡片需要先处理格式化前置条件。",
            reasonSummary = result.writeReason,
            recommendedAction = "请先完成格式化或清空后，再重新尝试写卡。",
            ctaLabel = "先去格式化",
        )

        else -> FlowNextStepGuidance(
            title = "推荐下一步",
            conclusion = "写入过程中发生异常。",
            reasonSummary = result.writeReason.ifBlank { result.message },
            recommendedAction = "请先检查卡片稳定性与兼容性，再重新贴卡重试。",
            ctaLabel = "重新写卡",
        )
    },
)

fun buildReadNextStepGuidance(result: ReadCardResult): FlowNextStepGuidance = when (result.readStatus) {
    "READ_SUCCESS" -> FlowNextStepGuidance(
        title = "推荐下一步",
        conclusion = "内容已成功读出，可根据卡片能力继续后续操作。",
        reasonSummary = result.readReason,
        recommendedAction = "可继续查看卡片信息；如需清空或重置，再去格式化。",
        ctaLabel = "查看结果",
    )

    "EMPTY_NDEF" -> FlowNextStepGuidance(
        title = "推荐下一步",
        conclusion = "当前卡片已是空 NDEF 标签。",
        reasonSummary = result.readReason,
        recommendedAction = "当前是空 NDEF 标签，可继续去写卡；若需先确认清空状态，也可先执行格式化。",
        ctaLabel = "去写卡",
    )

    "NON_NDEF" -> FlowNextStepGuidance(
        title = "推荐下一步",
        conclusion = "当前不是可直接读取的 NDEF 标签。",
        reasonSummary = result.readReason,
        recommendedAction = "当前不是可直接读取的 NDEF 标签，不建议直接继续写卡；请先确认卡片类型或先尝试格式化。",
        ctaLabel = "先去格式化",
    )

    "READ_ERROR" -> FlowNextStepGuidance(
        title = "推荐下一步",
        conclusion = "读取过程已发生异常。",
        reasonSummary = result.readReason,
        recommendedAction = "请先重新贴卡重试，仍失败再检查 NFC 开关与卡片稳定性。",
        ctaLabel = "重新读卡",
    )

    else -> FlowNextStepGuidance(
        title = "推荐下一步",
        conclusion = "当前读卡状态待确认。",
        reasonSummary = result.readReason,
        recommendedAction = "请重新读卡一次，确认当前卡片状态后再继续。",
        ctaLabel = "重新读卡",
    )
}

fun buildFormatNextStepGuidance(result: FormatCardResult): FlowNextStepGuidance = when (result.status) {
    "CLEARED_NDEF", "FORMAT_SUCCESS" -> FlowNextStepGuidance(
        title = "推荐下一步",
        conclusion = "格式化或清空已完成，可继续进入写卡流程。",
        reasonSummary = result.reason,
        recommendedAction = "当前卡片已准备好，可继续去写卡。",
        ctaLabel = "去写卡",
    )

    "UNSUPPORTED_TAG" -> FlowNextStepGuidance(
        title = "推荐下一步",
        conclusion = "已确认当前卡片不支持 NDEF 格式化。",
        reasonSummary = result.reason,
        recommendedAction = "停止继续格式化或写卡，改用支持 NDEF 的卡片。",
        ctaLabel = "更换卡片",
    )

    "CLEAR_ERROR" -> FlowNextStepGuidance(
        title = "推荐下一步",
        conclusion = "清空旧内容时已经发生失败。",
        reasonSummary = result.reason,
        recommendedAction = "清空失败已发生，请保持卡片稳定后重试，不要直接假定卡片已清空。",
        ctaLabel = "重新尝试",
    )

    "FORMAT_ERROR" -> FlowNextStepGuidance(
        title = "推荐下一步",
        conclusion = "格式化过程中发生异常，当前不能假定卡片已经可安全写入。",
        reasonSummary = result.reason,
        recommendedAction = "请先重试一次；若仍失败，保留现状并检查卡片兼容性。",
        ctaLabel = "再次格式化",
    )

    else -> FlowNextStepGuidance(
        title = "推荐下一步",
        conclusion = result.message,
        reasonSummary = result.reason,
        recommendedAction = "请重新确认卡片状态后，再决定是否继续格式化。",
        ctaLabel = "重新确认",
    )
}
