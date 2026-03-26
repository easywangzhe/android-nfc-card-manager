package com.opencode.nfccardmanager.core.nfc

import com.opencode.nfccardmanager.core.nfc.model.CardCapability
import com.opencode.nfccardmanager.core.nfc.model.CardInfo
import com.opencode.nfccardmanager.core.nfc.model.LockMode
import com.opencode.nfccardmanager.core.nfc.model.TechType

class CardCapabilityResolver {
    fun resolve(cardInfo: CardInfo): CardCapability {
        return when (cardInfo.techType) {
            TechType.NDEF -> CardCapability(
                canRead = true,
                canWrite = true,
                canLock = true,
                lockMode = LockMode.READ_ONLY_PERMANENT,
                canUnlock = false,
                requiresAuthForWrite = false,
            )

            TechType.ULTRALIGHT -> CardCapability(
                canRead = true,
                canWrite = true,
                canLock = true,
                lockMode = LockMode.PASSWORD_PROTECTED,
                canUnlock = true,
                requiresAuthForWrite = true,
            )

            TechType.MIFARE_CLASSIC -> CardCapability(
                canRead = true,
                canWrite = true,
                canLock = true,
                lockMode = LockMode.PASSWORD_PROTECTED,
                canUnlock = true,
                requiresAuthForWrite = true,
            )

            TechType.ISO_DEP,
            TechType.UNKNOWN,
            -> CardCapability(
                canRead = true,
                canWrite = false,
                canLock = false,
                lockMode = LockMode.NONE,
                canUnlock = false,
                requiresAuthForWrite = false,
            )
        }
    }
}
