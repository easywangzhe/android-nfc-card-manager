package com.opencode.nfccardmanager.core.nfc

import android.nfc.Tag
import android.nfc.tech.Ndef
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.LockCardResult
import com.opencode.nfccardmanager.core.nfc.model.TechType

class NdefLocker {
    fun makeReadOnly(tag: Tag): LockCardResult {
        val uid = tag.id?.joinToString(separator = "") { byte -> "%02X".format(byte) } ?: "UNKNOWN"
        val cardInfo = CardInfo(
            uid = uid,
            techType = if (tag.techList.any { it.endsWith("Ndef") }) TechType.NDEF else TechType.UNKNOWN,
            summary = "NDEF 锁卡目标",
        )

        val ndef = Ndef.get(tag)
            ?: return LockCardResult(
                cardInfo = cardInfo,
                success = false,
                message = "当前标签不支持 NDEF 锁定",
                lockMode = com.opencode.nfccardmanager.core.nfc.model.LockMode.READ_ONLY_PERMANENT,
                verified = false,
                verificationMessage = "无法执行只读校验",
            )

        return runCatching {
            ndef.connect()
            require(ndef.isWritable) { "当前标签已经不可写，可能已被锁定" }
            require(ndef.canMakeReadOnly()) { "当前标签不支持切换为只读" }
            val success = ndef.makeReadOnly()
            require(success) { "锁卡失败，标签未进入只读状态" }

            val verified = !ndef.isWritable
            LockCardResult(
                cardInfo = cardInfo,
                success = verified,
                message = if (verified) "锁卡成功，标签已进入永久只读" else "锁卡完成但只读校验失败",
                lockMode = com.opencode.nfccardmanager.core.nfc.model.LockMode.READ_ONLY_PERMANENT,
                irreversible = true,
                verified = verified,
                verificationMessage = if (verified) "已校验标签不可写" else "锁卡后标签仍可写",
            )
        }.getOrElse {
            LockCardResult(
                cardInfo = cardInfo,
                success = false,
                message = it.message ?: "锁卡失败",
                lockMode = com.opencode.nfccardmanager.core.nfc.model.LockMode.READ_ONLY_PERMANENT,
                irreversible = true,
                verified = false,
                verificationMessage = "未通过只读校验",
            )
        }.also {
            runCatching { ndef.close() }
        }
    }
}
