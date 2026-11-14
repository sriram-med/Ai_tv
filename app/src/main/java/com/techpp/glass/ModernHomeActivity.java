package com.techpp.glass;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.os.Build;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.net.Uri;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.EditText;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import android.text.InputType;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.BitmapFactory;
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
    private static final int BG=0xFF08151B, PANEL=0xE620303A, ACCENT=0xFFE5B97A, CYAN=0xFF89E5D4;
    private LinearLayout body, appRow, picksRow;
    private GridLayout appGrid;
    private HorizontalScrollView appStrip;
    private boolean gridView=true;
    private boolean hideLabels=false;
    private int gridColumns=5;
    private FrameLayout rootFrame;
    private ImageView wallpaperImage;
    private static final int WALLPAPER_PICKER=431;
    private static final int HOME_ROLE_REQUEST=432;
    private boolean allApps=false;
    private String filterText="";
    private boolean gamesOnly=false;
    private boolean favoritesOnly=false;
    private boolean showHidden=false;
    private int sortMode=0;
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
                new int[]{color,color==PANEL?0xE62A3B48:color});
        d.setCornerRadius(dp(radius));d.setStroke(dp(1),stroke);return d;
    }
    private TextView text(String s,int sp,boolean bold) {
        TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(sp);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private void focus(View v) {
        v.setFocusable(true);
        v.setOnFocusChangeListener((item,on)->{
            item.setBackground(shape(on?0xFF39564F:PANEL,on?CYAN:0x66556E77,16));
            item.animate().cancel();
            int motion=MotionPrefs.level(this);
            item.animate().scaleX(on&&motion>0?1.055f:1f).scaleY(on&&motion>0?1.055f:1f)
                    .translationZ(on&&motion>0?dp(12):0)
                    .rotationY(on&&motion==2?-0.7f:0f)
                    .setInterpolator(new DecelerateInterpolator(1.6f))
                    .setDuration(MotionPrefs.duration(this,210)).start();
            if(on&&subtitle!=null&&item.getTag() instanceof String)subtitle.setText((String)item.getTag());
        });
    }
    private TextView action(String label,String hint,Runnable run) {
        TextView b=text(label,17,true);b.setGravity(Gravity.CENTER);b.setPadding(dp(20),dp(14),dp(20),dp(14));
        b.setBackground(shape(PANEL,0x66556E77,16));b.setTag(hint);focus(b);
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
    private SharedPreferences hiddenApps(){return getSharedPreferences("aurora_hidden_apps",MODE_PRIVATE);}
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
        if(appGrid!=null)appGrid.removeAllViews();
        if(appRow!=null)appRow.removeAllViews();
        if(appStrip!=null)appStrip.setVisibility(gridView?View.GONE:View.VISIBLE);
        if(appGrid!=null)appGrid.setColumnCount(gridColumns);
        if(appGrid!=null)appGrid.setVisibility(gridView?View.VISIBLE:View.GONE);
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
            if(sortMode==2){
                List<String> recent=RecentApps.all(this);
                int ia=recent.indexOf(a.activityInfo.packageName),ib=recent.indexOf(b.activityInfo.packageName);
                if(ia<0)ia=Integer.MAX_VALUE;if(ib<0)ib=Integer.MAX_VALUE;
                if(ia!=ib)return Integer.compare(ia,ib);
            }
            if (!allApps && af != bf) return af ? -1 : 1;
            int result=String.CASE_INSENSITIVE_ORDER.compare(
                a.loadLabel(getPackageManager()).toString(),b.loadLabel(getPackageManager()).toString());
            return sortMode==1 ? -result : result;
        });
        int count=0;
        for(ResolveInfo info:apps) {
            String visible=info.loadLabel(getPackageManager()).toString();
            if(!filterText.isEmpty()&&!visible.toLowerCase(Locale.ROOT).contains(filterText.toLowerCase(Locale.ROOT)))continue;
            if(!showHidden && hiddenApps().getBoolean(info.activityInfo.packageName,false))continue;
            if(favoritesOnly && !favorites().getBoolean(info.activityInfo.packageName,false))continue;
            if(gamesOnly){try{
                if(getPackageManager().getApplicationInfo(info.activityInfo.packageName,0).category != android.content.pm.ApplicationInfo.CATEGORY_GAME)continue;
            }catch(Exception ex){continue;}}

            String pkg=info.activityInfo.packageName;
            LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);tile.setPadding(dp(12),dp(12),dp(12),dp(12));
            tile.setBackground(shape(PANEL,0x66556E77,16));
            ImageView icon=new ImageView(this);
            try {
                Drawable bannerArt=info.activityInfo.loadBanner(getPackageManager());
                icon.setImageDrawable(bannerArt != null ? bannerArt : info.loadIcon(getPackageManager()));
                icon.setScaleType(bannerArt != null ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
            } catch(Exception ignored){}
            int usable=(int)(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density)-86;
            int tileDp=gridView?Math.max(112,usable/gridColumns-15):173;
            tile.addView(icon,new LinearLayout.LayoutParams(dp(Math.max(72,tileDp-28)),dp(75)));
            boolean pinned=favorites().getBoolean(pkg,false);
            TextView title=text((pinned ? "★  " : "") + info.loadLabel(getPackageManager()).toString(),15,true);
            title.setGravity(Gravity.CENTER);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams tl=new LinearLayout.LayoutParams(-1,dp(36));tl.topMargin=dp(8);
            if(!hideLabels)tile.addView(title,tl);
            tile.setTag(title.getText().toString());focus(tile);
            tile.setOnLongClickListener(v->{showAppActions(pkg,title.getText().toString());return true;});
            tile.setOnClickListener(v->{
                Intent i=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
                if(i==null)i=getPackageManager().getLaunchIntentForPackage(pkg);
                if(i!=null){RecentApps.note(this,pkg);launch(i);}else subtitle.setText("No launchable activity for "+pkg);
            });
            if(gridView && appGrid!=null){
                GridLayout.LayoutParams lp=new GridLayout.LayoutParams(
                    GridLayout.spec(count/gridColumns),GridLayout.spec(count%gridColumns));
                lp.width=dp(tileDp);lp.height=dp(153);
                lp.setMargins(dp(3),dp(6),dp(12),dp(12));
                appGrid.addView(tile,lp);
            }else{
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(173),dp(153));
                lp.setMargins(dp(3),dp(7),dp(13),dp(12));appRow.addView(tile,lp);
            }
            if(++count==1)tile.requestFocus();
        }
        if(count==0){
            TextView empty=text("No apps match this view. Clear filters to show everything.",17,false);
            if(gridView && appGrid!=null)appGrid.addView(empty);else appRow.addView(empty);
        }
    }
    private void updateFeatured() {
        if(picksRow==null)return;
        picksRow.removeAllViews();
        List<Trending.Pick> picks;
        try{picks=new Trending(this).cached();}catch(Exception ex){return;}
        int count=0;
        for(Trending.Pick pick:picks) {
            if(pick==null||pick.title==null||pick.title.isEmpty())continue;
            LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);
            tile.setBackground(shape(PANEL,0x66556E77,18));
            tile.setPadding(dp(9),dp(9),dp(9),dp(9));
            ImageView picture=new ImageView(this);
            picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if(pick.image!=null&&pick.image.exists()){
                android.graphics.BitmapFactory.Options opts=new android.graphics.BitmapFactory.Options();
                opts.inSampleSize=3;
                try{picture.setImageBitmap(BitmapFactory.decodeFile(pick.image.getAbsolutePath(),opts));}
                catch(Exception ignored){}
            }
            tile.addView(picture,new LinearLayout.LayoutParams(-1,dp(105)));
            TextView label=text(pick.title,15,true);
            label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);
            tile.addView(label,lp);
            TextView service=text(pick.service==null?"Available pick":pick.service,12,false);
            service.setTextColor(0xFFACCCCA);tile.addView(service);
            tile.setTag(pick.title);focus(tile);
            tile.setOnClickListener(v->{
                Intent launchIntent=pick.pkg==null?null:getPackageManager().getLeanbackLaunchIntentForPackage(pick.pkg);
                if(launchIntent==null&&pick.pkg!=null)launchIntent=getPackageManager().getLaunchIntentForPackage(pick.pkg);
                if(launchIntent!=null){RecentApps.note(this,pick.pkg);launch(launchIntent);}
                else subtitle.setText("Provider not installed for "+pick.title);
            });
            LinearLayout.LayoutParams card=new LinearLayout.LayoutParams(dp(235),dp(174));
            card.rightMargin=dp(13);picksRow.addView(tile,card);
            if(++count>=8)break;
        }
        if(count==0){
            TextView placeholder=text("No curated titles yet  ·  Open Discover to refresh your catalog",15,false);
            placeholder.setPadding(dp(18),dp(14),dp(18),dp(14));
            picksRow.addView(placeholder);
        }
    }
    private void refreshFeatured() {
        new Trending(this).refreshIfStale(picks->{
            if(!isFinishing())updateFeatured();
        });
    }
    private void showMediaCompanions() {
        launch(new Intent(this,MediaHubActivity.class));
    }
    private void addContinueWatching() {
        List<Trending.Pick> seen=RecentStore.load(this);
        if(seen.isEmpty())return;
        TextView title=text("Continue watching",21,true);
        LinearLayout.LayoutParams label=new LinearLayout.LayoutParams(-1,-2);
        label.topMargin=dp(12);body.addView(title,label);
        HorizontalScrollView strip=new HorizontalScrollView(this);
        strip.setHorizontalScrollBarEnabled(false);
        LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);
        strip.addView(line);
        for(Trending.Pick pick:seen) {
            LinearLayout tile=new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);tile.setPadding(dp(11),dp(9),dp(11),dp(10));
            tile.setBackground(shape(PANEL,0x66556E77,16));
            ImageView art=new ImageView(this);
            art.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if(pick.image!=null && pick.image.isFile()){
                BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;
                art.setImageBitmap(BitmapFactory.decodeFile(pick.image.getAbsolutePath(),o));
            }else{
                try{art.setImageDrawable(getPackageManager().getApplicationIcon(pick.pkg));}
                catch(Exception ignored){}
            }
            tile.addView(art,new LinearLayout.LayoutParams(-1,dp(75)));
            TextView name=text(pick.title,14,true);
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tile.addView(name);
            TextView source=text(pick.service==null?"":pick.service,12,false);
            source.setTextColor(0xFF9DB9BE);tile.addView(source);
            tile.setTag(pick.title);
            focus(tile);
            tile.setOnClickListener(v->{
                if(pick.intentUri!=null&&!pick.intentUri.isEmpty()){
                    try{launch(Intent.parseUri(pick.intentUri,Intent.URI_INTENT_SCHEME));return;}
                    catch(Exception ignored){}
                }
                Intent i=getPackageManager().getLeanbackLaunchIntentForPackage(pick.pkg);
                if(i!=null)launch(i);
            });
            LinearLayout.LayoutParams card=new LinearLayout.LayoutParams(dp(207),dp(143));
            card.rightMargin=dp(13);
            line.addView(tile,card);
        }
        LinearLayout.LayoutParams row=new LinearLayout.LayoutParams(-1,dp(155));
        row.topMargin=dp(8);body.addView(strip,row);
    }
    private void showAppActions(String pkg,String name) {
        String[] items={"Launch","Toggle favorite","Hide / unhide app","App information","Uninstall"};
        new AlertDialog.Builder(this).setTitle(name.replace("★  ",""))
            .setItems(items,(dialog,which)->{
                if(which==0){Intent i=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
                    if(i==null)i=getPackageManager().getLaunchIntentForPackage(pkg);
                    if(i!=null)launch(i);}
                if(which==1)toggleFavorite(pkg);
                if(which==2){
                    boolean hide=!hiddenApps().getBoolean(pkg,false);
                    hiddenApps().edit().putBoolean(pkg,hide).apply();
                    showHidden=false;addApps();subtitle.setText(hide?"App hidden. Use MORE to show hidden apps.":"App visible.");
                }
                if(which==3)openAppInfo(pkg);
                if(which==4)launch(new Intent(Intent.ACTION_DELETE,Uri.parse("package:"+pkg)));
            }).show();
    }
    private void openAppInfo(String pkg) {
        Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+pkg));
        launch(i);
    }
    private void chooseDefault() {
        new AlertDialog.Builder(this).setTitle("Make AI TV your Home launcher")
            .setMessage("Android controls the default Home app. Choose AI TV 2.0 in Home app settings, or press the Home button and select AI TV 2.0 → Always if prompted. Your TV manufacturer may restrict replacement launchers. This option never disables your original launcher.")
            .setPositiveButton("Open Home app settings",(d,w)->{
                if(Build.VERSION.SDK_INT>=29) {
                    RoleManager roles=getSystemService(RoleManager.class);
                    if(roles!=null && roles.isRoleAvailable(RoleManager.ROLE_HOME)){
                        if(roles.isRoleHeld(RoleManager.ROLE_HOME)){
                            subtitle.setText("AI TV is already the default Home launcher.");return;
                        }
                        try{startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME),HOME_ROLE_REQUEST);return;}
                        catch(Exception ignored){}
                    }
                }
                try{startActivity(new Intent("android.settings.HOME_SETTINGS"));}
                catch(Exception e){systemSettings();subtitle.setText("Select Apps → Default apps → Home app, if supported.");}
            }).setNeutralButton("Home chooser",(d,w)->{
                Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_HOME);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try{startActivity(Intent.createChooser(i,"Choose Home application"));}
                catch(Exception e){subtitle.setText("Use Android TV settings to choose the Home app.");}
            }).setNegativeButton("Cancel",null).show();
    }
    private void configureWeather() {
        EditText city=new EditText(this);
        city.setSingleLine(true);
        city.setTextColor(Color.WHITE);
        city.setHintTextColor(0xFFB2CCCF);
        city.setHint("City or latitude,longitude");
        LinearLayout wrap=new LinearLayout(this);
        wrap.setPadding(dp(22),dp(8),dp(22),0);
        wrap.addView(city,new LinearLayout.LayoutParams(-1,-2));
        new AlertDialog.Builder(this).setTitle("Weather location")
                .setMessage("Enter your city (for example Hyderabad) or coordinates. Saved locally on your TV.")
                .setView(wrap).setPositiveButton("Save",(d,w)->{
                    String val=city.getText().toString().trim();
                    if(val.isEmpty() || val.length()>120){subtitle.setText("Enter a valid location");return;}
                    File dir=getExternalFilesDir(null);
                    if(dir==null){subtitle.setText("TV storage unavailable");return;}
                    try(FileOutputStream out=new FileOutputStream(new File(dir,"location.txt"))){
                        out.write(val.getBytes(StandardCharsets.UTF_8));
                        weather=new Weather(this);
                        subtitle.setText("Weather location saved: "+val);
                        refreshWeather();
                    }catch(Exception ex){subtitle.setText("Could not save weather location");}
                }).setNegativeButton("Cancel",null).show();
    }
    private void searchApps() {
        EditText field=new EditText(this);field.setSingleLine(true);
        field.setTextColor(Color.WHITE);field.setHintTextColor(0xFF8FA9AA);
        field.setHint("Find an installed app");field.setText(filterText);
        LinearLayout wrap=new LinearLayout(this);wrap.setPadding(dp(22),dp(5),dp(22),0);
        wrap.addView(field,new LinearLayout.LayoutParams(-1,-2));
        new AlertDialog.Builder(this).setTitle("Search your apps").setView(wrap)
            .setPositiveButton("Search",(d,w)->{
                filterText=field.getText().toString().trim();
                gamesOnly=false;addApps();
                subtitle.setText(filterText.isEmpty()?"All apps":"Showing matches for: "+filterText);
            }).setNeutralButton("Clear",(d,w)->{
                filterText="";gamesOnly=false;addApps();
            }).setNegativeButton("Cancel",null).show();
    }
    private void pickWallpaper() {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try{startActivityForResult(i,WALLPAPER_PICKER);}
        catch(Exception ex){subtitle.setText("Install a file explorer to select a wallpaper image.");}
    }
    private void loadWallpaper() {
        String path=getSharedPreferences("aurora_wallpaper",MODE_PRIVATE).getString("uri",null);
        if(path==null || wallpaperImage==null){if(wallpaperImage!=null)wallpaperImage.setVisibility(View.GONE);return;}
        final ImageView target=wallpaperImage;
        new Thread(()->{
            android.graphics.Bitmap bitmap=null;
            try(InputStream in=getContentResolver().openInputStream(Uri.parse(path))){
                BitmapFactory.Options options=new BitmapFactory.Options();
                options.inSampleSize=4;
                bitmap=BitmapFactory.decodeStream(in,null,options);
            }catch(Exception ignored){}
            final android.graphics.Bitmap result=bitmap;
            runOnUiThread(()->{
                if(isFinishing()||target!=wallpaperImage)return;
                if(result!=null){target.setImageBitmap(result);target.setVisibility(View.VISIBLE);}
                else {target.setVisibility(View.GONE);if(subtitle!=null)subtitle.setText("Wallpaper unavailable; Aurora background restored.");}
            });
        },"AuroraWallpaper").start();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==HOME_ROLE_REQUEST){
            if(subtitle!=null)subtitle.setText(result==RESULT_OK?"Home launcher selected. Press Home to verify.":"Home selection was cancelled or unavailable.");
            return;
        }
        if(request!=WALLPAPER_PICKER||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}
        catch(Exception ignored){}
        getSharedPreferences("aurora_wallpaper",MODE_PRIVATE).edit().putString("uri",uri.toString()).apply();
        loadWallpaper();
    }
    private void resetWallpaper() {
        getSharedPreferences("aurora_wallpaper",MODE_PRIVATE).edit().remove("uri").apply();
        if(wallpaperImage!=null){wallpaperImage.setImageDrawable(null);wallpaperImage.setVisibility(View.GONE);}
        if(subtitle!=null)subtitle.setText("Aurora background restored");
    }
    private void showSystemShortcuts() {
        final String[] names={"Network & Wi-Fi","Bluetooth","Display & picture","Sound & audio",
            "Storage","Accessibility","Date & time","Apps management","Input & keyboard",
            "Device settings"};
        final String[] actions={Settings.ACTION_WIFI_SETTINGS,Settings.ACTION_BLUETOOTH_SETTINGS,
            Settings.ACTION_DISPLAY_SETTINGS,Settings.ACTION_SOUND_SETTINGS,
            Settings.ACTION_INTERNAL_STORAGE_SETTINGS,Settings.ACTION_ACCESSIBILITY_SETTINGS,
            Settings.ACTION_DATE_SETTINGS,Settings.ACTION_APPLICATION_SETTINGS,
            Settings.ACTION_INPUT_METHOD_SETTINGS,Settings.ACTION_SETTINGS};
        new AlertDialog.Builder(this).setTitle("TV SETTINGS").setItems(names,(d,i)->{
            try{launch(new Intent(actions[i]));}
            catch(Exception ex){subtitle.setText("This TV does not expose "+names[i]);}
        }).setNegativeButton("Close",null).show();
    }
    private void showMore() {
        final String[] labels={"Display favorites first","Show all apps (alphabetical)","Reverse alphabetical sort",
            "Search installed apps","Show games only","Clear search and filters","Refresh weather","Set weather location",
            "Check for updates","Make default Home launcher","Android system settings","Motion level",
            "AI Discovery","Refresh app collection","Choose custom wallpaper","Restore Aurora wallpaper",
            "Switch grid / horizontal row","Show / hide app labels","Grid: 4 columns","Grid: 5 columns","Grid: 6 columns",
            "Favorites only","Show hidden apps","Hide hidden apps","Sort by recent usage","Reset app preferences","TV system shortcuts","Memo / Nuvio media hub","Open Memo","Open Nuvio"};
        new AlertDialog.Builder(this).setTitle("TV CONTROL CENTER").setItems(labels,(d,i)->{
            switch(i){
                case 0: allApps=false;gamesOnly=false;favoritesOnly=false;sortMode=0;addApps();break;
                case 1: allApps=true;gamesOnly=false;favoritesOnly=false;sortMode=0;addApps();break;
                case 2: allApps=true;gamesOnly=false;sortMode=1;addApps();break;
                case 3:searchApps();break;
                case 4:gamesOnly=true;filterText="";addApps();break;
                case 5:gamesOnly=false;favoritesOnly=false;filterText="";addApps();break;
                case 6:refreshWeather();break;
                case 7:configureWeather();break;
                case 8:checkUpdates();break;
                case 9:chooseDefault();break;
                case 10:systemSettings();break;
                case 11:showMotion();break;
                case 12:launch(new Intent(this,AuroraDiscoveryActivity.class));break;
                case 13:addApps();break;
                case 14:pickWallpaper();break;
                case 15:resetWallpaper();break;
                case 16:gridView=!gridView;addApps();break;
                case 17:hideLabels=!hideLabels;addApps();break;
                case 18:gridColumns=4;appGrid.setColumnCount(4);addApps();break;
                case 19:gridColumns=5;appGrid.setColumnCount(5);addApps();break;
                case 20:gridColumns=6;appGrid.setColumnCount(6);addApps();break;
                case 21:favoritesOnly=true;gamesOnly=false;filterText="";addApps();break;
                case 22:showHidden=true;addApps();break;
                case 23:showHidden=false;addApps();break;
                case 24:sortMode=2;favoritesOnly=false;addApps();break;
                case 25:new AlertDialog.Builder(this).setTitle("Reset app layout and filters")
                    .setMessage("Restore default ordering, labels and hidden apps? Favorites are preserved.")
                    .setPositiveButton("Reset",(x,y)->{
                        hiddenApps().edit().clear().apply();
                        showHidden=false;favoritesOnly=false;gamesOnly=false;filterText="";
                        gridView=true;gridColumns=5;hideLabels=false;allApps=false;sortMode=0;
                        addApps();
                    }).setNegativeButton("Cancel",null).show();break;
                case 26:showSystemShortcuts();break;
                case 27:showMediaCompanions();break;
                case 28:if(!MediaCompanions.launchMemo(this))showMediaCompanions();break;
                case 29:if(!MediaCompanions.launchNuvio(this))showMediaCompanions();break;
                default:break;
            }
        }).show();
    }
    private void navButton(LinearLayout bar,String title,String detail,Runnable click) {
        TextView t=action(title,detail,click);
        t.setTextSize(14);
        t.setPadding(dp(14),dp(8),dp(14),dp(8));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(47),1);
        p.rightMargin=dp(9);bar.addView(t,p);
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        rootFrame=new FrameLayout(this);
        rootFrame.addView(new AuroraBackdrop(this),new FrameLayout.LayoutParams(-1,-1));
        wallpaperImage=new ImageView(this);
        wallpaperImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        wallpaperImage.setAlpha(.32f);
        wallpaperImage.setVisibility(View.GONE);
        rootFrame.addView(wallpaperImage,new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll=new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setFillViewport(true);
        body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(43),dp(21),dp(43),dp(30));
        scroll.addView(body);
        rootFrame.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        setContentView(rootFrame);

        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView monogram=text("◈",31,true);monogram.setTextColor(ACCENT);
        header.addView(monogram);
        LinearLayout identity=new LinearLayout(this);identity.setOrientation(LinearLayout.VERTICAL);
        TextView logo=text(" AURORA  /  AI TV",21,true);logo.setLetterSpacing(.12f);
        identity.addView(logo);
        TextView tagline=text(" PERSONAL ENTERTAINMENT OS",11,true);
        tagline.setLetterSpacing(.15f);tagline.setTextColor(0xFF9DB9BE);identity.addView(tagline);
        LinearLayout.LayoutParams id=new LinearLayout.LayoutParams(0,-2,1);
        header.addView(identity,id);
        clockText=text("",15,true);
        clockText.setTextColor(0xFFE8E4D8);header.addView(clockText);
        body.addView(header);

        LinearLayout nav=new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams navp=new LinearLayout.LayoutParams(-1,-2);
        navp.topMargin=dp(18);body.addView(nav,navp);
        navButton(nav,"HOME","Your collection",()->{allApps=false;sortMode=0;addApps();});
        navButton(nav,"AI DISCOVER","Personalized recommendations",()->launch(new Intent(this,AuroraDiscoveryActivity.class)));
        navButton(nav,"MEDIA","Memo and Nuvio TV companions",()->launch(new Intent(this,MediaHubActivity.class)));
        navButton(nav,"ALL APPS","Browse every installed app",()->{allApps=true;gamesOnly=false;filterText="";addApps();});
        navButton(nav,"SEARCH","Find apps on your TV",this::searchApps);
        navButton(nav,"UPDATES","Download signed releases",this::checkUpdates);
        navButton(nav,"DEFAULT HOME","Set as default launcher",this::chooseDefault);
        navButton(nav,"MORE","Weather, settings and controls",this::showMore);

        LinearLayout feature=new LinearLayout(this);
        feature.setGravity(Gravity.CENTER_VERTICAL);
        feature.setPadding(dp(27),dp(22),dp(27),dp(22));
        feature.setBackground(shape(0xD9223940,0x665D8B88,24));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);
        fp.topMargin=dp(20);body.addView(feature,fp);

        LinearLayout heroCopy=new LinearLayout(this);
        heroCopy.setOrientation(LinearLayout.VERTICAL);
        TextView kicker=text("CURATED FOR YOUR SCREEN",12,true);
        kicker.setLetterSpacing(.17f);kicker.setTextColor(ACCENT);heroCopy.addView(kicker);
        TextView headline=text("Your next obsession starts here.",31,true);
        headline.setMaxLines(2);
        LinearLayout.LayoutParams h=new LinearLayout.LayoutParams(-1,-2);h.topMargin=dp(6);
        heroCopy.addView(headline,h);
        subtitle=text("Movies, series, and everything you love. Reimagined.",16,false);
        subtitle.setTextColor(0xFFCFDDDB);
        heroCopy.addView(subtitle);
        TextView explore=action("EXPLORE WITH AI  ›","Find your next movie or series",
                ()->launch(new Intent(this,AuroraDiscoveryActivity.class)));
        explore.setTextSize(15);
        explore.setBackground(shape(0xFFB6E9D6,0xFFB6E9D6,14));
        explore.setTextColor(0xFFF0FFF8);
        LinearLayout.LayoutParams exploreP=new LinearLayout.LayoutParams(dp(245),dp(53));
        exploreP.topMargin=dp(16);
        heroCopy.addView(explore,exploreP);
        feature.addView(heroCopy,new LinearLayout.LayoutParams(0,-2,3));

        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(18),dp(12),dp(16),dp(12));
        info.setBackground(shape(0xC810282F,0x66556E77,18));
        TextView forecastLabel=text("YOUR FORECAST  ↗",12,true);forecastLabel.setTextColor(ACCENT);
        info.addView(forecastLabel);
        weatherText=text("Checking forecast…",19,true);
        LinearLayout.LayoutParams weatherP=new LinearLayout.LayoutParams(-1,-2);weatherP.topMargin=dp(8);
        info.addView(weatherText,weatherP);
        info.setOnClickListener(v->configureWeather());
        info.setFocusable(true);
        TextView updateLabel=text("SYSTEM",12,true);updateLabel.setTextColor(ACCENT);
        LinearLayout.LayoutParams ul=new LinearLayout.LayoutParams(-1,-2);ul.topMargin=dp(17);
        info.addView(updateLabel,ul);
        updateText=text("v"+SelfUpdate.installed(this)+"  ·  Check updates",13,false);
        updateText.setTextColor(0xFFE6E6E2);info.addView(updateText);
        feature.addView(info,new LinearLayout.LayoutParams(0,-2,2));

        LinearLayout featuresTitle=new LinearLayout(this);featuresTitle.setGravity(Gravity.CENTER_VERTICAL);
        TextView featuredLabel=text("Selected for tonight",21,true);
        featuresTitle.addView(featuredLabel,new LinearLayout.LayoutParams(0,-2,1));
        TextView reload=action("REFRESH PICKS","Refresh featured streaming titles",()->{
            refreshFeatured();updateFeatured();});
        reload.setTextSize(13);featuresTitle.addView(reload);
        LinearLayout.LayoutParams ftitle=new LinearLayout.LayoutParams(-1,-2);
        ftitle.topMargin=dp(22);body.addView(featuresTitle,ftitle);
        HorizontalScrollView pickScroll=new HorizontalScrollView(this);
        pickScroll.setHorizontalScrollBarEnabled(false);pickScroll.setClipToPadding(false);
        picksRow=new LinearLayout(this);picksRow.setOrientation(LinearLayout.HORIZONTAL);
        picksRow.setClipChildren(false);
        pickScroll.addView(picksRow);
        LinearLayout.LayoutParams picksP=new LinearLayout.LayoutParams(-1,dp(184));
        picksP.topMargin=dp(10);body.addView(pickScroll,picksP);
        updateFeatured();
        refreshFeatured();

        LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView appsLabel=text("Your collection",23,true);
        titleRow.addView(appsLabel,new LinearLayout.LayoutParams(0,-2,1));
        TextView sort=action("LAYOUT & FILTER","Organize installed apps",this::showMore);
        sort.setTextSize(13);titleRow.addView(sort);
        LinearLayout.LayoutParams titleP=new LinearLayout.LayoutParams(-1,-2);
        titleP.topMargin=dp(19);body.addView(titleRow,titleP);
        TextView hint=text("Hold OK for favorites and app controls  ·  Use the remote to navigate",13,false);
        hint.setTextColor(0xFF93ACB1);body.addView(hint);
        appGrid=new GridLayout(this);appGrid.setColumnCount(gridColumns);
        appGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        appGrid.setClipChildren(false);appGrid.setClipToPadding(false);
        LinearLayout.LayoutParams gridP=new LinearLayout.LayoutParams(-1,-2);
        gridP.topMargin=dp(12);body.addView(appGrid,gridP);
        appStrip=new HorizontalScrollView(this);
        appStrip.setHorizontalScrollBarEnabled(false);
        appStrip.setClipToPadding(false);
        appStrip.setVisibility(View.GONE);
        appRow=new LinearLayout(this);appRow.setOrientation(LinearLayout.HORIZONTAL);
        appRow.setClipChildren(false);appRow.setClipToPadding(false);
        appStrip.addView(appRow);
        LinearLayout.LayoutParams appsP=new LinearLayout.LayoutParams(-1,dp(182));
        appsP.topMargin=dp(9);body.addView(appStrip,appsP);
        addApps();
        addContinueWatching();

        TextView footer=text("AURORA   •   AI TV 2.0   •   Designed for the big screen",12,false);
        footer.setTextColor(0xFF819EA5);
        LinearLayout.LayoutParams footerP=new LinearLayout.LayoutParams(-1,-2);
        footerP.topMargin=dp(8);body.addView(footer,footerP);
        timer.post(tick);
        refreshWeather();
        loadWallpaper();
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
            launch(new Intent(this,AuroraDiscoveryActivity.class));return true;
        }
        return super.onKeyDown(key,event);
    }
}
