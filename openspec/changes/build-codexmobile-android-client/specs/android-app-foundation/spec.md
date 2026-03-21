## ADDED Requirements

### Requirement: Android app shell preserves the local-first entry flow
The Android client SHALL present onboarding on first launch and SHALL route returning users to a main app shell that keeps pairing, connection status, threads, and settings accessible without assuming a hosted backend.

#### Scenario: First launch requires onboarding
- **WHEN** the user launches the app without completed onboarding state
- **THEN** the app shows onboarding content with a clear QR pairing call to action and local-first setup guidance

#### Scenario: Returning user resumes the app shell
- **WHEN** the user relaunches the app after onboarding has completed
- **THEN** the app restores the main shell and surfaces locally cached connection or thread state before live sync finishes

### Requirement: Navigation adapts the iOS information architecture to Android form factors
The Android client SHALL provide Android-native navigation that preserves direct access to threads, the active conversation, settings, and pairing recovery across phone and tablet layouts.

#### Scenario: Phone layout exposes the primary destinations
- **WHEN** the app runs on a handset-width layout
- **THEN** the user can reach threads, the current conversation, and settings without relying on iOS-only drawer gestures

#### Scenario: Expanded layout keeps context visible
- **WHEN** the app runs on a tablet or expanded-width layout
- **THEN** the app can present thread navigation and the active conversation concurrently without hiding recovery controls

### Requirement: Android visuals replace template defaults with Remodex branding
The Android client SHALL replace the default Android Studio template visuals with a Remodex-branded design system that remains legible in light and dark environments and meets Android accessibility expectations.

#### Scenario: Template starter content is removed
- **WHEN** the main activity starts
- **THEN** the user sees branded onboarding or conversation UI instead of the default greeting/template screen

#### Scenario: Accessibility settings remain supported
- **WHEN** the user changes system font scale or other accessibility-related display settings
- **THEN** the app keeps primary controls readable and actionable without clipping critical content
