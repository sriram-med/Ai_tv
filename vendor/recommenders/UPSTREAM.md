# Recommenders provenance

Cloned from https://github.com/recommenders-team/recommenders at commit
cc47866b0599721f8544d1cc850a5a1db091acb3 on 2026-11-14.

`tfidf_utils.py` is the unmodified upstream model, retained under its MIT license.
`ContentRecommender.java` adapts its content TF-IDF / cosine-similarity algorithm
for Android without shipping a Python runtime or requiring a cloud service.
The parity test executes the upstream model with `tokenization_method="none"`
and compares query-to-catalog scores with the Java implementation.

This is content-based ranking using title and overview metadata. It is not a
trained collaborative-filtering model, does not infer unavailable viewing history,
and does not supply film metadata or streaming rights.
