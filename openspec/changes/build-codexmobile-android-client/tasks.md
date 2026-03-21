## 1. Android foundation

- [x] 1.1 Replace the scaffold package and application identifiers with the agreed Remodex Android namespace.
- [x] 1.2 Add the Android dependencies needed for navigation, lifecycle/viewmodels, coroutines, serialization, websocket transport, secure storage, Room/DataStore, QR scanning, and media intake.
- [x] 1.3 Replace the starter `MainActivity` and default theme with a Remodex app entry point, branded design tokens, and app-level state wiring.
- [x] 1.4 Create the initial Android package structure for features, data, models, and platform integrations inside the existing `app` module.
- [x] 1.5 Add baseline unit-test and Compose-test infrastructure with repository/service fakes for Android feature work.

## 2. App shell and onboarding

- [x] 2.1 Implement first-launch onboarding state and a Remodex-branded onboarding screen with local-first setup guidance.
- [x] 2.2 Build adaptive Android navigation for phone and expanded layouts with thread, conversation, settings, and recovery entry points.
- [x] 2.3 Implement shell-level connection state presentation, including loading, disconnected, retrying, and recovery surfaces.
- [x] 2.4 Add accessibility and responsiveness refinements so the Android shell remains usable across font scaling, dark mode, and larger screen classes.

## 3. Secure pairing and reconnect

- [x] 3.1 Implement QR scanning, camera permission handling, and recovery UX for denied camera access.
- [x] 3.2 Port pairing payload models and validation rules from the existing bridge/iOS behavior.
- [x] 3.3 Implement Android-secure persistence for trusted Mac metadata and any reconnect prerequisites.
- [x] 3.4 Build the websocket transport, secure handshake bootstrap, and trusted reconnect state machine.
- [x] 3.5 Add actionable error and recovery UI for expired QR payloads, incompatible bridge versions, and invalid saved trust.
- [x] 3.6 Add unit tests for QR validation, secure pairing persistence, and reconnect state transitions.

## 4. Threads and timeline

- [x] 4.1 Implement thread data models, local caching, and sync plumbing for the Android client.
- [x] 4.2 Build the thread list UI with selection persistence and cross-project thread access without repo filtering.
- [x] 4.3 Implement timeline reducers that merge streaming assistant text, reasoning, activity, and late-arriving updates into existing items.
- [x] 4.4 Build the conversation screen with empty state, timeline rows, banners, and in-progress turn presentation.
- [x] 4.5 Add tests for thread selection persistence and item-scoped timeline reconciliation behavior.

## 5. Composer and runtime controls

- [x] 5.1 Implement Android composer state, send availability rules, and queued-draft persistence for running turns.
- [x] 5.2 Wire prompt send, stop, and queued follow-up behavior to the transport layer for the selected thread.
- [x] 5.3 Build Android-native runtime controls for reasoning, planning, and access configuration using bridge-provided metadata.
- [x] 5.4 Surface existing thread runtime overrides in the composer before the next send.
- [x] 5.5 Add tests for send, stop, queue, and runtime-override presentation flows.

## 6. Device integrations and release hardening

- [x] 6.1 Implement image attachment intake from Android device sources with preview, removal, and attachment-limit enforcement.
- [x] 6.2 Add Android notification channels, permission flow, and thread-deep-link notifications for completion and attention-needed events.
- [x] 6.3 Add regression coverage for attachment limits, notification permission denial, and notification routing back into a thread.
- [x] 6.4 Update shared docs to describe the Android client setup, current scope, and any intentionally deferred parity items.
- [x] 6.5 Run the relevant Android unit/UI test suites and fix the remaining parity or polish issues before implementation sign-off.
