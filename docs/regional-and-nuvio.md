# Regional catalogue and Nuvio integration

The Region selector offers Telugu, Hindi, Tamil, Malayalam, Kannada, Bengali,
Marathi, Punjabi and English, plus All languages. The selected language is saved
locally and applies to movie/series browsing, moods and recommendation searches.

Regional records come from Wikidata P364 original-language claims (CC0), with
IMDb identities and source entity references. Multilingual titles can occur in
more than one catalogue. The packaged fallback dataset is reproducible with
`integration/tvos/generate_regional_catalog.py`; it contains real metadata rather
than title-keyword guesses. Per-language caches are refreshed in the background
from the same query and remain usable offline. Catalogue coverage depends on
source metadata and is not a complete inventory of every regional release.
IMDb poster/background artwork continues to use the existing metadata artwork
provider. Metadata does not establish subscription or streaming availability.

Nuvio has its own launcher action and viewing option. If both Nuvio and Stremio
are installed, each remains independently selectable. Nuvio title links use its
official `nuvio://detail/movie/{imdb}` and `nuvio://detail/series/{imdb}` format,
verified against NuvioMedia/NuvioTV's DeepLinkParser. Missing-app handling links
to the official Nuvio TV releases. Existing Nuvio Cloud and continue-watching
support is retained. Playback depends on the installed Nuvio app and its setup.

Cinema/theatrical monetization and ticket-booking services/URLs are excluded from
watch offers. This removes booking actions; it does not uninstall user apps.
