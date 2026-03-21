package com.emanueledipietro.remodex.data.threads

import com.emanueledipietro.remodex.data.connection.InMemorySecureStore
import com.emanueledipietro.remodex.data.connection.ScriptedRpcRelayWebSocketFactory
import com.emanueledipietro.remodex.data.connection.SecureConnectionCoordinator
import com.emanueledipietro.remodex.data.connection.SecureConnectionState
import com.emanueledipietro.remodex.data.connection.UnusedTrustedSessionResolver
import com.emanueledipietro.remodex.data.connection.createTestMacIdentity
import com.emanueledipietro.remodex.data.connection.createTestPairingPayload
import com.emanueledipietro.remodex.data.connection.firstArray
import com.emanueledipietro.remodex.data.connection.firstString
import com.emanueledipietro.remodex.data.connection.jsonObjectOrNull
import com.emanueledipietro.remodex.model.RemodexThreadSyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BridgeThreadSyncServiceTest {
    @Test
    fun `bridge thread sync service loads real threads and hydrates history`() = runTest {
        val store = InMemorySecureStore()
        val macIdentity = createTestMacIdentity()
        val payload = createTestPairingPayload(
            macDeviceId = "mac-threads",
            macIdentityPublicKey = macIdentity.publicKeyBase64,
        )
        val relayFactory = ScriptedRpcRelayWebSocketFactory(
            macDeviceId = payload.macDeviceId,
            macIdentity = macIdentity,
            requestHandlers = mapOf(
                "initialize" to { buildJsonObject { } },
                "thread/list" to { message ->
                    val archived = message.params?.jsonObjectOrNull?.firstString("archived") == "true"
                    if (archived) {
                        buildJsonObject {
                            put(
                                "data",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("id", JsonPrimitive("thread-archived"))
                                            put("title", JsonPrimitive("Archived thread"))
                                            put("cwd", JsonPrimitive("/tmp/project-b"))
                                        },
                                    )
                                },
                            )
                        }
                    } else {
                        buildJsonObject {
                            put(
                                "data",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("id", JsonPrimitive("thread-live"))
                                            put("title", JsonPrimitive("Live thread"))
                                            put("preview", JsonPrimitive("Bridge-backed preview"))
                                            put("cwd", JsonPrimitive("/tmp/project-a"))
                                            put("updatedAt", JsonPrimitive(1_713_111_111))
                                        },
                                    )
                                },
                            )
                        }
                    }
                },
                "thread/read" to {
                    buildJsonObject {
                        put(
                            "thread",
                            buildJsonObject {
                                put("id", JsonPrimitive("thread-live"))
                                put("title", JsonPrimitive("Live thread"))
                                put(
                                    "turns",
                                    buildJsonArray {
                                        add(
                                            buildJsonObject {
                                                put("id", JsonPrimitive("turn-1"))
                                                put(
                                                    "items",
                                                    buildJsonArray {
                                                        add(
                                                            buildJsonObject {
                                                                put("id", JsonPrimitive("user-1"))
                                                                put("type", JsonPrimitive("user_message"))
                                                                put("text", JsonPrimitive("Pair the Android client with the local bridge."))
                                                            },
                                                        )
                                                        add(
                                                            buildJsonObject {
                                                                put("id", JsonPrimitive("assistant-1"))
                                                                put("type", JsonPrimitive("agent_message"))
                                                                put("text", JsonPrimitive("Thread hydration now uses real bridge data."))
                                                            },
                                                        )
                                                    },
                                                )
                                            },
                                        )
                                    },
                                )
                            },
                        )
                    }
                },
            ),
        )
        val coordinator = SecureConnectionCoordinator(
            store = store,
            trustedSessionResolver = UnusedTrustedSessionResolver,
            relayWebSocketFactory = relayFactory,
            scope = this,
        )
        val service = BridgeThreadSyncService(
            secureConnectionCoordinator = coordinator,
            scope = backgroundScope,
        )

        try {
            coordinator.rememberRelayPairing(payload)
            coordinator.retryConnection()
            awaitSecureState(coordinator, SecureConnectionState.ENCRYPTED)
            service.refreshThreads()
            advanceUntilIdle()
            awaitThreads(service, expectedCount = 2)
            service.hydrateThread("thread-live")
            advanceUntilIdle()

            val liveThread = service.threads.value.first { it.id == "thread-live" }
            val archivedThread = service.threads.value.first { it.id == "thread-archived" }
            assertEquals("Live thread", liveThread.title)
            assertEquals("/tmp/project-a", liveThread.projectPath)
            assertFalse(liveThread.timelineMutations.isEmpty())
            assertEquals(RemodexThreadSyncState.ARCHIVED_LOCAL, archivedThread.syncState)
            assertTrue(
                relayFactory.receivedRequests.any { request ->
                    request.method == "thread/read"
                },
            )
        } finally {
            coordinator.disconnect()
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

    private suspend fun TestScope.awaitThreads(
        service: BridgeThreadSyncService,
        expectedCount: Int,
    ) {
        repeat(40) {
            advanceUntilIdle()
            if (service.threads.value.size == expectedCount) {
                return
            }
            Thread.sleep(10)
        }
        fail("Expected $expectedCount threads but found ${service.threads.value.size}")
    }
}
