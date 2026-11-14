# Aurora v1.5: 52 wired capabilities and their verification requirements

**Status language:** Wired = implemented in code, not a guarantee that it works on every Android TV. Build = Android CI compilation and signing. Device = DPAD and feature tested on real TV. As of this document, device tests are **not verified**. Several features are conditional on app permissions, external providers, OEM launcher roles, and backend configuration.

## TV home and system (1–17)

1. [wired] Distinct Aurora HOME entry activity
2. [wired] Accessibility Home-button redirect to Aurora
3. [wired] Native DPAD-focused navigation
4. [wired] Original GPU-light aurora gradient background
5. [wired] Dynamic clock and date
6. [wired] Weather display using inherited Weather service
7. [wired] Weather location picker (local city/coordinates)
8. [wired] Weather refresh
9. [wired] Secure GitHub release check and install through SelfUpdate
10. [wired] Android Home-role request (OEM-dependent)
11. [wired] Network / Wi-Fi system settings shortcut
12. [wired] Bluetooth settings shortcut
13. [wired] Sound settings shortcut
14. [wired] Picture/display settings shortcut
15. [wired] Storage settings shortcut
16. [wired] Accessibility settings shortcut
17. [wired] Date/time, keyboard and apps settings shortcuts

## Apps & organization (18–38)

18. [wired] Discover Android TV app entries
19. [wired] Discover sideloaded launcher entries
20. [wired] Launch app (Leanback-first fallback)
21. [wired] Prefer TV banner art over app icon
22. [wired] Fallback to app icon if banner missing
23. [wired] Adaptive multi-column app grid
24. [wired] Alternative horizontal app row
25. [wired] 4/5/6-column grid preference
26. [wired] Show/hide app labels
27. [wired] Live title-based local app search
28. [wired] Games-only category filter
29. [wired] Favorites toggled via long OK-press
30. [wired] Favorites-first organization
31. [wired] Favorites-only filter
32. [wired] Alphabetical sort
33. [wired] Reverse alphabetical sort
34. [wired] Recent-launch ordering
35. [wired] Hide installed app from Aurora
36. [wired] Reveal hidden apps and unhide from app menu
37. [wired] App information shortcut
38. [wired] Android uninstall confirmation

## Media & AI discovery (39–50)

39. [wired] Featured streaming title rail from cached Trending picks
40. [wired] Background refresh of catalog
41. [wired] Continue Watching from existing media-session records, when permitted
42. [wired] Launch app from saved Continue Watching media
43. [wired] Search cached catalog without requiring an AI key
44. [wired] Genre quick searches (action, comedy, drama, sci-fi, thriller, romance, family)
45. [wired] Empty-catalog fallback to installed streaming apps
46. [wired] Direct streaming provider app launch
47. [wired] Optional server-side GPT recommendations for a paired TV
48. [wired] One-time gateway code pairing dialog (requires operator-issued code)
49. [wired] Secure session refresh and disconnect
50. [wired] User-readable offline and failure states, without asking to paste OpenAI keys

## Personalization & recovery (51–57)

51. [wired] Local wallpaper picker via Android document provider
52. [wired] Restore original aurora backdrop
53. [wired] Motion level full/subtle/off
54. [wired] Respect Android's system animations-off preference
55. [wired] Reset app layout, filters, and hidden items (favorites retained)
56. [wired] English-only Discovery and modern home
57. [wired] Signed upgrade compatibility via unchanged package name

## Blockers and honesty

- Gateway remains an authenticated server and **does not supply free GPT inference without pairing**. An operator must generate a one-time device code; never place an OpenAI API key into the TV APK or local UI.
- Catalog availability depends on TMDB/StreamRadar, country, provider inventory and caches. A missing catalog must not be portrayed as AI failure.
- Android Home role, TV settings shortcuts and uninstall flows vary by OEM and API level.
- This is **57 code paths/features, not 57 independently device-verified production behaviors**.
- Next: Android emulator screenshot suite, DPAD navigation test, feature smoke tests, crash recovery, performance profiling and accessibility pass.
