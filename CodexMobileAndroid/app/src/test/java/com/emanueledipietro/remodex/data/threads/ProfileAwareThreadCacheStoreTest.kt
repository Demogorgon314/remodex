package com.emanueledipietro.remodex.data.threads

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileAwareThreadCacheStoreTest {
    @Test
    fun `switching active profile closes previous store and publishes next threads`() = runTest {
        val createdStores = linkedMapOf<String, FakeThreadCacheStore>()
        val store = ProfileAwareThreadCacheStore(
            databaseNameResolver = { it },
            storeFactory = { databaseName ->
                createdStores.getOrPut(databaseName) { FakeThreadCacheStore() }
            },
            scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job()),
        )

        store.setActiveProfileId("alpha")
        createdStores.getValue("alpha").emitThreads(
            listOf(
                cachedThreadRecord(id = "alpha-thread"),
            ),
        )
        advanceUntilIdle()
        assertEquals(
            listOf("alpha-thread"),
            store.threads.firstIds(),
        )

        store.setActiveProfileId("beta")
        advanceUntilIdle()
        assertTrue(createdStores.getValue("alpha").closeCalls == 1)

        createdStores.getValue("beta").emitThreads(
            listOf(
                cachedThreadRecord(id = "beta-thread"),
            ),
        )
        advanceUntilIdle()
        assertEquals(
            listOf("beta-thread"),
            store.threads.firstIds(),
        )
    }

    @Test
    fun `close releases active store and clears published threads`() = runTest {
        val createdStore = FakeThreadCacheStore()
        val store = ProfileAwareThreadCacheStore(
            databaseNameResolver = { it },
            storeFactory = { createdStore },
            scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job()),
        )

        store.setActiveProfileId("alpha")
        createdStore.emitThreads(listOf(cachedThreadRecord(id = "alpha-thread")))
        advanceUntilIdle()

        store.close()
        advanceUntilIdle()

        assertEquals(1, createdStore.closeCalls)
        assertTrue(store.threads.firstIds().isEmpty())
    }

    @Test
    fun `legacy profile id keeps shared database name`() {
        assertEquals(
            "remodex_thread_cache.db",
            databaseNameForProfile(
                profileId = "profile-1",
                legacyProfileId = " profile-1 ",
            ),
        )
        assertEquals(
            "remodex_thread_cache_profile_2.db",
            databaseNameForProfile(
                profileId = "profile-2",
                legacyProfileId = "profile-1",
            ),
        )
    }
}

private class FakeThreadCacheStore : ThreadCacheStore {
    private val backingThreads = MutableStateFlow<List<CachedThreadRecord>>(emptyList())
    var closeCalls: Int = 0
        private set

    override val threads: Flow<List<CachedThreadRecord>> = backingThreads

    override suspend fun replaceThreads(threads: List<CachedThreadRecord>) {
        backingThreads.value = threads
    }

    override fun close() {
        closeCalls += 1
    }

    fun emitThreads(threads: List<CachedThreadRecord>) {
        backingThreads.value = threads
    }
}

private suspend fun Flow<List<CachedThreadRecord>>.firstIds(): List<String> {
    return first().map(CachedThreadRecord::id)
}

private fun cachedThreadRecord(id: String): CachedThreadRecord {
    return CachedThreadRecord(
        id = id,
        title = id,
        preview = "$id preview",
        projectPath = "/tmp/$id",
        lastUpdatedLabel = "Updated now",
        lastUpdatedEpochMs = 1L,
        isRunning = false,
        runtimeConfig = com.emanueledipietro.remodex.model.RemodexRuntimeConfig(),
        timelineItems = emptyList(),
    )
}
