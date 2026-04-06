# Conventions

**Analysis Date:** 2026-04-07

## General Style

- Follow the local-first guardrails in `AGENTS.md` and `CLAUDE.md`; do not reintroduce hosted-service assumptions or remote-first defaults.
- Match the existing platform idiom instead of forcing one style across Swift, Kotlin, and JavaScript.
- Prefer focused files with clear responsibilities, even when a feature spans several collaborating files.

## File Organization

**Swift:**
- Large service types are split into extensions, for example `CodexMobile/CodexMobile/Services/CodexService.swift` plus `CodexService+*.swift`.
- View-local logic that grows beyond pure rendering gets its own type, for example `CodexMobile/CodexMobile/Views/Turn/TurnViewModel.swift`.

**Kotlin:**
- Package organization carries a lot of the architecture: `data`, `feature`, `model`, and `platform`.
- Keep app composition in `RemodexAppContainer.kt` and avoid pulling dependency creation into screens.

**JavaScript:**
- Bridge and relay modules are file-oriented and export a few explicit functions rather than classes everywhere.
- Keep feature helpers adjacent to the boundary they support, for example transport helpers in `secure-transport.js` and `codex-transport.js`.

## Naming

- Use descriptive type/file names that reflect domain responsibility: `SecureConnectionCoordinator`, `BridgeThreadSyncService`, `TurnTimelineReducer`, `push-notification-tracker.js`.
- Preserve platform prefixes already in use: `Codex*` on iOS, `Remodex*` on Android.
- Keep enum cases/constants explicit rather than abbreviated, for example `ComposerVoiceButtonMode.TRANSCRIBING` and `CLOSE_CODE_SESSION_UNAVAILABLE`.

## Comments

- JavaScript and much of the Swift code use file header comments describing purpose, layer, exports, and dependencies. Match that pattern when editing those areas.
- Inline comments are present when the code needs domain framing, not for trivial restatement. Examples: stale relay watchdog notes in `phodex-bridge/src/bridge.js`, push-security notes in `relay/server.js`, and state-isolation notes in `SubscriptionService.swift`.
- Kotlin tends to favor fewer file headers, but still uses targeted explanatory comments near threading/performance-sensitive code such as `RemodexAppContainer.kt`.

## State Management

- Keep shared app state in long-lived services/repositories/view models, not directly in view hierarchies.
- Prefer derived/computed state over duplicated flags when practical, for example `canCreatePullRequest` in `TurnViewModel.swift` and `AppUiState` computed properties in `AppViewModel.kt`.
- Keep reconnection/timeline reconciliation logic centralized in reducers/coordinators instead of scattered UI callbacks.

## Error Handling

- Use user-facing messages that explain the next action, especially for pairing/reconnect flows. Examples appear in `SecureConnectionCoordinatorTest.kt` expectations and bridge startup errors in `phodex-bridge/src/bridge.js`.
- Preserve structured error codes for transport/HTTP boundaries, for example `invalid_request`, `session_unavailable`, and `rate_limited` in `relay/server.js` and `relay/push-service.js`.
- Fail fast at boundaries where silent drops would corrupt state, such as Mac absence handling in `relay/relay.js`.

## Testing Conventions Reflected In Code

- Tests read as behavior specs with sentence-style names, for example `testCollapseConsecutiveThinkingKeepsNewestState` and ``trusted reconnect invalid trust falls back to repair required``.
- Use in-memory or fake stores/factories where possible instead of real services. Examples: `InMemorySecureStore` in Android tests and explicit stubbed deps in `phodex-bridge/test/remodex-cli.test.js`.
- Keep regression tests narrow and scenario-driven for reducers, transport edge cases, and timeline behavior.

## What To Preserve When Editing

- Keep shared logic in services/coordinators/reducers rather than duplicating it across iOS and Android screens.
- Keep relay logs sanitized; the current code intentionally redacts bearer-like session identifiers in `relay/server.js`.
- Keep history/timeline item identity logic item-aware rather than simplifying it to turn-only matching.
- Keep the bridge as the owner of local runtime, git, and workspace behavior.
