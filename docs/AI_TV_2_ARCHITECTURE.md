# AI TV 2.0 — native-first architecture

Target: 55-inch Mini-LED Android TV, 16:9 landscape. Screen inches do not determine Android dp resolution: use actual density and safe areas, with 10-foot legibility and DPAD-first focus.

## Modules
- Native Android launcher (existing HomeActivity kept as working fallback while replacing screens incrementally)
- Metal UI renderer (opaque graphite, vivid spectral accents, focused metallic cards)
- Catalog adapters (existing Trending/TMDB and device-supported providers)
- Recommendation gateway (ai-gateway) that ranks only verified catalog candidates
- Offline local search and deterministic fallback recommendations
- Device controls with capability checks

## Release gates
1. Existing debug APK CI remains green.
2. Gateway unit/integration tests and error-path tests.
3. Secure device pairing and short-lived access tokens; never ship OpenAI key or permanent shared bearer token in APK.
4. Recommendation UI must show provider availability only when verified and timestamped.
5. 55-inch TV usability: DPAD focus always visible, no tiny labels, reduced-motion support, no flashing.
6. Real Mini-LED evaluation for blooming, dark-detail crush and perceived highlight intensity.
7. Measure frame pacing, memory and network latency on actual Android TV hardware.

## New capabilities to prototype
- Intent-based GPT discovery across verified catalog candidates
- Why-this-title explanation and provider-aware filtering
- Contextual command palette (apps, HDMI, settings, media)
- Offline-first local recommendation fallback
- Per-profile visual presets: Cinema, Vivid, Comfortable
- Local-only watchlist and optional explicit consent for personalization
- Performance governor with power saver and Lite modes
- TV contrast calibration patterns (not claims of HDR output)

## Current state
Gateway is an initial backend scaffold, not deployed or connected to the Android UI. The existing Java launcher and its APK pipeline are unchanged on this branch. No API key is present in the repository.
