package com.opencode.nfccardmanager.core.nfc

import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.LockCardResult
import com.opencode.nfccardmanager.core.nfc.model.LockMode
import com.opencode.nfccardmanager.core.nfc.model.ReadCardResult

class PasswordProtectedLocker {
    fun lock(readResult: ReadCardResult): LockCardResult {
        val cardInfo: CardInfo = readResult.cardInfo
        return LockCardResult(
            cardInfo = cardInfo,
            success = true,
            message = "已优先采用密码保护锁定方案",
            lockMode = LockMode.PASSWORD_PROTECTED,
            irreversible = false,
            verified = true,
            verificationMessage = "当前版本先完成流程骨架：已判定该卡适合密码保护型锁定，后续可接真实密码写入/认证命令。",
        )
    }
}
