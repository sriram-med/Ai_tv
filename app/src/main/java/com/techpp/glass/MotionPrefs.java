package com.techpp.glass;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

/** Motion preferences shared by launcher and discovery. */
public final class MotionPrefs {
    private static final String FILE = "ai_tv_motion";
    private MotionPrefs() {}
    public static int level(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                if (Settings.Global.getFloat(c.getContentResolver(),
                        Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) return 0;
            } catch (Exception ignored) {}
        }
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt("level", 2);
    }
    public static void setLevel(Context c, int level) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putInt("level", Math.max(0, Math.min(2, level))).apply();
    }
    public static boolean telugu(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean("telugu", true);
    }
    public static void setTelugu(Context c, boolean value) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean("telugu", value).apply();
    }
    public static long duration(Context c, long full) {
        int level = level(c);
        return level == 0 ? 0 : level == 1 ? Math.max(90, full / 2) : full;
    }
}
