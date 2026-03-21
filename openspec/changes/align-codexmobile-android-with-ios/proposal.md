## Why

`CodexMobileAndroid` now has a usable local-first baseline, but it still diverges from `CodexMobile` in both surface area and fidelity. The current Android client keeps important conversation flows on fake thread/runtime data, lacks the iOS sidebar/composer/git/review surfaces, and does not support managed background push on Android, so it cannot yet be considered a true peer client.

## What Changes

- Replace the Android fake thread/runtime foundation with bridge-backed repositories, parity-ready domain models, and persistence that matches iOS conversation semantics.
- Rebuild the Android shell, sidebar, settings, conversation, and composer to mirror iOS information architecture, copy, and interaction order as closely as Android platform rules allow.
- Add the missing high-value parity workflows on Android, including model/service-tier controls, archived chats, new-chat project picking, subagent/review/git/worktree affordances, and notification recovery banners.
- Extend Android device integration from local notifications only to full managed push parity by adding Firebase Cloud Messaging on Android and platform-aware push registration/delivery support in `phodex-bridge` and `relay`.

## Capabilities

### New Capabilities
- `android-bridge-state-parity`: Bridge-backed Android thread, runtime, and timeline state that matches iOS semantics instead of relying on fake sync data.
- `android-shell-parity`: iOS-matched Android shell, sidebar, settings, and navigation structure with archived chats, project grouping, and trusted-Mac surfaces.
- `android-composer-git-parity`: Full Android parity for the iOS conversation container, composer, runtime controls, git/worktree, review, and subagent workflows.
- `android-managed-push-parity`: Android FCM integration plus cross-platform push registration and completion delivery support in the bridge/relay stack.

### Modified Capabilities
- None.

## Impact

- Affected code: `CodexMobileAndroid`, `phodex-bridge`, `relay`, Android resources, Gradle dependencies, and Android/Node test suites.
- API impact: `notifications/push/register` becomes platform-aware and the Android secure connection layer must expose authenticated bridge request/event plumbing.
- Dependencies: Firebase Messaging for Android plus an FCM sender on the relay side; no secrets or config files are committed to the repo.
- Documentation impact: Android setup docs must describe the parity scope and managed-push configuration requirements.
