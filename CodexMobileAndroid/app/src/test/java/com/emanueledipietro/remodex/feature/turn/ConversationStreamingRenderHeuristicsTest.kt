package com.emanueledipietro.remodex.feature.turn

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationStreamingRenderHeuristicsTest {
    @Test
    fun `streaming plain text display preserves newlines and expands tabs`() {
        val formatted = formatStreamingPlainTextForDisplay("A\n\tB")

        assertEquals("A\n    B", formatted)
    }

    @Test
    fun `streaming plain text display preserves repeated spaces for tables`() {
        val formatted = formatStreamingPlainTextForDisplay("| a  | b |")

        assertEquals("| a  | b |", formatted)
    }
}
