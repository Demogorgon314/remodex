@file:OptIn(ExperimentalFoundationApi::class)

package com.emanueledipietro.remodex.feature.threads

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.emanueledipietro.remodex.R
import com.emanueledipietro.remodex.feature.appshell.AppUiState
import com.emanueledipietro.remodex.model.isCodexManagedWorktreeProject
import com.emanueledipietro.remodex.model.RemodexProjectDirectoryEntry
import com.emanueledipietro.remodex.model.RemodexProjectDirectoryListing
import com.emanueledipietro.remodex.model.RemodexProjectLocation
import com.emanueledipietro.remodex.model.RemodexThreadSummary
import com.emanueledipietro.remodex.model.RemodexThreadSyncState
import com.emanueledipietro.remodex.ui.theme.remodexConversationChrome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ProjectPreviewCount = 10
private const val SidebarFooterTestTag = "sidebar_footer"
private const val SidebarNewChatButtonTag = "sidebar_new_chat_button"
private const val SidebarProjectNewChatButtonTagPrefix = "sidebar_project_new_chat_button_"
private const val SidebarRenameTextFieldTag = "sidebar_rename_text_field"
private val SidebarRowCornerRadius = 14.dp

private data class ThreadRenamePromptState(
    val threadId: String = "",
    val currentTitle: String = "",
    val draftTitle: String = "",
    val isPresented: Boolean = false,
)

private data class DeleteManagedWorktreePromptState(
    val projectPath: String = "",
    val projectLabel: String = "",
    val isPresented: Boolean = false,
)

internal sealed interface SidebarRowModel {
    val key: String
    val contentType: String
}

internal data class ProjectHeaderSidebarRow(
    val group: SidebarThreadGroup,
    val canCreateThread: Boolean,
    val isCreatingThread: Boolean,
) : SidebarRowModel {
    override val key: String = "project-header:${group.id}"
    override val contentType: String = "project_header"
}

internal data class ArchivedHeaderSidebarRow(
    val group: SidebarThreadGroup,
    val isExpanded: Boolean,
) : SidebarRowModel {
    override val key: String = "archived-header:${group.id}"
    override val contentType: String = "archived_header"
}

internal data class ThreadSidebarRow(
    val thread: RemodexThreadSummary,
    val depth: Int,
    val isSelected: Boolean,
    val hasChildren: Boolean,
    val isExpanded: Boolean,
) : SidebarRowModel {
    override val key: String = "thread:${thread.id}"
    override val contentType: String = "thread"
}

