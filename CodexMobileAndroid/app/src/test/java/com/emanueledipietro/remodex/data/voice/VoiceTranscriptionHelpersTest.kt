package com.emanueledipietro.remodex.data.voice

import com.emanueledipietro.remodex.platform.media.encodeMonoPcm16Wav
import com.emanueledipietro.remodex.platform.media.resamplePcm16
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTranscriptionHelpersTest {
    @Test
    fun preflightAllowsSmallShortClip() {
        val preflight = RemodexVoiceTranscriptionPreflight(
            byteCount = 1_024,
            durationSeconds = 12.0,
        )

        assertNull(preflight.failureMessage)
    }

    @Test
    fun preflightRejectsLongClip() {
        val preflight = RemodexVoiceTranscriptionPreflight(
            byteCount = 1_024,
            durationSeconds = 61.0,
        )

        assertEquals("Voice clips must be 60 seconds or less.", preflight.failureMessage)
    }

    @Test
    fun preflightRejectsLargeClip() {
        val preflight = RemodexVoiceTranscriptionPreflight(
            byteCount = 10 * 1024 * 1024 + 1,
            durationSeconds = 10.0,
        )

        assertEquals("Voice clips must be smaller than 10 MB.", preflight.failureMessage)
    }

    @Test
    fun resampleKeepsOriginalSamplesWhenRatesMatch() {
        val input = shortArrayOf(1, 2, 3, 4)

        assertArrayEquals(input, resamplePcm16(input, fromSampleRate = 24_000, toSampleRate = 24_000))
    }

    @Test
    fun wavEncoderWritesExpectedHeader() {
        val wavBytes = encodeMonoPcm16Wav(
            samples = shortArrayOf(1, -1, 2, -2),
            sampleRate = 24_000,
        )

        assertTrue(wavBytes.copyOfRange(0, 4).contentEquals("RIFF".toByteArray()))
        assertTrue(wavBytes.copyOfRange(8, 12).contentEquals("WAVE".toByteArray()))
        assertEquals(44 + 8, wavBytes.size)
    }
}
