package com.emanueledipietro.remodex.model

import kotlinx.serialization.Serializable

@Serializable
data class RemodexQueuedDraft(
    val id: String,
    val text: String,
    val createdAtEpochMs: Long,
    val attachments: List<RemodexComposerAttachment> = emptyList(),
    val planningMode: RemodexPlanningMode? = null,
    val rawInput: String? = null,
    val rawMentionedFiles: List<RemodexComposerMentionedFile> = emptyList(),
    val rawMentionedSkills: List<RemodexComposerMentionedSkill> = emptyList(),
    val rawSubagentsSelectionArmed: Boolean = false,
)

data class RemodexQueuedDraftContext(
    val rawInput: String,
    val rawMentionedFiles: List<RemodexComposerMentionedFile> = emptyList(),
    val rawMentionedSkills: List<RemodexComposerMentionedSkill> = emptyList(),
    val rawSubagentsSelectionArmed: Boolean = false,
)
