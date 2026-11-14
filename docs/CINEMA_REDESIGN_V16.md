# Aurora Cinema — TV-native redesign v1.6

## Research and architecture
- Most-starred media reference in comparison: [NuvioMedia/NuvioTV](https://github.com/NuvioMedia/NuvioTV) (~3.5k stars, ~634 forks, November 2026).
- Most-starred traditional Android TV launcher in comparison: [JackyAndroid/AndroidTVLauncher](https://github.com/JackyAndroid/AndroidTVLauncher) (~3.1k stars, ~739 forks).
- [LtvLauncher](https://github.com/leanbitlab-org/LtvLauncher) adds DPAD navigation, focus states, wallpaper settings and rich TV channel handling.
- [Agregarr IMDb Ratings API](https://github.com/agregarr/imdb-ratings-api) exposes IMDb ratings from the official **noncommercial** ratings dataset (only when a valid IMDb id is known).

**Licensing:** Nuvio and JackyAndroid launchers are GPL-3.0. We inspect design patterns and implement original native Java components; we do not embed their licensed source in an incompatible module. Their complete codebases are not cloned into the APK. A full Nuvio/Compose port would be a separate Android project and license-compliance effort.

## Architecture

- `CinemaHomeActivity` is the actual HOME entry (also used by the accessibility Home redirect).
- Persistent left-side TV navigation; no webpage-like wide button bar.
- Hero spotlight is filled from cached verified Trending picks; no fabricated descriptions or ratings.
- Horizontal cinematic poster rails for:
  - AI-ranked movies (only with paired gateway and valid cached catalog)
  - AI-ranked series (separate from movies)
  - Recommended catalog movies
  - Recommended catalog series
  - Installed apps
  - Continue Watching
- `AuroraDiscoveryActivity` uses a five-column poster grid grouped into films and series instead of stacked text cards.
- `AuroraPosterCard` centralizes poster art, truncation, focus behavior and verified IMDb score badges.
- `AuroraImdbRatings` maps TMDB IDs to IMDb IDs via TV-local TMDB key and fetches a 7-day cached score from the Agregarr API. No TMDB key, unavailable score, HTTP failure or noncommercial license incompatibility → display IMDb unavailable, never a fabricated number.
- `ModernHomeActivity` remains available under APPS for advanced grid settings, favorites, hidden apps, sorting, wallpaper, detailed system shortcuts, and media companion settings.
- `MediaHubActivity` preserves safe Memo/Lumora and Nuvio companion launch integrations.

## Must test on real Android TV

- Initial HOME launch, accessibility Home key, app resumption, DPAD movement across nested horizontal rails.
- Scroll/focus performance on low-RAM TV hardware, app banner decoding and IMDb network backoff.
- Movie and series metadata accuracy, installed provider matching, meaningful empty states.
- Optional GPT backend pairing, network failure fallback, and correct separation of movies/series.
- Version signing and self-update; ability to switch back to OEM launcher.
- IMDb ratings only when properly mapped, permitted by the dataset's noncommercial terms; user must confirm use case.
