package com.emanueledipietro.remodex.model

data class RemodexAssistantResponseMetrics(
    val messageId: String,
    val turnId: String? = null,
    val outputTokens: Int,
    val tokensPerSecond: Double,
    val ttftMs: Long,
)
