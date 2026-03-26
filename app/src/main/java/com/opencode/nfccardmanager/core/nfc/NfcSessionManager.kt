package com.opencode.nfccardmanager.core.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build

class NfcSessionManager(
    private val activity: Activity,
) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    fun isNfcAvailable(): Boolean = adapter != null

    fun isNfcEnabled(): Boolean = adapter?.isEnabled == true

    fun startReaderMode(callback: NfcAdapter.ReaderCallback): Result<Unit> {
        val currentAdapter = adapter ?: return Result.failure(IllegalStateException("当前设备不支持 NFC"))
        if (!isActivityUsable()) {
            return Result.failure(IllegalStateException("当前页面未处于可用状态，无法启动 NFC 扫描"))
        }

        return runCatching {
            currentAdapter.enableReaderMode(
                activity,
                callback,
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null,
            )
        }
    }

    fun stopReaderMode() {
        if (!isActivityUsable()) return
        runCatching {
            adapter?.disableReaderMode(activity)
        }
    }

    fun createReaderCallback(onTagDiscovered: (Tag) -> Unit): NfcAdapter.ReaderCallback {
        return NfcAdapter.ReaderCallback { tag ->
            onTagDiscovered(tag)
        }
    }

    private fun isActivityUsable(): Boolean {
        if (activity.isFinishing) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) {
            return false
        }
        return true
    }
}
