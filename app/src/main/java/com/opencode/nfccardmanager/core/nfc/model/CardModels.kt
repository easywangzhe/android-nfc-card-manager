package com.opencode.nfccardmanager.core.nfc.model

enum class TechType {
    NDEF,
    MIFARE_CLASSIC,
    ULTRALIGHT,
    ISO_DEP,
    UNKNOWN,
}

enum class LockMode {
    NONE,
    READ_ONLY_PERMANENT,
    PASSWORD_PROTECTED,
}

data class CardInfo(
    val uid: String,
    val techType: TechType,
    val summary: String? = null,
)

data class CardCapability(
    val canRead: Boolean,
    val canWrite: Boolean,
    val canLock: Boolean,
    val lockMode: LockMode,
    val canUnlock: Boolean,
    val requiresAuthForWrite: Boolean,
)

fun CardInfo.maskedUid(): String {
    if (uid.length <= 4) return uid
    return uid.take(2) + "****" + uid.takeLast(2)
}
