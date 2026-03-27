package com.opencode.nfccardmanager.core.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.FormatCardResult
import com.opencode.nfccardmanager.core.nfc.model.TechType

class NdefFormatter {
    fun format(tag: Tag): FormatCardResult {
        val uid = tag.id?.joinToString(separator = "") { byte -> "%02X".format(byte) } ?: "UNKNOWN"
        val cardInfo = CardInfo(
            uid = uid,
            techType = when {
                tag.techList.any { it.endsWith("Ndef") } -> TechType.NDEF
                tag.techList.any { it.endsWith("MifareUltralight") } -> TechType.ULTRALIGHT
                else -> TechType.UNKNOWN
            },
            summary = "格式化目标卡片",
        )

        Ndef.get(tag)?.let { ndef ->
            return runCatching {
                ndef.connect()
                require(ndef.isWritable) { "当前卡片已是 NDEF，但不可写，无法清空内容" }
                ndef.writeNdefMessage(emptyNdefMessage())
                FormatCardResult(
                    cardInfo = cardInfo,
                    success = true,
                    message = "格式化成功，已清空现有内容",
                    status = "CLEARED_NDEF",
                    reason = "当前卡片已是 NDEF 格式，已通过写入空 NDEF 消息清空内容。",
                )
            }.getOrElse {
                FormatCardResult(
                    cardInfo = cardInfo,
                    success = false,
                    message = it.message ?: "清空 NDEF 内容失败",
                    status = "CLEAR_ERROR",
                    reason = it.message ?: "当前卡片已是 NDEF，但清空内容时发生异常。",
                )
            }.also {
                runCatching { ndef.close() }
            }
        }

        val formatable = NdefFormatable.get(tag)
            ?: return FormatCardResult(
                cardInfo = cardInfo,
                success = false,
                message = "当前卡片不支持 NDEF 格式化",
                status = "UNSUPPORTED_TAG",
                reason = "卡片既不是 NDEF，也不支持 NdefFormatable。",
            )

        return runCatching {
            formatable.connect()
            formatable.format(emptyNdefMessage())
            FormatCardResult(
                cardInfo = cardInfo,
                success = true,
                message = "格式化成功",
                status = "FORMAT_SUCCESS",
                reason = "已将卡片格式化为 NDEF，并初始化为空内容，可继续进行写卡。",
            )
        }.getOrElse {
            FormatCardResult(
                cardInfo = cardInfo,
                success = false,
                message = it.message ?: "格式化失败",
                status = "FORMAT_ERROR",
                reason = it.message ?: "格式化过程中发生未知异常。",
            )
        }.also {
            runCatching { formatable.close() }
        }
    }

    private fun emptyNdefMessage(): NdefMessage {
        val emptyRecord = NdefRecord(
            NdefRecord.TNF_EMPTY,
            ByteArray(0),
            ByteArray(0),
            ByteArray(0),
        )
        return NdefMessage(arrayOf(emptyRecord))
    }
}