internal data class ProjectShowMoreSidebarRow(
    val groupId: String,
    val hiddenCount: Int,
) : SidebarRowModel {
    override val key: String = "project-show-more:$groupId"
    override val contentType: String = "project_show_more"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadsScreen(
    uiState: AppUiState,
    onSelectThread: (String) -> Unit,
    onRefreshThreads: () -> Unit,
    onRetryConnection: () -> Unit,
    onCreateThread: (String?) -> Unit,
    onCreateWorktreeThread: (String) -> Unit,
    onFetchProjectQuickLocations: suspend () -> List<RemodexProjectLocation>,
    onListProjectDirectory: suspend (String) -> RemodexProjectDirectoryListing,
    onSearchProjectDirectories: suspend (String, String) -> List<RemodexProjectDirectoryEntry>,
    onCreateProjectDirectory: suspend (String, String) -> String,
    onSetProjectGroupCollapsed: (String, Boolean) -> Unit,
    onRenameThread: (String, String) -> Unit,
    onRegenerateThreadTitle: (String, (Boolean) -> Unit) -> Unit,
    onArchiveThread: (String) -> Unit,
    onUnarchiveThread: (String) -> Unit,
    onDeleteThread: (String) -> Unit,
    onDeleteManagedWorktreeProject: (String) -> Unit,
    onArchiveProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMyMacs: () -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    var isNewChatSheetPresented by rememberSaveable { mutableStateOf(false) }
    var isLocalFolderBrowserPresented by rememberSaveable { mutableStateOf(false) }
    var renamePromptState by remember {
        mutableStateOf(ThreadRenamePromptState())
    }
    var deleteManagedWorktreePromptState by remember {
        mutableStateOf(DeleteManagedWorktreePromptState())
    }
    var expandedProjectIds by remember { mutableStateOf(setOf<String>()) }
    var knownProjectGroupIds by remember { mutableStateOf(setOf<String>()) }
    var hasInitializedProjectExpansion by rememberSaveable { mutableStateOf(false) }
    var revealedProjectGroupIds by remember { mutableStateOf(setOf<String>()) }
    var expandedSubagentParentIds by remember { mutableStateOf(setOf<String>()) }
    var regeneratingTitleThreadIds by remember { mutableStateOf(setOf<String>()) }

    val groups = remember(uiState.threads, searchText) {
        SidebarThreadGrouping.makeGroups(
            threads = uiState.threads,
            query = searchText,
        )
    }
    val projectGroups = remember(groups) {
        groups.filter { group -> group.kind == SidebarThreadGroupKind.PROJECT }
    }
    val newChatProjectGroups = remember(uiState.threads) {
        SidebarThreadGrouping.makeGroups(
            threads = uiState.threads,
            query = "",
        ).filter { group ->
            group.kind == SidebarThreadGroupKind.PROJECT && !group.projectPath.isNullOrBlank()
        }
    }
    val projectGroupIds = remember(projectGroups) { projectGroups.map(SidebarThreadGroup::id).toSet() }
    val selectedThreadId = uiState.selectedThread?.id
    val selectedProjectGroupId = remember(groups, uiState.selectedThread) {
        SidebarProjectExpansionState.groupIdContainingSelectedThread(
            selectedThread = uiState.selectedThread,
            groups = groups,
        )
    }
    val selectedSubagentAncestorIds = remember(uiState.threads, selectedThreadId) {
        selectedSubagentAncestorIds(
            threads = uiState.threads,
            selectedThreadId = selectedThreadId,
        )
    }
    val visibleRows = remember(
        groups,
        selectedThreadId,
        searchText,
        expandedProjectIds,
        revealedProjectGroupIds,
        expandedSubagentParentIds,
        uiState.canCreateThread,
        uiState.isCreatingThread,
        archivedExpanded,
    ) {
        SidebarRowsBuilder.buildRows(
            groups = groups,
            selectedThreadId = selectedThreadId,
            isFiltering = searchText.isNotBlank(),
            expandedProjectIds = expandedProjectIds,
            revealedProjectGroupIds = revealedProjectGroupIds,
            expandedSubagentParentIds = expandedSubagentParentIds,
            canCreateThread = uiState.canCreateThread,
            isCreatingThread = uiState.isCreatingThread,
            archivedExpanded = archivedExpanded,
        )
    }
    val toggleSubagentExpansion: (String) -> Unit = { threadId ->
        expandedSubagentParentIds = expandedSubagentParentIds.toMutableSet().apply {
            if (!add(threadId)) {
                remove(threadId)
            }
        }
    }
    val presentRenamePrompt: (String, String) -> Unit = { threadId, currentTitle ->
        renamePromptState = ThreadRenamePromptState(
            threadId = threadId,
            currentTitle = currentTitle,
            draftTitle = currentTitle,
            isPresented = true,
        )
    }

    LaunchedEffect(projectGroupIds, uiState.collapsedProjectGroupIds) {
        val snapshot = SidebarProjectExpansionState.synchronizedState(
            currentExpandedGroupIds = expandedProjectIds,
            knownGroupIds = knownProjectGroupIds,
            visibleGroups = groups,
            hasInitialized = hasInitializedProjectExpansion,
            persistedCollapsedGroupIds = uiState.collapsedProjectGroupIds,
        )
        expandedProjectIds = snapshot.expandedGroupIds
        knownProjectGroupIds = snapshot.knownGroupIds
        hasInitializedProjectExpansion = true
        revealedProjectGroupIds = revealedProjectGroupIds.intersect(projectGroupIds)
    }

    LaunchedEffect(selectedProjectGroupId, uiState.collapsedProjectGroupIds) {
        selectedProjectGroupId?.let { groupId ->
            if (!SidebarProjectExpansionState.shouldAutoRevealSelectedGroup(groupId, uiState.collapsedProjectGroupIds)) {
                return@let
            }
            expandedProjectIds = expandedProjectIds + groupId
        }
    }

    LaunchedEffect(selectedSubagentAncestorIds) {
        if (selectedSubagentAncestorIds.isNotEmpty()) {
            expandedSubagentParentIds = expandedSubagentParentIds + selectedSubagentAncestorIds
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        SidebarHeader(
            uiState = uiState,
            searchText = searchText,
            onSearchTextChange = { searchText = it },
            onSearchActiveChange = onSearchActiveChange,
            onOpenNewChat = { isNewChatSheetPresented = true },
            onRetryConnection = onRetryConnection,
        )

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshingThreads,
            onRefresh = onRefreshThreads,
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp),
            ) {
                if (groups.isEmpty()) {
                    item {
                        EmptyThreadsState(
                            isConnected = uiState.isConnected,
                            isFiltering = searchText.isNotBlank(),
                        )
                    }
                } else {
                    items(
                        items = visibleRows,
                        key = SidebarRowModel::key,
                        contentType = SidebarRowModel::contentType,
                    ) { row ->
                        when (row) {
                            is ProjectHeaderSidebarRow -> {
                                ProjectHeaderRow(
                                    group = row.group,
                                    canCreateThread = row.canCreateThread,
                                    isCreatingThread = row.isCreatingThread,
                                    onToggleExpanded = {
                                        val isCurrentlyExpanded = row.group.id in expandedProjectIds
                                        expandedProjectIds = expandedProjectIds.toMutableSet().apply {
                                            if (isCurrentlyExpanded) {
                                                remove(row.group.id)
                                            } else {
                                                add(row.group.id)
                                            }
                                        }
                                        if (isCurrentlyExpanded) {
                                            revealedProjectGroupIds = revealedProjectGroupIds - row.group.id
                                        }
                                        onSetProjectGroupCollapsed(row.group.id, isCurrentlyExpanded)
                                    },
                                    onCreateThread = { onCreateThread(row.group.projectPath) },
                                    onArchiveProject = {
                                        row.group.projectPath?.let(onArchiveProject)
                                    },
                                    onDeleteManagedWorktreeProject = { projectPath, projectLabel ->
                                        deleteManagedWorktreePromptState = DeleteManagedWorktreePromptState(
                                            projectPath = projectPath,
                                            projectLabel = projectLabel,
                                            isPresented = true,
                                        )
                                    },
                                )
                            }

                            is ArchivedHeaderSidebarRow -> {
                                ArchivedHeaderRow(
                                    group = row.group,
                                    archivedExpanded = row.isExpanded,
                                    onArchivedExpandedChange = { archivedExpanded = it },
                                )
                            }

                            is ThreadSidebarRow -> {
                                ThreadRow(
                                    thread = row.thread,
                                    isSelected = row.isSelected,
                                    depth = row.depth,
                                    hasChildren = row.hasChildren,
                                    isExpanded = row.isExpanded,
                                    isRegeneratingTitle = row.thread.id in regeneratingTitleThreadIds,
                                    onToggleExpanded = if (row.hasChildren) {
                                        { toggleSubagentExpansion(row.thread.id) }
                                    } else {
                                        null
                                    },
                                    onSelectThread = { onSelectThread(row.thread.id) },
                                    onRenameThread = presentRenamePrompt,
                                    onRegenerateThreadTitle = {
                                        if (row.thread.id !in regeneratingTitleThreadIds) {
                                            regeneratingTitleThreadIds = regeneratingTitleThreadIds + row.thread.id
                                            onRegenerateThreadTitle(row.thread.id) {
                                                regeneratingTitleThreadIds = regeneratingTitleThreadIds - row.thread.id
                                            }
                                        }
                                    },
                                    onArchiveThread = { onArchiveThread(row.thread.id) },
                                    onUnarchiveThread = { onUnarchiveThread(row.thread.id) },
                                    onDeleteThread = { onDeleteThread(row.thread.id) },
                                )
                            }

                            is ProjectShowMoreSidebarRow -> {
                                ShowMoreButton(
                                    hiddenCount = row.hiddenCount,
                                    onRevealAll = {
                                        revealedProjectGroupIds = revealedProjectGroupIds + row.groupId
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        SidebarFooter(
            trustedMacName = uiState.trustedMac?.name,
            trustedMacTitle = if (uiState.isConnected) "Connected to computer" else "Saved computer",
            onOpenSettings = onOpenSettings,
            onOpenMyMacs = onOpenMyMacs,
        )
    }

    if (isNewChatSheetPresented) {
        SidebarNewChatSheet(
            projectGroups = newChatProjectGroups,
            canCreateThread = uiState.canCreateThread,
            isCreatingThread = uiState.isCreatingThread,
            supportsManagedWorktreeCreation = uiState.supportsManagedWorktreeCreation,
            onDismiss = { isNewChatSheetPresented = false },
            onBrowseLocalFolder = {
                isNewChatSheetPresented = false
                isLocalFolderBrowserPresented = true
            },
            onCreateThread = { projectPath ->
                if (!uiState.canCreateThread || uiState.isCreatingThread) {
                    return@SidebarNewChatSheet
                }
                isNewChatSheetPresented = false
                onCreateThread(projectPath)
            },
            onCreateWorktreeThread = { projectPath ->
                if (!uiState.canCreateThread || uiState.isCreatingThread) {
                    return@SidebarNewChatSheet
                }
                isNewChatSheetPresented = false
                onCreateWorktreeThread(projectPath)
            },
        )
    }

    if (isLocalFolderBrowserPresented) {
        SidebarLocalFolderBrowserSheet(
            onDismiss = { isLocalFolderBrowserPresented = false },
            onSelectFolder = { projectPath ->
                if (!uiState.canCreateThread || uiState.isCreatingThread) {
                    return@SidebarLocalFolderBrowserSheet
                }
                isLocalFolderBrowserPresented = false
                onCreateThread(projectPath)
            },
            onFetchProjectQuickLocations = onFetchProjectQuickLocations,
            onListProjectDirectory = onListProjectDirectory,
            onSearchProjectDirectories = onSearchProjectDirectories,
            onCreateProjectDirectory = onCreateProjectDirectory,
        )
    }

    if (renamePromptState.isPresented) {
        ThreadRenameDialog(
            state = renamePromptState,
            onDismiss = {
                renamePromptState = renamePromptState.copy(isPresented = false)
            },
            onDraftChange = { updatedDraft ->
                renamePromptState = renamePromptState.copy(draftTitle = updatedDraft)
            },
            onRename = { trimmedTitle ->
                val targetThreadId = renamePromptState.threadId
                renamePromptState = renamePromptState.copy(isPresented = false)
                onRenameThread(targetThreadId, trimmedTitle)
            },
        )
    }

    if (deleteManagedWorktreePromptState.isPresented) {
        DeleteManagedWorktreeDialog(
            state = deleteManagedWorktreePromptState,
            onDismiss = {
                deleteManagedWorktreePromptState = deleteManagedWorktreePromptState.copy(isPresented = false)
            },
            onConfirmDelete = { projectPath ->
                deleteManagedWorktreePromptState = deleteManagedWorktreePromptState.copy(isPresented = false)
                onDeleteManagedWorktreeProject(projectPath)
            },
        )
    }
}

@Composable
private fun SidebarHeader(
    uiState: AppUiState,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onOpenNewChat: () -> Unit,
    onRetryConnection: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(26.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
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

        SidebarNewChatButton(
            enabled = uiState.canCreateThread,
            isLoading = uiState.isCreatingThread,
            onClick = onOpenNewChat,
        )

        if (!uiState.isConnected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = uiState.connectionHeadline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRetryConnection) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun SidebarNewChatButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val isEnabled = enabled && !isLoading
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SidebarNewChatButtonTag)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .alpha(if (isEnabled) 1f else 0.35f)
            .combinedClickable(
                enabled = isEnabled,
                role = Role.Button,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = if (isLoading) "Creating..." else "New Chat",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SidebarNewChatSheet(
    projectGroups: List<SidebarThreadGroup>,
    canCreateThread: Boolean,
    isCreatingThread: Boolean,
    supportsManagedWorktreeCreation: Boolean,
    onDismiss: () -> Unit,
    onBrowseLocalFolder: () -> Unit,
    onCreateThread: (String?) -> Unit,
    onCreateWorktreeThread: (String) -> Unit,
) {
    val chrome = remodexConversationChrome()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f))
                .padding(top = 12.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.96f)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = chrome.panelSurfaceStrong,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(44.dp)
                                .size(width = 44.dp, height = 4.dp),
                            shape = CircleShape,
                            color = chrome.tertiaryText.copy(alpha = 0.35f),
                        ) {}
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Surface(
                            modifier = Modifier.align(Alignment.CenterStart),
                            shape = RoundedCornerShape(999.dp),
                            color = chrome.mutedSurface,
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(
                                    text = "Close",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = chrome.titleText,
                                )
                            }
                        }

                        Text(
                            text = "Start new chat",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = chrome.titleText,
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Choose a project for this chat",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = chrome.secondaryText,
                                )
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = chrome.panelSurface,
                                ) {
                                    SidebarNewChatActionRow(
                                        iconSystemName = "folder.badge.plus",
                                        title = "Add Local Folder",
                                        subtitle = "Browse or create a folder on your computer.",
                                        enabled = canCreateThread && !isCreatingThread,
                                        onClick = onBrowseLocalFolder,
                                    )
                                }
                            }
                        }

                        if (projectGroups.isNotEmpty()) {
                            item {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        text = "Local",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = chrome.titleText,
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(26.dp),
                                        color = chrome.panelSurface,
                                    ) {
                                        Column {
                                            projectGroups.forEachIndexed { index, group ->
                                                SidebarNewChatActionRow(
                                                    iconSystemName = group.iconSystemName,
                                                    title = group.label,
                                                    subtitle = null,
                                                    enabled = canCreateThread && !isCreatingThread,
                                                    onClick = { onCreateThread(group.projectPath) },
                                                )
                                                if (index < projectGroups.lastIndex) {
                                                    HorizontalDivider(
                                                        color = chrome.subtleBorder,
                                                        modifier = Modifier.padding(horizontal = 18.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (supportsManagedWorktreeCreation) {
                                item {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Text(
                                            text = "Worktree",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = chrome.titleText,
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(26.dp),
                                            color = chrome.panelSurface,
                                        ) {
                                            Column {
                                                projectGroups.forEachIndexed { index, group ->
                                                    SidebarNewChatActionRow(
                                                        iconSystemName = "arrow.triangle.branch",
                                                        title = group.label,
                                                        subtitle = "Detached worktree from the default branch.",
                                                        enabled = canCreateThread && !isCreatingThread,
                                                        onClick = { group.projectPath?.let(onCreateWorktreeThread) },
                                                    )
                                                    if (index < projectGroups.lastIndex) {
                                                        HorizontalDivider(
                                                            color = chrome.subtleBorder,
                                                            modifier = Modifier.padding(horizontal = 18.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            SidebarNewChatCloudCard(
                                enabled = canCreateThread && !isCreatingThread,
                                onClick = { onCreateThread(null) },
                            )
                        }

                        item {
                            Text(
                                text = "Chats started in a project stay scoped to that working directory. Worktree chats start in a managed detached worktree. If you pick Cloud, the chat is global.",
                                style = MaterialTheme.typography.bodySmall,
                                color = chrome.secondaryText,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarLocalFolderBrowserSheet(
    onDismiss: () -> Unit,
    onSelectFolder: (String) -> Unit,
    onFetchProjectQuickLocations: suspend () -> List<RemodexProjectLocation>,
    onListProjectDirectory: suspend (String) -> RemodexProjectDirectoryListing,
    onSearchProjectDirectories: suspend (String, String) -> List<RemodexProjectDirectoryEntry>,
    onCreateProjectDirectory: suspend (String, String) -> String,
) {
    val chrome = remodexConversationChrome()
    val coroutineScope = rememberCoroutineScope()
    var quickLocations by remember { mutableStateOf<List<RemodexProjectLocation>>(emptyList()) }
    var currentPath by rememberSaveable { mutableStateOf<String?>(null) }
    var parentPath by rememberSaveable { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<RemodexProjectDirectoryEntry>>(emptyList()) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<RemodexProjectDirectoryEntry>>(emptyList()) }
    var newFolderName by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var searchErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var isCreatingFolder by rememberSaveable { mutableStateOf(false) }
    var isShowingNewFolderPrompt by rememberSaveable { mutableStateOf(false) }
    var activeLoadRequestId by remember { mutableStateOf(0) }

    fun loadDirectory(path: String) {
        val requestId = activeLoadRequestId + 1
        activeLoadRequestId = requestId
        searchText = ""
        searchResults = emptyList()
        searchErrorMessage = null
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            runCatching { onListProjectDirectory(path) }
                .onSuccess { listing ->
                    if (activeLoadRequestId != requestId) {
                        return@onSuccess
                    }
                    currentPath = listing.path
                    parentPath = listing.parentPath
                    entries = listing.entries
                }
                .onFailure { error ->
                    if (activeLoadRequestId == requestId) {
                        errorMessage = error.message ?: "Could not load folders."
                    }
                }
            if (activeLoadRequestId == requestId) {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        runCatching { onFetchProjectQuickLocations() }
            .onSuccess { locations ->
                quickLocations = locations
                val startPath = locations.firstOrNull { it.id == "developer" }?.path
                    ?: locations.firstOrNull()?.path
                if (startPath != null) {
                    isLoading = false
                    loadDirectory(startPath)
                } else {
                    errorMessage = "No local folders are available from this computer."
                    isLoading = false
                }
            }
            .onFailure { error ->
                errorMessage = error.message ?: "Could not load folders."
                isLoading = false
            }
    }

    LaunchedEffect(searchText, currentPath) {
        val rootPath = currentPath
        val query = searchText.trim()
        if (rootPath.isNullOrBlank() || query.isEmpty()) {
            searchResults = emptyList()
            searchErrorMessage = null
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        searchErrorMessage = null
        delay(250)
        runCatching { onSearchProjectDirectories(rootPath, query) }
            .onSuccess { results -> searchResults = results }
            .onFailure { error ->
                if (error is CancellationException) {
                    throw error
                }
                searchResults = emptyList()
                searchErrorMessage = error.message ?: "Could not search folders."
            }
        isSearching = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f))
                .padding(top = 12.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.96f)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = chrome.panelSurfaceStrong,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier.size(width = 44.dp, height = 4.dp),
                            shape = CircleShape,
                            color = chrome.tertiaryText.copy(alpha = 0.35f),
                        ) {}
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        TextButton(
                            modifier = Modifier.align(Alignment.CenterStart),
                            onClick = onDismiss,
                        ) {
                            Text("Close", color = chrome.secondaryText)
                        }
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                enabled = currentPath != null && !isCreatingFolder,
                                onClick = {
                                    newFolderName = ""
                                    isShowingNewFolderPrompt = true
                                },
                            ) {
                                SidebarSymbolIcon(
                                    symbolName = "folder.badge.plus",
                                    tint = if (currentPath != null && !isCreatingFolder) {
                                        chrome.secondaryText
                                    } else {
                                        chrome.tertiaryText
                                    },
                                )
                            }
                        }
                        Text(
                            text = "Add Local Folder",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = chrome.titleText,
                        )
                    }
                    SidebarLocalFolderSearchBar(
                        query = searchText,
                        onQueryChange = { searchText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        errorMessage?.let { message ->
                            item {
                                SidebarLocalFolderSection {
                                    SidebarLocalFolderTextRow(
                                        text = message,
                                        isError = true,
                                    )
                                }
                            }
                        }
                        if (quickLocations.isNotEmpty()) {
                            item {
                                SidebarLocalFolderSection(title = "Locations") {
                                    quickLocations.forEachIndexed { index, location ->
                                        SidebarLocalFolderEntryRow(
                                            iconSystemName = "folder",
                                            title = location.label,
                                            subtitle = location.path,
                                            onClick = { loadDirectory(location.path) },
                                        )
                                        SidebarLocalFolderDivider(visible = index < quickLocations.lastIndex)
                                    }
                                }
                            }
                        }
                        item {
                            val query = searchText.trim()
                            SidebarLocalFolderSection(
                                title = if (query.isEmpty()) "Folders" else "Matching Folders",
                            ) {
                                if (query.isEmpty()) {
                                    SidebarLocalFolderBrowserRows(
                                        parentPath = parentPath,
                                        entries = entries,
                                        isLoading = isLoading,
                                        onOpenFolder = ::loadDirectory,
                                    )
                                } else {
                                    SidebarLocalFolderSearchRows(
                                        searchResults = searchResults,
                                        searchErrorMessage = searchErrorMessage,
                                        isSearching = isSearching,
                                        onOpenFolder = ::loadDirectory,
                                    )
                                }
                            }
                        }
                    }
                    SidebarLocalFolderBottomActionBar(
                        currentPath = currentPath,
                        isEnabled = currentPath != null,
                        onUseFolder = { currentPath?.let(onSelectFolder) },
                    )
                }
            }
        }
    }

    if (isShowingNewFolderPrompt) {
        AlertDialog(
            onDismissRequest = {
                if (!isCreatingFolder) {
                    isShowingNewFolderPrompt = false
                }
            },
            title = { Text("New Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Create this folder on your computer and start a chat there.")
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder name") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isCreatingFolder &&
                        !currentPath.isNullOrBlank() &&
                        newFolderName.trim().isNotEmpty(),
                    onClick = {
                        val parent = currentPath ?: return@TextButton
                        val folderName = newFolderName.trim()
                        isShowingNewFolderPrompt = false
                        coroutineScope.launch {
                            isCreatingFolder = true
                            runCatching { onCreateProjectDirectory(parent, folderName) }
                                .onSuccess { createdPath -> onSelectFolder(createdPath) }
                                .onFailure { error ->
                                    errorMessage = error.message ?: "Could not create folder."
                                }
                            isCreatingFolder = false
                        }
                    },
                ) {
                    if (isCreatingFolder) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Create")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isCreatingFolder,
                    onClick = { isShowingNewFolderPrompt = false },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SidebarLocalFolderSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = remodexConversationChrome()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = chrome.mutedSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = chrome.tertiaryText,
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = chrome.titleText),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search folders",
                                style = MaterialTheme.typography.bodyMedium,
                                color = chrome.tertiaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (query.isNotEmpty()) {
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = { onQueryChange("") },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = "Clear search",
                        modifier = Modifier.size(16.dp),
                        tint = chrome.tertiaryText,
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarLocalFolderSection(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val chrome = remodexConversationChrome()
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = chrome.secondaryText,
            )
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = chrome.panelSurface,
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SidebarLocalFolderBrowserRows(
    parentPath: String?,
    entries: List<RemodexProjectDirectoryEntry>,
    isLoading: Boolean,
    onOpenFolder: (String) -> Unit,
) {
    parentPath?.let { parent ->
        SidebarLocalFolderEntryRow(
            iconSystemName = "arrow.uturn.left",
            title = "Parent Folder",
            subtitle = parent,
            onClick = { onOpenFolder(parent) },
        )
        SidebarLocalFolderDivider(visible = true)
    }
    when {
        isLoading -> SidebarLocalFolderStatusRow("Loading")
        entries.isEmpty() -> SidebarLocalFolderTextRow("No child folders here.")
        else -> entries.forEachIndexed { index, entry ->
            SidebarLocalFolderEntryRow(
                iconSystemName = if (entry.isSymlink) "folder.badge.gearshape" else "folder",
                title = entry.name,
                subtitle = entry.path,
                onClick = { onOpenFolder(entry.path) },
            )
            SidebarLocalFolderDivider(visible = index < entries.lastIndex)
        }
    }
}

@Composable
private fun SidebarLocalFolderSearchRows(
    searchResults: List<RemodexProjectDirectoryEntry>,
    searchErrorMessage: String?,
    isSearching: Boolean,
    onOpenFolder: (String) -> Unit,
) {
    when {
        isSearching -> SidebarLocalFolderStatusRow("Searching folders...")
        searchErrorMessage != null -> SidebarLocalFolderTextRow(
            text = searchErrorMessage,
            isError = true,
        )
        searchResults.isEmpty() -> SidebarLocalFolderTextRow("No matching folders under this folder.")
        else -> searchResults.forEachIndexed { index, entry ->
            SidebarLocalFolderEntryRow(
                iconSystemName = if (entry.isSymlink) "folder.badge.gearshape" else "folder",
                title = entry.name,
                subtitle = entry.path,
                onClick = { onOpenFolder(entry.path) },
            )
            SidebarLocalFolderDivider(visible = index < searchResults.lastIndex)
        }
    }
}

@Composable
private fun SidebarLocalFolderDivider(visible: Boolean) {
    if (!visible) {
        return
    }
    val chrome = remodexConversationChrome()
    HorizontalDivider(
        color = chrome.subtleBorder,
        modifier = Modifier.padding(start = 48.dp, end = 0.dp),
    )
}

@Composable
private fun SidebarLocalFolderStatusRow(text: String) {
    val chrome = remodexConversationChrome()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = chrome.secondaryText,
        )
    }
}

@Composable
private fun SidebarLocalFolderTextRow(
    text: String,
    isError: Boolean = false,
) {
    val chrome = remodexConversationChrome()
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else chrome.secondaryText,
    )
}

@Composable
private fun SidebarLocalFolderEntryRow(
    iconSystemName: String,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    val chrome = remodexConversationChrome()
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
            )
    } else {
        Modifier.fillMaxWidth()
    }
    Row(
        modifier = rowModifier
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.width(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            SidebarSymbolIcon(
                symbolName = iconSystemName,
                tint = chrome.secondaryText,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = chrome.titleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                ),
                color = chrome.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(16.dp),
                tint = chrome.tertiaryText,
            )
        }
    }
}

@Composable
private fun SidebarLocalFolderBottomActionBar(
    currentPath: String?,
    isEnabled: Boolean,
    onUseFolder: () -> Unit,
) {
    val chrome = remodexConversationChrome()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = chrome.panelSurfaceStrong,
    ) {
        Column {
            HorizontalDivider(color = chrome.subtleBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.width(22.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    SidebarSymbolIcon(
                        symbolName = "folder.fill",
                        tint = if (currentPath == null) chrome.tertiaryText else chrome.secondaryText,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = currentPath?.let(::localFolderDisplayName) ?: "No folder selected",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (currentPath == null) chrome.secondaryText else chrome.titleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = currentPath ?: "Choose a folder to start a chat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = chrome.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    enabled = isEnabled,
                    onClick = onUseFolder,
                ) {
                    Text("Use Folder")
                }
            }
        }
    }
}

@Composable
private fun SidebarNewChatProjectRow(
    label: String,
    iconSystemName: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SidebarSymbolIcon(
            symbolName = iconSystemName,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SidebarNewChatActionRow(
    iconSystemName: String,
    title: String,
    subtitle: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val chrome = remodexConversationChrome()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = if (subtitle == null) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.width(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            SidebarSymbolIcon(
                symbolName = iconSystemName,
                tint = chrome.secondaryText,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = chrome.titleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let { value ->
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = chrome.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = chrome.tertiaryText,
        )
    }
}

@Composable
private fun SidebarNewChatWorktreeProjectRow(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val chrome = remodexConversationChrome()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SidebarSymbolIcon(
            symbolName = "arrow.triangle.branch",
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Start a new chat in a managed detached worktree from the repo default branch.",
                style = MaterialTheme.typography.bodyMedium,
                color = chrome.secondaryText,
            )
        }
    }
}

@Composable
private fun SidebarNewChatCloudCard(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val chrome = remodexConversationChrome()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = chrome.panelSurface,
    ) {
        SidebarNewChatActionRow(
            iconSystemName = "cloud",
            title = "Cloud",
            subtitle = "Start a chat without a working directory.",
            enabled = enabled,
            onClick = onClick,
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(SidebarRowCornerRadius),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = sidebarSymbolImageVector("magnifyingglass"),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    IconButton(
                        modifier = Modifier.size(24.dp),
                        onClick = { onTextChange("") },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cancel,
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
private fun ProjectHeaderRow(
    group: SidebarThreadGroup,
    canCreateThread: Boolean,
    isCreatingThread: Boolean,
    onToggleExpanded: () -> Unit,
    onCreateThread: () -> Unit,
    onArchiveProject: () -> Unit,
    onDeleteManagedWorktreeProject: (String, String) -> Unit,
) {
    var projectMenuExpanded by remember(group.id) { mutableStateOf(false) }
    val managedWorktreeProjectPath = remember(group.projectPath) {
        group.projectPath?.takeIf(::isCodexManagedWorktreeProject)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 18.dp, bottom = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 52.dp)
                .combinedClickable(
                    role = Role.Button,
                    onClick = onToggleExpanded,
                    onLongClick = { projectMenuExpanded = true },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = sidebarSymbolImageVector(group.iconSystemName),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = group.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        ProjectHeaderActionButton(
            icon = Icons.Outlined.Add,
            contentDescription = "New conversation in ${group.label}",
            enabled = canCreateThread && !isCreatingThread,
            isLoading = isCreatingThread,
            onClick = onCreateThread,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .testTag("$SidebarProjectNewChatButtonTagPrefix${group.label}"),
        )

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
            managedWorktreeProjectPath?.let { projectPath ->
                DropdownMenuItem(
                    text = { Text("Delete Worktree") },
                    onClick = {
                        projectMenuExpanded = false
                        onDeleteManagedWorktreeProject(projectPath, group.label)
                    },
                )
            }
        }
    }
}

@Composable
private fun ProjectHeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(30.dp)
            .alpha(if (enabled) 1f else 0.45f),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    enabled = enabled,
                    onClickLabel = contentDescription,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                Icon(
                    modifier = Modifier.size(12.dp),
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ShowMoreButton(
    hiddenCount: Int,
    onRevealAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onRevealAll) {
            Text("Show $hiddenCount more")
        }
    }
}

@Composable
private fun ArchivedHeaderRow(
    group: SidebarThreadGroup,
    archivedExpanded: Boolean,
    onArchivedExpandedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                role = Role.Button,
                onClick = { onArchivedExpandedChange(!archivedExpanded) },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = sidebarSymbolImageVector(group.iconSystemName),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = group.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(if (archivedExpanded) 1f else 0.8f),
        )
    }
}

@Composable
private fun ThreadTreeRow(
    thread: RemodexThreadSummary,
    selectedThreadId: String?,
    depth: Int,
    childrenByParentId: Map<String, List<RemodexThreadSummary>>,
    expandedSubagentParentIds: Set<String>,
    regeneratingTitleThreadIds: Set<String>,
    onToggleSubagentExpansion: (String) -> Unit,
    onSelectThread: (String) -> Unit,
    onRenameThread: (String, String) -> Unit,
    onRegenerateThreadTitle: (String, (Boolean) -> Unit) -> Unit,
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
        isRegeneratingTitle = thread.id in regeneratingTitleThreadIds,
        onToggleExpanded = if (childThreads.isNotEmpty()) {
            { onToggleSubagentExpansion(thread.id) }
        } else {
            null
        },
        onSelectThread = { onSelectThread(thread.id) },
        onRenameThread = onRenameThread,
        onRegenerateThreadTitle = { onRegenerateThreadTitle(thread.id) {} },
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
                regeneratingTitleThreadIds = regeneratingTitleThreadIds,
                onToggleSubagentExpansion = onToggleSubagentExpansion,
                onSelectThread = onSelectThread,
                onRenameThread = onRenameThread,
                onRegenerateThreadTitle = onRegenerateThreadTitle,
                onArchiveThread = onArchiveThread,
                onUnarchiveThread = onUnarchiveThread,
                onDeleteThread = onDeleteThread,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThreadRow(
    thread: RemodexThreadSummary,
    isSelected: Boolean,
    depth: Int,
    hasChildren: Boolean,
    isExpanded: Boolean,
    isRegeneratingTitle: Boolean,
    onToggleExpanded: (() -> Unit)?,
    onSelectThread: () -> Unit,
    onRenameThread: (String, String) -> Unit,
    onRegenerateThreadTitle: () -> Unit,
    onArchiveThread: () -> Unit,
    onUnarchiveThread: () -> Unit,
    onDeleteThread: () -> Unit,
) {
    var menuExpanded by remember(thread.id) { mutableStateOf(false) }
    var menuOffset by remember(thread.id) { mutableStateOf(IntOffset.Zero) }
    val timingLabel = compactTimingLabel(thread.lastUpdatedLabel)
    val performLongPressHaptic = rememberLongPressHaptic()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .background(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    } else {
                        Color.Transparent
                    },
                    shape = RoundedCornerShape(SidebarRowCornerRadius),
                )
                .pointerInput(thread.id, isSelected, hasChildren) {
                    detectTapGestures(
                        onTap = {
                            if (isSelected && hasChildren) {
                                onToggleExpanded?.invoke()
                            } else {
                                onSelectThread()
                            }
                        },
                        onLongPress = { pressOffset ->
                            performLongPressHaptic()
                            menuOffset = IntOffset(
                                x = pressOffset.x.toInt(),
                                y = pressOffset.y.toInt(),
                            )
                            menuExpanded = true
                        },
                    )
                }
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    onClick {
                        if (isSelected && hasChildren) {
                            onToggleExpanded?.invoke()
                        } else {
                            onSelectThread()
                        }
                        true
                    }
                    onLongClick {
                        performLongPressHaptic()
                        menuOffset = IntOffset.Zero
                        menuExpanded = true
                        true
                    }
                }
                .padding(horizontal = 12.dp, vertical = if (thread.isSubagent) 4.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThreadLeadingIndicator(thread = thread)

            val titleStyle = if (thread.isSubagent) {
                MaterialTheme.typography.bodySmall
            } else {
                MaterialTheme.typography.bodyMedium
            }
            val titleWeight = if (thread.isSubagent) FontWeight.Medium else FontWeight.Normal
            if (isRegeneratingTitle) {
                ShimmeringThreadTitle(
                    text = threadDisplayTitle(thread),
                    style = titleStyle,
                    fontWeight = titleWeight,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Text(
                    text = threadDisplayTitle(thread),
                    style = titleStyle,
                    fontWeight = titleWeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (thread.isWaitingOnApproval && !isSelected) {
                    ThreadMetaBadge(
                        text = "Approval",
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.error,
                    )
                }

                if (thread.syncState == RemodexThreadSyncState.ARCHIVED_LOCAL) {
                    ThreadMetaBadge(
                        text = "Archived",
                        containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.tertiary,
                    )
                }

                if (hasChildren && onToggleExpanded != null) {
                    IconButton(
                        modifier = Modifier.size(18.dp),
                        onClick = onToggleExpanded,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = if (isExpanded) {
                                "Collapse subagents"
                            } else {
                                "Expand subagents"
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                timingLabel?.let { label ->
                    Text(
                        text = label,
                        style = if (thread.isSubagent) {
                            MaterialTheme.typography.labelSmall
                        } else {
                            MaterialTheme.typography.bodySmall
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        ThreadContextMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            pressOffset = menuOffset,
        ) {
            ThreadContextMenuItem(
                text = { Text("Rename") },
                onClick = {
                    menuExpanded = false
                    onRenameThread(thread.id, thread.displayTitle)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                    )
                },
            )
            ThreadContextMenuItem(
                text = { Text("Regenerate Title") },
                onClick = {
                    menuExpanded = false
                    onRegenerateThreadTitle()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                    )
                },
            )
            ThreadContextMenuItem(
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
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Archive,
                        contentDescription = null,
                    )
                },
            )
            ThreadContextMenuItem(
                text = { Text("Delete") },
                onClick = {
                    menuExpanded = false
                    onDeleteThread()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@Composable
private fun ShimmeringThreadTitle(
    text: String,
    style: TextStyle,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    val baseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.94f)
    val transition = rememberInfiniteTransition(label = "thread_title_generation_shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = -320f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "thread_title_generation_shimmer_offset",
    )
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, baseColor, highlightColor, baseColor, baseColor),
        start = Offset(shimmerOffset, 0f),
        end = Offset(shimmerOffset + 260f, 0f),
    )

    Text(
        text = text,
        style = style.merge(TextStyle(brush = brush)),
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun rememberLongPressHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}

@Composable
private fun ThreadContextMenu(
    expanded: Boolean,
    pressOffset: IntOffset,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val chrome = remodexConversationChrome()
    val density = LocalDensity.current
    val verticalGapPx = with(density) { 8.dp.roundToPx() }
    val windowMarginPx = with(density) { 12.dp.roundToPx() }
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(expanded) {
        transitionState.targetState = expanded
    }
    if (!transitionState.currentState && !transitionState.targetState) {
        return
    }

    Popup(
        popupPositionProvider = remember(verticalGapPx, windowMarginPx, pressOffset) {
            ThreadContextMenuPositionProvider(
                verticalGapPx = verticalGapPx,
                windowMarginPx = windowMarginPx,
                pressOffset = pressOffset,
            )
        },
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(animationSpec = tween(durationMillis = 150)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 180),
                    initialOffsetY = { fullHeight -> fullHeight / 10 },
                ) +
                scaleIn(
                    animationSpec = tween(durationMillis = 160),
                    initialScale = 0.97f,
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 110)) +
                slideOutVertically(
                    animationSpec = tween(durationMillis = 120),
                    targetOffsetY = { fullHeight -> fullHeight / 14 },
                ) +
                scaleOut(
                    animationSpec = tween(durationMillis = 110),
                    targetScale = 0.985f,
                ),
        ) {
            Surface(
                color = chrome.panelSurfaceStrong,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, chrome.subtleBorder),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 184.dp, max = 236.dp)
                        .padding(vertical = 2.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun ThreadContextMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val chrome = remodexConversationChrome()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides chrome.titleText) {
            leadingIcon?.invoke()
            Box(modifier = Modifier.weight(1f)) {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    text()
                }
            }
        }
    }
}

private class ThreadContextMenuPositionProvider(
    private val verticalGapPx: Int,
    private val windowMarginPx: Int,
    private val pressOffset: IntOffset,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchorX = anchorBounds.left + pressOffset.x
        val preferredX = when (layoutDirection) {
            LayoutDirection.Ltr -> anchorX
            LayoutDirection.Rtl -> anchorX - popupContentSize.width
        }
        val maxX = (windowSize.width - popupContentSize.width - windowMarginPx).coerceAtLeast(windowMarginPx)
        val resolvedX = preferredX.coerceIn(windowMarginPx, maxX)

        val anchorY = anchorBounds.top + pressOffset.y
        val belowY = anchorY + verticalGapPx
        val aboveY = anchorY - popupContentSize.height - verticalGapPx
        val maxY = (windowSize.height - popupContentSize.height - windowMarginPx).coerceAtLeast(windowMarginPx)
        val resolvedY = when {
            belowY <= maxY -> belowY
            aboveY >= windowMarginPx -> aboveY
            else -> belowY.coerceIn(windowMarginPx, maxY)
        }

        return IntOffset(resolvedX, resolvedY)
    }
}

@Composable
private fun ThreadRenameDialog(
    state: ThreadRenamePromptState,
    onDismiss: () -> Unit,
    onDraftChange: (String) -> Unit,
    onRename: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var draftFieldValue by remember(state.threadId) {
        mutableStateOf(
            TextFieldValue(
                text = state.draftTitle,
                selection = TextRange(state.draftTitle.length),
            ),
        )
    }
    LaunchedEffect(state.threadId) {
        draftFieldValue = draftFieldValue.copy(selection = TextRange(draftFieldValue.text.length))
        focusRequester.requestFocus()
    }
    val trimmedDraft = draftFieldValue.text.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onRename(trimmedDraft) },
                enabled = trimmedDraft.isNotEmpty(),
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Rename Conversation") },
        text = {
            OutlinedTextField(
                value = draftFieldValue,
                onValueChange = { value ->
                    draftFieldValue = value
                    onDraftChange(value.text)
                },
                singleLine = true,
                label = { Text("Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag(SidebarRenameTextFieldTag),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
    )
}

@Composable
private fun DeleteManagedWorktreeDialog(
    state: DeleteManagedWorktreePromptState,
    onDismiss: () -> Unit,
    onConfirmDelete: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmDelete(state.projectPath) },
            ) {
                Text(
                    text = "Delete Worktree",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        title = { Text("Delete worktree") },
        text = {
            Text(
                "Delete the managed worktree for ${state.projectLabel} and remove its chats from Android? " +
                    "Your main local checkout will stay untouched.",
            )
        },
    )
}

@Composable
private fun ThreadLeadingIndicator(thread: RemodexThreadSummary) {
    Row(
        modifier = Modifier.width(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (thread.isRunning && !thread.isSubagent) {
            Surface(
                modifier = Modifier.size(10.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {}
        }
    }
}

@Composable
private fun ThreadMetaBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        modifier = Modifier
            .background(containerColor, shape = CircleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun EmptyThreadsState(
    isConnected: Boolean,
    isFiltering: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
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
                "Your paired computer will populate live and archived chats here."
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
    onOpenMyMacs: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SidebarFooterTestTag)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
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
                        imageVector = sidebarSymbolImageVector("gearshape.fill"),
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (!trustedMacName.isNullOrBlank()) {
            Surface(
                onClick = onOpenMyMacs,
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
}

@Composable
private fun SidebarSymbolIcon(
    symbolName: String,
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    if (symbolName == "folder.badge.plus") {
        SidebarFolderBadgeIcon(
            tint = tint,
            badgeIcon = Icons.Outlined.Add,
            modifier = modifier,
            contentDescription = contentDescription,
        )
        return
    }
    if (symbolName == "folder.badge.gearshape") {
        SidebarFolderBadgeIcon(
            tint = tint,
            badgeIcon = Icons.Filled.Settings,
            modifier = modifier,
            contentDescription = contentDescription,
        )
        return
    }
    Icon(
        imageVector = sidebarSymbolImageVector(symbolName),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier,
    )
}

@Composable
private fun SidebarFolderBadgeIcon(
    tint: Color,
    badgeIcon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier.size(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(19.dp),
        )
        Surface(
            modifier = Modifier
                .size(10.dp)
                .align(Alignment.BottomEnd),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.padding(1.dp),
            )
        }
    }
}

private fun sidebarSymbolImageVector(symbolName: String): ImageVector {
    return when (symbolName) {
        "laptopcomputer" -> Icons.Outlined.Computer
        "arrow.triangle.branch" -> Icons.Outlined.AccountTree
        "archivebox" -> Icons.Outlined.Archive
        "magnifyingglass" -> Icons.Outlined.Search
        "gearshape.fill" -> Icons.Filled.Settings
        "cloud" -> Icons.Outlined.Cloud
        "folder.badge.plus" -> Icons.Outlined.Add
        "folder" -> Icons.Outlined.Folder
        "folder.fill" -> Icons.Outlined.Folder
        "folder.badge.gearshape" -> Icons.Outlined.Folder
        "arrow.uturn.left" -> Icons.AutoMirrored.Outlined.ArrowBack
        else -> Icons.Outlined.Folder
    }
}

private fun localFolderDisplayName(path: String): String {
    val normalized = path.trimEnd('/', '\\')
    val lastSeparatorIndex = maxOf(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'))
    val name = if (lastSeparatorIndex >= 0) normalized.substring(lastSeparatorIndex + 1) else normalized
    return name.ifBlank { path }
}

private fun rootThreads(threads: List<RemodexThreadSummary>): List<RemodexThreadSummary> {
    val ids = threads.map(RemodexThreadSummary::id).toSet()
    return threads.filter { thread ->
        thread.parentThreadId.isNullOrBlank() || thread.parentThreadId !in ids
    }
}

internal object SidebarRowsBuilder {
    fun buildRows(
        groups: List<SidebarThreadGroup>,
        selectedThreadId: String?,
        isFiltering: Boolean,
        expandedProjectIds: Set<String>,
        revealedProjectGroupIds: Set<String>,
        expandedSubagentParentIds: Set<String>,
        canCreateThread: Boolean,
        isCreatingThread: Boolean,
        archivedExpanded: Boolean,
    ): List<SidebarRowModel> {
        return buildList {
            groups.forEach { group ->
                when (group.kind) {
                    SidebarThreadGroupKind.PROJECT -> {
                        add(
                            ProjectHeaderSidebarRow(
                                group = group,
                                canCreateThread = canCreateThread,
                                isCreatingThread = isCreatingThread,
                            ),
                        )
                        if (group.id !in expandedProjectIds) {
                            return@forEach
                        }

                        val visibleRoots = SidebarProjectThreadPreviewState.visibleRootThreads(
                            group = group,
                            selectedThreadId = selectedThreadId,
                            isFiltering = isFiltering,
                            manuallyExpanded = group.id in revealedProjectGroupIds,
                        )
                        val childrenByParentId = group.threads.childThreadsByParentId()
                        visibleRoots.forEach { thread ->
                            appendThreadRows(
                                thread = thread,
                                depth = 0,
                                selectedThreadId = selectedThreadId,
                                childrenByParentId = childrenByParentId,
                                expandedSubagentParentIds = expandedSubagentParentIds,
                            )
                        }
                        if (
                            SidebarProjectThreadPreviewState.shouldShowMoreButton(
                                group = group,
                                selectedThreadId = selectedThreadId,
                                isFiltering = isFiltering,
                                manuallyExpanded = group.id in revealedProjectGroupIds,
                            )
                        ) {
                            add(
                                ProjectShowMoreSidebarRow(
                                    groupId = group.id,
                                    hiddenCount = SidebarProjectThreadPreviewState.hiddenRootThreadCount(
                                        group = group,
                                        selectedThreadId = selectedThreadId,
                                    ),
                                ),
                            )
                        }
                    }

                    SidebarThreadGroupKind.ARCHIVED -> {
                        add(
                            ArchivedHeaderSidebarRow(
                                group = group,
                                isExpanded = archivedExpanded,
                            ),
                        )
                        if (!archivedExpanded) {
                            return@forEach
                        }

                        val childrenByParentId = group.threads.childThreadsByParentId()
                        rootThreads(group.threads).forEach { thread ->
                            appendThreadRows(
                                thread = thread,
                                depth = 0,
                                selectedThreadId = selectedThreadId,
                                childrenByParentId = childrenByParentId,
                                expandedSubagentParentIds = expandedSubagentParentIds,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun MutableList<SidebarRowModel>.appendThreadRows(
        thread: RemodexThreadSummary,
        depth: Int,
        selectedThreadId: String?,
        childrenByParentId: Map<String, List<RemodexThreadSummary>>,
        expandedSubagentParentIds: Set<String>,
    ) {
        val childThreads = childrenByParentId[thread.id].orEmpty()
        val isExpanded = thread.id in expandedSubagentParentIds
        add(
            ThreadSidebarRow(
                thread = thread,
                depth = depth,
                isSelected = selectedThreadId == thread.id,
                hasChildren = childThreads.isNotEmpty(),
                isExpanded = isExpanded,
            ),
        )
        if (!isExpanded) {
            return
        }
        childThreads.forEach { child ->
            appendThreadRows(
                thread = child,
                depth = depth + 1,
                selectedThreadId = selectedThreadId,
                childrenByParentId = childrenByParentId,
                expandedSubagentParentIds = expandedSubagentParentIds,
            )
        }
    }
}

private fun List<RemodexThreadSummary>.childThreadsByParentId(): Map<String, List<RemodexThreadSummary>> {
    return filter { thread -> !thread.parentThreadId.isNullOrBlank() }
        .groupBy { thread -> thread.parentThreadId.orEmpty() }
}

internal object SidebarProjectThreadPreviewState {
    fun visibleRootThreads(
        group: SidebarThreadGroup,
        selectedThreadId: String?,
        isFiltering: Boolean,
        manuallyExpanded: Boolean,
    ): List<RemodexThreadSummary> {
        val rootThreads = rootThreads(group.threads)
        if (shouldRevealAllRootThreads(group, rootThreads, selectedThreadId, isFiltering, manuallyExpanded)) {
            return rootThreads
        }
        return rootThreads.take(ProjectPreviewCount)
    }

    fun shouldShowMoreButton(
        group: SidebarThreadGroup,
        selectedThreadId: String?,
        isFiltering: Boolean,
        manuallyExpanded: Boolean,
    ): Boolean {
        val rootThreads = rootThreads(group.threads)
        if (
            group.kind != SidebarThreadGroupKind.PROJECT ||
            rootThreads.size <= ProjectPreviewCount ||
            isFiltering ||
            manuallyExpanded
        ) {
            return false
        }
        return !selectedThreadRequiresExpansion(group, selectedThreadId, rootThreads)
    }

    fun hiddenRootThreadCount(
        group: SidebarThreadGroup,
        selectedThreadId: String?,
    ): Int {
        val rootThreads = rootThreads(group.threads)
        if (selectedThreadRequiresExpansion(group, selectedThreadId, rootThreads)) {
            return 0
        }
        return (rootThreads.size - ProjectPreviewCount).coerceAtLeast(0)
    }

    private fun shouldRevealAllRootThreads(
        group: SidebarThreadGroup,
        rootThreads: List<RemodexThreadSummary>,
        selectedThreadId: String?,
        isFiltering: Boolean,
        manuallyExpanded: Boolean,
    ): Boolean {
        if (group.kind != SidebarThreadGroupKind.PROJECT || rootThreads.size <= ProjectPreviewCount) {
            return true
        }
        if (isFiltering || manuallyExpanded) {
            return true
        }
        return selectedThreadRequiresExpansion(group, selectedThreadId, rootThreads)
    }

    private fun selectedThreadRequiresExpansion(
        group: SidebarThreadGroup,
        selectedThreadId: String?,
        rootThreads: List<RemodexThreadSummary>,
    ): Boolean {
        val selectedThread = group.threads.firstOrNull { thread -> thread.id == selectedThreadId } ?: return false
        val visibleRootThreadIds = rootThreads
            .take(ProjectPreviewCount)
            .mapTo(mutableSetOf(), RemodexThreadSummary::id)
        val selectedRootThreadId = rootThreadIdContaining(
            selectedThread = selectedThread,
            groupThreads = group.threads,
        ) ?: selectedThread.id
        return selectedRootThreadId !in visibleRootThreadIds
    }

    private fun rootThreadIdContaining(
        selectedThread: RemodexThreadSummary,
        groupThreads: List<RemodexThreadSummary>,
    ): String? {
        val threadsById = groupThreads.associateBy(RemodexThreadSummary::id)
        val visitedThreadIds = mutableSetOf(selectedThread.id)
        var currentThread = selectedThread

        while (!currentThread.parentThreadId.isNullOrBlank()) {
            val parentThreadId = currentThread.parentThreadId ?: break
            if (!visitedThreadIds.add(parentThreadId)) {
                break
            }
            val parentThread = threadsById[parentThreadId] ?: break
            currentThread = parentThread
        }

        return currentThread.id
    }
}

private fun selectedSubagentAncestorIds(
    threads: List<RemodexThreadSummary>,
    selectedThreadId: String?,
): Set<String> {
    val selectedThread = threads.firstOrNull { thread -> thread.id == selectedThreadId } ?: return emptySet()
    val threadsById = threads.associateBy(RemodexThreadSummary::id)
    val ancestorIds = mutableSetOf<String>()
    var parentThreadId = selectedThread.parentThreadId

    while (!parentThreadId.isNullOrBlank() && ancestorIds.add(parentThreadId)) {
        parentThreadId = threadsById[parentThreadId]?.parentThreadId
    }

    return ancestorIds
}

private fun compactTimingLabel(lastUpdatedLabel: String): String? {
    return when {
        lastUpdatedLabel == "Updated just now" -> "now"
        lastUpdatedLabel == "Updated yesterday" -> "1d"
        lastUpdatedLabel.startsWith("Updated ") && lastUpdatedLabel.endsWith(" ago") -> {
            lastUpdatedLabel.removePrefix("Updated ").removeSuffix(" ago")
        }
        lastUpdatedLabel.startsWith("Updated ") -> {
            lastUpdatedLabel.removePrefix("Updated ")
        }
        lastUpdatedLabel.isBlank() -> null
        else -> lastUpdatedLabel
    }
}

private fun threadDisplayTitle(thread: RemodexThreadSummary): String {
    return thread.displayTitle
}
