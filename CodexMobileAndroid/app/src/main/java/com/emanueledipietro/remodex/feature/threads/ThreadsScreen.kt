package com.emanueledipietro.remodex.feature.threads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanueledipietro.remodex.feature.appshell.AppUiState
import com.emanueledipietro.remodex.model.RemodexThreadSummary
import com.emanueledipietro.remodex.model.RemodexThreadSyncState

private const val ProjectPreviewCount = 4

@Composable
fun ThreadsScreen(
    uiState: AppUiState,
    onSelectThread: (String) -> Unit,
    onRetryConnection: () -> Unit,
    onCreateThread: (String?) -> Unit,
    onRenameThread: (String, String) -> Unit,
    onArchiveThread: (String) -> Unit,
    onUnarchiveThread: (String) -> Unit,
    onDeleteThread: (String) -> Unit,
    onArchiveProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    var newChatExpanded by rememberSaveable { mutableStateOf(false) }
    var expandedProjectIds by remember { mutableStateOf(setOf<String>()) }
    var revealedProjectGroupIds by remember { mutableStateOf(setOf<String>()) }
    var expandedSubagentParentIds by remember { mutableStateOf(setOf<String>()) }

    val groups = remember(uiState.threads, searchText) {
        SidebarThreadGrouping.makeGroups(
            threads = uiState.threads,
            query = searchText,
        )
    }
    val projectGroups = groups.filter { group -> group.kind == SidebarThreadGroupKind.PROJECT }
    val effectiveExpandedProjectIds = remember(groups, expandedProjectIds) {
        if (expandedProjectIds.isEmpty()) {
            groups.filter { it.kind == SidebarThreadGroupKind.PROJECT }.mapTo(mutableSetOf(), SidebarThreadGroup::id)
        } else {
            expandedProjectIds
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SidebarHeader(
                uiState = uiState,
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onSearchActiveChange = onSearchActiveChange,
                newChatExpanded = newChatExpanded,
                onNewChatExpandedChange = { newChatExpanded = it },
                projectGroups = projectGroups,
                onCreateThread = onCreateThread,
                onRetryConnection = onRetryConnection,
            )
        }

        if (groups.isEmpty()) {
            item {
                EmptyThreadsState(
                    isConnected = uiState.isConnected,
                    isFiltering = searchText.isNotBlank(),
                )
            }
        } else {
            groups.forEach { group ->
                item(key = group.id) {
                    when (group.kind) {
                        SidebarThreadGroupKind.PROJECT -> {
                            ProjectGroupSection(
                                group = group,
                                selectedThreadId = uiState.selectedThread?.id,
                                expanded = effectiveExpandedProjectIds.contains(group.id),
                                revealAll = revealedProjectGroupIds.contains(group.id) ||
                                    group.threads.any { thread -> thread.id == uiState.selectedThread?.id },
                                expandedSubagentParentIds = expandedSubagentParentIds,
                                onToggleExpanded = {
                                    expandedProjectIds = expandedProjectIds.toMutableSet().apply {
                                        if (!add(group.id)) {
                                            remove(group.id)
                                        }
                                    }
                                },
                                onToggleSubagentExpansion = { threadId ->
                                    expandedSubagentParentIds = expandedSubagentParentIds.toMutableSet().apply {
                                        if (!add(threadId)) {
                                            remove(threadId)
                                        }
                                    }
                                },
                                onRevealAll = {
                                    revealedProjectGroupIds = revealedProjectGroupIds + group.id
                                },
                                onCreateThread = { onCreateThread(group.projectPath) },
                                onArchiveProject = {
                                    group.projectPath?.let(onArchiveProject)
                                },
                                onSelectThread = onSelectThread,
                                onRenameThread = onRenameThread,
                                onArchiveThread = onArchiveThread,
                                onUnarchiveThread = onUnarchiveThread,
                                onDeleteThread = onDeleteThread,
                            )
                        }

                        SidebarThreadGroupKind.ARCHIVED -> {
                            ArchivedSection(
                                group = group,
                                selectedThreadId = uiState.selectedThread?.id,
                                archivedExpanded = archivedExpanded,
                                expandedSubagentParentIds = expandedSubagentParentIds,
                                onArchivedExpandedChange = { archivedExpanded = it },
                                onToggleSubagentExpansion = { threadId ->
                                    expandedSubagentParentIds = expandedSubagentParentIds.toMutableSet().apply {
                                        if (!add(threadId)) {
                                            remove(threadId)
                                        }
                                    }
                                },
                                onSelectThread = onSelectThread,
                                onRenameThread = onRenameThread,
                                onArchiveThread = onArchiveThread,
                                onUnarchiveThread = onUnarchiveThread,
                                onDeleteThread = onDeleteThread,
                            )
                        }
                    }
                }
            }
        }

        item {
            SidebarFooter(
                trustedMacName = uiState.trustedMac?.name,
                trustedMacTitle = if (uiState.isConnected) "Connected to Mac" else "Saved Mac",
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun SidebarHeader(
    uiState: AppUiState,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    newChatExpanded: Boolean,
    onNewChatExpandedChange: (Boolean) -> Unit,
    projectGroups: List<SidebarThreadGroup>,
    onCreateThread: (String?) -> Unit,
    onRetryConnection: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(26.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "R",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                text = "Remodex",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
        }

        SidebarSearchField(
            text = searchText,
            onTextChange = onSearchTextChange,
            onSearchActiveChange = onSearchActiveChange,
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(
                onClick = {
                    if (projectGroups.isEmpty()) {
                        onCreateThread(null)
                    } else {
                        onNewChatExpandedChange(true)
                    }
                },
                enabled = uiState.isConnected,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Chat")
            }

            DropdownMenu(
                expanded = newChatExpanded,
                onDismissRequest = { onNewChatExpandedChange(false) },
            ) {
                DropdownMenuItem(
                    text = { Text("Without project") },
                    onClick = {
                        onNewChatExpandedChange(false)
                        onCreateThread(null)
                    },
                )
                projectGroups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group.label) },
                        onClick = {
                            onNewChatExpandedChange(false)
                            onCreateThread(group.projectPath)
                        },
                    )
                }
            }
        }

        if (!uiState.isConnected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = uiState.connectionHeadline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRetryConnection) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun SidebarSearchField(
    text: String,
    onTextChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "Search conversations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                                onSearchActiveChange(focusState.isFocused)
                            },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        ),
                    )
                }
                if (text.isNotEmpty()) {
                    IconButton(onClick = { onTextChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (isFocused) {
            TextButton(
                onClick = {
                    onTextChange("")
                    focusManager.clearFocus(force = true)
                    onSearchActiveChange(false)
                },
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun ProjectGroupSection(
    group: SidebarThreadGroup,
    selectedThreadId: String?,
    expanded: Boolean,
    revealAll: Boolean,
    expandedSubagentParentIds: Set<String>,
    onToggleExpanded: () -> Unit,
    onToggleSubagentExpansion: (String) -> Unit,
    onRevealAll: () -> Unit,
    onCreateThread: () -> Unit,
    onArchiveProject: () -> Unit,
    onSelectThread: (String) -> Unit,
    onRenameThread: (String, String) -> Unit,
    onArchiveThread: (String) -> Unit,
    onUnarchiveThread: (String) -> Unit,
    onDeleteThread: (String) -> Unit,
) {
    val rootThreads = remember(group.threads) { rootThreads(group.threads) }
    val visibleRoots = if (revealAll) rootThreads else rootThreads.take(ProjectPreviewCount)
    val childrenByParentId = remember(group.threads) {
        group.threads
            .filter { thread -> !thread.parentThreadId.isNullOrBlank() }
            .groupBy { thread -> thread.parentThreadId.orEmpty() }
    }
    var projectMenuExpanded by remember(group.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = group.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCreateThread) {
                Text("New")
            }
            Box {
                IconButton(onClick = { projectMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreHoriz,
                        contentDescription = "Project actions",
                    )
                }
                DropdownMenu(
                    expanded = projectMenuExpanded,
                    onDismissRequest = { projectMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Archive Project") },
                        onClick = {
                            projectMenuExpanded = false
                            onArchiveProject()
                        },
                    )
                }
            }
        }

        if (expanded) {
            visibleRoots.forEach { thread ->
                ThreadTreeRow(
                    thread = thread,
                    selectedThreadId = selectedThreadId,
                    depth = 0,
                    childrenByParentId = childrenByParentId,
                    expandedSubagentParentIds = expandedSubagentParentIds,
                    onToggleSubagentExpansion = onToggleSubagentExpansion,
                    onSelectThread = onSelectThread,
                    onRenameThread = onRenameThread,
                    onArchiveThread = onArchiveThread,
                    onUnarchiveThread = onUnarchiveThread,
                    onDeleteThread = onDeleteThread,
                )
            }
        }

        if (expanded && !revealAll && rootThreads.size > ProjectPreviewCount) {
            TextButton(
                modifier = Modifier.padding(start = 34.dp),
                onClick = onRevealAll,
            ) {
                Text("Show ${rootThreads.size - ProjectPreviewCount} more")
            }
        }
    }
}

@Composable
private fun ArchivedSection(
    group: SidebarThreadGroup,
    selectedThreadId: String?,
    archivedExpanded: Boolean,
    expandedSubagentParentIds: Set<String>,
    onArchivedExpandedChange: (Boolean) -> Unit,
    onToggleSubagentExpansion: (String) -> Unit,
    onSelectThread: (String) -> Unit,
    onRenameThread: (String, String) -> Unit,
    onArchiveThread: (String) -> Unit,
    onUnarchiveThread: (String) -> Unit,
    onDeleteThread: (String) -> Unit,
) {
    val childrenByParentId = remember(group.threads) {
        group.threads
            .filter { thread -> !thread.parentThreadId.isNullOrBlank() }
            .groupBy { thread -> thread.parentThreadId.orEmpty() }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onArchivedExpandedChange(!archivedExpanded) }
                .padding(top = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (archivedExpanded) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.Archive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = group.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }

        if (archivedExpanded) {
            rootThreads(group.threads).forEach { thread ->
                ThreadTreeRow(
                    thread = thread,
                    selectedThreadId = selectedThreadId,
                    depth = 0,
                    childrenByParentId = childrenByParentId,
                    expandedSubagentParentIds = expandedSubagentParentIds,
                    onToggleSubagentExpansion = onToggleSubagentExpansion,
                    onSelectThread = onSelectThread,
                    onRenameThread = onRenameThread,
                    onArchiveThread = onArchiveThread,
                    onUnarchiveThread = onUnarchiveThread,
                    onDeleteThread = onDeleteThread,
                )
            }
        }
    }
}

@Composable
private fun ThreadTreeRow(
    thread: RemodexThreadSummary,
    selectedThreadId: String?,
    depth: Int,
    childrenByParentId: Map<String, List<RemodexThreadSummary>>,
    expandedSubagentParentIds: Set<String>,
    onToggleSubagentExpansion: (String) -> Unit,
    onSelectThread: (String) -> Unit,
    onRenameThread: (String, String) -> Unit,
    onArchiveThread: (String) -> Unit,
    onUnarchiveThread: (String) -> Unit,
    onDeleteThread: (String) -> Unit,
) {
    val childThreads = childrenByParentId[thread.id].orEmpty()
    val expanded = expandedSubagentParentIds.contains(thread.id)
    ThreadRow(
        thread = thread,
        isSelected = selectedThreadId == thread.id,
        depth = depth,
        hasChildren = childThreads.isNotEmpty(),
        isExpanded = expanded,
        onToggleExpanded = if (childThreads.isNotEmpty()) {
            { onToggleSubagentExpansion(thread.id) }
        } else {
            null
        },
        onSelectThread = { onSelectThread(thread.id) },
        onRenameThread = { name -> onRenameThread(thread.id, name) },
        onArchiveThread = { onArchiveThread(thread.id) },
        onUnarchiveThread = { onUnarchiveThread(thread.id) },
        onDeleteThread = { onDeleteThread(thread.id) },
    )
    if (expanded) {
        childThreads.forEach { child ->
            ThreadTreeRow(
                thread = child,
                selectedThreadId = selectedThreadId,
                depth = depth + 1,
                childrenByParentId = childrenByParentId,
                expandedSubagentParentIds = expandedSubagentParentIds,
                onToggleSubagentExpansion = onToggleSubagentExpansion,
                onSelectThread = onSelectThread,
                onRenameThread = onRenameThread,
                onArchiveThread = onArchiveThread,
                onUnarchiveThread = onUnarchiveThread,
                onDeleteThread = onDeleteThread,
            )
        }
    }
}

@Composable
private fun ThreadRow(
    thread: RemodexThreadSummary,
    isSelected: Boolean,
    depth: Int,
    hasChildren: Boolean,
    isExpanded: Boolean,
    onToggleExpanded: (() -> Unit)?,
    onSelectThread: () -> Unit,
    onRenameThread: (String) -> Unit,
    onArchiveThread: () -> Unit,
    onUnarchiveThread: () -> Unit,
    onDeleteThread: () -> Unit,
) {
    var menuExpanded by remember(thread.id) { mutableStateOf(false) }
    var renameDraft by remember(thread.id) { mutableStateOf(thread.title) }
    var renameExpanded by remember(thread.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                },
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasChildren && onToggleExpanded != null) {
            IconButton(onClick = onToggleExpanded) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            ThreadStatusDot(thread = thread)
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onSelectThread),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = threadDisplayTitle(thread),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (thread.isSubagent) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = thread.lastUpdatedLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (thread.isRunning) {
                    Text(
                        text = "Running",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (thread.syncState == RemodexThreadSyncState.ARCHIVED_LOCAL) {
                    Text(
                        text = "Archived",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreHoriz,
                    contentDescription = "Thread actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        menuExpanded = false
                        renameExpanded = true
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (thread.syncState == RemodexThreadSyncState.ARCHIVED_LOCAL) {
                                "Unarchive"
                            } else {
                                "Archive"
                            },
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        if (thread.syncState == RemodexThreadSyncState.ARCHIVED_LOCAL) {
                            onUnarchiveThread()
                        } else {
                            onArchiveThread()
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        menuExpanded = false
                        onDeleteThread()
                    },
                )
            }
        }

        DropdownMenu(
            expanded = renameExpanded,
            onDismissRequest = { renameExpanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = renameDraft,
                            onValueChange = { renameDraft = it },
                            label = { Text("Thread name") },
                            singleLine = true,
                        )
                        OutlinedButton(
                            onClick = {
                                renameExpanded = false
                                onRenameThread(renameDraft)
                            },
                        ) {
                            Text("Save")
                        }
                    }
                },
                onClick = {},
            )
        }
    }
}

