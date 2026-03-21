package com.emanueledipietro.remodex.data.threads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryThreadCacheStore(
    initialThreads: List<CachedThreadRecord> = emptyList(),
) : ThreadCacheStore {
    private val backingThreads = MutableStateFlow(initialThreads)

    override val threads: Flow<List<CachedThreadRecord>> = backingThreads

    override suspend fun replaceThreads(threads: List<CachedThreadRecord>) {
        backingThreads.value = threads.sortedByDescending(CachedThreadRecord::lastUpdatedEpochMs)
    }
}
