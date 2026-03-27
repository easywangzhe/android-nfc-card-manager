package com.opencode.nfccardmanager.core.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.IOException
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.TechType
import com.opencode.nfccardmanager.core.nfc.model.WriteCardRequest
import com.opencode.nfccardmanager.core.nfc.model.WriteCardResult

class NdefWriter {
    private val tagParser = TagParser()

    data class PrecheckResult(
        val canProceed: Boolean,
        val supportNdefWrite: Boolean,
        val isWritable: Boolean? = null,
        val capacityBytes: Int? = null,
        val requiredBytes: Int,
        val reason: String,
    )

    fun precheck(tag: Tag, request: WriteCardRequest): PrecheckResult {
        val messageSize = buildTextMessage(request.text).toByteArray().size

        Ndef.get(tag)?.let { ndef ->
            return runCatching {
                ndef.connect()
                val writable = ndef.isWritable
                val capacity = ndef.maxSize
                when {
                    !writable -> PrecheckResult(
                        canProceed = false,
                        supportNdefWrite = true,
                        isWritable = false,
                        capacityBytes = capacity,
                        requiredBytes = messageSize,
                        reason = "标签已识别为 NDEF，但当前不可写。",
                    )

                    capacity < messageSize -> PrecheckResult(
                        canProceed = false,
                        supportNdefWrite = true,
                        isWritable = true,
                        capacityBytes = capacity,
                        requiredBytes = messageSize,
                        reason = "标签容量不足：需要 ${messageSize} bytes，当前仅 ${capacity} bytes。",
                    )

                    else -> PrecheckResult(
                        canProceed = true,
                        supportNdefWrite = true,
                        isWritable = true,
                        capacityBytes = capacity,
                        requiredBytes = messageSize,
                        reason = "预检通过，可执行 NDEF 写入。",
                    )
                }
            }.getOrElse {
                PrecheckResult(
                    canProceed = false,
                    supportNdefWrite = true,
                    requiredBytes = messageSize,
                    reason = it.message ?: "NDEF 预检失败。",
                )
            }.also {
                runCatching { ndef.close() }
            }
        }

        NdefFormatable.get(tag)?.let {
            return PrecheckResult(
                canProceed = true,
                supportNdefWrite = true,
                isWritable = true,
                capacityBytes = null,
                requiredBytes = messageSize,
                reason = "标签支持 NDEF 格式化写入，可尝试先格式化后写入。",
            )
        }

        return PrecheckResult(
            canProceed = false,
            supportNdefWrite = false,
            requiredBytes = messageSize,
            reason = "当前标签既不是 Ndef，也不是 NdefFormatable，无法执行 NDEF 写入。",
        )
    }

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
                val detail = buildExceptionDetail(it)
                WriteCardResult(
                    cardInfo = cardInfo,
                    success = false,
                    message = detail,
                    payloadPreview = request.text,
                    writeStatus = "WRITE_ERROR",
                    writeReason = detail,
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
                val detail = buildExceptionDetail(it)
                WriteCardResult(
                    cardInfo = cardInfo,
                    success = false,
                    message = detail,
                    payloadPreview = request.text,
                    writeStatus = "FORMAT_ERROR",
                    writeReason = detail,
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
            writeStatus = "UNSUPPORTED_TAG",
            writeReason = "当前标签既不是 Ndef，也不是 NdefFormatable，无法按 NDEF 写入。",
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
            writeStatus = if (verified) "WRITE_SUCCESS" else "VERIFY_FAILED",
            writeReason = if (verified) {
                "NDEF 写入成功，且回读内容一致。"
            } else {
                "写入命令已执行，但回读校验未通过。"
            },
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

    private fun buildExceptionDetail(throwable: Throwable): String {
        return when (throwable) {
            is TagLostException -> "TagLostException: 写卡过程中标签已移开，请将卡片持续贴紧手机背部后重试"
            is IOException -> {
                val message = throwable.message?.takeIf { it.isNotBlank() }
                if (message == null) {
                    "IOException: 卡片连接中断或标签移开，请保持卡片稳定贴近手机背部后重试"
                } else {
                    "IOException: $message"
                }
            }
            else -> {
                val type = throwable::class.java.simpleName.ifBlank { "UnknownException" }
                val message = throwable.message?.takeIf { it.isNotBlank() } ?: "无详细错误信息"
                "$type: $message"
            }
        }
    }
}
