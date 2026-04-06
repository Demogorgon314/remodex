# Concerns

**Analysis Date:** 2026-04-07

## High-Risk Hotspots

**Very large files concentrate critical behavior:**
- `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/feature/turn/ConversationScreen.kt` (~13k lines)
- `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/data/threads/BridgeThreadSyncService.kt` (~10.5k lines)
- `CodexMobileAndroid/app/src/main/java/com/emanueledipietro/remodex/feature/appshell/AppViewModel.kt` (~5k lines)
- `CodexMobile/CodexMobile/Services/CodexService+Messages.swift` and `CodexMobile/CodexMobile/Services/CodexService+Incoming.swift` (~3k lines each)
- `phodex-bridge/src/bridge.js` (~1.5k lines)

Impact:
- These files mix many edge cases and make regressions harder to isolate.
- Small edits in timeline, sync, or transport logic can ripple across UX and reconnect behavior.

Suggested direction:
- Continue extracting reducers/helpers/coordinators from the biggest files instead of adding more branches inline.

## Cross-Platform Drift Risk

- iOS and Android both implement timeline parsing, reconnect behavior, turn state, and feature surfaces, but they do so in separate native stacks.
- Files like `CodexMobile/CodexMobile/Views/Turn/TurnTimelineReducer.swift` and `CodexMobileAndroid/.../feature/turn/TurnTimelineReducer.kt` encode similar intent independently.

Impact:
- Bug fixes can land on one platform and miss the other.
- Feature parity work has a higher regression risk than single-platform features.

Suggested direction:
- When touching timeline, pairing, or thread sync behavior, audit both mobile clients together and add parallel regression tests when applicable.

## Relay / Bridge Boundary Sensitivity

- The system depends on a narrow but security-sensitive transport boundary across `relay/relay.js`, `relay/server.js`, `phodex-bridge/src/secure-transport.js`, and mobile secure connection code.
- Session replacement, trust resolution, buffering, and close-code semantics are all behaviorally significant.

Impact:
- Small protocol mismatches can break reconnect, pairing, or push delivery in ways that are hard to diagnose from one runtime alone.

Suggested direction:
- Preserve close-code semantics and structured error codes.
- Add tests whenever changing session lifecycle, trusted-session resolution, or relay logging behavior.

## Local-First Guardrail Pressure

- The repo contains both local-first app code and a self-hostable relay. That makes it easy for future changes to accidentally re-centralize behavior.
- `AGENTS.md` explicitly warns against remote deployment assumptions, selected-repo filtering regressions, and logging bearer-like pairing identifiers.

Impact:
- Architectural drift would directly contradict current product intent and could create privacy/security regressions.

Suggested direction:
- Treat `AGENTS.md`, `CLAUDE.md`, and `relay/README.md` as architectural constraints, not optional docs.

## Partial Tooling / Test Fragmentation

- There is no single monorepo-wide build/test command surfaced at the root.
- Runtime-specific test entrypoints live in each subproject: `phodex-bridge/package.json`, `relay/package.json`, Xcode targets, and Android Gradle.

Impact:
- Cross-runtime refactors are slower to verify end to end.
- Contributors can easily validate one runtime and miss regressions in another.

Suggested direction:
- For multi-surface changes, explicitly verify each affected runtime rather than assuming one test suite is representative.

## Minor Observed Debt

- The only visible TODO in first-party source is Android backup XML in `CodexMobileAndroid/app/src/main/res/xml/data_extraction_rules.xml`.
- Bundled third-party/minified assets such as `CodexMobile/CodexMobile/Resources/Mermaid/mermaid.min.js` and `CodexMobileAndroid/app/src/main/assets/mermaid.min.js` are large and noisy in repo-wide searches.

Impact:
- Search results can get polluted unless queries exclude bundled assets.
- Asset updates should be handled deliberately because they obscure diffs.

Suggested direction:
- Exclude bundled/minified assets from code searches when investigating first-party logic.
