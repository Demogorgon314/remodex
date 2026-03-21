package com.emanueledipietro.remodex.data.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureConnectionPersistenceTest {
    @Test
    fun `remember relay pairing persists reconnect prerequisites`() {
        val store = InMemorySecureStore()
        val macIdentity = createTestMacIdentity()
        val payload = createTestPairingPayload(
            macDeviceId = "mac-remembered",
            macIdentityPublicKey = macIdentity.publicKeyBase64,
            sessionId = "session-persisted",
        )

        SecureCrypto.rememberRelayPairing(store, payload)

        val pairingState = SecureCrypto.relayPairingStateFromStore(store)
        requireNotNull(pairingState)
        assertEquals(payload.sessionId, pairingState.sessionId)
        assertEquals(payload.relay, pairingState.relayUrl)
        assertEquals(payload.macDeviceId, pairingState.macDeviceId)
        assertEquals(payload.macIdentityPublicKey, pairingState.macIdentityPublicKey)
        assertTrue(pairingState.shouldForceQrBootstrap)
    }

    @Test
    fun `trusted mac registry round trips through secure storage`() {
        val store = InMemorySecureStore()
        val macIdentity = createTestMacIdentity()
        val registry = TrustedMacRegistry(
            records = mapOf(
                "mac-known" to TrustedMacRecord(
                    macDeviceId = "mac-known",
                    macIdentityPublicKey = macIdentity.publicKeyBase64,
                    lastPairedAtEpochMs = 1_713_000_000_000,
                    relayUrl = "ws://127.0.0.1:7777/relay",
                    displayName = "Work Mac",
                ),
            ),
        )

        SecureCrypto.writeTrustedMacRegistry(store, registry, "mac-known")

        assertEquals(registry, SecureCrypto.trustedMacRegistryFromStore(store))
        assertEquals("mac-known", store.readString(SecureStoreKeys.LAST_TRUSTED_MAC_DEVICE_ID))
    }
}
