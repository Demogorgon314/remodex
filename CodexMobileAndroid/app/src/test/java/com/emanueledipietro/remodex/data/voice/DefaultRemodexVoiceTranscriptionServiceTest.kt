package com.emanueledipietro.remodex.data.voice

import com.emanueledipietro.remodex.data.connection.InMemorySecureStore
import com.emanueledipietro.remodex.data.connection.SecureConnectionCoordinator
import com.emanueledipietro.remodex.data.connection.SecureConnectionSnapshot
import com.emanueledipietro.remodex.data.connection.SecureConnectionState
import com.emanueledipietro.remodex.data.connection.SecureTransportException
import com.emanueledipietro.remodex.data.connection.UnexpectedRelayWebSocketFactory
import com.emanueledipietro.remodex.data.connection.UnusedTrustedSessionResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRemodexVoiceTranscriptionServiceTest {
    @Test
    fun `transport race does not invalidate voice auth state`() = runTest {
        val coordinator = SecureConnectionCoordinator(
            store = InMemorySecureStore(),
            trustedSessionResolver = UnusedTrustedSessionResolver,
            relayWebSocketFactory = UnexpectedRelayWebSocketFactory(),
            scope = this,
        )
        setSecureConnectionState(
            coordinator = coordinator,
            snapshot = SecureConnectionSnapshot(
                phaseMessage = "Connected",
                secureState = SecureConnectionState.ENCRYPTED,
                attempt = 1,
            ),
        )
        var invalidationCount = 0
        val service = DefaultRemodexVoiceTranscriptionService(
            secureConnectionCoordinator = coordinator,
            transcriptionClient = object : VoiceTranscriptionClient {
                override suspend fun transcribe(
                    wavFile: java.io.File,
                    token: String,
                ): String {
                    fail("Voice transcription client should not run without a secure transport session.")
                    return ""
                }
            },
            onAuthStateInvalidated = {
                invalidationCount += 1
            },
        )
        val wavFile = java.io.File.createTempFile("remodex-voice-", ".wav").apply {
            writeBytes(ByteArray(32))
            deleteOnExit()
        }

        try {
            service.transcribeVoiceAudioFile(
                file = wavFile,
                durationSeconds = 1.0,
            )
            fail("Expected secure transport failure.")
        } catch (error: Throwable) {
            assertTrue(error is SecureTransportException)
        } finally {
            wavFile.delete()
        }

        assertEquals(0, invalidationCount)
    }

    @Suppress("UNCHECKED_CAST")
    private fun setSecureConnectionState(
        coordinator: SecureConnectionCoordinator,
        snapshot: SecureConnectionSnapshot,
    ) {
        val field = coordinator.javaClass.getDeclaredField("connectionState")
        field.isAccessible = true
        val state = field.get(coordinator) as MutableStateFlow<SecureConnectionSnapshot>
        state.value = snapshot
    }
}
