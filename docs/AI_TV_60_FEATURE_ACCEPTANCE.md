# AI TV 2.0 — 60 feature acceptance checklist

**Status rule:** An item is DONE only after it is wired into the launcher, passes CI, and its behavior is exercised by automated tests or a device test. Existing upstream features require revalidation; infrastructure or standalone class methods are not complete features.

## Native Metal UI
- [ ] 1. Cinematic hero rail
- [ ] 2. Titanium focus cards
- [ ] 3. Spring DPAD transitions
- [ ] 4. Vivid/Cinema/Comfort modes
- [ ] 5. High-contrast accessibility
- [ ] 6. Reduced-motion mode
- [ ] 7. Ambient light animations
- [ ] 8. Adaptive theme by artwork
- [ ] 9. Focus-safe overscan layout
- [ ] 10. Performance/Lite rendering

## AI Discovery
- [ ] 11. Natural-language title search
- [ ] 12. Catalog-grounded GPT ranking
- [ ] 13. Explain recommendations
- [ ] 14. Mood discovery
- [ ] 15. Runtime-aware suggestions
- [ ] 16. Language-aware discovery
- [ ] 17. Provider-aware discovery
- [ ] 18. Similar-title discovery
- [ ] 19. Offline search fallback
- [ ] 20. Recommendation feedback

## Personalization
- [ ] 21. Local watchlist
- [ ] 22. Watched-title filtering
- [ ] 23. Like/dislike controls
- [ ] 24. Hide unwanted titles
- [ ] 25. Kids-mode filtering
- [ ] 26. Multiple local profiles
- [ ] 27. Per-profile preferences
- [ ] 28. Preferred streaming provider
- [ ] 29. Preferred language
- [ ] 30. Personalization data reset

## Playback and Apps
- [ ] 31. Installed-app discovery
- [ ] 32. Favorites dock
- [ ] 33. Recently opened apps
- [ ] 34. Continue watching rail
- [ ] 35. Provider deep links
- [ ] 36. Trailer fallback
- [ ] 37. HDMI input shortcuts
- [ ] 38. Universal command palette
- [ ] 39. App quick search
- [ ] 40. Resume last app

## Privacy and Security
- [ ] 41. OpenAI key server-only
- [ ] 42. Secure pairing screen
- [ ] 43. One-use pairing codes
- [ ] 44. Expiring device tokens
- [ ] 45. Persistent device registry
- [ ] 46. Device revocation
- [ ] 47. Explicit AI opt-in
- [ ] 48. Data-saver mode
- [ ] 49. No-history mode
- [ ] 50. Per-device API quotas

## Smart TV Utilities
- [ ] 51. Network status dashboard
- [ ] 52. Audio output status
- [ ] 53. Sleep timer shortcuts
- [ ] 54. Battery-saver detection
- [ ] 55. Quiet mode
- [ ] 56. Clock and weather
- [ ] 57. Mini-LED calibration patterns
- [ ] 58. Streaming service diagnostics
- [ ] 59. Offline mode indication
- [ ] 60. Crash-safe launcher fallback

## Current implementation foundations
- Existing launcher, app navigation, Trending catalog and utilities remain present.
- Gateway source and Render deployment exist; pairing backend is prototype and needs persistence and revocation.
- Android recommendation and pairing clients are standalone, not wired into a screen.
- AiTvPersonalization implements local settings and deterministic offline ranking; no UI yet.
- No 60-feature APK is certified. Do not market as finished before acceptance.
