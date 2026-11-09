package com.techpp.glass;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Offline-first preference and recommendation core. No permissions or API key required.
 * These operations are usable independently of the upcoming 10-foot UI.
 */
public final class AiTvPersonalization {
    private static final String STORE = "ai_tv_2_personalization";
    private final SharedPreferences prefs;

    public AiTvPersonalization(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(STORE, Context.MODE_PRIVATE);
    }

    public void setConsent(boolean enabled) { prefs.edit().putBoolean("consent", enabled).apply(); }
    public boolean hasConsent() { return prefs.getBoolean("consent", false); }
    public void setProfile(String name) { prefs.edit().putString("profile", safe(name, 40)).apply(); }
    public String profile() { return prefs.getString("profile", "Default"); }
    private String key(String name) { return profile() + ":" + name; }
    private static String safe(String value, int max) {
        return value == null ? "" : value.trim().substring(0, Math.min(max, value.trim().length()));
    }

    public void setVisualMode(String mode) {
        if (!mode.equals("Cinema") && !mode.equals("Vivid") && !mode.equals("Comfort"))
            throw new IllegalArgumentException("Unknown visual mode");
        prefs.edit().putString(key("visual"), mode).apply();
    }
    public String visualMode() { return prefs.getString(key("visual"), "Cinema"); }
    public void setReducedMotion(boolean enabled) { prefs.edit().putBoolean(key("reduced_motion"), enabled).apply(); }
    public boolean reducedMotion() { return prefs.getBoolean(key("reduced_motion"), false); }
    public void setHighContrast(boolean enabled) { prefs.edit().putBoolean(key("high_contrast"), enabled).apply(); }
    public boolean highContrast() { return prefs.getBoolean(key("high_contrast"), true); }
    public void setQuietMode(boolean enabled) { prefs.edit().putBoolean(key("quiet"), enabled).apply(); }
    public boolean quietMode() { return prefs.getBoolean(key("quiet"), false); }
    public void setDataSaver(boolean enabled) { prefs.edit().putBoolean(key("data_saver"), enabled).apply(); }
    public boolean dataSaver() { return prefs.getBoolean(key("data_saver"), false); }
    public void setAiEnabled(boolean enabled) { prefs.edit().putBoolean(key("ai_enabled"), enabled).apply(); }
    public boolean aiEnabled() { return prefs.getBoolean(key("ai_enabled"), false); }
    public void setAutoplayPreviews(boolean enabled) { prefs.edit().putBoolean(key("autoplay"), enabled).apply(); }
    public boolean autoplayPreviews() { return prefs.getBoolean(key("autoplay"), false); }

    public void addToWatchlist(String id) { modifySet("watchlist", id, true); }
    public void removeFromWatchlist(String id) { modifySet("watchlist", id, false); }
    public boolean isWatchlisted(String id) { return values("watchlist").contains(safe(id, 100)); }
    public Set<String> watchlist() { return values("watchlist"); }
    public void hideTitle(String id) { modifySet("hidden", id, true); }
    public void unhideTitle(String id) { modifySet("hidden", id, false); }
    public boolean isHidden(String id) { return values("hidden").contains(safe(id, 100)); }
    public void markWatched(String id) { modifySet("watched", id, true); }
    public void markUnwatched(String id) { modifySet("watched", id, false); }
    public boolean isWatched(String id) { return values("watched").contains(safe(id, 100)); }
    public void likeTitle(String id) { modifySet("liked", id, true); modifySet("disliked", id, false); }
    public void dislikeTitle(String id) { modifySet("disliked", id, true); modifySet("liked", id, false); }
    public void clearRating(String id) { modifySet("liked", id, false); modifySet("disliked", id, false); }
    public int rating(String id) {
        return values("liked").contains(id) ? 1 : values("disliked").contains(id) ? -1 : 0;
    }

    private Set<String> values(String field) {
        return new HashSet<>(prefs.getStringSet(key(field), Collections.emptySet()));
    }
    private void modifySet(String field, String id, boolean add) {
        id = safe(id, 100);
        if (id.isEmpty()) return;
        Set<String> values = values(field);
        if (add) values.add(id); else values.remove(id);
        prefs.edit().putStringSet(key(field), values).apply();
    }

    public void setPreferredLanguage(String language) {
        prefs.edit().putString(key("language"), safe(language, 12).toLowerCase(Locale.ROOT)).apply();
    }
    public String preferredLanguage() { return prefs.getString(key("language"), ""); }
    public void setPreferredProvider(String provider) { prefs.edit().putString(key("provider"), safe(provider, 60)).apply(); }
    public String preferredProvider() { return prefs.getString(key("provider"), ""); }
    public void setMaximumRuntime(int minutes) {
        prefs.edit().putInt(key("max_runtime"), Math.max(0, Math.min(minutes, 360))).apply();
    }
    public int maximumRuntime() { return prefs.getInt(key("max_runtime"), 0); }
    public void setKidsMode(boolean enabled) { prefs.edit().putBoolean(key("kids"), enabled).apply(); }
    public boolean kidsMode() { return prefs.getBoolean(key("kids"), false); }

    public static final class Candidate {
        public final String id, title, language, provider;
        public final int runtimeMinutes;
        public final boolean suitableForKids;
        public Candidate(String id, String title, String language, String provider,
                         int runtimeMinutes, boolean suitableForKids) {
            this.id=id; this.title=title; this.language=language; this.provider=provider;
            this.runtimeMinutes=runtimeMinutes; this.suitableForKids=suitableForKids;
        }
    }

    public List<Candidate> filter(List<Candidate> catalog) {
        List<Candidate> result = new ArrayList<>();
        for (Candidate c : catalog) {
            if (isHidden(c.id) || isWatched(c.id)) continue;
            if (kidsMode() && !c.suitableForKids) continue;
            if (maximumRuntime() > 0 && c.runtimeMinutes > maximumRuntime()) continue;
            if (!preferredLanguage().isEmpty() && !preferredLanguage().equalsIgnoreCase(c.language)) continue;
            if (!preferredProvider().isEmpty() && !preferredProvider().equalsIgnoreCase(c.provider)) continue;
            result.add(c);
        }
        return result;
    }

    public List<Candidate> offlineRank(List<Candidate> catalog, String query) {
        List<Candidate> result = filter(catalog);
        final String q = safe(query, 100).toLowerCase(Locale.ROOT);
        result.sort((a,b) -> Integer.compare(score(b,q), score(a,q)));
        return result;
    }
    private int score(Candidate c, String query) {
        int score = rating(c.id) * 30 + (isWatchlisted(c.id) ? 20 : 0);
        if (!query.isEmpty()) {
            String title = c.title.toLowerCase(Locale.ROOT);
            if (title.contains(query)) score += 40;
            for (String word : query.split("\\s+"))
                if (word.length() > 2 && title.contains(word)) score += 8;
        }
        return score;
    }

    public void clearCurrentProfile() {
        SharedPreferences.Editor edit = prefs.edit();
        String prefix = profile() + ":";
        for (String k : prefs.getAll().keySet()) if (k.startsWith(prefix)) edit.remove(k);
        edit.apply();
    }
}
