package com.emanueledipietro.remodex.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemodexConversationMetadataTest {
    @Test
    fun `agent rows mark top level model as requested hint when receiver model is missing`() {
        val action = RemodexSubagentAction(
            tool = "spawnAgent",
            status = "completed",
            model = "gpt-5.4-mini",
            receiverThreadIds = listOf("child-thread-1"),
        )

        val row = action.agentRows.single()

        assertEquals("gpt-5.4-mini", row.model)
        assertTrue(row.modelIsRequestedHint)
    }

    @Test
    fun `agent rows prefer receiver model without requested hint`() {
        val action = RemodexSubagentAction(
            tool = "spawnAgent",
            status = "completed",
            model = "gpt-5.4-mini",
            receiverAgents = listOf(
                RemodexSubagentRef(
                    threadId = "child-thread-1",
                    model = "gpt-5.4",
                    nickname = "Locke",
                    role = "explorer",
                ),
            ),
        )

        val row = action.agentRows.single()

        assertEquals("gpt-5.4", row.model)
        assertFalse(row.modelIsRequestedHint)
        assertEquals("Locke [explorer]", row.displayLabel)
    }
}
