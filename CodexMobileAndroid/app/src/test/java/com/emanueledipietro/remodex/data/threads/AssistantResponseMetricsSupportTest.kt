package com.emanueledipietro.remodex.data.threads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AssistantResponseMetricsSupportTest {
    @Test
    fun `extract legacy codex output tokens prefers total token usage for final metrics`() {
        val payload = buildJsonObject {
            put(
                "info",
                buildJsonObject {
                    put(
                        "total_token_usage",
                        buildJsonObject {
                            put("output_tokens", JsonPrimitive(12))
                        },
                    )
                    put(
                        "last_token_usage",
                        buildJsonObject {
                            put("output_tokens", JsonPrimitive(34))
                        },
                    )
                },
            )
        }

        assertEquals(12, extractLegacyCodexOutputTokens(payload)?.totalOutputTokens)
        assertEquals(34, extractLegacyCodexOutputTokens(payload)?.lastOutputTokens)
    }

    @Test
    fun `extract legacy codex output tokens returns null when output tokens missing`() {
        val payload = buildJsonObject {
            put(
                "info",
                buildJsonObject {
                    put("input_tokens", JsonPrimitive(21))
                },
            )
        }

        assertNull(extractLegacyCodexOutputTokens(payload))
    }

    @Test
    fun `extract thread token usage updated output tokens prefers total breakdown for final metrics`() {
        val payload = buildJsonObject {
            put(
                "tokenUsage",
                buildJsonObject {
                    put(
                        "total",
                        buildJsonObject {
                            put("outputTokens", JsonPrimitive(18))
                        },
                    )
                    put(
                        "last",
                        buildJsonObject {
                            put("outputTokens", JsonPrimitive(7))
                        },
                    )
                },
            )
        }

        assertEquals(18, extractThreadTokenUsageUpdatedOutputTokens(payload)?.totalOutputTokens)
        assertEquals(7, extractThreadTokenUsageUpdatedOutputTokens(payload)?.lastOutputTokens)
    }

    @Test
    fun `build assistant response metrics uses visible output token timing`() {
        val metrics = buildAssistantResponseMetrics(
            messageId = "assistant-message",
            turnId = "turn-1",
            outputTokens = 48,
            requestStartedAtMs = 1_000L,
            firstOutputAtMs = 1_850L,
            lastOutputAtMs = 3_050L,
        )

        assertEquals("assistant-message", metrics?.messageId)
        assertEquals("turn-1", metrics?.turnId)
        assertEquals(48, metrics?.outputTokens)
        assertEquals(40.0, metrics?.tokensPerSecond ?: 0.0, 0.0001)
        assertEquals(850L, metrics?.ttftMs ?: -1L)
    }
}
