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
# Preserve the AI TV release tag scheme (ai-tv-v1.8.0) in update comparisons.
# The unmodified upstream parser strips only leading 'v', producing a confusing label.
anchor_version = 'String s = v.trim();'
assert content.count(anchor_version) == 1
content = content.replace(anchor_version, 'String s = v.trim();\n        if (s.startsWith("ai-tv-")) s = s.substring("ai-tv-".length());')
assert 'techgenius1/tvOS-Launcher/releases/latest' not in content
assert 'ai-tv.apk' in content
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
    private boolean aiMode, syncingAiInput;
    private android.widget.EditText aiSearchInput;
    private View aiFirstKey;
    private LinearLayout aiSearchChrome;

    /** Persistent search tools live outside result rails so recommendations cannot hide them. */
    private void setupAiSearch() {
        aiMode = true;
        if (aiSearchChrome != null) return;
        keyScroll.setVisibility(View.GONE);
        queryView.setText("AI DISCOVERY  /  YOUR NEXT GREAT WATCH");
        queryView.setTextColor(0xFFB8FFF0);
        queryView.setAlpha(1f);
        LinearLayout col = (LinearLayout) keyScroll.getParent();
        aiSearchChrome = new LinearLayout(a);
        aiSearchChrome.setOrientation(LinearLayout.VERTICAL);
        aiSearchChrome.setPadding(a.dockOuter, a.dp(8), a.dockOuter, a.dp(8));
        GradientDrawable surface = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xF0233948, 0xE7101828});
        surface.setCornerRadius(a.dp(18));
        surface.setStroke(a.dp(1), 0x805A9EAC);
        aiSearchChrome.setBackground(surface);
        col.addView(aiSearchChrome, 1, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout bar = new LinearLayout(a);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        aiSearchInput = new android.widget.EditText(a);
        aiSearchInput.setId(View.generateViewId());
        aiSearchInput.setSingleLine(true);
        aiSearchInput.setTextColor(Color.WHITE);
        aiSearchInput.setHintTextColor(0xFFAAC1D1);
        aiSearchInput.setHint("Search a title, actor, genre or mood...");
        aiSearchInput.setTextSize(16);
        aiSearchInput.setPadding(a.dp(12), a.dp(8), a.dp(12), a.dp(8));
        aiSearchInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        aiSearchInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        GradientDrawable field = new GradientDrawable();
        field.setColor(0xFF0D1927); field.setCornerRadius(a.dp(10));
        field.setStroke(a.dp(1), 0xFF5D8EAA); aiSearchInput.setBackground(field);
        bar.addView(aiSearchInput, new LinearLayout.LayoutParams(0, a.dp(44), 1f));
        bar.addView(chip("SEARCH", () -> submitAiSearch(false)));
        bar.addView(chip("GPT RANK", () -> submitAiSearch(true)));
        aiSearchChrome.addView(bar);
        aiSearchInput.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                if (!syncingAiInput) setQuery(s.toString());
            }
            public void afterTextChanged(android.text.Editable text) {}
        });
        aiSearchInput.setOnEditorActionListener((v, action, event) -> {
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                 event.getAction() == KeyEvent.ACTION_UP)) {
                submitAiSearch(false); return true;
            }
            return false;
        });
        LinearLayout keyboard = new LinearLayout(a);
        keyboard.setOrientation(LinearLayout.VERTICAL);
        keyboard.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String letters : new String[]{"qwertyuiop", "asdfghjkl", "zxcvbnm"}) {
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER);
            for (int i=0; i<letters.length(); i++) {
                final String letter = letters.substring(i,i+1);
                View k = key(letter, a.dp(40), () -> type(letter));
                if (aiFirstKey == null) aiFirstKey = k;
                row.addView(k);
            }
            keyboard.addView(row);
        }
        LinearLayout controls = new LinearLayout(a);
        controls.setGravity(Gravity.CENTER);
        controls.addView(key("SPACE", a.dp(96), () -> type(" ")));
        controls.addView(key("DELETE", a.dp(85), this::backspace));
        controls.addView(key("123 / !?", a.dp(92), () -> {
            aiSearchInput.requestFocus();
            android.view.inputmethod.InputMethodManager ime =
                (android.view.inputmethod.InputMethodManager) a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (ime != null) ime.showSoftInput(aiSearchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }));
        controls.addView(key("CLEAR", a.dp(75), this::clear));
        controls.addView(key("BROWSE", a.dp(85), this::openAiChooser));
        keyboard.addView(controls);
        aiSearchChrome.addView(keyboard);
    }

    private void submitAiSearch(boolean rank) {
        String prompt = aiSearchInput.getText().toString().trim();
        if (prompt.isEmpty()) { openAiChooser(); return; }
        android.view.inputmethod.InputMethodManager ime =
            (android.view.inputmethod.InputMethodManager) a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (ime != null) ime.hideSoftInputFromWindow(aiSearchInput.getWindowToken(), 0);
        if (rank) {
            if (new AiTvPairingClient(a).hasSession()) aiPick(prompt, "");
            else pairForAi(prompt, "");
        } else setQuery(prompt);
    }

    private static final String AI_GATEWAY = "https://ai-tv-2-gpt-gateway.onrender.com";

    /** AI recommendations use the SAME original tvOS poster components, logos and animations. */
    void openAiChooser() {
        setupAiSearch();
        a.handler.removeCallbacks(fetch);
        gen++;
        Search.cancel();
        String[] topics = {"MOVIES  /  Tonight", "SERIES  /  Binge-worthy",
            "THRILLERS", "SCIENCE FICTION", "COMEDY", "FAMILY", "DRAMA", "ACTION",
            "CONNECT GPT"};
        String[] searches = {"popular movies", "popular series", "thriller", "science fiction",
            "comedy", "family", "drama", "action", "movies"};
        list.removeAllViews();
        List<View> chips = new ArrayList<>();
        for(int i=0;i<topics.length;i++){
            final int which=i;
            chips.add(chip(topics[i], () -> {
                String q = query.length() >= 2 ? q() : searches[which];
                String preference = which == 0 ? "movie" : which == 1 ? "tv" : "";
                if (which == topics.length - 1) pairForAi(q, preference);
                else if (new AiTvPairingClient(a).hasSession()) aiPick(q, preference);
                else showCuratedPicks(q, preference);
            }));
        }
        addRow("EXPLORE  /  MOVIES, SERIES & MOODS", chips, a.dp(52));
        addNote("Select movies or series for native poster recommendations. Connect GPT for personalized ranking.");
        aiSearchInput.requestFocus();
    }

    /**
     * Always usable without GPT credentials. Reuses ORIGINAL upstream JustWatch /
     * Stremio recommendation sources and poster card builder.
     */
    void showCuratedPicks(String q, String kind) {
        final int requestGeneration = ++gen;
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
                if (closed || gen != requestGeneration) return;
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
        final int requestGeneration = ++gen;
        if (prompt == null || prompt.trim().length() < 2) prompt = "movies";
        final String queryText = prompt.trim();
        addNote("Finding verified streaming titles for " + queryText + "...");
        net.execute(() -> {
            List<Search.Title> all;
            try {all = Search.titles(a, queryText, apps);}
            catch (Exception e) {all = new ArrayList<>();}
            List<Search.Title> retrieved = all;
            a.handler.post(() -> {
                if (closed || gen != requestGeneration) return;
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
                    if (!closed && gen == requestGeneration) {
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
                        if (closed || gen != requestGeneration) return;
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
                        if (closed || gen != requestGeneration) return;
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

# Empty AI searches return to the mood browser, with search tools still visible.
data = data.replace('    void render() {',
    '    void render() {\n        if (aiMode && q().isEmpty()) { openAiChooser(); return; }', 1)
# Keep custom keyboard and hardware/IME input synchronized. Hardware text goes to
# EditText itself when it owns focus; elsewhere retain upstream remote typing.
data = data.replace('    View firstKey() {\n',
    '    View firstKey() {\n        if (aiMode && aiFirstKey != null) return aiFirstKey;\n', 1)
data = data.replace('        showQuery();\n        a.handler.removeCallbacks(fetch);',
    '        showQuery();\n        if (aiMode && aiSearchInput != null) {\n            if (!aiSearchInput.getText().toString().equals(query.toString())) {\n                syncingAiInput = true;\n                aiSearchInput.setText(query.toString());\n                aiSearchInput.setSelection(aiSearchInput.length());\n                syncingAiInput = false;\n            }\n            queryView.setText("AI DISCOVERY  /  YOUR NEXT GREAT WATCH");\n            queryView.setAlpha(1f);\n        }\n        a.handler.removeCallbacks(fetch);', 1)
data = data.replace('        View f = a.getCurrentFocus();\n        if (mic != null',
    '        View f = a.getCurrentFocus();\n        if (aiMode && f == aiSearchInput) return false;\n        if (mic != null', 1)
# Bypass upstream character/delete interception while the editable field is focused.
data = data.replace('        if (k == KeyEvent.KEYCODE_DEL) {',
    '        if (aiMode && a.getCurrentFocus() == aiSearchInput && k != KeyEvent.KEYCODE_SEARCH && k != KeyEvent.KEYCODE_VOICE_ASSIST) return false;\n        if (k == KeyEvent.KEYCODE_DEL) {', 1)

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
# Make AI visible in the actual default Night Scenes home, not just Classic mode.
scene = dest / "SceneHome.java"
scene_code = scene.read_text()
scene_anchor = '        // Top-right Control Panel Button'
assert scene_code.count(scene_anchor) == 1
scene_integration = r'''
        // AI TV integration in UPSTREAM Night Scenes control strip.
        LinearLayout aiSceneButton = new LinearLayout(host);
        aiSceneButton.setGravity(Gravity.CENTER);
        aiSceneButton.setOrientation(LinearLayout.HORIZONTAL);
        MainActivity.LiquidGlass aiSceneGlass = new MainActivity.LiquidGlass(
                mSize / 2f, 0x6614161C, host.glassSource, host.screenW, host.screenH, host.unit);
        aiSceneGlass.lens = dp(5);
        aiSceneGlass.rimStrength = 0.45f;
        aiSceneButton.setBackground(aiSceneGlass);
        aiSceneButton.setFocusable(true);
        aiSceneButton.setClickable(true);
        TextView aiSceneText = new TextView(host);
        aiSceneText.setText("\u2726 AI");
        Ui.textSize(aiSceneText, 12f);
        aiSceneText.setTypeface(Ui.font(host, "semibold"));
        aiSceneText.setTextColor(Color.WHITE);
        aiSceneButton.addView(aiSceneText, new LinearLayout.LayoutParams(dp(48), dp(20)));
        aiSceneText.setGravity(Gravity.CENTER);
        GradientDrawable aiSceneOutline = new GradientDrawable();
        aiSceneOutline.setCornerRadius(mSize / 2f);
        aiSceneOutline.setColor(Color.TRANSPARENT);
        aiSceneOutline.setStroke(dp(2), Color.WHITE);
        aiSceneButton.setOnFocusChangeListener((v, focused) -> {
            v.setForeground(focused ? aiSceneOutline : null);
            v.animate().cancel();
            v.animate().scaleX(focused ? 1.08f : 1f).scaleY(focused ? 1.08f : 1f)
                    .setDuration(180).setInterpolator(MainActivity.LIFT).start();
            if (focused) { hideShelf(); showDashboard(); }
        });
        aiSceneButton.setOnClickListener(v -> {
            host.showSearch(null);
            if (host.search != null) host.search.openAiChooser();
        });
        LinearLayout.LayoutParams aiSceneParams = new LinearLayout.LayoutParams(dp(58), mSize);
        aiSceneParams.rightMargin = dp(10);
        topRightRow.addView(aiSceneButton, aiSceneParams);

'''
scene_code = scene_code.replace(scene_anchor, scene_integration + scene_anchor)
scene.write_text(scene_code)

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

# The upstream top-bar key handler explicitly enumerates its controls. Include
# our AI control in that state machine; adding a focusable view alone is insufficient.
scene_code = scene.read_text()
scene_code = scene_code.replace('LinearLayout aiSceneButton = new LinearLayout(host);',
                                'aiSceneButton = new LinearLayout(host);')
scene_code = scene_code.replace('    boolean dispatchKey(KeyEvent ev) {',
    '    private LinearLayout aiSceneButton;\n\n    boolean dispatchKey(KeyEvent ev) {')
scene_code = scene_code.replace('f == whoBar || f == weatherBtn || f == moonBtn',
                                'f == whoBar || f == weatherBtn || f == aiSceneButton || f == moonBtn')
scene_code = scene_code.replace('else moonBtn.requestFocus();', 'else aiSceneButton.requestFocus();')
scene_code = scene_code.replace('} else if (f == weatherBtn) {\n                        moonBtn.requestFocus();',
    '} else if (f == weatherBtn) {\n                        aiSceneButton.requestFocus();\n                    } else if (f == aiSceneButton) {\n                        moonBtn.requestFocus();')
scene_code = scene_code.replace('if (f == moonBtn) {\n                        if (wxVisible)',
    'if (f == moonBtn) {\n                        aiSceneButton.requestFocus();\n                    } else if (f == aiSceneButton) {\n                        if (wxVisible)')
scene.write_text(scene_code)

# Retain the downloaded package across the Android permission round-trip.
content = updater.read_text()
content = content.replace('    private static final String PREF_NAME',
    '    private static final String PENDING_INSTALL = "pending_install_permission";\n    private static final String PREF_NAME', 1)
content = content.replace('if (name.endsWith(".apk") && !dl.isEmpty())',
    'if ("ai-tv.apk".equals(name) && dl.startsWith("https://github.com/sriram-med/Ai_tv/releases/download/"))')
content = content.replace('String apkUrl = GITHUB_FALLBACK_APK;', 'String apkUrl = "";')
content = content.replace('                prefs(appCtx).edit()\n',
    '                if (apkUrl.isEmpty()) throw new Exception("Release has no AI TV APK");\n                prefs(appCtx).edit()\n', 1)
content = content.replace('                try {\n                    Intent permIntent',
    '                prefs(activity).edit().putBoolean(PENDING_INSTALL, true).apply();\n                try {\n                    Intent permIntent', 1)
content = content.replace('                } catch (Throwable ignored) {\n                }\n                return;\n            }\n\n            PackageInstaller pi',
    '                } catch (Throwable unavailable) {\n                    prefs(activity).edit().remove(PENDING_INSTALL).apply();\n                    Toast.makeText(activity, "Open Android Settings and allow AI TV to install apps, then retry Updates.", Toast.LENGTH_LONG).show();\n                }\n                return;\n            }\n            prefs(activity).edit().remove(PENDING_INSTALL).apply();\n\n            PackageInstaller pi', 1)
method = r'''    /** Resume only an install explicitly requested before opening Android permission settings. */
    public static void resumePendingInstall(Activity activity) {
        if (!prefs(activity).getBoolean(PENDING_INSTALL, false)) return;
        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) return;
        prefs(activity).edit().remove(PENDING_INSTALL).apply();
        File apk = new File(activity.getCacheDir(), "tvos-update.apk");
        if (apk.exists()) installWithPackageInstaller(activity, apk);
    }

'''
content = content.replace('    private static void copyFile(', method + '    private static void copyFile(', 1)
content = content.replace('"Could not check for updates.",', '"Could not check for updates: " + errorMsg,')
content = content.replace('            } else if (status == PackageInstaller.STATUS_SUCCESS) {',
    '            } else if (status != PackageInstaller.STATUS_SUCCESS) {\n                Toast.makeText(context, "Update not installed: " + intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE), Toast.LENGTH_LONG).show();\n            } else if (status == PackageInstaller.STATUS_SUCCESS) {')
updater.write_text(content)
for activity in [main, settings]:
    code = activity.read_text()
    assert code.count('        super.onResume();') == 1
    code = code.replace('        super.onResume();',
        '        super.onResume();\n        AppUpdater.resumePendingInstall(this);', 1)
    activity.write_text(code)

# Apache-2.0 requires a clear change notice on modified source files.
for edited in [main, search, updater, settings, scene]:
    source_code = edited.read_text(encoding="utf-8")
    edited.write_text("// Modified for AI TV migration preview (2026): UI bridge, safety and update routing.\n" +
                      source_code, encoding="utf-8")

print("Upstream patch applied to pinned Java tvOS Launcher; original UI and licences preserved.")
