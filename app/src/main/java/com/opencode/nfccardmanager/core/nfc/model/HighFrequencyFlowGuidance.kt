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
    executionTitle = result.writeStatus,
    executionConclusion = result.message,
    verificationTitle = if (result.verified) "已校验" else "未校验",
    verificationConclusion = result.verificationMessage,
    nextStep = FlowNextStepGuidance(
        title = "下一步",
        conclusion = result.message,
        reasonSummary = result.writeReason,
        recommendedAction = "请根据现场情况继续操作",
        ctaLabel = "继续",
    ),
)

fun buildReadNextStepGuidance(result: ReadCardResult): FlowNextStepGuidance = FlowNextStepGuidance(
    title = "下一步",
    conclusion = result.readStatus,
    reasonSummary = result.readReason,
    recommendedAction = "继续检查卡片",
    ctaLabel = "继续",
)

fun buildFormatNextStepGuidance(result: FormatCardResult): FlowNextStepGuidance = FlowNextStepGuidance(
    title = "下一步",
    conclusion = result.status,
    reasonSummary = result.reason,
    recommendedAction = "继续处理卡片",
    ctaLabel = "继续",
)
