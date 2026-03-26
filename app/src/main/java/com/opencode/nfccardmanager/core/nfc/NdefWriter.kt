package com.opencode.nfccardmanager.core.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.TechType
import com.opencode.nfccardmanager.core.nfc.model.WriteCardRequest
import com.opencode.nfccardmanager.core.nfc.model.WriteCardResult

class NdefWriter {
    private val tagParser = TagParser()

    fun writeText(tag: Tag, request: WriteCardRequest): WriteCardResult {
        val uid = tag.id?.joinToString(separator = "") { byte -> "%02X".format(byte) } ?: "UNKNOWN"
        val cardInfo = CardInfo(
            uid = uid,
            techType = if (tag.techList.any { it.endsWith("Ndef") }) TechType.NDEF else TechType.UNKNOWN,
            summary = "NDEF 写卡目标",
        )

        val message = buildTextMessage(request.text)

        Ndef.get(tag)?.let { ndef ->
            return runCatching {
                ndef.connect()
                require(ndef.isWritable) { "当前标签不可写" }
                require(ndef.maxSize >= message.toByteArray().size) { "标签容量不足，无法写入当前内容" }
                ndef.writeNdefMessage(message)
                verifyWrite(tag, cardInfo, request.text, "写卡成功")
            }.getOrElse {
                WriteCardResult(
                    cardInfo = cardInfo,
                    success = false,
                    message = it.message ?: "写卡失败",
                    payloadPreview = request.text,
                    verified = false,
                    verificationMessage = "未执行回读校验",
                )
            }.also {
                runCatching { ndef.close() }
            }
        }

        NdefFormatable.get(tag)?.let { formatable ->
            return runCatching {
                formatable.connect()
                formatable.format(message)
                verifyWrite(tag, cardInfo, request.text, "标签已格式化并写入成功")
            }.getOrElse {
                WriteCardResult(
                    cardInfo = cardInfo,
                    success = false,
                    message = it.message ?: "标签格式化写入失败",
                    payloadPreview = request.text,
                    verified = false,
                    verificationMessage = "未执行回读校验",
                )
            }.also {
                runCatching { formatable.close() }
            }
        }

        return WriteCardResult(
            cardInfo = cardInfo,
            success = false,
            message = "当前标签不支持 NDEF 写入",
            payloadPreview = request.text,
            verified = false,
            verificationMessage = "未执行回读校验",
        )
    }

    private fun verifyWrite(
        tag: Tag,
        cardInfo: CardInfo,
        expectedText: String,
        successMessage: String,
    ): WriteCardResult {
        val readBack = runCatching { tagParser.parse(tag) }.getOrNull()
        val actualText = readBack
            ?.records
            ?.firstOrNull()
            ?.payloadPreview
            ?.trim()
            .orEmpty()

        val verified = actualText == expectedText.trim()
        val verificationMessage = when {
            readBack == null -> "回读校验失败：无法重新读取标签内容"
            readBack.records.isEmpty() -> "回读校验失败：未读取到 NDEF 记录"
            verified -> "回读校验通过"
            else -> "回读校验失败：写入内容与回读内容不一致"
        }

        return WriteCardResult(
            cardInfo = cardInfo,
            success = verified,
            message = if (verified) successMessage else "写卡完成但校验失败",
            payloadPreview = expectedText,
            verified = verified,
            verificationMessage = verificationMessage,
        )
    }

    private fun buildTextMessage(text: String): NdefMessage {
        val languageCode = "zh"
        val languageBytes = languageCode.toByteArray(Charsets.US_ASCII)
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(1 + languageBytes.size + textBytes.size)
        payload[0] = languageBytes.size.toByte()
        languageBytes.copyInto(payload, destinationOffset = 1)
        textBytes.copyInto(payload, destinationOffset = 1 + languageBytes.size)

        val record = NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0),
            payload,
        )
        return NdefMessage(arrayOf(record))
    }
}
