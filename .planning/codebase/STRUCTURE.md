# Structure

**Analysis Date:** 2026-04-07

## Top-Level Layout

- `CodexMobile/` - iOS app, tests, UI tests, and helper scripts
- `CodexMobileAndroid/` - Android app, test suites, Gradle config, and scripts
- `phodex-bridge/` - Node CLI/daemon bridge that owns local runtime and git/workspace integration
- `relay/` - self-hostable relay and optional push service
- `Docs/`, `Legal/`, `assets/` - supporting content/assets
- `.codex/` - local GSD/workflow/agent metadata
- `.planning/` - generated planning artifacts, including this codebase map

## iOS Layout

**Application root:**
- `CodexMobile/CodexMobile/CodexMobileApp.swift`
- `CodexMobile/CodexMobile/ContentView.swift`

**Core folders:**
- `CodexMobile/CodexMobile/Models/` - serializable and presentation models
- `CodexMobile/CodexMobile/Services/` - transport, persistence, payments, secure store, haptics, handoff
- `CodexMobile/CodexMobile/Views/` - SwiftUI features split by domain (`Home/`, `Onboarding/`, `Sidebar/`, `Turn/`, `Shared/`, `Payments/`)
- `CodexMobile/CodexMobile/Resources/` - bundled assets such as Mermaid runtime
- `CodexMobile/CodexMobileTests/` - unit tests
- `CodexMobile/CodexMobileUITests/` - UI tests

**Where to add code:**
- Add new app-wide service behavior under `Services/`, usually as a new type or `CodexService+Feature.swift`.
- Add reusable models under `Models/`.
- Add screen-specific SwiftUI pieces under the closest `Views/<Feature>/` folder.
- Add view-local state objects only when the logic is too specific for `CodexService`.

## Android Layout

**Application root:**
- `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/MainActivity.kt`
- `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/RemodexApplication.kt`

**Core folders:**
- `.../data/app/` - app container and repository composition
- `.../data/connection/` - relay pairing, secure transport, trusted session resolution
- `.../data/threads/` - thread sync, hydration, cache stores
- `.../data/preferences/` - DataStore-backed settings
- `.../data/voice/` - voice transcription and cookie handling
- `.../feature/` - Compose screens and feature-local reducers/renderers
- `.../model/` - domain/presentation models and mappers
- `.../platform/` - notifications, media, window helpers
- `app/src/test/` - JVM tests
- `app/src/androidTest/` - instrumentation/Compose UI tests

**Where to add code:**
- Put new transport or persistence adapters under the relevant `data/` package.
- Put app wiring changes in `data/app/RemodexAppContainer.kt`.
- Put user-facing screens and feature reducers under `feature/<feature-name>/`.
- Keep general models in `model/`, not embedded inside screens.

## Bridge Layout

- `phodex-bridge/bin/` - CLI wrapper
- `phodex-bridge/src/bridge.js` - main coordinator
- `phodex-bridge/src/*-handler.js` - request-specific local handlers
- `phodex-bridge/src/*state*.js` - persisted state/session helpers
- `phodex-bridge/src/scripts/` - AppleScript helpers
- `phodex-bridge/test/` - Node tests
- `phodex-bridge/scripts/` - packaging/default-prep helpers

**Where to add code:**
- Add new request handlers as focused modules in `phodex-bridge/src/` and wire them from `bridge.js`.
- Keep CLI surface changes in `bin/remodex.js` and `src/index.js`.
- Put platform-service wrappers in `linux-systemd.js` or `macos-launch-agent.js` rather than `bridge.js`.

## Relay Layout

- `relay/server.js` - HTTP + upgrade entrypoint
- `relay/relay.js` - live session room management
- `relay/push-service.js` - push registration and completion notifications
- `relay/*.test.js` - Node tests
- `relay/README.md` - protocol/deploy notes

**Where to add code:**
- Extend WebSocket session logic in `relay/relay.js`.
- Extend HTTP endpoints in `relay/server.js`.
- Put push-only behavior in `relay/push-service.js` or provider-specific client files.

## Naming Patterns

- Swift types use `Codex*` prefixes on iOS, with view files often matching the top-level type name, for example `TurnViewModel.swift` or `SidebarThreadListView.swift`.
- Android types use `Remodex*` prefixes widely across models and app wiring, for example `RemodexAppContainer.kt`, `RemodexThreadSummary.kt`, and `RemodexNotificationCoordinator.kt`.
- Bridge/relay JavaScript names are descriptive and file-based, for example `secure-device-state.js`, `rollout-live-mirror.js`, and `push-notification-tracker.js`.

## Practical Navigation Rules

- If the change touches local machine behavior, start in `phodex-bridge/src/`.
- If the change touches mobile timeline rendering, inspect both `CodexMobile/CodexMobile/Views/Turn/` and `CodexMobileAndroid/.../feature/turn/`.
- If the change touches pairing/trust/reconnect, inspect both mobile connection packages and the relay/bridge secure transport modules together.
- If the change adds tests, mirror the platform-specific test location instead of creating a new top-level test style.
