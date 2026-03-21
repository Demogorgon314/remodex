## ADDED Requirements

### Requirement: Android shell SHALL mirror the iOS sidebar and settings information architecture
The Android client SHALL match the iOS shell layout hierarchy, including grouped sidebar threads, search, new chat, archived chats, trusted-Mac status, and settings sections.

#### Scenario: Sidebar groups threads by project and archived state
- **WHEN** the Android sidebar renders bridge-backed threads
- **THEN** it groups active chats by project path, keeps archived chats in a dedicated section, and exposes the same group-level actions as iOS

#### Scenario: Settings reflects the same sections as iOS
- **WHEN** the user opens Android settings
- **THEN** the app shows runtime defaults, appearance, notifications, archived chats, and trusted-Mac controls in the same order and with the same product meaning as iOS
