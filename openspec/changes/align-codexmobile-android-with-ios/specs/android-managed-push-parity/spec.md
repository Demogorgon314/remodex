## ADDED Requirements

### Requirement: Android SHALL support managed completion push parity with iOS
The Android client SHALL register for managed push notifications and the bridge/relay stack SHALL deliver Android completion notifications using a provider-aware registration flow.

#### Scenario: Android syncs its FCM token through the bridge
- **WHEN** Android has notification permission and an FCM token
- **THEN** it sends a `notifications/push/register` request that identifies the Android platform/provider while preserving the shared payload shape used by iOS

#### Scenario: Relay sends completion pushes to the correct provider
- **WHEN** a paired session has a registered Android device token and a turn completes while the Android app is away from the foreground
- **THEN** the relay sends the completion push through FCM instead of APNs and the notification payload still routes the user back to the correct thread
