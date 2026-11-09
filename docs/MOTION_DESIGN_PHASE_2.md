# AI TV 2.0 — Motion design phase 2

## Goals
- macOS-inspired, original Android TV interface, not a pixel-for-pixel copy.
- Butter-smooth DPAD focus, app launch and return-to-home motion.
- Subtle 3D depth and expressive Telugu reactions without obstructing content.

## Motion rules
- Focus: spring-driven scale, 1–2 degree perspective, 14dp elevation, 190–220ms elevation settle.
- Open: native tile-origin scale-up for external apps; 300ms zoom/fade for internal AI Discovery.
- Close: 160–210ms internal screen shrink/fade; for external apps animate launcher return where supported. Third-party close animations cannot be controlled universally by an ordinary launcher.
- Respect the existing 'lite' performance setting (zero-duration transforms).
- Avoid heavy blur, repeated bitmaps, GPU overdraw and blocking UI-thread decode.
- Aim for stable 60fps on supported TVs; measure jank and frame times on real hardware.

## Telugu animation plan
- Use original reaction animations or media with explicit redistribution rights.
- Provide small, high-quality, optimized animated WebP assets (GIF-compatible source pipeline), cached and asynchronously decoded.
- Reaction themes: playful success, searching, empty results, failed network, welcome, update complete.
- No autoplay with sound; no flashing, strobing, or distracting loops.
- Never use unlicensed Telugu film clips or celebrity likenesses.
- Provide English fallback and optional Telugu display language.

## Delivery checklist
- [x] AI Discovery zoom/fade opening and closing.
- [x] 3D-style focus on AI Discovery actions.
- [x] Launcher-wide subtle 3D focus depth and spring scale.
- [x] Lightweight return-to-home animation (device frame pacing still unmeasured).
- [x] Original animated GIF sparkle asset and Telugu reaction captions (further variants pending).
- [x] Discovery-screen preferences: motion level (full / subtle / off) and Telugu reactions on/off.
- [ ] Performance profiling on low-end Android TV and real-device QA.
- [ ] Bump versionName/versionCode only when ready to publish the next tested release.
