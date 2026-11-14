package com.techpp.glass;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Native, DPAD-first AI TV 2.0 home. Original Talkies home remains available as a recovery activity. */
public final class ModernHomeActivity extends Activity {
    private static final int BG=0xFF090F22, PANEL=0xFF182341, ACCENT=0xFF8C73FF, CYAN=0xFF57D7FF;
    private LinearLayout body, appRow;
    private TextView subtitle, weatherText, clockText, updateText;
    private final Handler timer = new Handler(Looper.getMainLooper());
    private Weather weather;
    private final Runnable tick = new Runnable() { @Override public void run() {
        if (clockText != null) clockText.setText(new SimpleDateFormat("EEE, MMM d  •  h:mm a", Locale.getDefault()).format(new Date()));
        timer.postDelayed(this, 30000);
    }};
    private int dp(float value) {return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int stroke,int radius) {
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{color,color==PANEL?0xFF222D4F:color});
        d.setCornerRadius(dp(radius));d.setStroke(dp(1),stroke);return d;
    }
    private TextView text(String s,int sp,boolean bold) {
        TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(sp);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private void focus(View v) {
        v.setFocusable(true);
        v.setOnFocusChangeListener((item,on)->{
            item.setBackground(shape(on?0xFF30335F:PANEL,on?CYAN:0xFF364568,18));
            item.animate().cancel();
            int motion=MotionPrefs.level(this);
            item.animate().scaleX(on&&motion>0?1.055f:1f).scaleY(on&&motion>0?1.055f:1f)
                    .translationZ(on&&motion>0?dp(12):0)
                    .rotationY(on&&motion==2?-1.3f:0f)
                    .setInterpolator(new DecelerateInterpolator(1.6f))
                    .setDuration(MotionPrefs.duration(this,210)).start();
            if(on&&subtitle!=null&&item.getTag() instanceof String)subtitle.setText((String)item.getTag());
        });
    }
    private TextView action(String label,String hint,Runnable run) {
        TextView b=text(label,17,true);b.setGravity(Gravity.CENTER);b.setPadding(dp(20),dp(14),dp(20),dp(14));
        b.setBackground(shape(PANEL,0xFF364568,18));b.setTag(hint);focus(b);
        b.setOnClickListener(v->run.run());return b;
    }
    private void launch(Intent intent) {
        try {startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
        catch(ActivityNotFoundException|SecurityException ex) {
            if(subtitle!=null)subtitle.setText("Cannot open this app.");
        }
    }
    private void checkUpdates() {
        if (updateText != null) updateText.setText("Checking GitHub releases...");
        SelfUpdate.checkAndInstall(this, status -> runOnUiThread(() -> {
            if (updateText != null) updateText.setText(status);
            if (subtitle != null) subtitle.setText(status);
        }));
    }
    private void refreshWeather() {
        if (weather == null) weather = new Weather(this);
        if (weatherText != null) {
            String cached = weather.cached();
            weatherText.setText(cached == null ? "Weather · locating..." : cached);
        }
        weather.refreshIfStale(line -> runOnUiThread(() -> {
            if (weatherText != null) weatherText.setText(line == null ? "Weather unavailable" : line);
        }));
    }
    private void systemSettings() {
        launch(new Intent(android.provider.Settings.ACTION_SETTINGS));
    }
    private SharedPreferences favorites() {
        return getSharedPreferences("ai_tv_favorites", MODE_PRIVATE);
    }
    private void toggleFavorite(String pkg) {
        SharedPreferences p = favorites();
        p.edit().putBoolean(pkg, !p.getBoolean(pkg, false)).apply();
        addApps();
        subtitle.setText(p.getBoolean(pkg, false) ? "Added to favorites" : "Removed from favorites");
    }
    private void addApps() {
        appRow.removeAllViews();
        Intent tv=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        Intent mobile=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        for(Intent query:new Intent[]{tv,mobile}) {
            for(ResolveInfo info:getPackageManager().queryIntentActivities(query,0)) {
                String pkg=info.activityInfo.packageName;
                if(pkg.equals(getPackageName())||!seen.add(pkg))continue;
                apps.add(info);
            }
        }
        Collections.sort(apps,(a,b)->{
            boolean af=favorites().getBoolean(a.activityInfo.packageName,false);
            boolean bf=favorites().getBoolean(b.activityInfo.packageName,false);
            if (af != bf) return af ? -1 : 1;
            return String.CASE_INSENSITIVE_ORDER.compare(
                a.loadLabel(getPackageManager()).toString(),b.loadLabel(getPackageManager()).toString());
        });
        int count=0;
        for(ResolveInfo info:apps) {
            String pkg=info.activityInfo.packageName;
            LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);tile.setPadding(dp(12),dp(12),dp(12),dp(12));
            tile.setBackground(shape(PANEL,0xFF364568,18));
            ImageView icon=new ImageView(this);
            try {icon.setImageDrawable(info.loadIcon(getPackageManager()));}catch(Exception ignored){}
            tile.addView(icon,new LinearLayout.LayoutParams(dp(64),dp(64)));
            boolean pinned=favorites().getBoolean(pkg,false);
            TextView title=text((pinned ? "★  " : "") + info.loadLabel(getPackageManager()).toString(),15,true);
            title.setGravity(Gravity.CENTER);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams tl=new LinearLayout.LayoutParams(-1,dp(36));tl.topMargin=dp(8);
            tile.addView(title,tl);tile.setTag(title.getText().toString());focus(tile);
            tile.setOnLongClickListener(v->{toggleFavorite(pkg);return true;});
            tile.setOnClickListener(v->{
                Intent i=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
                if(i==null)i=getPackageManager().getLaunchIntentForPackage(pkg);
                if(i!=null)launch(i);else subtitle.setText("No launchable activity for "+pkg);
            });
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(152),dp(146));lp.setMargins(dp(6),dp(8),dp(10),dp(14));
            appRow.addView(tile,lp);if(++count==1)tile.requestFocus();
        }
        if(count==0)appRow.addView(text("No apps found. Check launcher permissions.",17,false));
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(42),dp(20),dp(42),dp(38));
        scroll.addView(body);setContentView(scroll);

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo=text("AI  /  TV",30,true);logo.setTextColor(CYAN);
        header.addView(logo,new LinearLayout.LayoutParams(0,dp(60),1));
        clockText=text("",16,true);clockText.setTextColor(0xFFD0DCF5);
        header.addView(clockText);
        body.addView(header);

        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);
        addNav(nav,"DISCOVER","AI movie and series discovery",
                ()->launch(new Intent(this,AiDiscoveryActivity.class)));
        addNav(nav,"UPDATES","Check for and install signed GitHub releases",this::checkUpdates);
        addNav(nav,"WEATHER","Refresh live weather",this::refreshWeather);
        addNav(nav,"SETTINGS","Android TV system settings",this::systemSettings);
        addNav(nav,"MOTION","Animation intensity",this::showMotion);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(12);
        body.addView(nav,np);

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(28),dp(24),dp(28),dp(24));
        hero.setBackground(shape(0xFF24254A,ACCENT,26));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(20);
        body.addView(hero,hp);
        TextView eyebrow=text("YOUR SPACE  /  ENTERTAINMENT",14,true);
        eyebrow.setTextColor(0xFF92DDFF);hero.addView(eyebrow);
        TextView headline=text("Everything you love. One beautiful home.",30,true);
        LinearLayout.LayoutParams hl=new LinearLayout.LayoutParams(-1,-2);hl.topMargin=dp(10);
        hero.addView(headline,hl);
        subtitle=text("Choose an app, discover a new show, or personalize your TV.",16,false);
        subtitle.setTextColor(0xFFD1D9F0);hero.addView(subtitle);
        weatherText=text("Weather · loading",19,true);
        weatherText.setTextColor(0xFFFFE0A8);
        LinearLayout.LayoutParams wl=new LinearLayout.LayoutParams(-1,-2);wl.topMargin=dp(16);
        hero.addView(weatherText,wl);
        updateText=text("Installed v"+SelfUpdate.installed(this)+"  •  CHECK UPDATES in navigation",14,false);
        updateText.setTextColor(0xFFBFCBF2);hero.addView(updateText);

        LinearLayout featured=new LinearLayout(this);featured.setGravity(Gravity.CENTER_VERTICAL);
        TextView recommend=action("✦  EXPLORE WITH AI","Search personalized recommendations",
                ()->launch(new Intent(this,AiDiscoveryActivity.class)));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(70),1);
        featured.addView(recommend,rp);
        TextView refresh=action("↻  REFRESH","Reload installed apps and weather",()->{
            addApps();refreshWeather();subtitle.setText("Home refreshed.");
        });
        LinearLayout.LayoutParams rf=new LinearLayout.LayoutParams(0,dp(70),1);rf.leftMargin=dp(16);
        featured.addView(refresh,rf);
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.topMargin=dp(18);
        body.addView(featured,fp);

        TextView appsLabel=text("YOUR APPS",22,true);
        LinearLayout.LayoutParams al=new LinearLayout.LayoutParams(-1,-2);al.topMargin=dp(24);
        body.addView(appsLabel,al);
        TextView hint=text("Hold OK on an app to add or remove it from favorites. Favorites appear first.",14,false);
        hint.setTextColor(0xFF9EACCD);body.addView(hint);
        HorizontalScrollView hs=new HorizontalScrollView(this);
        hs.setClipToPadding(false);hs.setHorizontalScrollBarEnabled(false);
        appRow=new LinearLayout(this);appRow.setOrientation(LinearLayout.HORIZONTAL);
        appRow.setClipChildren(false);hs.addView(appRow);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(190));
        ap.topMargin=dp(10);body.addView(hs,ap);
        addApps();
        TextView footer=text("AI TV 2.0  •  Native Android TV  •  Secure signed updates",13,false);
        footer.setTextColor(0xFF8899B8);body.addView(footer);
        timer.post(tick);refreshWeather();
    }
    private void addNav(LinearLayout row,String label,String hint,Runnable task) {
        TextView b=action(label,hint,task);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(54),1);
        lp.rightMargin=dp(10);row.addView(b,lp);
    }
    @Override protected void onDestroy() {
        timer.removeCallbacks(tick);
        super.onDestroy();
    }
    private void showMotion() {
        final String[] options={"Full 3D","Subtle","Off"};
        int level=MotionPrefs.level(this);
        int selected=level==2?0:level==1?1:2;
        new android.app.AlertDialog.Builder(this).setTitle("Motion & animations")
                .setSingleChoiceItems(options,selected,(dialog,which)->{
                    MotionPrefs.setLevel(this,which==0?2:which==1?1:0);
                    dialog.dismiss();subtitle.setText("Motion: "+options[which]);
                }).setNeutralButton("Reset motion", (dialog,which)->{
                    MotionPrefs.setLevel(this,1);
                    subtitle.setText("Motion set to subtle");
                }).setNegativeButton("Cancel",null).show();
    }
    @Override public boolean onKeyDown(int key,KeyEvent event) {
        if(key==KeyEvent.KEYCODE_SEARCH) {
            launch(new Intent(this,AiDiscoveryActivity.class));return true;
        }
        return super.onKeyDown(key,event);
    }
}
