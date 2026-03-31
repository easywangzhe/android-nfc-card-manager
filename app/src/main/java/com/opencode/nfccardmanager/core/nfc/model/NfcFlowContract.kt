package com.opencode.nfccardmanager.core.nfc.model

import com.opencode.nfccardmanager.core.security.ProtectedAction
import com.opencode.nfccardmanager.feature.format.FormatStage
import com.opencode.nfccardmanager.feature.lock.LockStage
import com.opencode.nfccardmanager.feature.scan.ScanStage
import com.opencode.nfccardmanager.feature.unlock.UnlockStage
import com.opencode.nfccardmanager.feature.write.WriteStage

enum class NfcFlowStage {
    WAITING,
    SCANNING,
    PROCESSING,
    SUCCESS,
    FAILURE,
}

enum class NfcFlowTone {
    NEUTRAL,
    INFO,
    WARNING,
    SUCCESS,
    DANGER,
}

data class NfcFlowStagePresentation(
    val title: String,
    val detail: String,
    val tone: NfcFlowTone,
    val isBusy: Boolean,
    val isTerminal: Boolean,
)

enum class CapabilityAuthenticity {
    SUPPORTED,
    UNVERIFIED,
    DEMO_ONLY,
    UNSUPPORTED,
}

data class CapabilityAuthenticityPresentation(
    val label: String,
    val detail: String,
    val tone: NfcFlowTone,
)

fun NfcFlowStage.presentation(): NfcFlowStagePresentation {
    return when (this) {
        NfcFlowStage.WAITING -> NfcFlowStagePresentation(
            title = "等待操作",
            detail = "请先确认卡片和操作条件，再开始 NFC 流程。",
            tone = NfcFlowTone.NEUTRAL,
            isBusy = false,
            isTerminal = false,
        )

        NfcFlowStage.SCANNING -> NfcFlowStagePresentation(
            title = "扫描中",
            detail = "请将卡片贴近手机背部 NFC 区域。",
            tone = NfcFlowTone.INFO,
            isBusy = true,
            isTerminal = false,
        )

        NfcFlowStage.PROCESSING -> NfcFlowStagePresentation(
            title = "处理中",
            detail = "系统正在读取或写入卡片，请勿移开卡片。",
            tone = NfcFlowTone.WARNING,
            isBusy = true,
            isTerminal = false,
        )

        NfcFlowStage.SUCCESS -> NfcFlowStagePresentation(
            title = "操作成功",
            detail = "本次 NFC 流程已完成。",
            tone = NfcFlowTone.SUCCESS,
            isBusy = false,
            isTerminal = true,
        )

        NfcFlowStage.FAILURE -> NfcFlowStagePresentation(
            title = "操作失败",
            detail = "本次 NFC 流程未完成，请根据提示重试。",
            tone = NfcFlowTone.DANGER,
            isBusy = false,
            isTerminal = true,
        )
    }
}

fun CapabilityAuthenticity.presentation(): CapabilityAuthenticityPresentation {
    return when (this) {
        CapabilityAuthenticity.SUPPORTED -> CapabilityAuthenticityPresentation(
            label = "已验证支持",
            detail = "当前能力已在真实卡片能力模型中验证，可按正式流程执行。",
            tone = NfcFlowTone.SUCCESS,
        )

        CapabilityAuthenticity.UNVERIFIED -> CapabilityAuthenticityPresentation(
            label = "待验证",
            detail = "当前能力尚未完成真实设备或卡片验证，执行前请再次确认。",
            tone = NfcFlowTone.WARNING,
        )

        CapabilityAuthenticity.DEMO_ONLY -> CapabilityAuthenticityPresentation(
            label = "仅演示",
            detail = "当前流程仅用于演示交互，不代表设备一定具备真实能力。",
            tone = NfcFlowTone.INFO,
        )

        CapabilityAuthenticity.UNSUPPORTED -> CapabilityAuthenticityPresentation(
            label = "未支持",
            detail = "当前设备或卡片能力不支持该操作。",
            tone = NfcFlowTone.DANGER,
        )
    }
}

