## ADDED Requirements

### Requirement: QR pairing validates bridge payloads before bootstrap
The Android client SHALL scan pairing QR payloads, validate required fields and compatibility metadata, and block secure bootstrap when the payload is expired, malformed, or incompatible with the app build.

#### Scenario: Valid QR payload starts pairing
- **WHEN** the user scans a QR code with a supported payload version and valid expiry
- **THEN** the app starts secure pairing against the bridge session described by that payload

#### Scenario: Incompatible QR payload is rejected with recovery guidance
- **WHEN** the user scans a QR code whose payload version or expiry is invalid
- **THEN** the app refuses to connect and shows actionable recovery guidance instead of attempting a broken handshake

### Requirement: Trusted Mac metadata is stored securely for reconnect
The Android client SHALL persist trusted Mac metadata and cryptographic material in Android-secure storage and SHALL avoid exposing bearer-like pairing identifiers in logs or user-visible diagnostics.

#### Scenario: Successful pairing persists trust
- **WHEN** secure bootstrap completes successfully
- **THEN** the app stores the trusted Mac record locally so the same Mac can be reconnected without rescanning a QR code

#### Scenario: Diagnostics redact sensitive pairing values
- **WHEN** a pairing or reconnect error is surfaced
- **THEN** the app presents recovery information without displaying raw session identifiers or secret material

### Requirement: Trusted reconnect and recovery follow the saved trust model
The Android client SHALL attempt reconnect from saved trust state when available, SHALL fall back to QR recovery when trust is invalid, and SHALL explain the next recovery step in plain language.

#### Scenario: Saved trust reconnects automatically
- **WHEN** the app has valid trusted Mac state and the bridge session is available
- **THEN** the app reconnects without requiring the user to scan a fresh QR code

#### Scenario: Invalid trust falls back to recovery
- **WHEN** reconnect fails because the saved trust no longer matches the bridge state
- **THEN** the app resets into a recovery path that prompts the user to scan a new QR code

### Requirement: QR scanning handles Android camera permissions gracefully
The Android client SHALL request camera permission only when scanning is needed and SHALL provide a recoverable UX when camera access is denied.

#### Scenario: Camera permission is granted at scan time
- **WHEN** the user opens the scanner and grants camera permission
- **THEN** the app starts live QR scanning without requiring an app restart

#### Scenario: Camera permission is denied
- **WHEN** the user denies camera permission for QR scanning
- **THEN** the app shows a non-blocking explanation with actions to retry or open system settings
