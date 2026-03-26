package com.opencode.nfccardmanager.core.nfc

import com.opencode.nfccardmanager.core.nfc.model.LockMode
import com.opencode.nfccardmanager.core.nfc.model.ReadCardResult
import com.opencode.nfccardmanager.core.nfc.model.UnlockCardRequest
import com.opencode.nfccardmanager.core.nfc.model.UnlockCardResult

class UnlockExecutor {
    fun execute(
        readResult: ReadCardResult,
        request: UnlockCardRequest,
    ): UnlockCardResult {
        val cardInfo = readResult.cardInfo
        val capability = readResult.capability

        if (!capability.canUnlock || capability.lockMode != LockMode.PASSWORD_PROTECTED) {
            return UnlockCardResult(
                cardInfo = cardInfo,
                success = false,
                message = "当前卡片不支持通用解锁",
                verificationMessage = "若为 NDEF 永久只读锁定，则通常不可逆，无法解锁",
            )
        }

        if (request.credential != "123456") {
            return UnlockCardResult(
                cardInfo = cardInfo,
                success = false,
                message = "解锁凭据错误",
                verificationMessage = "未通过凭据校验，未执行后续解锁流程",
            )
        }

        return UnlockCardResult(
            cardInfo = cardInfo,
            success = true,
            message = "解锁流程骨架执行成功",
            verificationMessage = "当前版本仅完成密码保护型卡片的流程演示，未接入真实底层解除写保护命令",
        )
    }
}
