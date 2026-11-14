#!/usr/bin/env python3
"""Apply narrow, auditable AI TV changes to a pinned Apache-2.0 tvOS-Launcher clone.

This preserves the upstream Java UI, layouts, motion system, app switching, weather,
JustWatch poster search, Nuvio integration, and attribution. Fail on drift.
"""
from pathlib import Path

root = Path("upstream")
assert (root / "LICENSE").exists(), "Missing upstream LICENSE"
assert (root / "NOTICE").exists(), "Missing upstream NOTICE"

# Keep the installed AI TV application ID and generated resource package.
for file in [root / "AndroidManifest.xml", *root.glob("src/**/*.java")]:
    data = file.read_text(encoding="utf-8")
    if "com.techgenius.tvoslauncher" in data:
        file.write_text(data.replace("com.techgenius.tvoslauncher", "com.techpp.glass"), encoding="utf-8")
source = root / "src/com/techgenius/tvoslauncher"
dest = root / "src/com/techpp/glass"
dest.parent.mkdir(parents=True, exist_ok=True)
source.rename(dest)

manifest = root / "AndroidManifest.xml"
content = manifest.read_text()
assert 'android:label="tvOS Launcher"' in content
content = content.replace('android:label="tvOS Launcher"', 'android:label="AI TV · Glass"')
manifest.write_text(content)

# Upstream's real updater must never suggest its differently signed APK.
updater = dest / "AppUpdater.java"
content = updater.read_text()
assert "https://api.github.com/repos/techgenius1/tvOS-Launcher/releases/latest" in content
content = content.replace("https://api.github.com/repos/techgenius1/tvOS-Launcher/releases/latest",
                          "https://api.github.com/repos/sriram-med/Ai_tv/releases/latest")
content = content.replace("https://github.com/techgenius1/tvOS-Launcher/releases/latest/download/tvos-home.apk",
                          "https://github.com/sriram-med/Ai_tv/releases/latest/download/ai-tv.apk")
updater.write_text(content)

main = dest / "MainActivity.java"
data = main.read_text()
anchor = "        searchButton.setOnClickListener(v -> showSearch(null));"
assert data.count(anchor) == 1, "Upstream changed Search button; manual review required"
# Additional visible control in the original upstream dock/header, not an invented home.
injection = r'''
        // AI TV integration: visible AI button in upstream tvOS search header.
        TextView aiTvButton = text(14, Ui.font(this, "semibold"), 1f);
        aiTvButton.setText("\u2726 AI");
        aiTvButton.setGravity(Gravity.CENTER);
        aiTvButton.setTextColor(Color.WHITE);
        aiTvButton.setFocusable(true);
        aiTvButton.setClickable(true);
        GradientDrawable aiTvGlass = new GradientDrawable();
        aiTvGlass.setColor(0xAA203A3D);
        aiTvGlass.setCornerRadius(dp(19));
        aiTvButton.setBackground(aiTvGlass);
        LinearLayout.LayoutParams aiTvButtonParams = new LinearLayout.LayoutParams(dp(66), btn);
        aiTvButtonParams.leftMargin = dp(10);
        clockBox.addView(aiTvButton, aiTvButtonParams);
        aiTvButton.setOnFocusChangeListener((v, focused) -> {
            v.animate().cancel();
            v.animate().scaleX(focused ? 1.10f : 1f).scaleY(focused ? 1.10f : 1f)
                .setDuration(175).setInterpolator(LIFT).start();
            aiTvButton.setTextColor(focused ? 0xFFB8FFF0 : Color.WHITE);
        });
        aiTvButton.setOnClickListener(v -> {
            showSearch(null);
            if (search != null) search.openAiChooser();
        });
        searchButton.setOnLongClickListener(v -> {
            showSearch(null);
            if (search != null) search.openAiChooser();
            return true;
        });
'''
data = data.replace(anchor, anchor + "\n" + injection)
main.write_text(data)

search = dest / "SearchPanel.java"
data = search.read_text()
assert data.startswith("package com.techpp.glass;")
data = data.replace("import android.content.Intent;",
                    "import android.content.Intent;\nimport android.app.AlertDialog;\nimport org.json.JSONArray;\nimport org.json.JSONObject;")
