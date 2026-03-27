package com.emanueledipietro.remodex.feature.turn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationStreamingRenderHeuristicsTest {
    @Test
    fun `plain prose uses lightweight streaming renderer`() {
        assertTrue(shouldUseLightweightStreamingAssistantText("This is a plain streaming paragraph."))
    }

    @Test
    fun `markdown links still use lightweight streaming renderer`() {
        assertTrue(shouldUseLightweightStreamingAssistantText("See [README](README.md) for details."))
    }

    @Test
    fun `code fences force full renderer`() {
        assertFalse(
            shouldUseLightweightStreamingAssistantText(
                """
                ```kotlin
                println("hello")
                ```
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `bold inline markdown still uses lightweight renderer`() {
        assertTrue(shouldUseLightweightStreamingAssistantText("Use **bold** text while streaming."))
    }

    @Test
    fun `inline code still uses lightweight renderer`() {
        assertTrue(shouldUseLightweightStreamingAssistantText("Run `./gradlew test` while streaming."))
    }

    @Test
    fun `blockquote falls back to full renderer`() {
        assertFalse(
            shouldUseLightweightStreamingAssistantText(
                """
                > quoted status
                >
                > more detail
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `lists fall back to full renderer`() {
        assertFalse(
            shouldUseLightweightStreamingAssistantText(
                """
                - first item
                - second item
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `markdown images fall back to full renderer`() {
        assertFalse(
            shouldUseLightweightStreamingAssistantText(
                "![Architecture](https://example.com/diagram.png)",
            ),
        )
    }
}
