package com.emanueledipietro.remodex.model

enum class RemodexThreadSyncState {
    LIVE,
    ARCHIVED_LOCAL,
}

data class RemodexThreadSummary(
    val id: String,
    val title: String,
    val preview: String,
    val projectPath: String,
    val lastUpdatedLabel: String,
    val isRunning: Boolean,
    val syncState: RemodexThreadSyncState = RemodexThreadSyncState.LIVE,
    val parentThreadId: String? = null,
    val agentNickname: String? = null,
    val agentRole: String? = null,
    val queuedDrafts: Int,
    val queuedDraftItems: List<RemodexQueuedDraft> = emptyList(),
    val runtimeLabel: String,
    val runtimeConfig: RemodexRuntimeConfig = RemodexRuntimeConfig(),
    val messages: List<RemodexConversationItem>,
) {
    val isSubagent: Boolean
        get() = !parentThreadId.isNullOrBlank()
}
