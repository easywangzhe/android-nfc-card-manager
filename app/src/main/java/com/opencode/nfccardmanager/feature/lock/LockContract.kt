package com.opencode.nfccardmanager.feature.lock

import com.opencode.nfccardmanager.core.nfc.model.LockMode
import com.opencode.nfccardmanager.core.nfc.model.LockCardResult

enum class LockStage {
    IDLE,
    READY,
    LOCKING,
    SUCCESS,
    ERROR,
}

data class LockUiState(
    val riskAcknowledged: Boolean = false,
    val confirmText: String = "",
    val stage: LockStage = LockStage.IDLE,
    val recommendedMode: LockMode = LockMode.NONE,
    val modeHint: String = "系统会优先尝试密码保护锁定；若卡片不支持，则降级为永久只读锁定。",
    val message: String = "确认风险后点击开始锁卡，系统才会进入识别与锁定流程。",
    val result: LockCardResult? = null,
)
