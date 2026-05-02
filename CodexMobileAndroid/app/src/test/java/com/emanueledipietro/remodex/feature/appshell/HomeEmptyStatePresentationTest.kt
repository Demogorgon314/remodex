package com.emanueledipietro.remodex.feature.appshell

import com.emanueledipietro.remodex.data.connection.SecureConnectionSnapshot
import com.emanueledipietro.remodex.data.connection.SecureConnectionState
import com.emanueledipietro.remodex.model.RemodexConnectionPhase
import com.emanueledipietro.remodex.model.RemodexConnectionStatus
import com.emanueledipietro.remodex.model.RemodexTrustedMacPresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeEmptyStatePresentationTest {
    @Test
    fun `saved pairing idle state stays offline while keeping reconnect actions`() {
        val presentation = AppUiState(
            connectionStatus = RemodexConnectionStatus(RemodexConnectionPhase.DISCONNECTED),
            connectionMessage = "Saved pairing ready.",
            recoveryState = SecureConnectionSnapshot(
                phaseMessage = "Saved pairing ready.",
                secureState = SecureConnectionState.TRUSTED_MAC,
            ),
            trustedMac = trustedMac(),
        ).toHomeEmptyStatePresentation()

        assertEquals(HomeEmptyStateStatus.OFFLINE, presentation.status)
        assertEquals("Reconnect", presentation.primaryTitle)
        assertFalse(presentation.isPrimaryBusy)
        assertTrue(presentation.primaryEnabled)
        assertTrue(presentation.showsScanNewQrAction)
        assertTrue(presentation.showsForgetPairAction)
    }

    @Test
    fun `trusted reconnect attempt becomes busy and disables the primary button`() {
        val presentation = AppUiState(
            connectionStatus = RemodexConnectionStatus(RemodexConnectionPhase.RETRYING, attempt = 2),
            connectionMessage = "Resolving the live session for your trusted computer before reconnecting the Android socket.",
            recoveryState = SecureConnectionSnapshot(
                phaseMessage = "Resolving the live session for your trusted computer before reconnecting the Android socket.",
                secureState = SecureConnectionState.RECONNECTING,
                attempt = 2,
            ),
            trustedMac = trustedMac(),
        ).toHomeEmptyStatePresentation()

        assertEquals(HomeEmptyStateStatus.CONNECTING, presentation.status)
        assertEquals("Reconnecting...", presentation.primaryTitle)
        assertTrue(presentation.isPrimaryBusy)
        assertFalse(presentation.primaryEnabled)
        assertTrue(presentation.showsScanNewQrAction)
        assertTrue(presentation.showsForgetPairAction)
    }

    @Test
    fun `auto reconnect backoff does not keep the shell in a busy state`() {
        val presentation = AppUiState(
            connectionStatus = RemodexConnectionStatus(RemodexConnectionPhase.RETRYING, attempt = 1),
            connectionMessage = "Reconnecting...",
            recoveryState = SecureConnectionSnapshot(
                phaseMessage = "Saved pairing ready.",
                secureState = SecureConnectionState.TRUSTED_MAC,
                attempt = 1,
            ),
            trustedMac = trustedMac(),
        ).toHomeEmptyStatePresentation()

        assertEquals(HomeEmptyStateStatus.OFFLINE, presentation.status)
        assertEquals("Reconnect", presentation.primaryTitle)
        assertFalse(presentation.isPrimaryBusy)
        assertTrue(presentation.primaryEnabled)
    }

    @Test
    fun `disconnected state without a saved pairing leads with scan qr`() {
        val presentation = AppUiState(
            connectionStatus = RemodexConnectionStatus(RemodexConnectionPhase.DISCONNECTED),
            connectionMessage = "在电脑上运行 remodex up, 然后用 QR code 配对这台 Android 设备.",
            recoveryState = SecureConnectionSnapshot(
                secureState = SecureConnectionState.NOT_PAIRED,
            ),
        ).toHomeEmptyStatePresentation()

        assertEquals(HomeEmptyStateStatus.OFFLINE, presentation.status)
        assertEquals("Scan QR Code", presentation.primaryTitle)
        assertFalse(presentation.showsForgetPairAction)
        assertFalse(presentation.showsScanNewQrAction)
    }

    private fun trustedMac(): RemodexTrustedMacPresentation {
        return RemodexTrustedMacPresentation(
            deviceId = "mac-1",
            title = "Saved Pair",
            name = "Kai-Wang---MBP-lan.lan",
            detail = "Reconnecting securely · E8FF06BB3E7B",
        )
    }
}
