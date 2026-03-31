package com.opencode.nfccardmanager.core.nfc

import com.opencode.nfccardmanager.core.nfc.model.CapabilityAuthenticity
import com.opencode.nfccardmanager.core.nfc.model.NfcFlowStage
import com.opencode.nfccardmanager.core.nfc.model.presentation
import com.opencode.nfccardmanager.core.nfc.model.toNfcFlowStage
import com.opencode.nfccardmanager.feature.format.FormatStage
import com.opencode.nfccardmanager.feature.lock.LockStage
import com.opencode.nfccardmanager.feature.scan.ScanStage
import com.opencode.nfccardmanager.feature.unlock.UnlockStage
import com.opencode.nfccardmanager.feature.write.WriteStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcFlowContractTest {

    @Test
    fun `scan and format stages map to shared flow stages`() {
        assertEquals(NfcFlowStage.WAITING, ScanStage.IDLE.toNfcFlowStage())
        assertEquals(NfcFlowStage.SCANNING, ScanStage.SCANNING.toNfcFlowStage())
        assertEquals(NfcFlowStage.PROCESSING, ScanStage.TAG_DETECTED.toNfcFlowStage())
        assertEquals(NfcFlowStage.FAILURE, ScanStage.ERROR.toNfcFlowStage())

        assertEquals(NfcFlowStage.WAITING, FormatStage.IDLE.toNfcFlowStage())
        assertEquals(NfcFlowStage.SCANNING, FormatStage.SCANNING.toNfcFlowStage())
        assertEquals(NfcFlowStage.SUCCESS, FormatStage.SUCCESS.toNfcFlowStage())
    }

    @Test
    fun `write lock and unlock stages map to shared flow stages`() {
        assertEquals(NfcFlowStage.WAITING, WriteStage.IDLE.toNfcFlowStage())
        assertEquals(NfcFlowStage.WAITING, WriteStage.READY.toNfcFlowStage())
        assertEquals(NfcFlowStage.PROCESSING, WriteStage.WRITING.toNfcFlowStage())
        assertEquals(NfcFlowStage.SUCCESS, WriteStage.SUCCESS.toNfcFlowStage())

        assertEquals(NfcFlowStage.WAITING, LockStage.READY.toNfcFlowStage())
        assertEquals(NfcFlowStage.PROCESSING, LockStage.LOCKING.toNfcFlowStage())

        assertEquals(NfcFlowStage.WAITING, UnlockStage.READY.toNfcFlowStage())
        assertEquals(NfcFlowStage.SCANNING, UnlockStage.SCANNING.toNfcFlowStage())
    }

    @Test
    fun `shared stage presentation exposes stable semantics`() {
        val waiting = NfcFlowStage.WAITING.presentation()
        val success = NfcFlowStage.SUCCESS.presentation()
        val failure = NfcFlowStage.FAILURE.presentation()

        assertEquals("等待操作", waiting.title)
        assertFalse(waiting.isBusy)
        assertFalse(waiting.isTerminal)

        assertEquals("操作成功", success.title)
        assertTrue(success.isTerminal)

        assertEquals("操作失败", failure.title)
        assertTrue(failure.isTerminal)
    }

    @Test
    fun `capability authenticity presentation distinguishes real and demo states`() {
        val supported = CapabilityAuthenticity.SUPPORTED.presentation()
        val demoOnly = CapabilityAuthenticity.DEMO_ONLY.presentation()
        val unsupported = CapabilityAuthenticity.UNSUPPORTED.presentation()

        assertEquals("已验证支持", supported.label)
        assertTrue(supported.detail.contains("真实卡片能力模型"))

        assertEquals("仅演示", demoOnly.label)
        assertTrue(demoOnly.detail.contains("不代表设备一定具备真实能力"))

        assertEquals("未支持", unsupported.label)
    }
}
