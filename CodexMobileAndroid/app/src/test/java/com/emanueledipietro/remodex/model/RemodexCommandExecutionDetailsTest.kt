package com.emanueledipietro.remodex.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RemodexCommandExecutionDetailsTest {
    @Test
    fun `appended output keeps only the last thirty lines`() {
        val seeded = (1..28).joinToString(separator = "\n") { index -> "line-$index" }
        val details = RemodexCommandExecutionDetails(
            fullCommand = "pwd",
            outputTail = seeded,
        ).appendedOutput("\nline-29\nline-30\nline-31")

        val lines = details.outputTail.lines()
        assertEquals(RemodexCommandExecutionDetails.MaxOutputLines, lines.size)
        assertEquals("line-2", lines.first())
        assertEquals("line-31", lines.last())
    }
}
