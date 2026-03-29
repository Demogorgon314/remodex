package com.emanueledipietro.remodex.feature.threads

import com.emanueledipietro.remodex.model.RemodexThreadSummary
import com.emanueledipietro.remodex.model.RemodexThreadSyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SidebarThreadGroupingTest {
    @Test
    fun `groups live chats by project and keeps archived chats separate`() {
        val groups = SidebarThreadGrouping.makeGroups(
            threads = listOf(
                thread(id = "1", title = "Alpha", projectPath = "/tmp/alpha"),
                thread(id = "2", title = "Beta", projectPath = "/tmp/beta"),
                thread(
                    id = "3",
                    title = "Archived",
                    projectPath = "/tmp/alpha",
                    syncState = RemodexThreadSyncState.ARCHIVED_LOCAL,
                ),
            ),
            query = "",
        )

        assertEquals(3, groups.size)
        assertEquals(SidebarThreadGroupKind.PROJECT, groups[0].kind)
        assertEquals("alpha", groups[0].label)
        assertEquals(SidebarThreadGroupKind.PROJECT, groups[1].kind)
        assertEquals("beta", groups[1].label)
        assertEquals(SidebarThreadGroupKind.ARCHIVED, groups[2].kind)
        assertEquals(listOf("3"), groups[2].threads.map(RemodexThreadSummary::id))
    }

    @Test
    fun `search query filters project and preview text`() {
        val groups = SidebarThreadGrouping.makeGroups(
            threads = listOf(
                thread(id = "1", title = "Alpha", preview = "fix notifications", projectPath = "/tmp/alpha"),
                thread(id = "2", title = "Beta", preview = "composer parity", projectPath = "/tmp/beta"),
            ),
            query = "composer",
        )

        assertEquals(1, groups.size)
        assertEquals("beta", groups.single().label)
        assertTrue(groups.single().threads.single().preview.contains("composer"))
    }

    @Test
    fun `search query uses iOS display title instead of generic placeholders`() {
        val groups = SidebarThreadGrouping.makeGroups(
            threads = listOf(
                thread(
                    id = "1",
                    title = "Conversation",
                    preview = "sidebar parity follow up",
                    projectPath = "/tmp/alpha",
                ),
            ),
            query = "follow",
        )

        assertEquals(1, groups.size)
        assertEquals("1", groups.single().threads.single().id)
    }

    @Test
    fun `project groups expose iOS aligned icon semantics`() {
        val groups = SidebarThreadGrouping.makeGroups(
            threads = listOf(
                thread(id = "1", title = "Local", projectPath = "/tmp/remodex"),
                thread(id = "2", title = "Worktree", projectPath = "/tmp/remodex/.codex/worktrees/feature-a"),
                thread(id = "3", title = "Cloud", projectPath = ""),
            ),
            query = "",
        )

        assertEquals("laptopcomputer", groups[0].iconSystemName)
        assertEquals("arrow.triangle.branch", groups[1].iconSystemName)
        assertEquals("cloud", groups[2].iconSystemName)
    }

    @Test
    fun `project expansion state initially keeps persisted collapsed groups closed`() {
        val groups = listOf(
            projectGroup(id = "project:/tmp/app"),
            projectGroup(id = "project:/tmp/site"),
        )

        val snapshot = SidebarProjectExpansionState.synchronizedState(
            currentExpandedGroupIds = emptySet(),
            knownGroupIds = emptySet(),
            visibleGroups = groups,
            hasInitialized = false,
            persistedCollapsedGroupIds = setOf("project:/tmp/site"),
        )

        assertEquals(setOf("project:/tmp/app"), snapshot.expandedGroupIds)
        assertEquals(groups.map(SidebarThreadGroup::id).toSet(), snapshot.knownGroupIds)
    }

    @Test
    fun `project expansion state auto expands new groups only`() {
        val existingGroups = listOf(
            projectGroup(id = "project:/tmp/app"),
            projectGroup(id = "project:/tmp/site"),
        )
        val updatedGroups = existingGroups + projectGroup(id = "project:/tmp/docs")

        val snapshot = SidebarProjectExpansionState.synchronizedState(
            currentExpandedGroupIds = setOf("project:/tmp/app"),
            knownGroupIds = existingGroups.map(SidebarThreadGroup::id).toSet(),
            visibleGroups = updatedGroups,
            hasInitialized = true,
        )

        assertEquals(
            setOf("project:/tmp/app", "project:/tmp/docs"),
            snapshot.expandedGroupIds,
        )
    }

    @Test
    fun `selected thread auto reveal skips persisted collapsed group`() {
        assertFalse(
            SidebarProjectExpansionState.shouldAutoRevealSelectedGroup(
                groupId = "project:/tmp/app",
                persistedCollapsedGroupIds = setOf("project:/tmp/app"),
            ),
        )
    }

    @Test
    fun `group id containing selected thread returns owning project group`() {
        val selectedThread = thread(id = "1", title = "Alpha", projectPath = "/tmp/app")
        val groups = listOf(
            SidebarThreadGroup(
                id = "project:/tmp/app",
                label = "app",
                kind = SidebarThreadGroupKind.PROJECT,
                projectPath = "/tmp/app",
                threads = listOf(selectedThread),
            ),
            projectGroup(id = "project:/tmp/site"),
        )

        assertEquals(
            "project:/tmp/app",
            SidebarProjectExpansionState.groupIdContainingSelectedThread(selectedThread, groups),
        )
    }

    private fun thread(
        id: String,
        title: String,
        preview: String = "",
        projectPath: String,
        syncState: RemodexThreadSyncState = RemodexThreadSyncState.LIVE,
    ): RemodexThreadSummary {
        return RemodexThreadSummary(
            id = id,
            title = title,
            preview = preview,
            projectPath = projectPath,
            lastUpdatedLabel = "Updated just now",
            isRunning = false,
            syncState = syncState,
            queuedDrafts = 0,
            runtimeLabel = "Auto",
            messages = emptyList(),
        )
    }

    private fun projectGroup(id: String): SidebarThreadGroup {
        return SidebarThreadGroup(
            id = id,
            label = id.substringAfterLast('/'),
            kind = SidebarThreadGroupKind.PROJECT,
            projectPath = id.removePrefix("project:"),
            threads = emptyList(),
        )
    }
}
