package com.opencode.nfccardmanager.core.nfc

import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.MifareUltralight
import android.util.Log
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.NdefRecordContent
import com.opencode.nfccardmanager.core.nfc.model.ReadDetailItem
import com.opencode.nfccardmanager.core.nfc.model.ReadCardResult
import com.opencode.nfccardmanager.core.nfc.model.TechType
import java.nio.charset.Charset

class TagParser {
    private val capabilityResolver = CardCapabilityResolver()

    fun parse(tag: Tag): ReadCardResult {
        val techType = resolveTechType(tag.techList)
        val uid = tag.id?.joinToString(separator = "") { byte -> "%02X".format(byte) } ?: "UNKNOWN"
        val cardInfo = CardInfo(
            uid = uid,
            techType = techType,
            summary = "已识别 ${techType.name} 卡片",
        )
        val capability = capabilityResolver.resolve(cardInfo)
        val ndefReadResult = readNdef(tag)
        val ndefMessageCount = ndefReadResult.messageCount
        val records = ndefReadResult.records
        val isNdefTag = tag.techList.any { it.endsWith("Ndef") }
        val detailItems = buildDetailItems(tag, isNdefTag)
        val readStatus = when {
            !isNdefTag -> "NON_NDEF"
            ndefReadResult.errorMessage.isNotBlank() -> "READ_ERROR"
            records.isEmpty() -> "EMPTY_NDEF"
            else -> "READ_SUCCESS"
        }
        val readReason = when (readStatus) {
            "NON_NDEF" -> "当前卡片未暴露 Ndef 技术栈，不能按 NDEF 内容直接读取。"
            "READ_ERROR" -> ndefReadResult.errorMessage
            "EMPTY_NDEF" -> "当前卡片支持 NDEF，但未读取到任何消息记录，可能是空标签。"
            else -> "已成功读取到 NDEF 内容。"
        }

        return ReadCardResult(
            cardInfo = cardInfo,
            capability = capability,
            ndefMessageCount = ndefMessageCount,
            records = records,
            detailItems = detailItems,
            rawTechList = tag.techList.toList(),
            isNdefTag = isNdefTag,
            readStatus = readStatus,
            readReason = readReason,
            debugMessage = buildString {
                append("techCount=${tag.techList.size}")
                append("; isNdefTag=$isNdefTag")
                append("; readStatus=$readStatus")
                append("; ndefMessageCount=$ndefMessageCount")
                append("; recordCount=${records.size}")
                append("; detailCount=${detailItems.size}")
                if (ndefReadResult.errorMessage.isNotBlank()) {
                    append("; error=${ndefReadResult.errorMessage}")
                }
            },
        )
    }

    private fun buildDetailItems(tag: Tag, isNdefTag: Boolean): List<ReadDetailItem> {
        val items = mutableListOf<ReadDetailItem>()

        items += ReadDetailItem("UID 长度", "${tag.id?.size ?: 0} 字节")
        items += ReadDetailItem("Tech 数量", tag.techList.size.toString())

        NfcA.get(tag)?.let { nfcA ->
            val atqa = nfcA.atqa?.joinToString(" ") { "%02X".format(it) }.orEmpty().ifBlank { "未知" }
            items += ReadDetailItem("NfcA.ATQA", atqa)
            items += ReadDetailItem("NfcA.SAK", "0x%02X".format(nfcA.sak))
            items += ReadDetailItem("NfcA 最大收发", "${nfcA.maxTransceiveLength} bytes")
        }

        MifareUltralight.get(tag)?.let { ultra ->
            val type = when (ultra.type) {
                MifareUltralight.TYPE_ULTRALIGHT -> "ULTRALIGHT"
                MifareUltralight.TYPE_ULTRALIGHT_C -> "ULTRALIGHT_C"
                else -> "UNKNOWN"
            }
            items += ReadDetailItem("Ultralight 类型", type)
            items += ReadDetailItem("Ultralight 最大收发", "${ultra.maxTransceiveLength} bytes")
        }

        IsoDep.get(tag)?.let { isoDep ->
            items += ReadDetailItem("IsoDep 最大收发", "${isoDep.maxTransceiveLength} bytes")
            items += ReadDetailItem("IsoDep 超时", "${isoDep.timeout} ms")
        }

        Ndef.get(tag)?.let { ndef ->
            items += ReadDetailItem("NDEF 技术", if (isNdefTag) "支持" else "未识别")
            items += ReadDetailItem("NDEF 类型", ndef.type ?: "未知")
            items += ReadDetailItem("NDEF 容量", "${ndef.maxSize} bytes")
            items += ReadDetailItem("NDEF 可写", if (ndef.isWritable) "是" else "否")
            items += ReadDetailItem("可设只读", if (ndef.canMakeReadOnly()) "是" else "否")
        }

        return items
    }

