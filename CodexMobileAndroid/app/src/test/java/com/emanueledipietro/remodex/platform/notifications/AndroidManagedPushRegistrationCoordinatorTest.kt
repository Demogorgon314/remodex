package com.emanueledipietro.remodex.platform.notifications

import com.emanueledipietro.remodex.data.connection.InMemorySecureStore
import com.emanueledipietro.remodex.data.connection.ScriptedRpcRelayWebSocketFactory
import com.emanueledipietro.remodex.data.connection.SecureConnectionCoordinator
import com.emanueledipietro.remodex.data.connection.SecureConnectionState
import com.emanueledipietro.remodex.data.connection.SecureStoreKeys
import com.emanueledipietro.remodex.data.connection.UnusedTrustedSessionResolver
import com.emanueledipietro.remodex.data.connection.createTestMacIdentity
import com.emanueledipietro.remodex.data.connection.createTestPairingPayload
import com.emanueledipietro.remodex.data.connection.firstString
import com.emanueledipietro.remodex.data.connection.jsonObjectOrNull
import com.emanueledipietro.remodex.model.RemodexManagedPushPlatform
import com.emanueledipietro.remodex.model.RemodexManagedPushProvider
import com.emanueledipietro.remodex.model.RemodexNotificationAuthorizationStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidManagedPushRegistrationCoordinatorTest {
    @Test
    fun `managed push coordinator syncs Android FCM registration through the secure bridge`() = runTest {
        val store = InMemorySecureStore()
        val macIdentity = createTestMacIdentity()
        val payload = createTestPairingPayload(
            macDeviceId = "mac-fcm",
            macIdentityPublicKey = macIdentity.publicKeyBase64,
        )
        val relayFactory = ScriptedRpcRelayWebSocketFactory(
            macDeviceId = payload.macDeviceId,
            macIdentity = macIdentity,
            requestHandlers = mapOf(
                "notifications/push/register" to { buildJsonObject { } },
            ),
        )
        val secureConnectionCoordinator = SecureConnectionCoordinator(
            store = store,
            trustedSessionResolver = UnusedTrustedSessionResolver,
            relayWebSocketFactory = relayFactory,
            scope = this,
        )
        val registrationCoordinator = AndroidManagedPushRegistrationCoordinator(
            secureConnectionCoordinator = secureConnectionCoordinator,
            secureStore = store,
            statusProvider = TestManagedPushStatusProvider(
                authorizationStatus = RemodexNotificationAuthorizationStatus.AUTHORIZED,
                alertsEnabled = true,
            ),
            tokenProvider = TestManagedPushTokenProvider(
                supported = true,
                token = "fcm-token-android-1",
            ),
            scope = backgroundScope,
            appEnvironment = "development",
        )

        try {
            secureConnectionCoordinator.rememberRelayPairing(payload)
            secureConnectionCoordinator.retryConnection()
            awaitSecureState(secureConnectionCoordinator, SecureConnectionState.ENCRYPTED)
            registrationCoordinator.refresh(force = true)
            awaitBridgeRequest(relayFactory, "notifications/push/register")

            val request = relayFactory.receivedRequests.last { it.method == "notifications/push/register" }
            val params = request.params?.jsonObjectOrNull
            assertEquals("fcm-token-android-1", params?.firstString("deviceToken"))
            assertEquals("authorized", params?.firstString("authorizationStatus"))
            assertEquals("android", params?.firstString("platform"))
            assertEquals("fcm", params?.firstString("pushProvider"))
            assertEquals(
                "fcm-token-",
                registrationCoordinator.state.value.deviceTokenPreview,
            )
            assertEquals(
                RemodexManagedPushPlatform.ANDROID,
                registrationCoordinator.state.value.platform,
            )
            assertEquals(
                RemodexManagedPushProvider.FCM,
                registrationCoordinator.state.value.pushProvider,
            )
            assertNull(registrationCoordinator.state.value.lastErrorMessage)
            assertEquals(
                "fcm-token-android-1",
                store.readString(SecureStoreKeys.PUSH_FCM_TOKEN),
            )
            assertEquals(
                true,
                store.readString(SecureStoreKeys.PUSH_REGISTRATION_SIGNATURE)?.contains("android|fcm"),
            )
        } finally {
            secureConnectionCoordinator.disconnect()
            advanceUntilIdle()
        }
    }

    @Test
    fun `managed push coordinator reports unsupported Firebase configuration without sending a request`() = runTest {
        val store = InMemorySecureStore()
        val statusProvider = TestManagedPushStatusProvider(
            authorizationStatus = RemodexNotificationAuthorizationStatus.NOT_DETERMINED,
            alertsEnabled = false,
        )
        val registrationCoordinator = AndroidManagedPushRegistrationCoordinator(
            secureConnectionCoordinator = SecureConnectionCoordinator(
                store = store,
                trustedSessionResolver = UnusedTrustedSessionResolver,
                relayWebSocketFactory = com.emanueledipietro.remodex.data.connection.UnexpectedRelayWebSocketFactory(),
                scope = backgroundScope,
            ),
            secureStore = store,
            statusProvider = statusProvider,
            tokenProvider = TestManagedPushTokenProvider(
                supported = false,
                token = null,
            ),
            scope = backgroundScope,
        )

        registrationCoordinator.refresh(force = true)
        advanceUntilIdle()

        assertEquals(false, registrationCoordinator.state.value.managedPushSupported)
        assertEquals(
            "Managed push is unavailable until Firebase is configured for this Android build.",
            registrationCoordinator.state.value.lastErrorMessage,
        )
    }

    @Test
    fun `managed push coordinator skips registration when encrypted snapshot outlives secure transport`() = runTest {
        val store = InMemorySecureStore()
        val macIdentity = createTestMacIdentity()
        val payload = createTestPairingPayload(
            macDeviceId = "mac-fcm-stale",
            macIdentityPublicKey = macIdentity.publicKeyBase64,
        )
        val relayFactory = ScriptedRpcRelayWebSocketFactory(
            macDeviceId = payload.macDeviceId,
            macIdentity = macIdentity,
            requestHandlers = mapOf(
                "notifications/push/register" to { buildJsonObject { } },
            ),
        )
        val secureConnectionCoordinator = SecureConnectionCoordinator(
            store = store,
            trustedSessionResolver = UnusedTrustedSessionResolver,
            relayWebSocketFactory = relayFactory,
            scope = this,
        )
        val registrationCoordinator = AndroidManagedPushRegistrationCoordinator(
            secureConnectionCoordinator = secureConnectionCoordinator,
            secureStore = store,
            statusProvider = TestManagedPushStatusProvider(
                authorizationStatus = RemodexNotificationAuthorizationStatus.AUTHORIZED,
                alertsEnabled = true,
            ),
            tokenProvider = TestManagedPushTokenProvider(
                supported = true,
                token = "fcm-token-android-stale",
            ),
            scope = backgroundScope,
            appEnvironment = "development",
        )

        try {
            secureConnectionCoordinator.rememberRelayPairing(payload)
            secureConnectionCoordinator.retryConnection()
            awaitSecureState(secureConnectionCoordinator, SecureConnectionState.ENCRYPTED)
            relayFactory.receivedRequests.clear()
            clearSecureTransportSession(secureConnectionCoordinator)

            registrationCoordinator.refresh(force = true)
            advanceUntilIdle()

            assertTrue(relayFactory.receivedRequests.none { it.method == "notifications/push/register" })
            assertNull(registrationCoordinator.state.value.lastErrorMessage)
        } finally {
            secureConnectionCoordinator.disconnect()
            advanceUntilIdle()
        }
    }

    private suspend fun TestScope.awaitSecureState(
        coordinator: SecureConnectionCoordinator,
        expectedState: SecureConnectionState,
    ) {
        repeat(40) {
            advanceUntilIdle()
            if (coordinator.state.value.secureState == expectedState) {
                return
            }
            Thread.sleep(10)
        }
        fail("Expected $expectedState but was ${coordinator.state.value.secureState}")
    }

    private suspend fun TestScope.awaitBridgeRequest(
        relayFactory: ScriptedRpcRelayWebSocketFactory,
        method: String,
    ) {
        repeat(40) {
            advanceUntilIdle()
            if (relayFactory.receivedRequests.any { it.method == method }) {
                return
            }
            Thread.sleep(10)
        }
        fail("Expected a secure bridge request for $method.")
    }

    private fun clearSecureTransportSession(
        coordinator: SecureConnectionCoordinator,
    ) {
        val field = coordinator.javaClass.getDeclaredField("secureSession")
        field.isAccessible = true
        field.set(coordinator, null)
    }
}

private class TestManagedPushStatusProvider(
    private val authorizationStatus: RemodexNotificationAuthorizationStatus,
    private val alertsEnabled: Boolean,
) : ManagedPushStatusProvider {
    private val refreshEventsFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    override val refreshEvents: Flow<Unit> = refreshEventsFlow

    override fun authorizationStatus(): RemodexNotificationAuthorizationStatus = authorizationStatus

    override fun alertsEnabled(): Boolean = alertsEnabled
}

private class TestManagedPushTokenProvider(
    private val supported: Boolean,
    private val token: String?,
) : ManagedPushTokenProvider {
    override fun isSupported(): Boolean = supported

    override suspend fun currentToken(): String? = token
}
