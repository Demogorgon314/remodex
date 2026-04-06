# Technology Stack

**Analysis Date:** 2026-04-07

## Languages

**Primary:**
- Swift - iOS app code under `CodexMobile/CodexMobile/`
- Kotlin - Android app code under `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/`
- JavaScript (CommonJS) - local bridge under `phodex-bridge/src/` and relay under `relay/`

**Secondary:**
- Java - Android syntax-highlighting helpers under `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/feature/turn/RemodexPrism4jFactory.java`
- Gradle Kotlin DSL - Android build config in `CodexMobileAndroid/build.gradle.kts` and `CodexMobileAndroid/app/build.gradle.kts`
- XML - Android manifest/resources in `CodexMobileAndroid/app/src/main/`
- Shell - helper scripts in `CodexMobile/scripts/` and `CodexMobileAndroid/scripts/`
- AppleScript - desktop handoff helpers in `phodex-bridge/src/scripts/`

## Runtime

**Environment:**
- Node.js for the bridge CLI and relay server in `phodex-bridge/package.json` and `relay/package.json`
- iOS runtime with SwiftUI/Observation entrypoints in `CodexMobile/CodexMobile/CodexMobileApp.swift`
- Android runtime targeting SDK 36 in `CodexMobileAndroid/app/build.gradle.kts`

**Package Manager:**
- npm for `phodex-bridge/` and `relay/`
- Gradle version catalogs for Android in `CodexMobileAndroid/gradle/libs.versions.toml`
- Xcode project-managed dependencies for iOS in `CodexMobile/CodexMobile.xcodeproj`
- Lockfiles: present in `phodex-bridge/package-lock.json` and `relay/package-lock.json`

## Frameworks

**Core:**
- SwiftUI + Observation - iOS UI/state in `CodexMobile/CodexMobile/Views/` and `CodexMobile/CodexMobile/Services/CodexService.swift`
- Jetpack Compose - Android UI in `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/feature/`
- OkHttp + kotlinx.serialization + coroutines - Android networking/state flow in `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/data/`
- `ws` - WebSocket transport for bridge and relay in `phodex-bridge/src/bridge.js` and `relay/server.js`

**Testing:**
- XCTest for iOS unit/UI tests in `CodexMobile/CodexMobileTests/` and `CodexMobile/CodexMobileUITests/`
- JUnit4 + Compose UI test APIs for Android tests in `CodexMobileAndroid/app/src/test/` and `CodexMobileAndroid/app/src/androidTest/`
- `node:test` + `node:assert/strict` for bridge and relay tests in `phodex-bridge/test/` and `relay/*.test.js`

**Build/Dev:**
- Android Gradle Plugin 8.13.2 and Kotlin 2.0.21 from `CodexMobileAndroid/gradle/libs.versions.toml`
- Room + KSP + DataStore on Android from `CodexMobileAndroid/app/build.gradle.kts`
- RevenueCat on iOS from `CodexMobile/CodexMobile/CodexMobileApp.swift` and `CodexMobile/CodexMobile/Services/Payments/SubscriptionService.swift`

## Key Dependencies

**Critical:**
- `ws` - bridge-to-relay and relay-to-client transport in `phodex-bridge/package.json` and `relay/package.json`
- RevenueCat - subscription/paywall flows in `CodexMobile/CodexMobile/Services/Payments/SubscriptionService.swift`
- Firebase Messaging - Android push delivery in `CodexMobileAndroid/app/build.gradle.kts`
- Room - Android thread cache persistence in `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/data/threads/RoomThreadCacheStore.kt`
- OkHttp - Android relay/trusted-session/voice HTTP transport in `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/data/app/RemodexAppContainer.kt`

**Infrastructure:**
- APNs + FCM clients in `relay/apns-client.js`, `relay/fcm-client.js`, and `relay/push-service.js`
- QR pairing via `qrcode-terminal` in `phodex-bridge/package.json` and `phodex-bridge/src/qr.js`
- Camera, ZXing, security-crypto, BouncyCastle, Markwon, Prism4j, and Coil on Android from `CodexMobileAndroid/gradle/libs.versions.toml`

## Configuration

**Environment:**
- Bridge runtime is read from local config through `readBridgeConfig()` in `phodex-bridge/src/bridge.js`
- Relay uses environment variables for push, proxy trust, and credentials as documented in `relay/README.md`
- Android signing and version metadata come from Gradle properties or env vars in `CodexMobileAndroid/app/build.gradle.kts`
- iOS RevenueCat bootstrap depends on Info.plist-backed config accessed through `CodexMobile/CodexMobile/Services/AppEnvironment.swift`

**Build:**
- Android root settings in `CodexMobileAndroid/settings.gradle.kts`
- Android dependency versions in `CodexMobileAndroid/gradle/libs.versions.toml`
- Bridge CLI entrypoint in `phodex-bridge/bin/remodex.js`
- Relay entrypoint in `relay/server.js`

## Platform Requirements

**Development:**
- macOS + Xcode for `CodexMobile/`
- Android SDK + JDK 11 compatible Gradle toolchain for `CodexMobileAndroid/`
- Node.js for `phodex-bridge/` and `relay/`

**Production:**
- Local Mac host running the bridge/CLI from `phodex-bridge/`
- Native iOS client from `CodexMobile/`
- Native Android client from `CodexMobileAndroid/`
- Optional self-hosted relay/push service from `relay/`

## Observed Defaults

- The repo is local-first. The bridge is the system boundary that keeps Codex, git, and workspace access on the user's Mac in `phodex-bridge/src/bridge.js` and `relay/README.md`.
- Hosted-service assumptions are intentionally constrained. Relay code exists for transparency and optional self-hosting, not as the source of business logic.
