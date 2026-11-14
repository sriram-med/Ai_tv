# AI TV 2.0 — 23 launcher repositories and forks considered

Research date: November 2026. These are design/architecture references, **not copied or merged dependencies**. Review licenses and compatibility before incorporating code.

| Repository | Candidate lesson |
|---|---|
| simonsruggi/simonslauncher | real TV channel previews, safe-mode fallback |
| Kevsosmooth/glass-launcher | frosted dock, DPAD controls, motion performance |
| teja2495/my-toolkit-tv | Compose app grid, native audio shortcut |
| zompinc/flauncher | air-mouse support, telemetry-free launcher |
| leanbitlab-org/LtvLauncher | custom categories, watch-next, accessibility |
| Archonw/arclauncher | customizable grid, wallpaper options |
| yoramvandevelde/pitchforklauncher | button remapping, HDMI input switching |
| madeinusmate/android-tv-lite-launcher | compact native launcher, TMDB hero |
| JackyAndroid/AndroidTVLauncher | Leanback UI and media details |
| conreo/couchy-launcher | lightweight one-screen TV UX |
| icaroharry/lite-launcher | fuzzy search, favorites, minimal dependencies |
| nielsvanvelzen/tv-launcher | Compose big-screen navigation |
| meddouribadis/arclauncher | advanced wallpapers, backup and recovery |
| rkot1/arclauncher | Arc Launcher fork |
| ctnkyaumt/flaunchermod | input shortcuts, weather and speed tests |
| eponce00/OpenCore-TV | device profiles, fallback and setup docs |
| Manyl-Mansouri/shelf | tvOS-inspired top shelf |
| vaari-dev/aangan | India-centric TV home and QR setup |
| strdex/couchy-launcher-mod | low-profile device adaptations |
| mhzworld/megalauncher | screensaver and widgets |
| etienn01/FLauncher | upstream customizable launcher |
| osrosal/FLauncher | community FLauncher fork lineage |
| sebastien-byte/LtvLauncher | customization fork, backup dialog |

## Priorities

1. Make AI TV 2.0 the actual HOME entry activity, not a hidden Search-key screen.
2. Keep installed app discovery and launch via native Android package manager.
3. Provide direct, visible DPAD access to AI Discovery and motion settings.
4. Preserve existing package ID and signed update compatibility.
5. Follow with safe-mode fallback, input sources, app ordering, wallpapers, favorites and real-device performance tests.

## Legal and reliability guardrails

Many launcher references use GPL-3.0; do not copy their source into this project without license and attribution review. The new home screen is independently implemented in Java with Android framework APIs. External app close animations remain system-controlled. Keep stock launcher enabled until a new build is tested on hardware.
