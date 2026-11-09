# AI TV — development plan

## Baseline
Fork of raju pp/Talkies. Android application: Java 17, compile/target SDK 35, minimum SDK 26. Preserve existing launcher functionality and attribution.

## Phase 1: Safe foundation
- Establish independent AI TV application identity (applicationId, display name, icons) with migration strategy; do not change package identifiers without checking manifest and hard-coded service references.
- Set up CI to build debug APK on pull requests, upload artifact, and report failures.
- Keep signing credentials out of Git; release signing via encrypted CI secrets only.
- Preserve DPAD focus navigation, accessibility, low-memory performance, and offline launcher usability.
- Test Android TV 8 through current Google TV versions.

## Phase 2: Premium interface
- Introduce coherent dark theme, typography, configurable backgrounds and smooth but lightweight focus animations.
- Make reduced-motion and Lite effects options available.
- Keep apps and essential controls usable without network or AI.

## Phase 3: Optional AI
- Add opt-in natural-language search and recommendations.
- Never embed provider API keys in the APK. Use an authenticated backend/proxy or user-managed keys with clear limitations.
- Explicit consent for any viewing-history or voice data transfer; no tracking by default.
- Handle provider errors, quotas, and offline operation gracefully.

## Phase 4: Release
- Automated tests, versioning, reproducible APK builds and signed release workflow.
- Update checks must point to this fork's own releases only after a signing and update migration plan.
- Maintain upstream MIT attribution and third-party notices.

## First milestone acceptance criteria
1. Clean checkout builds a debug APK in GitHub Actions.
2. APK launches on Android TV emulator and supports DPAD navigation.
3. No regressions in apps dock, settings, weather or controls.
4. No credentials committed to repository.
