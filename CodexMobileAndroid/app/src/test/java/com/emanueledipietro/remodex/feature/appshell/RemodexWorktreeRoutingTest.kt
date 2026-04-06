package com.emanueledipietro.remodex.feature.appshell

import com.emanueledipietro.remodex.model.RemodexThreadSummary
import com.emanueledipietro.remodex.model.RemodexThreadSyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemodexWorktreeRoutingTest {
    @Test
    fun `canonical project path collapses dot segments for worktree comparisons`() {
        val canonicalPath = RemodexWorktreeRouting.comparableProjectPath(
            "/tmp/remodex/.codex/worktrees/feature/../feature/test",
        )
        val normalizedReference = RemodexWorktreeRouting.comparableProjectPath(
            "/tmp/remodex/.codex/worktrees/feature/test",
        )

        assertEquals(normalizedReference, canonicalPath)
    }

    @Test
    fun `matching live thread ignores archived thread entries`() {
        val matchingThread = RemodexWorktreeRouting.matchingLiveThread(
            threads = listOf(
                threadSummary(
                    id = "archived-thread",
                    projectPath = "/tmp/remodex/.codex/worktrees/feature/test",
                    syncState = RemodexThreadSyncState.ARCHIVED_LOCAL,
                ),
                threadSummary(
                    id = "live-thread",
                    projectPath = "/tmp/remodex/.codex/worktrees/feature/test",
                    syncState = RemodexThreadSyncState.LIVE,
                ),
            ),
            projectPath = "/tmp/remodex/.codex/worktrees/feature/test",
        )

        assertEquals("live-thread", matchingThread?.id)
    }

    @Test
    fun `live thread lookup returns null when current thread already points at matching worktree`() {
        val currentThread = threadSummary(
            id = "current-thread",
            projectPath = "/tmp/remodex/.codex/worktrees/feature/../feature/test",
        )

        val matchingThread = RemodexWorktreeRouting.liveThreadForCheckedOutElsewhereBranch(
            projectPath = "/tmp/remodex/.codex/worktrees/feature/test",
            currentThread = currentThread,
            threads = listOf(currentThread),
        )

        assertNull(matchingThread)
    }

    private fun threadSummary(
        id: String,
        projectPath: String,
        syncState: RemodexThreadSyncState = RemodexThreadSyncState.LIVE,
    ): RemodexThreadSummary {
        return RemodexThreadSummary(
            id = id,
            title = id,
            preview = "",
            projectPath = projectPath,
            lastUpdatedLabel = "now",
            isRunning = false,
            syncState = syncState,
            queuedDrafts = 0,
            runtimeLabel = "Local",
            messages = emptyList(),
        )
    }
}
