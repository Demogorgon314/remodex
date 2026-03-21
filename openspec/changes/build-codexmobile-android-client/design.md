## Context

`CodexMobile` is already a feature-rich local-first iOS client with established behavior around onboarding, QR pairing, trusted reconnect, thread navigation, streaming turns, queued follow-ups, and runtime controls. `CodexMobileAndroid` currently contains only the default Android Studio Compose starter app, so this change must introduce both a production-ready Android architecture and the first Android implementation of the bridge protocol.

Key constraints:

- The repo is local-first and must not reintroduce hosted-service assumptions or hardcoded production relay domains.
- Android should preserve the same bridge semantics and trust model as iOS, but it should adopt Android-native UX patterns instead of cloning SwiftUI structures literally.
- The initial codebase should stay maintainable inside the existing single `app` module unless modularization becomes necessary during implementation.
- iOS-specific guardrails around thread routing, item-scoped timeline reconciliation, stop behavior, and secure pairing need Android equivalents so the two clients do not drift on critical behavior.

## Goals / Non-Goals

**Goals:**

- Build an Android client that can complete the core Remodex lifecycle: onboarding, QR pairing, trusted reconnect, thread browsing, streaming conversation, and prompt submission.
- Reuse protocol semantics and UX intent from `CodexMobile` while mapping them to Compose, Android permissions, and adaptive navigation.
- Establish an Android architecture that keeps transport, persistence, reducers, and feature UI separated so parity work can continue incrementally.
- Support Android-native integrations that make the client practical for daily use, especially image attachments and local notifications.

**Non-Goals:**

- Achieving full one-shot parity with every iOS-only feature before the first Android release candidate.
- Introducing a hosted backend, remote-only defaults, or Android-specific behavior that weakens the local-first trust model.
- Starting with a heavily modularized Android project before the product surface and package boundaries have stabilized.
- Solving managed background push delivery beyond what the current bridge/session model can support without additional product decisions.

## Decisions

### Decision: Use a single-app-module, package-by-feature Android architecture first

The implementation will keep `CodexMobileAndroid` as a single Gradle application module and organize code by feature and responsibility, for example `feature/onboarding`, `feature/threads`, `feature/turn`, `data/connection`, `data/persistence`, `model`, and `platform`.

Why:

- The Android app is starting from a blank scaffold, so multi-module overhead would slow down delivery more than it would reduce risk.
- Feature packages give enough separation to keep services, reducers, and UI composables from collapsing into `MainActivity`.
- This mirrors the iOS separation of `Views`, `Services`, and `Models` without forcing a one-to-one file structure port.

Alternatives considered:

- Immediate multi-module split: rejected for the first milestone because the domain boundaries are not yet proven on Android.
- A flat package structure under `ui` and `network`: rejected because it would become brittle once thread/timeline state grows.

### Decision: Build the UI with Compose, Navigation Compose, and adaptive navigation primitives

The Android UI will use Jetpack Compose throughout, with Navigation Compose and window-size-aware layouts to map the iOS shell into Android-native navigation on phones and larger screens.

Why:

- The current project is already set up for Compose.
- Compose makes it practical to share visual intent with iOS while still using Android-native navigation, permission, and state patterns.
- Adaptive layouts let the Android client preserve thread-plus-conversation workflows on larger devices without duplicating view hierarchies.

Alternatives considered:

- Fragment-based screens: rejected because it would add ceremony without helping parity or incremental delivery.
- A custom iOS-style drawer-first shell on every form factor: rejected because it would fight Android navigation expectations.

### Decision: Model bridge state with repositories, reducers, and `StateFlow`

Bridge I/O will be wrapped in repositories/managers that expose immutable UI state through `StateFlow`, while feature-level `ViewModel`s coordinate user intents and reducers reconcile incoming events into thread and timeline state.

Why:

- The iOS client already depends on centralized service state and reducer-like reconciliation for threads and timeline updates.
- Android needs a similar state model to correctly merge streaming deltas, late reasoning updates, queued drafts, and reconnect recovery.
- `StateFlow` and coroutines fit Android lifecycle handling better than pushing raw websocket callbacks directly into composables.

Alternatives considered:

- Direct mutable Compose state inside activities/screens: rejected because transport and lifecycle concerns would bleed into UI code.
- A heavy MVI framework: rejected because the app does not need another abstraction layer before the Android behaviors are proven.

