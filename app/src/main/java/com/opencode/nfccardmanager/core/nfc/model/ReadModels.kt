package com.opencode.nfccardmanager.core.nfc.model

data class NdefRecordContent(
    val tnf: Short,
    val type: String,
    val payloadPreview: String,
)

data class ReadCardResult(
    val cardInfo: CardInfo,
    val capability: CardCapability,
    val ndefMessageCount: Int = 0,
    val records: List<NdefRecordContent> = emptyList(),
    val rawTechList: List<String> = emptyList(),
)
