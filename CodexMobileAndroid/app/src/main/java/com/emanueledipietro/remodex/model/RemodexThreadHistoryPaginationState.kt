package com.emanueledipietro.remodex.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RemodexThreadHistoryPaginationState(
    val olderCursor: JsonElement? = null,
    val exhaustedOlderCursor: JsonElement? = null,
    val hasAuthoritativeLocalHistoryStart: Boolean = false,
)