anchor = "    void fillTitles(List<Search.Title> ts) {"
assert data.count(anchor) == 1
helper = r'''
    private static final String AI_GATEWAY = "https://ai-tv-2-gpt-gateway.onrender.com";

    /** AI recommendations use the SAME original tvOS poster components, logos and animations. */
    void openAiChooser() {
        String[] topics = {"AI MOVIES  /  Tonight", "AI SERIES  /  Binge-worthy",
            "THRILLERS", "SCIENCE FICTION", "COMEDY", "FAMILY", "DRAMA", "ACTION",
            "CONNECT GPT / SECURE PAIRING"};
        String[] searches = {"popular movies", "popular series", "thriller", "science fiction",
            "comedy", "family", "drama", "action", "movies"};
        new AlertDialog.Builder(a).setTitle("AI DISCOVERY  ·  Movies & Series")
            .setItems(topics, (dialog, which) -> {
                String q = query.length() >= 2 ? q() : searches[which];
                String preference = which == 0 ? "movie" : which == 1 ? "tv" : "";
                if (which == topics.length - 1) pairForAi(q, preference);
                else if (new AiTvPairingClient(a).hasSession()) aiPick(q, preference);
                else showCuratedPicks(q, preference);
            }).setNegativeButton("Cancel", null).show();
    }

    /**
     * Always usable without GPT credentials. Reuses ORIGINAL upstream JustWatch /
     * Stremio recommendation sources and poster card builder.
     */
    void showCuratedPicks(String q, String kind) {
        list.removeAllViews();
        addNote("Loading locally available movie and series recommendations...");
        net.execute(() -> {
            List<Suggestions.Item> source = Suggestions.cached(a, true);
            if (source == null || source.isEmpty()) {
                try { source = Suggestions.fetch(a); } catch (Exception ignored) {}
            }
            List<Search.Title> movies = new ArrayList<>();
            List<Search.Title> series = new ArrayList<>();
            if (source != null) {
                Set<String> seen = new HashSet<>();
                for (Suggestions.Item item : source) {
                    if (item.title == null || item.title.isEmpty() ||
                        (!"movie".equals(item.type) && !"tv".equals(item.type)) ||
                        !seen.add(item.type + ":" + item.title)) continue;
                    Search.Title title = new Search.Title();
                    title.title = item.title;
                    title.type = item.type;
                    title.overview = item.overview == null ? "" : item.overview;
                    title.year = item.year == null ? "" : item.year;
                    title.poster = item.backdrop == null ? "" : item.backdrop;
                    title.rating = item.rating;
                    Suggestions.Provider provider = item.provider(a);
                    if (provider != null) {
                        Search.Offer offer = new Search.Offer();
                        offer.service = provider.name;
                        offer.pkg = provider.pkg;
                        offer.monetization = "FLATRATE";
                        title.offers.add(offer);
                    }
                    if ("tv".equals(title.type)) series.add(title);
                    else movies.add(title);
                }
            }
            a.handler.post(() -> {
                if (closed) return;
                list.removeAllViews();
                List<View> movieTiles = new ArrayList<>(), seriesTiles = new ArrayList<>();
                if (!"tv".equals(kind))
                    for (Search.Title movie : movies) { movieTiles.add(titleCard(movie)); if(movieTiles.size()>=18)break; }
                if (!"movie".equals(kind))
                    for (Search.Title show : series) { seriesTiles.add(titleCard(show)); if(seriesTiles.size()>=18)break; }
                addNote("Curated from streaming providers; connect GPT separately for AI-ranked picks.");
                if (!movieTiles.isEmpty()) addRow("RECOMMENDED MOVIES", movieTiles, a.dp(176));
                if (!seriesTiles.isEmpty()) addRow("RECOMMENDED SERIES", seriesTiles, a.dp(176));
                if (movieTiles.isEmpty() && seriesTiles.isEmpty()) {
                    addNote("The recommendations catalog is empty. Search movie or series titles to browse.");
                    setQuery(q);
                }
            });
        });
    }

    private void pairForAi(String q, String kind) {
        android.widget.EditText input = new android.widget.EditText(a);
        input.setSingleLine(true);
        input.setHint("One-time device pairing code");
        new AlertDialog.Builder(a).setTitle("Secure AI pairing")
            .setMessage("AI recommendations require a one-time code issued by the gateway administrator. Do not enter an OpenAI API key. Local poster search works without pairing.")
            .setView(input)
            .setPositiveButton("PAIR", (dialog, which) -> {
                String code = input.getText().toString().trim();
                new AiTvPairingClient(a).exchange(AI_GATEWAY, code, new AiTvPairingClient.Callback() {
                    public void onPaired() {a.runOnUiThread(() -> aiPick(q, kind));}
                    public void onError(String reason) {
                        a.runOnUiThread(() -> {
                            new AlertDialog.Builder(a).setTitle("AI pairing unavailable")
                                .setMessage(reason + "\nYou can still browse movies and series with the normal search.")
                                .setPositiveButton("Search", (d,w) -> setQuery(q))
                                .setNegativeButton("Close",null).show();
                        });
                    }
                });
            }).setNeutralButton("LOCAL SEARCH", (dialog,which) -> setQuery(q))
            .setNegativeButton("Cancel",null).show();
    }

    void aiPick(String prompt, String type) {
        if (prompt == null || prompt.trim().length() < 2) prompt = "movies";
        final String queryText = prompt.trim();
        addNote("Finding verified streaming titles for " + queryText + "...");
        net.execute(() -> {
            List<Search.Title> all;
            try {all = Search.titles(a, queryText, apps);}
            catch (Exception e) {all = new ArrayList<>();}
            List<Search.Title> retrieved = all;
            a.handler.post(() -> {
                if (closed) return;
                if (retrieved.isEmpty()) {
                    addNote("No catalog available for " + queryText + ". Search locally or try another genre.");
                    setQuery(queryText);
                    return;
                }
            });
            if (retrieved.isEmpty()) return;
            LinkedHashMap<String, Search.Title> ids = new LinkedHashMap<>();
            JSONArray catalog = new JSONArray();
            int index = 0;
            for (Search.Title title : retrieved) {
                if (!type.isEmpty() && !type.equals(title.type)) continue;
                String key = title.imdb.isEmpty() ? "local:" + index : title.imdb;
                index++;
                if (ids.containsKey(key)) continue;
                ids.put(key, title);
                try {
                    catalog.put(new JSONObject().put("id",key).put("title",title.title)
                        .put("overview",title.overview).put("providers",new JSONArray()));
                } catch (Exception ignored) {}
                if (catalog.length() >= 40) break;
            }
            if (catalog.length() == 0) {
                a.handler.post(() -> {
                    if (!closed) {
                        list.removeAllViews();
                        addNote("No matching " + ("tv".equals(type) ? "series" : "movies") + " found. Try a different term.");
                    }
                }); return;
            }
            new AiRecommendationClient().recommend(AI_GATEWAY,
                new AiTvPairingClient(a).currentToken(),
                "Recommend the best " + ("movie".equals(type) ? "movies" : "tv".equals(type) ? "series" : "movies and shows") +
                    " matching: " + queryText, catalog, new AiRecommendationClient.Callback() {
                @Override public void onSuccess(JSONArray response) {
                    a.handler.post(() -> {
                        if (closed) return;
                        List<View> movies = new ArrayList<>(), shows = new ArrayList<>();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject rec = response.optJSONObject(i);
                            Search.Title t = rec == null ? null : ids.get(rec.optString("id"));
                            if (t == null) continue;
                            if ("tv".equals(t.type)) shows.add(titleCard(t));
                            else movies.add(titleCard(t));
                        }
                        list.removeAllViews();
                        if (!movies.isEmpty()) addRow("\u2726 AI MOVIES", movies, a.dp(176));
                        if (!shows.isEmpty()) addRow("\u2726 AI SERIES", shows, a.dp(176));
                        if (movies.isEmpty() && shows.isEmpty())
                            addNote("AI returned no matching titles. Local search remains available.");
                    });
                }
                @Override public void onError(String reason) {
                    a.handler.post(() -> {
                        if (closed) return;
                        list.removeAllViews();
                        List<View> movies = new ArrayList<>(), shows = new ArrayList<>();
                        for (Search.Title t : ids.values()) {
                            if ("tv".equals(t.type)) shows.add(titleCard(t));
                            else movies.add(titleCard(t));
                        }
                        addNote("AI gateway unavailable — showing verified local catalog results.");
                        if (!movies.isEmpty()) addRow("MOVIES", movies, a.dp(176));
                        if (!shows.isEmpty()) addRow("SERIES", shows, a.dp(176));
                    });
                }
            });
        });
    }

'''
data = data.replace(anchor, helper + anchor)
# Upstream already includes provider artwork. Place the actual JustWatch IMDb rating
# in the poster overlay; unknown values remain absent rather than invented.
anchor = "        art.addView(logos, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,\n                Gravity.BOTTOM | Gravity.START));"
assert data.count(anchor) == 1, "Poster layout changed"
badge = r'''
        if (t.rating > 0 && t.rating <= 10) {
            TextView imdbBadge = a.text(10, Ui.font(a, "semibold"), 1f);
            imdbBadge.setText(String.format(java.util.Locale.US,"IMDb %.1f",t.rating));
            imdbBadge.setTextColor(0xFFFFD77C);
            imdbBadge.setPadding(a.dp(4), a.dp(3), a.dp(4), a.dp(3));
            imdbBadge.setBackgroundColor(0xBB081015);
            FrameLayout.LayoutParams scoreP = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.RIGHT);
            scoreP.topMargin = a.dp(4);
            scoreP.rightMargin = a.dp(4);
            art.addView(imdbBadge, scoreP);
        }
'''
data = data.replace(anchor, anchor + "\n" + badge)
search.write_text(data)
# Disable potentially destructive stock-launcher debloating in this preview.
# The user can select Aurora as Home through Android settings, without ADB changes.
settings = dest / "SettingsActivity.java"
settings_text = settings.read_text()
cyt_start = '        section(tr("CLEANYOURTV (BETA)", "CLEANYOURTV (BETA)"));'
cyt_end = '        // Expensive PackageManager scans (launchers, debloat status) run off the UI thread'
assert settings_text.count(cyt_start) == 1 and settings_text.count(cyt_end) == 1
settings_text = settings_text.replace(cyt_start,
    '        if (false) { // AI TV preview: disable destructive stock-launcher debloat controls\n' + cyt_start)
settings_text = settings_text.replace(cyt_end, '        }\n\n' + cyt_end)
settings.write_text(settings_text)

# Apache-2.0 requires a clear change notice on modified source files.
for edited in [main, search, updater, settings]:
    source_code = edited.read_text(encoding="utf-8")
    edited.write_text("// Modified for AI TV migration preview (2026): UI bridge, safety and update routing.\n" +
                      source_code, encoding="utf-8")

print("Upstream patch applied to pinned Java tvOS Launcher; original UI and licences preserved.")
