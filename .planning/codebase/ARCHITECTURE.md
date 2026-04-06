# Architecture

**Analysis Date:** 2026-04-07

## System Shape

The repo is a local-first multi-runtime system with four main components:

1. `phodex-bridge/` runs on the user's Mac, launches or connects to the local Codex runtime, and owns git/workspace access.
2. `relay/` is an optional self-hostable transport hop for pairing, trusted reconnect, and push-session coordination.
3. `CodexMobile/` is the iOS client built with SwiftUI.
4. `CodexMobileAndroid/` is the Android client built with Jetpack Compose.

The bridge is the authoritative boundary for local machine actions. The relay is intentionally narrower and does not become the application backend.

## Key Layers

**Bridge:**
- CLI/service entrypoints: `phodex-bridge/src/index.js`
- Long-running bridge coordinator: `phodex-bridge/src/bridge.js`
- Local capability handlers: `phodex-bridge/src/git-handler.js`, `phodex-bridge/src/workspace-handler.js`, `phodex-bridge/src/thread-context-handler.js`, `phodex-bridge/src/desktop-handler.js`
- Transport/security helpers: `phodex-bridge/src/codex-transport.js`, `phodex-bridge/src/secure-transport.js`, `phodex-bridge/src/secure-device-state.js`

**Relay:**
- HTTP + WebSocket server shell: `relay/server.js`
- Session routing and trusted-session logic: `relay/relay.js`
- Optional push subsystem: `relay/push-service.js`, `relay/apns-client.js`, `relay/fcm-client.js`

**iOS:**
- App root and dependency bootstrapping: `CodexMobile/CodexMobile/CodexMobileApp.swift`
- Central state/service object: `CodexMobile/CodexMobile/Services/CodexService.swift`
- Responsibility split by extensions: `CodexMobile/CodexMobile/Services/CodexService+*.swift`
- Feature/UI layers: `CodexMobile/CodexMobile/Views/`
- Smaller per-view state objects when needed: `CodexMobile/CodexMobile/Views/Home/ContentViewModel.swift`, `CodexMobile/CodexMobile/Views/Turn/TurnViewModel.swift`

**Android:**
- App-scoped dependency container: `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/data/app/RemodexAppContainer.kt`
- Repository + service composition: `.../data/app/DefaultRemodexAppRepository.kt`, `.../data/threads/BridgeThreadSyncService.kt`
- UI-facing state holder: `.../feature/appshell/AppViewModel.kt`
- Feature screens/components: `.../feature/`
- Connection, persistence, notification, and media adapters: `.../data/connection/`, `.../data/threads/`, `.../platform/`

## Primary Data Flow

**Conversation / turn flow:**
- Mobile UIs build local state in `CodexMobile/CodexMobile/Views/Turn/TurnViewModel.swift` and `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/feature/appshell/AppViewModel.kt`.
- Client services/repositories translate UI actions into RPC-like payloads.
- Secure transport sends those payloads over the relay-backed channel.
- The bridge receives decrypted requests, dispatches to Codex/git/workspace handlers, and streams results back through the same channel.
- Clients reconcile incremental updates into timeline state with reducers/parsers like `CodexMobile/CodexMobile/Views/Turn/TurnTimelineReducer.swift` and `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/feature/turn/TurnTimelineReducer.kt`.

**Pairing / reconnect flow:**
- The bridge creates or loads device state in `phodex-bridge/src/secure-device-state.js`.
- Mobile apps scan or restore trust data through their connection coordinators.
- The relay provides session rendezvous and trusted-session resolution but does not hold application state beyond live-session metadata.

## Architectural Patterns

**Coordinator/service split:**
- Cross-cutting runtime orchestration stays in service/coordinator objects, not in UI files. Examples: `phodex-bridge/src/bridge.js`, `CodexMobile/CodexMobile/Services/CodexService.swift`, `CodexMobileAndroid/.../SecureConnectionCoordinator.kt`.

**Extension/file partitioning for large types:**
- The iOS `CodexService` family uses extension files to keep transport/history/account/runtime concerns separated.
- Android keeps domain-specific packages under `data/`, `feature/`, `model/`, and `platform/`.

**Repository/container on Android:**
- Android uses an explicit composition root in `RemodexAppContainer.kt`, then pushes capabilities into `DefaultRemodexAppRepository.kt` and `AppViewModel.kt`.

**Reducer/parsing utilities for timeline rendering:**
- Timeline-specific state reconciliation lives in dedicated parser/reducer files instead of screens where feasible, such as `ThinkingDisclosureParser` and `TurnTimelineReducer` on both mobile platforms.

## Entry Points

- Bridge CLI: `phodex-bridge/bin/remodex.js`
- Bridge module exports: `phodex-bridge/src/index.js`
- Relay server entry: `relay/server.js`
- iOS app root: `CodexMobile/CodexMobile/CodexMobileApp.swift`
- Android app root: `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/MainActivity.kt`

## Boundaries To Preserve

- Keep git/workspace access on the Mac bridge in `phodex-bridge/src/`.
- Keep relay responsibilities transport-scoped; do not move application logic there.
- Keep shared logic out of screens/views when it can live in services, repositories, reducers, or coordinators.
- Preserve item-aware timeline reconciliation rather than flattening by turn only; dedicated reducer files show that this is intentional on both mobile clients.