fun ProtectedAction.toCapabilityAuthenticity(capability: CardCapability? = null): CapabilityAuthenticity {
    return when (this) {
        ProtectedAction.READ -> when {
            capability == null -> CapabilityAuthenticity.UNVERIFIED
            capability.canRead -> CapabilityAuthenticity.SUPPORTED
            else -> CapabilityAuthenticity.UNSUPPORTED
        }

        ProtectedAction.WRITE -> when {
            capability == null -> CapabilityAuthenticity.UNVERIFIED
            capability.canWrite -> CapabilityAuthenticity.SUPPORTED
            else -> CapabilityAuthenticity.UNSUPPORTED
        }

        ProtectedAction.FORMAT -> when {
            capability == null -> CapabilityAuthenticity.UNVERIFIED
            capability.canWrite -> CapabilityAuthenticity.UNVERIFIED
            else -> CapabilityAuthenticity.UNSUPPORTED
        }

        ProtectedAction.LOCK -> when {
            capability == null -> CapabilityAuthenticity.UNVERIFIED
            capability.canLock -> CapabilityAuthenticity.SUPPORTED
            else -> CapabilityAuthenticity.UNSUPPORTED
        }

        ProtectedAction.UNLOCK -> when {
            capability == null -> CapabilityAuthenticity.DEMO_ONLY
            capability.canUnlock -> CapabilityAuthenticity.DEMO_ONLY
            else -> CapabilityAuthenticity.UNSUPPORTED
        }

        ProtectedAction.TEMPLATE,
        ProtectedAction.AUDIT,
        ProtectedAction.AUDIT_DETAIL,
        -> CapabilityAuthenticity.UNVERIFIED
    }
}

fun ScanStage.toNfcFlowStage(): NfcFlowStage {
    return when (this) {
        ScanStage.IDLE -> NfcFlowStage.WAITING
        ScanStage.SCANNING -> NfcFlowStage.SCANNING
        ScanStage.TAG_DETECTED -> NfcFlowStage.PROCESSING
        ScanStage.SUCCESS -> NfcFlowStage.SUCCESS
        ScanStage.ERROR -> NfcFlowStage.FAILURE
    }
}

fun WriteStage.toNfcFlowStage(): NfcFlowStage {
    return when (this) {
        WriteStage.IDLE,
        WriteStage.READY,
        -> NfcFlowStage.WAITING

        WriteStage.WRITING -> NfcFlowStage.PROCESSING
        WriteStage.SUCCESS -> NfcFlowStage.SUCCESS
        WriteStage.ERROR -> NfcFlowStage.FAILURE
    }
}

fun FormatStage.toNfcFlowStage(): NfcFlowStage {
    return when (this) {
        FormatStage.IDLE -> NfcFlowStage.WAITING
        FormatStage.SCANNING -> NfcFlowStage.SCANNING
        FormatStage.SUCCESS -> NfcFlowStage.SUCCESS
        FormatStage.ERROR -> NfcFlowStage.FAILURE
    }
}

fun LockStage.toNfcFlowStage(): NfcFlowStage {
    return when (this) {
        LockStage.IDLE,
        LockStage.READY,
        -> NfcFlowStage.WAITING

        LockStage.LOCKING -> NfcFlowStage.PROCESSING
        LockStage.SUCCESS -> NfcFlowStage.SUCCESS
        LockStage.ERROR -> NfcFlowStage.FAILURE
    }
}

fun UnlockStage.toNfcFlowStage(): NfcFlowStage {
    return when (this) {
        UnlockStage.IDLE,
        UnlockStage.READY,
        -> NfcFlowStage.WAITING

        UnlockStage.SCANNING -> NfcFlowStage.SCANNING
        UnlockStage.SUCCESS -> NfcFlowStage.SUCCESS
        UnlockStage.ERROR -> NfcFlowStage.FAILURE
    }
}
