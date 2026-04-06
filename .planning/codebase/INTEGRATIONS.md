# Integrations

**Analysis Date:** 2026-04-07

## Core Transport

**Bridge <-> Relay:**
- The Mac bridge opens a WebSocket session to the relay using the saved/local pairing state in `phodex-bridge/src/bridge.js`.
- The relay accepts `/relay/{sessionId}` upgrades and routes one Mac socket plus one iPhone/phone client per session in `relay/server.js` and `relay/relay.js`.

**Phone <-> Bridge Secure Session:**
- Trusted reconnect and secure-session bootstrap are implemented as relay-assisted but end-to-end encrypted flows in `relay/README.md`, `relay/relay.js`, `phodex-bridge/src/secure-transport.js`, and `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/data/connection/SecureConnectionCoordinator.kt`.
- Do not treat relay payloads as application plaintext once secure transport is active; current code treats the relay as a transport hop only.

## Local Runtime Integrations

**Codex Runtime:**
- The bridge launches or connects to the local Codex runtime through `createCodexTransport()` in `phodex-bridge/src/bridge.js` and `phodex-bridge/src/codex-transport.js`.
- Desktop refresh/handoff hooks live in `phodex-bridge/src/codex-desktop-refresher.js` and `phodex-bridge/src/desktop-handler.js`.

**Git and Workspace Control:**
- Git requests are handled locally in `phodex-bridge/src/git-handler.js`.
- Workspace/thread context routing lives in `phodex-bridge/src/workspace-handler.js` and `phodex-bridge/src/thread-context-handler.js`.
- The relay never executes git or touches repository state; `relay/README.md` explicitly keeps those concerns on the Mac.

## Push and Notifications

**Relay Push Service:**
- Optional push registration and run-completion delivery are implemented in `relay/push-service.js`.
- APNs integration is in `relay/apns-client.js`.
- FCM integration is in `relay/fcm-client.js`.
- Push endpoints are exposed only when enabled in `relay/server.js`.

**Bridge Push Hooks:**
- The bridge registers and emits completion events via `phodex-bridge/src/push-notification-service-client.js`, `phodex-bridge/src/push-notification-tracker.js`, and `phodex-bridge/src/notifications-handler.js`.

**Client Push Hooks:**
- iOS notification configuration is triggered from `CodexMobile/CodexMobile/CodexMobileApp.swift` and `CodexMobile/CodexMobile/Services/CodexService+Notifications.swift`.
- Android push registration and notification handling live in `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/platform/notifications/`.

## Pairing and Trust

**QR Pairing:**
- QR payload creation/printing is handled in `phodex-bridge/src/qr.js` and the CLI/service wrappers under `phodex-bridge/src/index.js`, `phodex-bridge/src/macos-launch-agent.js`, and `phodex-bridge/src/linux-systemd.js`.
- Android scanner and pairing models live in `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/feature/recovery/` and `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/data/connection/`.
- iOS scanner UI lives in `CodexMobile/CodexMobile/Views/QRScannerView.swift` and `CodexMobile/CodexMobile/Views/QRScannerPairingValidator.swift`.

**Trusted Session Resolve:**
- Relay HTTP resolution endpoint is `POST /v1/trusted/session/resolve` in `relay/server.js`.
- Android consumes trusted-session resolution through `OkHttpTrustedSessionResolver` wiring in `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/data/app/RemodexAppContainer.kt`.

## Subscription and External Services

**RevenueCat:**
- iOS config and purchase flows use RevenueCat in `CodexMobile/CodexMobile/CodexMobileApp.swift` and `CodexMobile/CodexMobile/Services/Payments/SubscriptionService.swift`.
- No matching Android subscription integration is visible in the sampled Android container/build wiring.

**Voice/Account Flows:**
- Bridge voice/auth helpers live in `phodex-bridge/src/voice-handler.js` and `phodex-bridge/src/account-status.js`.
- Android voice transcription services are wired in `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/data/voice/`.
- iOS voice/GPT flows are split across `CodexMobile/CodexMobile/Services/CodexService+Voice.swift`, `CodexMobile/CodexMobile/Services/CodexService+VoiceCompatibility.swift`, and `CodexMobile/CodexMobile/Services/GPTVoiceTranscriptionManager.swift`.

## Persistence Boundaries

**Local Device Persistence:**
- Bridge pairing/session state lives under bridge-side storage helpers such as `phodex-bridge/src/secure-device-state.js`, `phodex-bridge/src/session-state.js`, and `phodex-bridge/src/daemon-state.js`.
- Android uses encrypted preferences, Room, and DataStore in `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/data/connection/`, `.../data/threads/`, and `.../data/preferences/`.
- iOS persistence helpers live under `CodexMobile/CodexMobile/Services/` including `CodexMessagePersistence.swift`, `AIChangeSetPersistence.swift`, and `SecureStore.swift`.

**Hosted Persistence:**
- Relay push state can be file-backed in `relay/push-service.js`.
- The relay session registry itself is in-memory in `relay/relay.js`.

## Integration Guidance

- Add new hosted integrations only if they preserve the local-first boundary documented in `AGENTS.md` and `relay/README.md`.
- Put bridge-to-runtime or bridge-to-local-OS integrations in `phodex-bridge/src/`.
- Put mobile-only service integrations under `CodexMobile/CodexMobile/Services/` or `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/data/` and expose them through the existing service/container layer rather than directly from views/screens.
