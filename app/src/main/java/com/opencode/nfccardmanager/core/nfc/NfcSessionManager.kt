package com.opencode.nfccardmanager.core.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build

data class ReaderModeSession(
    val token: String,
    val owner: String,
    val operation: NfcOperationType,
)

class NfcSessionManager(
    private val activity: Activity,
) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    fun isNfcAvailable(): Boolean = adapter != null

    fun isNfcEnabled(): Boolean = adapter?.isEnabled == true

    fun requestReaderMode(
        owner: String,
        operation: NfcOperationType,
        callback: NfcAdapter.ReaderCallback,
    ): Result<ReaderModeSession> {
        val currentAdapter = adapter ?: return Result.failure(IllegalStateException("当前设备不支持 NFC"))
        if (!isNfcEnabled()) {
            return Result.failure(IllegalStateException("NFC 未开启，请先打开系统 NFC 开关"))
        }
        if (!isActivityUsable()) {
            return Result.failure(IllegalStateException("当前页面未处于可用状态，无法启动 NFC 扫描"))
        }

        return when (val result = NfcSessionCoordinator.requestStart(owner = owner, operation = operation)) {
            is NfcSessionStartResult.Rejected -> Result.failure(
                IllegalStateException(
                    "已有进行中的 ${result.activeSession.operation.name.lowercase()} 会话，请先完成当前操作再继续"
                )
            )

            is NfcSessionStartResult.Granted -> {
                val session = ReaderModeSession(
                    token = result.session.token,
                    owner = result.session.owner,
                    operation = result.session.operation,
                )

                runCatching {
                    currentAdapter.enableReaderMode(
                        activity,
                        callback,
                        NfcAdapter.FLAG_READER_NFC_A or
                            NfcAdapter.FLAG_READER_NFC_B or
                            NfcAdapter.FLAG_READER_NFC_F or
                            NfcAdapter.FLAG_READER_NFC_V,
                        null,
                    )
                    session
                }.onFailure {
                    NfcSessionCoordinator.release(session.token, session.owner)
                }
            }
        }
    }

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
                    NfcAdapter.FLAG_READER_NFC_V,
                null,
            )
        }
    }

    fun releaseReaderMode(session: ReaderModeSession?) {
        if (session == null) return
        stopReaderMode()
        NfcSessionCoordinator.release(token = session.token, owner = session.owner)
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