    private fun readNdef(tag: Tag): NdefReadResult {
        val ndef = Ndef.get(tag) ?: return NdefReadResult()
        return runCatching {
            ndef.connect()
            val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
            if (message == null) {
                NdefReadResult(
                    messageCount = 0,
                    records = emptyList(),
                    errorMessage = "NDEF 连接成功，但标签未返回任何 NDEF 消息。",
                )
            } else {
                val parsedRecords = message.records
                    .filterNot { record ->
                        record.tnf == NdefRecord.TNF_EMPTY.toShort() &&
                            record.type.isEmpty() &&
                            record.payload.isEmpty()
                    }
                    .map { record -> record.toRecordContent() }
                NdefReadResult(
                    messageCount = if (parsedRecords.isEmpty()) 0 else 1,
                    records = parsedRecords,
                )
            }
        }.getOrElse {
            Log.w("TagParser", "读取 NDEF 失败", it)
            NdefReadResult(
                messageCount = 0,
                records = emptyList(),
                errorMessage = it.message ?: "读取 NDEF 时发生未知异常。",
            )
        }.also {
            runCatching { ndef.close() }
        }
    }

    private data class NdefReadResult(
        val messageCount: Int = 0,
        val records: List<NdefRecordContent> = emptyList(),
        val errorMessage: String = "",
    )

    private fun NdefRecord.toRecordContent(): NdefRecordContent {
        val typeName = type?.toString(Charsets.UTF_8).orEmpty()
        return NdefRecordContent(
            tnf = tnf,
            type = if (typeName.isBlank()) fallbackTypeName() else typeName,
            payloadPreview = payloadPreview(),
        )
    }

    private fun NdefRecord.fallbackTypeName(): String {
        return when (tnf) {
            NdefRecord.TNF_WELL_KNOWN.toShort() -> "WELL_KNOWN"
            NdefRecord.TNF_MIME_MEDIA.toShort() -> "MIME_MEDIA"
            NdefRecord.TNF_ABSOLUTE_URI.toShort() -> "ABSOLUTE_URI"
            else -> "UNKNOWN"
        }
    }

    private fun NdefRecord.payloadPreview(): String {
        return when {
            tnf == NdefRecord.TNF_WELL_KNOWN.toShort() && type.contentEquals(NdefRecord.RTD_TEXT) -> {
                payload.parseTextPayload()
            }

            tnf == NdefRecord.TNF_WELL_KNOWN.toShort() && type.contentEquals(NdefRecord.RTD_URI) -> {
                payload.parseUriPayload()
            }

            else -> payload.toHexPreview()
        }
    }

    private fun ByteArray.parseTextPayload(): String {
        if (isEmpty()) return ""
        val status = this[0].toInt()
        val languageCodeLength = status and 0x3F
        val encoding = if (status and 0x80 == 0) Charsets.UTF_8 else Charset.forName("UTF-16")
        return copyOfRange(1 + languageCodeLength, size).toString(encoding)
    }

    private fun ByteArray.parseUriPayload(): String {
        if (isEmpty()) return ""
        val prefixMap = listOf(
            "",
            "http://www.",
            "https://www.",
            "http://",
            "https://",
            "tel:",
            "mailto:",
            "ftp://anonymous:anonymous@",
            "ftp://ftp.",
            "ftps://",
            "sftp://",
            "smb://",
            "nfs://",
            "ftp://",
            "dav://",
            "news:",
            "telnet://",
        )
        val prefix = prefixMap.getOrElse(this[0].toInt() and 0xFF) { "" }
        return prefix + copyOfRange(1, size).toString(Charsets.UTF_8)
    }

    private fun ByteArray.toHexPreview(): String {
        return joinToString(separator = " ") { "%02X".format(it) }
            .take(120)
            .ifBlank { "<empty>" }
    }

    private fun resolveTechType(techList: Array<String>): TechType {
        return when {
            techList.any { it.endsWith("Ndef") } -> TechType.NDEF
            techList.any { it.endsWith("MifareClassic") } -> TechType.MIFARE_CLASSIC
            techList.any { it.endsWith("MifareUltralight") } -> TechType.ULTRALIGHT
            techList.any { it.endsWith("IsoDep") } -> TechType.ISO_DEP
            else -> TechType.UNKNOWN
        }
    }
}
