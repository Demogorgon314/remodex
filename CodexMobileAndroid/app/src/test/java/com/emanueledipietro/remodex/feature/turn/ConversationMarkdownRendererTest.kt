package com.emanueledipietro.remodex.feature.turn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ConversationMarkdownRendererTest {
    @Test
    fun `render token stays stable for identical markdown`() {
        val markdown = """
            ## Status

            See [README](README.md) and run `./gradlew test`.
        """.trimIndent()

        assertEquals(
            conversationMarkdownRenderToken(markdown),
            conversationMarkdownRenderToken(markdown),
        )
    }

    @Test
    fun `render token changes when markdown body changes`() {
        val first = buildString {
            append("prefix-")
            repeat(64) { append('a') }
            append("-suffix")
        }
        val second = buildString {
            append("prefix-")
            repeat(32) { append('a') }
            repeat(32) { append('b') }
            append("-suffix")
        }

        assertNotEquals(
            conversationMarkdownRenderToken(first),
            conversationMarkdownRenderToken(second),
        )
    }
}