@Composable
private fun ThreadStatusDot(thread: RemodexThreadSummary) {
    val color = when {
        thread.isRunning -> MaterialTheme.colorScheme.primary
        thread.syncState == RemodexThreadSyncState.ARCHIVED_LOCAL -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        modifier = Modifier.size(10.dp),
        shape = CircleShape,
        color = color,
    ) {}
}

@Composable
private fun EmptyThreadsState(
    isConnected: Boolean,
    isFiltering: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = when {
                isFiltering -> "No matching conversations"
                isConnected -> "No conversations"
                else -> "Connect to view conversations"
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = if (isFiltering) {
                "Try a different project name, thread title, or preview snippet."
            } else {
                "Your paired Mac will populate live and archived chats here."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SidebarFooter(
    trustedMacName: String?,
    trustedMacTitle: String,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenSettings) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (!trustedMacName.isNullOrBlank()) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = trustedMacTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = trustedMacName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun rootThreads(threads: List<RemodexThreadSummary>): List<RemodexThreadSummary> {
    val ids = threads.map(RemodexThreadSummary::id).toSet()
    return threads.filter { thread ->
        thread.parentThreadId.isNullOrBlank() || thread.parentThreadId !in ids
    }
}

private fun threadDisplayTitle(thread: RemodexThreadSummary): String {
    if (!thread.agentNickname.isNullOrBlank()) {
        return buildString {
            append(thread.agentNickname)
            thread.agentRole?.takeIf(String::isNotBlank)?.let { role ->
                append(" [")
                append(role)
                append(']')
            }
        }
    }
    return thread.title
}
