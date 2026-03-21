## Context

`build-codexmobile-android-client` delivered the first Android baseline, but that change intentionally stopped short of full parity. Android still wires `RemodexAppContainer` to `FakeRemodexAppRepository` and `FakeThreadSyncService`, so many current UI surfaces are projections of seeded or local-only state instead of the real bridge protocol that powers iOS. At the same time, the repository contains the iOS reference implementation for sidebar grouping, runtime config, archived chats, composer actions, git/worktree flows, subagent identity, review routing, and managed push registration.

Key constraints:

- iOS `CodexMobile` is the source of truth for product behavior and layout hierarchy for this pass.
- Android must stay local-first and keep the secure pairing / trusted reconnect model already implemented in the baseline.
- The repo currently has APNs-only managed push support; Android parity requires new FCM support in both app and relay layers.
- We should keep Android in the existing `app` module unless the implementation proves that impossible.

## Goals / Non-Goals

**Goals:**

- Replace the Android fake conversation foundation with bridge-backed repositories and parity-ready models.
- Match the iOS shell, sidebar, settings, conversation, composer, git/review/subagent flows, and notification affordances closely enough that the same product surface exists on both mobile clients.
- Add Android managed push using FCM while keeping shared bridge payload shapes and local-first recovery behavior.
- Preserve the subtle iOS runtime semantics that matter for thread routing, item-scoped timeline reconciliation, stop recovery, queueing, archived chat handling, and trusted reconnect.

**Non-Goals:**

- Reimagining the Android app with a distinct Material-first IA or visual language.
- Reverting the existing Android baseline work or reintroducing hosted-only assumptions.
- Committing Firebase or relay secrets/config artifacts into the repository.
- Splitting the Android project into multiple Gradle modules as part of this change.

## Decisions

### Decision: Treat the Android fake app repository as technical debt and remove it from production wiring

Android parity cannot be built on top of `FakeRemodexAppRepository` / `FakeThreadSyncService`, because the missing iOS surfaces depend on real bridge state: archived and grouped threads, subagent metadata, runtime model/service-tier support, git/worktree status, review targets, queue pause state, and notification routing.

Therefore:

- `RemodexAppContainer` will wire a bridge-backed repository in production.
- The fake repository/service can remain only as test fixtures if still useful.
- `SecureConnectionCoordinator` becomes the single secure transport entry point and must expose authenticated request/notification plumbing beyond pair/retry state.

### Decision: Port iOS service semantics first, then align Compose presentation

The Android UI must follow the same state machine as iOS before visual parity work, otherwise “matching” screens would still diverge in behavior under reconnects, archived chat sync, queued drafts, and late timeline deltas.

Therefore:

- Port the iOS service-side semantics for `thread/list`, `thread/read`, `turn/start`, `turn/stop`, queued drafts, archived merge, runtime fallbacks, and thread/fork/git/review handling.
- Expand Android models to carry the same information the iOS views expect to render: sync state, parent/child thread relationships, run badges, diff totals, notification prompts, completion banners, model/service-tier metadata, and subagent identity.
- Keep persistence aligned with those semantics so relaunch, background recovery, and reconnect behave the same way.

### Decision: Mirror iOS information architecture directly, with only system-owned Android substitutions

The user wants the interface fully aligned with iOS, so this change will use the iOS hierarchy, section ordering, entry placement, and copy as the default. Android can substitute system-owned affordances such as permission sheets, activity result contracts, and back handling, but it should not redesign the product structure.

Therefore:

- Sidebar/search/new-chat/project-picker/archived-chat/settings placement follows iOS.
- Conversation/composer layout follows iOS, including attachments strip, queued drafts, chips, secondary bar, pinned plan accessory, and banners.
- Visual tokens should move away from generic Material defaults and align with the iOS look-and-feel already established for Remodex.

### Decision: Extend managed push to Android with provider-aware registration and delivery

The existing repo already has a managed push path for iOS APNs. Full Android parity requires FCM support rather than a second local-only notification path.

Therefore:

- Android registers an FCM token and syncs it through the existing `notifications/push/register` bridge RPC.
- That RPC becomes provider-aware, adding `platform` and `pushProvider` while remaining backward compatible for older iOS clients.
- `relay` stores platform-specific registrations per session and sends completion pushes through APNs or FCM based on the provider.
- Missing local config leaves managed push unavailable but explicit in UI/docs; no secrets are checked into git.

## Risks / Trade-offs

- [Android parity scope is large] -> Mitigation: do the work in layers, starting with bridge-backed state and notification protocol changes that unblock the rest.
- [iOS semantics are easy to under-port] -> Mitigation: study the relevant `CodexService` extensions and match behavior before chasing Compose polish.
- [Push support adds new backend/config surface] -> Mitigation: keep the wire shape shared with iOS, gate FCM behind explicit config, and cover bridge/relay flows with tests.
- [UI parity regresses Android baseline behavior] -> Mitigation: preserve the existing secure pairing and reconnect tests while expanding UI and repository coverage.
- [Single app module grows crowded] -> Mitigation: keep package boundaries explicit and postpone module extraction until a separate change if pressure appears.

## Migration Plan

1. Scaffold the parity follow-up change and codify the new requirements, then wire production Android state away from fake thread sync.
2. Port bridge-backed thread/runtime/timeline semantics and notification registration support into Android and shared Node services.
3. Rebuild Android shell/sidebar/settings/conversation/composer surfaces on top of the new state model, matching iOS layout and behavior.
4. Add FCM integration, platform-aware relay push delivery, and the remaining notification banners/recovery flows.
5. Update docs and finish parity verification by comparing Android behavior to the existing iOS screens and tests.

Rollback strategy:

- Android-side rollback remains isolated under `CodexMobileAndroid`.
- Provider-aware push registration will be backward compatible, so iOS-managed push continues working even if Android support is reverted.
- Fake repository classes can temporarily remain in tests, so removing them from production wiring is reversible without losing baseline fixtures.
