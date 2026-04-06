# Testing

**Analysis Date:** 2026-04-07

## Test Frameworks

**iOS:**
- XCTest unit tests in `CodexMobile/CodexMobileTests/`
- XCTest UI tests in `CodexMobile/CodexMobileUITests/`

**Android:**
- JUnit4 unit tests in `CodexMobileAndroid/app/src/test/java/com/emanueledipietro/remodex/`
- Compose/instrumentation tests in `CodexMobileAndroid/app/src/androidTest/java/com/emanueledipietro/remodex/`

**Bridge and Relay:**
- Native Node test runner via `node --test` in `phodex-bridge/package.json` and `relay/package.json`

## Test Layout

- iOS tests mostly mirror feature/service subjects by filename, for example `TurnTimelineReducerTests.swift`, `CodexServiceIncomingCommandExecutionTests.swift`, and `SidebarThreadGroupingTests.swift`.
- Android tests mirror package structure under `data/`, `feature/`, `model/`, and `platform/`, for example `data/connection/SecureConnectionCoordinatorTest.kt` and `feature/turn/ConversationScreenTest.kt`.
- Bridge tests sit under `phodex-bridge/test/` and validate public behavior or boundary helpers, for example `bridge.test.js`, `secure-transport.test.js`, and `remodex-cli.test.js`.
- Relay tests stay alongside the runtime files in `relay/*.test.js`.

## Dominant Test Patterns

**Reducer/parser regression tests:**
- iOS: `CodexMobile/CodexMobileTests/TurnTimelineReducerTests.swift`
- Android: `CodexMobileAndroid/app/src/test/java/com/emanueledipietro/remodex/data/threads/ThreadHistoryReconcilerTest.kt`, `.../feature/turn/ThinkingDisclosureParserTest.kt`

**Coordinator/service tests with fakes:**
- Android secure transport and reconnect flows use fake resolvers/websocket factories in `SecureConnectionCoordinatorTest.kt`
- Bridge CLI tests inject fake dependencies through `main({... deps })` in `phodex-bridge/test/remodex-cli.test.js`

**UI behavior tests:**
- Android Compose screen interaction tests in `ConversationScreenTest.kt`, `ThreadsScreenTest.kt`, and onboarding/tests under `feature/appshell/`
- iOS UI-level behavior is mostly validated with unit tests around view models/reducers plus a smaller `CodexMobileUITests/` surface

## Mocking and Fakes

- Prefer in-memory stores, fake factories, and dependency injection rather than network calls or filesystem-heavy integration tests.
- Android examples: `InMemorySecureStore`, `StaticTrustedSessionResolver`, `ClosingRelayWebSocketFactory`
- JavaScript examples: inline `deps` objects passed into CLI entrypoints
- Swift examples: scenario-specific helper builders inside test targets rather than app-runtime singletons

## Coverage Shape

- Strongest coverage is around timeline reconciliation, pairing/reconnect, transport edge cases, and view-model behavior.
- There is broad evidence of regression coverage for subtle UX/state bugs, especially on iOS and Android turn/timeline flows.
- There is less evidence of end-to-end multi-runtime integration coverage spanning bridge + relay + mobile in one automated suite.

## How To Add Tests

- For iOS service/reducer/view-model changes, add XCTest cases under `CodexMobile/CodexMobileTests/` with a filename matching the edited type or feature.
- For Android model/repository/service changes, add a JVM test under `app/src/test/`; reserve `androidTest` for Compose UI or instrumentation-dependent behavior.
- For bridge/relay changes, add a `node:test` file close to the touched runtime (`phodex-bridge/test/` or `relay/`).
- Prefer scenario names that describe the behavior and expected outcome rather than generic "works" language.

## Useful Existing Anchors

- `CodexMobile/CodexMobileTests/TurnTimelineReducerTests.swift`
- `CodexMobile/CodexMobileTests/CodexServiceIncomingCommandExecutionTests.swift`
- `CodexMobileAndroid/app/src/test/java/com/emanueledipietro/remodex/data/connection/SecureConnectionCoordinatorTest.kt`
- `CodexMobileAndroid/app/src/androidTest/java/com/emanueledipietro/remodex/feature/turn/ConversationScreenTest.kt`
- `phodex-bridge/test/bridge.test.js`
- `phodex-bridge/test/remodex-cli.test.js`
- `relay/server.test.js`

## Testing Gaps To Watch

- Large files such as `ConversationScreen.kt`, `BridgeThreadSyncService.kt`, `AppViewModel.kt`, and `CodexService+Incoming.swift` are likely to need extra regression tests for any non-trivial edit.
- Keep tests focused on local-first reconnect, item-aware timeline reconciliation, and bridge/relay failure modes because those are the most behaviorally sensitive areas exposed by the current codebase.
