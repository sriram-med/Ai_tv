# Recommendation engine comparison — 14 November 2026

Counts retrieved from the GitHub repository API; they change over time.

| Repository | Stars | Forks | License | Fit |
|---|---:|---:|---|---|
| [Twitter algorithm](https://github.com/twitter/the-algorithm) | 74,076 | 13,251 | AGPL-3.0 | Highest counts among the reviewed repositories; social-feed system, not a drop-in TV engine. |
| [Recommenders](https://github.com/recommenders-team/recommenders) | 21,992 | 3,334 | MIT | Highest counts among the reviewed general-purpose recommendation toolkits. Python models, training/evaluation utilities and notebooks. |
| [Gorse](https://github.com/gorse-io/gorse) | 9,923 | 928 | Apache-2.0 | Best service-shaped fit: standalone recommendation server; requires catalog IDs, interaction feedback and deployment. |
| [RecBole](https://github.com/RUCAIBox/RecBole) | 4,619 | 755 | MIT | Research/model training toolkit. |
| [TensorFlow Recommenders](https://github.com/tensorflow/recommenders) | 2,040 | 301 | Apache-2.0 | TensorFlow retrieval/ranking model library. |

Recommendation: choose Recommenders when prioritizing popularity and model experimentation, or Gorse for a separately hosted production recommendation service. Neither provides licensed film metadata or streaming availability. The TV's existing verified search/catalog sources are still needed.

This change implements native search and keyboard support. It does not claim that any of these backends is already deployed or trained. GPT ranking remains the existing optional paired-gateway path. Adding a recommendation backend should preserve verified catalog IDs and use explicit viewing/like feedback; stars alone do not improve predictions without data.
