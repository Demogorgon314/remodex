## ADDED Requirements

### Requirement: Thread navigation stays synchronized with bridge history
The Android client SHALL fetch, display, and switch threads while preserving the active selection and SHALL not require repository filtering to access or open a conversation.

#### Scenario: Thread sync keeps the active thread selected
- **WHEN** the bridge refreshes the thread list while the user is viewing an existing thread
- **THEN** the app keeps that thread selected when it is still available and updates the visible metadata in place

#### Scenario: Threads remain accessible across projects
- **WHEN** the user opens or creates a thread associated with a different local project
- **THEN** the app can enter that conversation without first filtering the sidebar or content to a selected repository

### Requirement: The turn timeline preserves item-scoped streaming state
The Android client SHALL render conversation timelines with item-scoped assistant activity, streaming text, reasoning, status, and late-arriving deltas without flattening or duplicating rows.

#### Scenario: Streaming updates extend the active assistant response
- **WHEN** the bridge emits incremental assistant content for a running turn
- **THEN** the app appends that content to the existing in-progress timeline item for that turn

#### Scenario: Late reasoning updates merge into the existing item
- **WHEN** reasoning or activity updates arrive after the initial assistant row has already rendered
- **THEN** the app merges the new detail into the existing item instead of creating a fake extra "Thinking" message

### Requirement: The composer supports send, stop, and queued follow-ups
The Android client SHALL let the user compose prompts, stop active turns, and queue follow-up drafts when a turn is already running.

#### Scenario: Sending while idle starts a turn immediately
- **WHEN** the selected thread has no active turn and the user submits a valid prompt
- **THEN** the app sends the prompt to the bridge and locks the relevant composer interactions for that turn

#### Scenario: Sending while a turn is already running queues the draft
- **WHEN** the selected thread already has a running turn and the user submits another valid prompt
- **THEN** the app stores the follow-up as a queued draft instead of discarding it or interrupting the current turn

#### Scenario: Stop interrupts the active turn
- **WHEN** the user taps Stop for the active turn
- **THEN** the app sends the interrupt request for the correct thread and updates the UI to reflect the stopped state

### Requirement: Runtime controls reflect bridge-supported options
The Android client SHALL expose runtime controls such as reasoning, planning, and access configuration based on bridge-provided capabilities and SHALL preserve thread-specific overrides in the composer UI.

#### Scenario: Runtime metadata populates available controls
- **WHEN** the bridge sync includes supported runtime options for the current thread
- **THEN** the composer presents those options with Android-native controls instead of hardcoded static values

#### Scenario: Thread override state is visible in the composer
- **WHEN** a thread already has runtime overrides in effect
- **THEN** the composer shows those effective selections before the user submits the next prompt
