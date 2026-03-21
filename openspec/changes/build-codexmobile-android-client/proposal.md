## Why

Remodex already has a mature local-first iOS client, but the Android app is still only a default Compose scaffold. Shipping an Android client now unlocks the same Mac-to-phone Codex workflows for Android users while reusing the bridge protocol, local-first pairing model, and interaction patterns that already exist in `CodexMobile`.

## What Changes

- Add a production Android client under `CodexMobileAndroid` that can pair with the local bridge, reconnect to a trusted Mac, browse threads, and participate in Codex conversations.
- Mirror the iOS app's core information architecture and interaction model where it makes sense, while adapting layout, navigation, permissions, and system integrations to Android best practices.
- Establish Android-native foundations for onboarding, QR pairing, local persistence, connection recovery, thread/timeline rendering, composer actions, and settings.
- Introduce a phased implementation path so the Android client can ship a usable daily-driver baseline before advanced parity features are hardened.

## Capabilities

### New Capabilities
- `android-app-foundation`: Android-native app shell, onboarding, navigation, theming, and local-first configuration defaults for the Remodex client.
- `android-secure-connection`: QR bootstrap, trusted Mac persistence, secure transport handshake, reconnect, and recovery UX for the Android app.
- `android-conversation-workflows`: Thread list, turn timeline, streaming updates, composer send/stop/queue behavior, and runtime controls for Android conversations.
- `android-device-integrations`: Android support for attachments, notifications, and other device-level integrations needed to make the mobile client practical beyond basic chat.

### Modified Capabilities
- None.

## Impact

- Affected code: `CodexMobileAndroid` app structure, Compose UI, Android resources, Gradle dependencies, and test suites.
- Reused concepts: protocol models, bridge message semantics, pairing lifecycle, and UI/state patterns already implemented in `CodexMobile`.
- New dependencies likely required: AndroidX Navigation, Lifecycle ViewModel, Kotlin serialization, a websocket client, QR scanning, secure local storage, and local persistence.
- Documentation impact: Android setup and local run guidance in shared project docs will need to acknowledge the new client once implementation lands.
