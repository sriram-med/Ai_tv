# Aurora media companion integration — Memo and Nuvio TV

## Source inspection

- User fork: [sriram-med/memo](https://github.com/sriram-med/memo), fork of Lumora. Kotlin Android media client in package `com.lumora`. Supports separately configured IPTV, M3U, Xtream, Stalker, Jellyfin, Plex and compatible Stremio addons.
- Memo upstream license: **PolyForm Noncommercial 1.0.0**; it restricts commercial use, and carries name/attribution obligations. Its source, player, UI and assets are **not merged into Aurora**.
- [NuvioMedia/NuvioTV](https://github.com/NuvioMedia/NuvioTV), GPL-3.0 Android TV app; package `com.nuvio.tv` (and `com.nuvio.app` variant). Nuvio implements `nuvio://` metadata links, verified against its `DeepLinkParser.kt` and manifest.
- Aurora remains a launcher, not an IPTV provider or alternative copy of either player.

## Implemented compatibility

1. Explicit package visibility for Nuvio and Memo on Android 11+.
2. Installed-app detection with Leanback-first launch intent.
3. A **Media Companions** screen that shows installation status and opens the respective app.
4. GitHub Releases links for independently installing these apps, when missing.
5. Home navigation and quick-control actions to open Memo, Nuvio, or companion setup.
6. AI Discovery action and empty-catalog fallback to open companions.
7. Nuvio title-detail deeplink `nuvio://tmdb/movie/<id>` or `nuvio://tmdb/series/<id>` for backed TMDB picks. Never assumes playback succeeded.
8. Open in Nuvio directly from Discovery when Nuvio is present.
9. Long OK press on selected home-screen catalog picks to open their Nuvio metadata detail (or companion setup).
10. Recent-app note when a companion launches successfully.
11. No imported credentials, no unverified addon repository, no default piracy streams, no sideload without user's own install choice.
12. Consistent Aurora appearance in the companion hub.

## Not implemented (would require supported APIs and possibly licensing work)

- A unified, embedded Lumora player or Lumora-owned IPTV streams.
- Synchronizing credentials, M3U playlists, Plex/Jellyfin authentication or Nuvio personal addons into the launcher.
- Nuvio playback position, favorites, account, Trakt or watch-state synchronization through private storage.
- Direct playback of specific Memo titles: stable exported metadata deep-link contract not verified.
- Deep link success is subject to the Nuvio version installed and availability of relevant metadata/addons. Some TVs may require the app's first-run setup.

## Testing on device

Install Memo and/or Nuvio separately, then check: MEDIA screen shows each installed app; OPEN launches; AI Discovery shows the companion option; a TMDB title detail can open in Nuvio; Back returns to Aurora; long OK on featured tiles opens Nuvio; missing companion shows only its releases link. Verify the installed build against GitHub Actions before using it as the default launcher.
