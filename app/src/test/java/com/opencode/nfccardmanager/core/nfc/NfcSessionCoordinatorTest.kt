package com.opencode.nfccardmanager.core.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NfcSessionCoordinatorTest {

    @Before
    fun setUp() {
        NfcSessionCoordinator.resetForTest()
    }

    @Test
    fun `request start grants first session with owner and token`() {
        val result = NfcSessionCoordinator.requestStart(
            owner = "write-screen",
            operation = NfcOperationType.WRITE,
            nowMillis = 123L,
        )

        val granted = result as NfcSessionStartResult.Granted
        assertEquals("write-screen", granted.session.owner)
        assertEquals(NfcOperationType.WRITE, granted.session.operation)
        assertEquals("write-1", granted.session.token)
        assertEquals(123L, granted.session.startedAtMillis)
        assertNotNull(NfcSessionCoordinator.activeSession())
    }

    @Test
    fun `request start rejects concurrent session and keeps original owner`() {
        val first = NfcSessionCoordinator.requestStart(
            owner = "scan-screen",
            operation = NfcOperationType.READ,
        ) as NfcSessionStartResult.Granted

        val second = NfcSessionCoordinator.requestStart(
            owner = "format-screen",
            operation = NfcOperationType.FORMAT,
        )

        val rejected = second as NfcSessionStartResult.Rejected
        assertEquals(first.session, rejected.activeSession)
        assertEquals(first.session, NfcSessionCoordinator.activeSession())
    }

    @Test
    fun `release only succeeds for matching owner and token`() {
        val granted = NfcSessionCoordinator.requestStart(
            owner = "lock-screen",
            operation = NfcOperationType.LOCK,
        ) as NfcSessionStartResult.Granted

        assertFalse(
            NfcSessionCoordinator.release(
                token = granted.session.token,
                owner = "other-screen",
            )
        )
        assertEquals(granted.session, NfcSessionCoordinator.activeSession())

        assertFalse(
            NfcSessionCoordinator.release(
                token = "stale-token",
                owner = granted.session.owner,
            )
        )
        assertEquals(granted.session, NfcSessionCoordinator.activeSession())

        assertTrue(
            NfcSessionCoordinator.release(
                token = granted.session.token,
                owner = granted.session.owner,
            )
        )
        assertNull(NfcSessionCoordinator.activeSession())
    }

    @Test
    fun `is owner reflects active session state`() {
        val granted = NfcSessionCoordinator.requestStart(
            owner = "unlock-screen",
            operation = NfcOperationType.UNLOCK,
        ) as NfcSessionStartResult.Granted

        assertTrue(
            NfcSessionCoordinator.isOwner(
                token = granted.session.token,
                owner = granted.session.owner,
            )
        )
        assertFalse(NfcSessionCoordinator.isOwner(token = granted.session.token, owner = "scan-screen"))

        NfcSessionCoordinator.release(
            token = granted.session.token,
            owner = granted.session.owner,
        )

        assertFalse(
            NfcSessionCoordinator.isOwner(
                token = granted.session.token,
                owner = granted.session.owner,
            )
        )
    }
}
