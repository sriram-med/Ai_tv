# Native Recommenders integration

AI TV uses the TF-IDF content model from `recommenders-team/recommenders`,
cloned at `cc47866b0599721f8544d1cc850a5a1db091acb3`.
The original Python module and MIT notice are retained in `vendor/recommenders`.

The Android adaptation runs in `ContentRecommender.java`, ranking verified title
and overview metadata with the same word tokenization, English stopwords,
unigrams/bigrams/trigrams, smooth IDF, L2 normalization and cosine similarity.
The query is treated as another document, matching the upstream model's
item-to-item approach. Numeric parity is checked against that original model.
Only verified catalog objects are returned; source IDs, providers and posters
are retained. Stable zero-score ties preserve source order.

The AI tab uses this ranking for recommendation rails and the Recommend action.
There is no GPT-key entry, pairing code prompt or account requirement in this
flow. Search stays available alongside the QWERTY keyboard. Network access is
still necessary to fetch fresh title/provider metadata; ranking itself is local.
The existing optional server gateway is not used by this release's AI tab.

This initial model is content-based. Collaborative personalization would require
explicit user feedback and a separately trained model; it is not claimed here.

Validation:

```sh
python3 -m pip install numpy pandas scikit-learn
python3 integration/tvos/tests/check_recommenders.py
```

## Catalog repair in 1.8.5

AI discovery now loads its own movie and series catalogs rather than inheriting
Home shelf enablement, selected providers or TMDB-key settings. It uses Cinemeta's
paginated catalog path and a second metadata host, bounded HTTP requests and
independent per-kind disk caches. Mood buttons filter actual genre metadata, which
is also included in ranking. Empty-source failures show Retry rather than claiming
that recommendations are ready. Metadata availability is separate from streaming
subscription availability.

Catalog regressions exercise host failure, missing Home-provider configuration,
genre filtering, fresh/stale offline cache and empty-source errors. Emulator
checks require positive counts plus actual poster title labels for default and
thriller results. The keyboard is toggled separately to keep poster rails visible.