### Decision: Keep protocol compatibility by porting semantics, not UI structure

The Android implementation will reuse the existing bridge message contracts and behavioral rules from the iOS client, especially around trusted pairing, thread routing, turn lifecycle, queued follow-ups, and item-scoped timeline updates.

Why:

- The bridge already has a working contract with the iOS client, so Android should converge on the same behavior instead of inventing a second protocol interpretation.
- Preserving semantics reduces regression risk for local-first flows such as reconnect, stop, and late event reconciliation.
- Porting intent rather than SwiftUI structure leaves room for Android-native UI choices.

Alternatives considered:

- Rebuilding behavior from README-level descriptions only: rejected because too many subtle runtime rules already exist in `CodexMobile`.
- Mirroring Swift file boundaries exactly: rejected because platform UI concerns differ too much.

### Decision: Use secure local persistence with purpose-specific stores

Small local settings and UI preferences will live in DataStore, cached thread/timeline data will live in Room, and trusted pairing secrets will be stored through Android Keystore-backed encryption.

Why:

- The app needs durable local state for reconnect and for rendering useful shell state before the bridge fully syncs.
- Pairing material has a different security profile than ordinary UI preferences and should not live in plain preferences storage.
- Room gives the Android client a clean path for caching thread lists and conversation history without hand-rolled file formats.

Alternatives considered:

- SharedPreferences for everything: rejected because it is weak for structured caches and too loose for secret material.
- Encrypting the full local database from day one: rejected because it adds migration complexity before the data model is stable.

### Decision: Phase device integrations after the core conversation loop is usable

Implementation will deliver Android foundations, secure connection, and conversation workflows first, then layer image attachments and notification handling once the core loop is reliable.

Why:

- Pairing, reconnect, thread sync, and turn rendering are the critical path for an Android client that users can actually validate.
- Attachments and notifications depend on a stable composer, permission model, and event pipeline.
- This keeps the first implementation milestone testable without collapsing every Android integration into one patch.

Alternatives considered:

- Building every iOS-adjacent feature in parallel: rejected because it would widen the failure surface before the core transport loop is proven.
- Shipping a chat-only MVP with no path to richer Android integrations: rejected because it would underfit the stated goal of a real client.

## Risks / Trade-offs

- [Protocol drift between iOS and Android] -> Mitigation: port behavior from concrete iOS services/tests, especially pairing, reconnect, timeline, and stop semantics, instead of relying on ad hoc reinterpretation.
- [Android navigation diverges too far from the proven iOS information architecture] -> Mitigation: preserve the same destination model and state transitions while adapting only the interaction surface.
- [Secure storage or reconnect flows become brittle across process death] -> Mitigation: keep trusted pairing persistence isolated behind a dedicated repository and cover restore/recovery flows with unit tests.
- [Timeline rendering duplicates or misorders streaming content] -> Mitigation: implement explicit reducers for item-aware reconciliation before polishing presentation details.
- [Notifications create expectations of full background push parity] -> Mitigation: document that the first Android implementation targets Android-native notification handling for supported session states, not guaranteed managed push parity.
- [Single-module structure becomes crowded] -> Mitigation: use clear package boundaries now and postpone module extraction until real pressure appears.

## Migration Plan

1. Replace the default starter app with a branded Android shell and introduce the feature/package structure.
2. Add protocol models, connection services, secure pairing persistence, and a QR-driven onboarding flow.
3. Implement thread list, conversation timeline, composer, stop/queue behavior, and runtime controls against bridge events.
4. Layer image attachments, notification plumbing, and remaining Android-specific integrations after the core loop is stable.
5. Update shared docs once the Android app is runnable enough to describe setup honestly.

Rollback strategy:

- Because the Android client is isolated under `CodexMobileAndroid`, rollback can happen by reverting Android-specific changes without disturbing the existing bridge or iOS app.
- New protocol interpretations should avoid server-side contract changes unless they are backward compatible with the iOS client.

## Open Questions

- What should the long-term Android `applicationId` and signing identity be instead of the current scaffold namespace?
- Which iOS power-user surfaces must be included in the first Android implementation milestone versus a follow-up parity pass: git actions, review flows, and subagent affordances?
- Do we want a strictly branded light-first Android theme that mirrors iOS more closely, or a stronger Material adaptation with Remodex branding tokens?
