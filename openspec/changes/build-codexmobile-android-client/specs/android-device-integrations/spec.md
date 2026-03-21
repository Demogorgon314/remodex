## ADDED Requirements

### Requirement: Android conversations support image attachments from device sources
The Android client SHALL allow the user to attach images from Android device sources, preview them before sending, and enforce the same per-message attachment limits expected by the bridge.

#### Scenario: User attaches supported images
- **WHEN** the user selects or captures supported images within the remaining attachment limit
- **THEN** the composer shows those attachments in a removable preview state before send

#### Scenario: Attachment limit is enforced
- **WHEN** the user tries to add more images than the per-message limit allows
- **THEN** the app keeps only the allowed attachments and shows an actionable limit message

### Requirement: Completion and attention events surface through Android notifications
The Android client SHALL integrate with Android's notification model so that completion and attention events can be surfaced through system notifications when the app has permission and an active session can deliver them.

#### Scenario: Notification permission granted
- **WHEN** the app receives a turn-complete or attention-needed event while notification permission is granted
- **THEN** the app posts an Android notification that can return the user to the relevant thread

#### Scenario: Notification permission denied
- **WHEN** notification permission is unavailable or denied
- **THEN** the app continues to surface the event in-app without blocking the underlying conversation workflow
