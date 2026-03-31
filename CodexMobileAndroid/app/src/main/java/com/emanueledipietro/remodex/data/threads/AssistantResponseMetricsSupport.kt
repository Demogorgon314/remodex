package com.emanueledipietro.remodex.data.threads

import com.emanueledipietro.remodex.data.connection.firstInt
import com.emanueledipietro.remodex.data.connection.firstObject
import com.emanueledipietro.remodex.model.RemodexAssistantResponseMetrics
import kotlinx.serialization.json.JsonObject

internal data class AssistantOutputTokenUsage(
    val totalOutputTokens: Int? = null,
    val lastOutputTokens: Int? = null,
)

internal fun extractLegacyCodexOutputTokens(payload: JsonObject): AssistantOutputTokenUsage? {
    val infoObject = payload.firstObject("info") ?: payload.firstObject("usage") ?: payload
    val totalOutputTokens = infoObject
        .firstObject("total_token_usage", "totalTokenUsage")
        ?.firstInt("output_tokens", "outputTokens")
        ?.takeIf { value -> value >= 0 }
    val lastOutputTokens = infoObject
        .firstObject("last_token_usage", "lastTokenUsage")
        ?.firstInt("output_tokens", "outputTokens")
        ?.takeIf { value -> value >= 0 }
    val fallbackOutputTokens = infoObject
        .firstInt("output_tokens", "outputTokens")
        ?.takeIf { value -> value >= 0 }
    return AssistantOutputTokenUsage(
        totalOutputTokens = totalOutputTokens ?: fallbackOutputTokens,
        lastOutputTokens = lastOutputTokens,
    ).takeIf {
        it.totalOutputTokens != null || it.lastOutputTokens != null
    }
}

internal fun extractThreadTokenUsageUpdatedOutputTokens(payload: JsonObject): AssistantOutputTokenUsage? {
    val usageObject = payload.firstObject("tokenUsage", "token_usage")
        ?: payload.firstObject("usage")
        ?: payload
    val totalOutputTokens = usageObject
        .firstObject("total")
        ?.firstInt("outputTokens", "output_tokens")
        ?.takeIf { value -> value >= 0 }
        ?: usageObject
            .firstObject("totalTokenUsage", "total_token_usage")
            ?.firstInt("outputTokens", "output_tokens")
            ?.takeIf { value -> value >= 0 }
    val lastOutputTokens = usageObject
        .firstObject("last")
        ?.firstInt("outputTokens", "output_tokens")
        ?.takeIf { value -> value >= 0 }
        ?: usageObject
            .firstObject("lastTokenUsage", "last_token_usage")
            ?.firstInt("outputTokens", "output_tokens")
            ?.takeIf { value -> value >= 0 }
    val fallbackOutputTokens = usageObject
        .firstInt("outputTokens", "output_tokens")
        ?.takeIf { value -> value >= 0 }
    return AssistantOutputTokenUsage(
        totalOutputTokens = totalOutputTokens ?: fallbackOutputTokens,
        lastOutputTokens = lastOutputTokens,
    ).takeIf {
        it.totalOutputTokens != null || it.lastOutputTokens != null
    }
}

internal fun buildAssistantResponseMetrics(
    messageId: String,
    turnId: String?,
    outputTokens: Int,
    requestStartedAtMs: Long,
    firstOutputAtMs: Long,
    lastOutputAtMs: Long,
    outputObservationCount: Int = Int.MAX_VALUE,
): RemodexAssistantResponseMetrics? {
    if (outputTokens <= 0) {
        return null
    }
    val generationDurationMs = if (outputObservationCount > 1) {
        (lastOutputAtMs - firstOutputAtMs).coerceAtLeast(1L)
    } else {
        (lastOutputAtMs - requestStartedAtMs).coerceAtLeast(250L)
    }
    val ttftMs = (firstOutputAtMs - requestStartedAtMs).coerceAtLeast(0L)
    return RemodexAssistantResponseMetrics(
        messageId = messageId,
        turnId = turnId,
        outputTokens = outputTokens,
        tokensPerSecond = (outputTokens * 1000.0) / generationDurationMs.toDouble(),
        ttftMs = ttftMs,
    )
}
