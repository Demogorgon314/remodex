package com.emanueledipietro.remodex.model

import kotlinx.serialization.Serializable

@Serializable
data class RemodexQueuedDraft(
    val id: String,
    val text: String,
    val createdAtEpochMs: Long,
    val attachments: List<RemodexComposerAttachment> = emptyList(),
    val planningMode: RemodexPlanningMode? = null,
)
