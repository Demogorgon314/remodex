## ADDED Requirements

### Requirement: Android composer SHALL match the iOS conversation toolchain
The Android client SHALL provide the same composer capabilities as iOS, including attachments, queued drafts, runtime controls, file/skill/slash-command input, and send/stop behavior.

#### Scenario: Composer exposes iOS-parity runtime controls
- **WHEN** the bridge exposes model, reasoning, service-tier, planning, and access metadata
- **THEN** Android shows those controls in the composer with the same selection and override behavior as iOS

#### Scenario: Composer supports iOS command workflows
- **WHEN** the user invokes file mentions, skills, slash commands, code review, or subagent selection
- **THEN** Android presents the corresponding autocomplete panels or chips and sends the resulting request with the same product semantics as iOS

### Requirement: Android conversation workflows SHALL include iOS git, review, and fork flows
The Android client SHALL expose the git/worktree, thread fork, code review, and assistant revert flows that exist in iOS.

#### Scenario: Git/worktree actions stay available in Android conversation context
- **WHEN** a thread has git context available from the bridge
- **THEN** Android exposes the same git status, branch, worktree, and fork affordances as iOS from the conversation experience
