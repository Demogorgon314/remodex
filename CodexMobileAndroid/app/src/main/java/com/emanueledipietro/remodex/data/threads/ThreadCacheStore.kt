package com.emanueledipietro.remodex.data.threads

import com.emanueledipietro.remodex.model.RemodexConversationItem
import com.emanueledipietro.remodex.model.RemodexRuntimeConfig
import com.emanueledipietro.remodex.model.RemodexAssistantChangeSet
import com.emanueledipietro.remodex.model.RemodexTurnTerminalState
import com.emanueledipietro.remodex.model.RemodexThreadSyncState
import kotlinx.coroutines.flow.Flow

data class CachedThreadRecord(
    val id: String,
    val title: String,
    val name: String? = null,
    val preview: String,
    val projectPath: String,
    val lastUpdatedLabel: String,
    val lastUpdatedEpochMs: Long,
    val isRunning: Boolean,
    val isWaitingOnApproval: Boolean = false,
    val syncState: RemodexThreadSyncState = RemodexThreadSyncState.LIVE,
    val parentThreadId: String? = null,
    val agentNickname: String? = null,
    val agentRole: String? = null,
    val activeTurnId: String? = null,
    val latestTurnTerminalState: RemodexTurnTerminalState? = null,
    val stoppedTurnIds: Set<String> = emptySet(),
    val runtimeConfig: RemodexRuntimeConfig,
    val timelineItems: List<RemodexConversationItem>,
    val assistantChangeSets: List<RemodexAssistantChangeSet> = emptyList(),
)

interface ThreadCacheStore {
    val threads: Flow<List<CachedThreadRecord>>

    fun setActiveProfileId(profileId: String?) = Unit

    fun peekThreads(): List<CachedThreadRecord> = emptyList()

    suspend fun replaceThreads(threads: List<CachedThreadRecord>)

    fun close() = Unit
}
