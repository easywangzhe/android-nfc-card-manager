package com.opencode.nfccardmanager.core.nfc

import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.util.Log
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.NdefRecordContent
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
        val (ndefMessageCount, records) = readNdef(tag)

        return ReadCardResult(
            cardInfo = cardInfo,
            capability = capability,
            ndefMessageCount = ndefMessageCount,
            records = records,
            rawTechList = tag.techList.toList(),
        )
    }

    private fun readNdef(tag: Tag): Pair<Int, List<NdefRecordContent>> {
        val ndef = Ndef.get(tag) ?: return 0 to emptyList()
        return runCatching {
            ndef.connect()
            val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
            if (message == null) {
                0 to emptyList()
            } else {
                1 to message.records.map { record -> record.toRecordContent() }
            }
        }.getOrElse {
            Log.w("TagParser", "读取 NDEF 失败", it)
            0 to emptyList()
        }.also {
            runCatching { ndef.close() }
        }
    }

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
