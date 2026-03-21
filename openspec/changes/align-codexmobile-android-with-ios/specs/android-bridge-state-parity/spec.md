## ADDED Requirements

### Requirement: Android thread and timeline state SHALL be bridge-backed
The Android client SHALL derive production thread, runtime, and timeline state from the secure bridge connection instead of seeded or fake local sync data.

#### Scenario: Production wiring uses bridge-backed repository state
- **WHEN** the Android app starts with a paired or trusted Mac
- **THEN** the app container wires a bridge-backed repository and the visible thread list comes from real bridge-backed state rather than seeded fake snapshots

#### Scenario: Relaunch keeps bridge-backed cached state available
- **WHEN** the user relaunches Android before the bridge finishes reconnecting
- **THEN** the app restores the latest cached bridge-backed thread and runtime state locally and reconciles it when live bridge data arrives

### Requirement: Android SHALL preserve iOS thread and timeline semantics
The Android client SHALL preserve iOS semantics for archived chat reconciliation, queued drafts, item-scoped streaming timeline updates, and stop recovery.

#### Scenario: Archived threads survive refresh
- **WHEN** the bridge returns archived threads or a local thread has already been archived
- **THEN** Android keeps archived chats separate from active chats and does not drop them during later syncs

#### Scenario: Stop resolves the correct active turn after reconnect
- **WHEN** the user taps Stop after the active turn id has gone stale locally
- **THEN** Android resolves the active turn from bridge state and interrupts the correct turn instead of hiding Stop prematurely
