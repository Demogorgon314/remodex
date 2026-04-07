package com.emanueledipietro.remodex.feature.threads

import com.emanueledipietro.remodex.model.RemodexThreadSummary
import com.emanueledipietro.remodex.model.RemodexThreadSyncState
import com.emanueledipietro.remodex.model.isCodexManagedWorktreeProject
import com.emanueledipietro.remodex.model.normalizeRemodexFilesystemProjectPath
import com.emanueledipietro.remodex.model.remodexProjectDisplayLabel
import java.util.Locale

enum class SidebarThreadGroupKind {
    PROJECT,
    ARCHIVED,
}

data class SidebarThreadGroup(
    val id: String,
    val label: String,
    val kind: SidebarThreadGroupKind,
    val projectPath: String? = null,
    val threads: List<RemodexThreadSummary>,
    val iconSystemName: String = when (kind) {
        SidebarThreadGroupKind.PROJECT -> projectIconSystemName(projectPath)
        SidebarThreadGroupKind.ARCHIVED -> "archivebox"
    },
)

data class SidebarProjectExpansionSnapshot(
    val expandedGroupIds: Set<String>,
    val knownGroupIds: Set<String>,
)

object SidebarThreadGrouping {
    private const val CloudProjectGroupKey = "__no_project__"

    fun makeGroups(
        threads: List<RemodexThreadSummary>,
        query: String,
    ): List<SidebarThreadGroup> {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        val filteredThreads = if (normalizedQuery.isEmpty()) {
            threads
        } else {
            threads.filter { thread ->
                buildString {
                    append(thread.displayTitle)
                    append(' ')
                    append(thread.preview)
                    append(' ')
                    append(thread.projectPath)
                }.lowercase(Locale.ROOT).contains(normalizedQuery)
            }
        }
        val archivedThreads = filteredThreads
            .filter { it.syncState == RemodexThreadSyncState.ARCHIVED_LOCAL }
            .sortedWith(threadRecencyComparator())
        val liveThreads = filteredThreads
            .filter { it.syncState != RemodexThreadSyncState.ARCHIVED_LOCAL }

        val projectGroups = liveThreads
            .groupBy { thread ->
                normalizeRemodexFilesystemProjectPath(thread.projectPath) ?: CloudProjectGroupKey
            }
            .map { (projectKey, projectThreads) ->
                SidebarThreadGroup(
                    id = "project:$projectKey",
                    label = projectLabel(projectKey),
                    kind = SidebarThreadGroupKind.PROJECT,
                    projectPath = projectKey.takeUnless { it == CloudProjectGroupKey },
                    threads = projectThreads.sortedWith(threadRecencyComparator()),
                )
            }
            .sortedWith(compareByDescending<SidebarThreadGroup> { groupSortEpochMs(it) }
                .thenBy { it.label.lowercase(Locale.ROOT) }
                .thenBy { it.id }
            )

        return buildList {
            addAll(projectGroups)
            if (archivedThreads.isNotEmpty()) {
                add(
                    SidebarThreadGroup(
                        id = "archived",
                        label = "Archived (${archivedThreads.size})",
                        kind = SidebarThreadGroupKind.ARCHIVED,
                        threads = archivedThreads,
                    ),
                )
            }
        }
    }

    private fun groupSortEpochMs(group: SidebarThreadGroup): Long {
        return group.threads.maxOfOrNull(RemodexThreadSummary::lastUpdatedEpochMs) ?: Long.MIN_VALUE
    }

    private fun threadRecencyComparator(): Comparator<RemodexThreadSummary> {
        return compareByDescending<RemodexThreadSummary> { it.lastUpdatedEpochMs }
            .thenBy { it.id }
    }
}

object SidebarProjectExpansionState {
    fun synchronizedState(
        currentExpandedGroupIds: Set<String>,
        knownGroupIds: Set<String>,
        visibleGroups: List<SidebarThreadGroup>,
        hasInitialized: Boolean,
        persistedCollapsedGroupIds: Set<String> = emptySet(),
    ): SidebarProjectExpansionSnapshot {
        val visibleGroupIds = visibleGroups
            .asSequence()
            .filter { group -> group.kind == SidebarThreadGroupKind.PROJECT }
            .map(SidebarThreadGroup::id)
            .toSet()
        if (!hasInitialized) {
            return SidebarProjectExpansionSnapshot(
                expandedGroupIds = visibleGroupIds - persistedCollapsedGroupIds,
                knownGroupIds = visibleGroupIds,
            )
        }

        val newGroupIds = visibleGroupIds - knownGroupIds
        return SidebarProjectExpansionSnapshot(
            expandedGroupIds = currentExpandedGroupIds
                .intersect(visibleGroupIds)
                .union(newGroupIds - persistedCollapsedGroupIds),
            knownGroupIds = visibleGroupIds,
        )
    }

    fun groupIdContainingSelectedThread(
        selectedThread: RemodexThreadSummary?,
        groups: List<SidebarThreadGroup>,
    ): String? {
        val selectedThreadId = selectedThread?.id ?: return null
        return groups.firstOrNull { group ->
            group.kind == SidebarThreadGroupKind.PROJECT &&
                group.threads.any { thread -> thread.id == selectedThreadId }
        }?.id
    }

    fun shouldAutoRevealSelectedGroup(
        groupId: String,
        persistedCollapsedGroupIds: Set<String>,
    ): Boolean = groupId !in persistedCollapsedGroupIds
}

private fun projectLabel(projectPath: String): String {
    return remodexProjectDisplayLabel(projectPath)
}

private fun projectIconSystemName(projectPath: String?): String {
    val normalizedProjectPath = normalizeRemodexFilesystemProjectPath(projectPath) ?: return "cloud"
    return if (isCodexManagedWorktreeProject(normalizedProjectPath)) {
        "arrow.triangle.branch"
    } else {
        "laptopcomputer"
    }
}
