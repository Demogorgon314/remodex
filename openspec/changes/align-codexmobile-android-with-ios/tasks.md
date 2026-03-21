## 1. Change scaffolding and parity foundation

- [x] 1.1 Create the parity follow-up OpenSpec artifacts and validate the change.
- [x] 1.2 Replace `FakeRemodexAppRepository` / `FakeThreadSyncService` in production wiring with a bridge-backed Android repository foundation.
- [x] 1.3 Expand Android session, thread, runtime, and notification models to carry the state required by iOS-parity screens.
- [x] 1.4 Expose authenticated bridge request / notification plumbing from `SecureConnectionCoordinator` for repository use.
- [x] 1.5 Add or update baseline tests that cover the new Android bridge-backed repository foundation.

## 2. Thread, runtime, and notification protocol parity

- [x] 2.1 Port Android support for real `thread/list`, `thread/read(includeTurns=true)`, send/stop, queued drafts, and archived-thread reconciliation.
- [x] 2.2 Add Android runtime metadata support for model selection, reasoning, service tier, access, and per-thread overrides.
- [x] 2.3 Make `notifications/push/register` provider-aware in Android, `phodex-bridge`, and `relay`, while preserving iOS backward compatibility.
- [x] 2.4 Add Android FCM token persistence/registration plumbing and relay-side FCM completion delivery support.
- [x] 2.5 Add tests for protocol compatibility, push registration, and relay completion delivery.

## 3. Shell, sidebar, and settings parity

- [x] 3.1 Rebuild the Android shell/sidebar to match iOS grouping, search, new-chat, archived chats, subagent tree, badges, and trusted-Mac status.
- [x] 3.2 Add project picker, rename/archive/delete/archive-project actions, and cross-project routing parity.
- [x] 3.3 Rebuild Android settings to match iOS runtime defaults, appearance, notifications, archived chats, and trusted-Mac sections.
- [x] 3.4 Add tests for sidebar grouping, archived state, routing, and settings-driven state presentation.

## 4. Conversation, composer, and git/review parity

- [x] 4.1 Rebuild the Android conversation container to match iOS timeline layout, pinned plan accessory, banners, overlays, and reconnect behavior.
- [x] 4.2 Rebuild the Android composer with attachment strip, queued drafts, `@files`, `$skills`, `/commands`, chips, and iOS-matched runtime controls.
- [x] 4.3 Add Android parity for subagent selection, code review, thread fork, git/worktree flows, and assistant revert preview/apply.
- [x] 4.4 Add tests for composer gating, runtime fallback behavior, review/subagent flows, and timeline rendering.

## 5. Verification and documentation

- [x] 5.1 Update shared docs for Android parity scope and managed-push configuration requirements.
- [x] 5.2 Run Android unit/UI tests plus the relevant bridge/relay suites, then fix remaining parity regressions.
- [x] 5.3 Validate the OpenSpec change and update all completed task checkboxes before sign-off.
