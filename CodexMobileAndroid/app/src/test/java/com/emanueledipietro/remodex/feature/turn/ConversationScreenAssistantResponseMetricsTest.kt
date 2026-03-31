package com.emanueledipietro.remodex.feature.turn

import com.emanueledipietro.remodex.model.RemodexAssistantResponseMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationScreenAssistantResponseMetricsTest {
    @Test
    fun `assistant response metrics footer uses compact stable formatting`() {
        val formatted = formatAssistantResponseMetricsLabel(
            RemodexAssistantResponseMetrics(
                messageId = "assistant-message",
                turnId = "turn-1",
                outputTokens = 56,
                tokensPerSecond = 23.44,
                ttftMs = 820L,
            ),
        )

        assertEquals("23.4 token/s · TTFT 0.82s", formatted)
    }
}
