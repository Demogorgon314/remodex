package com.emanueledipietro.remodex.feature.appshell

import org.junit.Assert.assertEquals
import org.junit.Test

class AppViewModelVoiceHelpersTest {
    @Test
    fun appendVoiceTranscriptUsesTranscriptWhenDraftIsEmpty() {
        assertEquals(
            "hello from voice",
            appendVoiceTranscriptToDraft(
                currentDraft = "",
                transcript = "  hello from voice  ",
            ),
        )
    }

    @Test
    fun appendVoiceTranscriptAddsSingleSpaceWhenDraftHasText() {
        assertEquals(
            "hello world",
            appendVoiceTranscriptToDraft(
                currentDraft = "hello",
                transcript = "world",
            ),
        )
    }

    @Test
    fun appendVoiceTranscriptRespectsExistingTrailingWhitespace() {
        assertEquals(
            "hello world",
            appendVoiceTranscriptToDraft(
                currentDraft = "hello ",
                transcript = "world",
            ),
        )
    }
}
