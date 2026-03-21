package com.emanueledipietro.remodex.data.connection

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingQrValidatorTest {
    @Test
    fun versionMismatchRequiresBridgeUpdate() {
        val result = validatePairingQrCode(
            code = pairingQrCode(
                version = remodexPairingQrVersion + 1,
                expiresAt = 1_900_000_000_000,
            ),
        )

        val prompt = (result as PairingQrValidationResult.BridgeUpdateRequired).prompt
        assertEquals("Update Remodex on your Mac before scanning", prompt.title)
        assertEquals("npm install -g remodex@latest", prompt.command)
        assertTrue(prompt.message.contains("different Remodex npm version"))
    }

    @Test
    fun legacyBridgePayloadRequiresUpdate() {
        val result = validatePairingQrCode("""{"relay":"wss://relay.example","sessionId":"session-123"}""")

        val prompt = (result as PairingQrValidationResult.BridgeUpdateRequired).prompt
        assertTrue(prompt.message.contains("older Remodex bridge"))
    }

    @Test
    fun validPayloadReturnsSuccess() {
        val result = validatePairingQrCode(
            code = pairingQrCode(
                version = remodexPairingQrVersion,
                expiresAt = 1_900_000_000_000,
            ),
            now = Instant.ofEpochSecond(1_800_000_000),
        )

        val payload = (result as PairingQrValidationResult.Success).payload
        assertEquals("session-123", payload.sessionId)
        assertEquals("wss://relay.example", payload.relay)
    }

    @Test
    fun expiredPayloadReturnsScanError() {
        val result = validatePairingQrCode(
            code = pairingQrCode(
                version = remodexPairingQrVersion,
                expiresAt = 1_700_000_000_000,
            ),
            now = Instant.ofEpochSecond(1_800_000_000),
        )

        val message = (result as PairingQrValidationResult.ScanError).message
        assertEquals("This pairing QR code has expired. Generate a new one from the Mac bridge.", message)
    }

    @Test
    fun missingRelayReturnsScanError() {
        val result = validatePairingQrCode(
            code = """{"v":2,"relay":" ","sessionId":"session-123","macDeviceId":"mac-123","macIdentityPublicKey":"pub-key","expiresAt":1900000000000}""",
            now = Instant.ofEpochSecond(1_800_000_000),
        )

        val message = (result as PairingQrValidationResult.ScanError).message
        assertEquals("QR code is missing the relay URL. Re-generate the code from the bridge.", message)
    }

    private fun pairingQrCode(
        version: Int,
        expiresAt: Long,
    ): String {
        return """{"v":$version,"relay":"wss://relay.example","sessionId":"session-123","macDeviceId":"mac-123","macIdentityPublicKey":"pub-key","expiresAt":$expiresAt}"""
    }
}
