# Aurora — AI TV 2.0 design & feature implementation

## Research anchors (2026-11-14)

- [JackyAndroid/AndroidTVLauncher](https://github.com/JackyAndroid/AndroidTVLauncher): ~3.1k GitHub stars, high-star Leanback reference; older, GPL-3.0.
- [meddouribadis/arclauncher](https://github.com/meddouribadis/arclauncher): ~620 stars, settings, wallpaper and categories; GPL-3.0.
- [leanbitlab-org/LtvLauncher](https://github.com/leanbitlab-org/LtvLauncher): ~470 stars, clean TV customization and screensaver; GPL-3.0.
- [nielsvanvelzen/tv-launcher](https://github.com/nielsvanvelzen/tv-launcher): ~499 stars, big-screen Compose patterns; GPL-3.0.
- [zompinc/flauncher](https://github.com/zompinc/flauncher): mouse/pointer interaction and offline-first reference.
- [maetthu/arclauncher](https://github.com/maetthu/arclauncher): focus navigation, color presets and favorites.
- Earlier comparative list: [LAUNCHER_RESEARCH_23.md](LAUNCHER_RESEARCH_23.md).

This is an **original design implementation**, not a merged copy of GPL assets/code. Repo star counts are snapshots.

## Design language: Aurora

- Toned charcoal/navy background, sea-glass emerald accent, warm champagne bronze, restrained lavender-blue atmosphere.
- Vertical information hierarchy: brand/time → single-line TV nav → hero/action + weather/system status → cinematic apps.
- DPAD focus uses outline, gentle transform, and depth (not scale-only).
- No Telugu text, noisy emoji UI, or mandatory popups.
- No looping full-screen animation or expensive background shader frame loop.
- Aim for 55-inch/10-foot readability and prevent action overlap.

## 23-item feature acceptance roadmap

Status is intentionally conservative: **implemented** means wired in code, not hardware verified.

1. [x] Original Aurora gradient atmosphere
2. [x] Rebuilt compact navigation bar
3. [x] Responsive multi-column hero / fixed-width AI CTA
4. [x] TV app banners with icon fallback
5. [x] Installed app discovery
6. [x] AI Discovery navigation
7. [x] Current clock/date
8. [x] Live weather service
9. [x] Weather city/coordinates picker
10. [x] Secure GitHub update check/install
11. [x] Set default launcher via Android Home settings
12. [x] Favorites via long-press
13. [x] Favorites-first organization
14. [x] Alphabetical/reverse sort
15. [x] App title search
16. [x] Games-only filter
17. [x] Launch app
18. [x] App details menu
19. [x] Uninstall via system confirmation
20. [x] Open system settings
21. [x] Reduced motion / intensity settings
22. [ ] User-uploaded wallpaper / wallpaper library
23. [ ] Personalized Continue Watching row with provider permission checks

Additional future work: TV input selector, live memory/network status, remote key customization, onboarding, tests, accessibility, app reorder, crash-safe launcher recovery, animation profiling.

## Critical safety notes

Home-role availability depends on OEM firmware. Only direct the user to Android's system chooser; **never** automatically disable factory launcher, execute ADB, or claim default-home selection was applied.

Keep the previous launcher installed and available for rollback. Android emulator build success does not prove appearance, 60 FPS, weather connectivity, updater permissions, or DPAD navigation on a real TV.

Screenshots must come from a real app instance, not concept art.
