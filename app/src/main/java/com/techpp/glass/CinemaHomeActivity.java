package com.techpp.glass;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;
import java.util.Set;

/**
 * Aurora Cinema: full-screen, remote-first native TV layout.
 * Independently implemented Leanback-inspired rails; no external GPL source or assets copied.
 */
public final class CinemaHomeActivity extends Activity {
    private static final int BG=0xFF071015, PANEL=0xE41B2C32, FOCUS=0xFFB8F4DC, GOLD=0xFFE9BD7D;
    private LinearLayout content,nav,railMovies,railShows,railApps,railContinue,railAiMovies,railAiShows;
    private FrameLayout root;
    private TextView clock,weather,featureTitle,featureMeta,headline;
    private ImageView featureArt;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable clockTask=new Runnable(){public void run(){
        if(clock!=null)clock.setText(new SimpleDateFormat("EEE  d MMM   •   h:mm a",Locale.getDefault()).format(new Date()));
        handler.postDelayed(this,30000);
    }};
    private int d(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable panel(int fill,int stroke,int radius){
        GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(d(radius));
        g.setStroke(d(1.5f),stroke);return g;
    }
    private TextView text(String value,int size,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextColor(0xFFF2F7F5);
        t.setTextSize(size);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private TextView button(String caption,String detail,Runnable click){
        TextView t=text(caption,14,true);t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(d(16),d(9),d(12),d(9));
        t.setBackground(panel(0xCC162B30,0x443D6967,14));t.setFocusable(true);t.setTag(detail);
        t.setOnClickListener(v->click.run());
        t.setOnFocusChangeListener((v,on)->{
            t.setBackground(panel(on?0xFF325650:0xCC162B30,on?FOCUS:0x443D6967,14));
            v.animate().cancel();
            v.animate().scaleX(on?1.035f:1f).scaleY(on?1.035f:1f)
                .translationZ(on?d(10):0).setInterpolator(new DecelerateInterpolator())
                .setDuration(MotionPrefs.duration(this,170)).start();
            if(on&&headline!=null)headline.setText(detail);
        });return t;
    }
    private void navItem(String label,String subtitle,Runnable action){
        TextView t=button(label,subtitle,action);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,d(49));
        lp.bottomMargin=d(9);nav.addView(t,lp);
    }
    private void open(Class<? extends Activity> activity){
        startActivity(new Intent(this,activity));
        if(MotionPrefs.level(this)!=0)overridePendingTransition(R.anim.mac_window_open,R.anim.mac_home_dim);
    }
    private void openSettings(){
        try{startActivity(new Intent(Settings.ACTION_SETTINGS));}
        catch(Exception ex){headline.setText("This TV does not expose system settings.");}
    }
    private void updates(){
        headline.setText("Checking for updates...");
        SelfUpdate.checkAndInstall(this,message->runOnUiThread(()->{
            if(headline!=null)headline.setText(message);
        }));
    }
    private void defaultHome(){
        if(Build.VERSION.SDK_INT>=29){
            RoleManager rm=getSystemService(RoleManager.class);
            if(rm!=null && rm.isRoleAvailable(RoleManager.ROLE_HOME)){
                if(rm.isRoleHeld(RoleManager.ROLE_HOME)){
                    headline.setText("Aurora is the default Home launcher.");return;
                }
                try{startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),291);return;}
                catch(Exception ignored){}
            }
        }
        try{startActivity(new Intent("android.settings.HOME_SETTINGS"));}
        catch(Exception ex){headline.setText("Use TV Settings > Default apps > Home app (if offered).");}
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        root=new FrameLayout(this);
        root.addView(new AuroraBackdrop(this),new FrameLayout.LayoutParams(-1,-1));
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(shell,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);

        nav=new LinearLayout(this);nav.setOrientation(LinearLayout.VERTICAL);
        nav.setPadding(d(18),d(25),d(12),d(16));
        nav.setBackgroundColor(0xD10A1A20);
        shell.addView(nav,new LinearLayout.LayoutParams(d(184),-1));
        TextView brand=text("◈  AURORA",22,true);brand.setTextColor(FOCUS);
        LinearLayout.LayoutParams brandP=new LinearLayout.LayoutParams(-1,d(73));
        nav.addView(brand,brandP);
        navItem("⌂   HOME","A cinematic home for everything you watch",this::scrollTop);
        navItem("◇   DISCOVER","Explore movies and series by poster",()->open(AuroraDiscoveryActivity.class));
        navItem("▦   APPS","Full app library and customization",()->open(ModernHomeActivity.class));
        navItem("▣   MEDIA","Memo and Nuvio TV companion apps",()->open(MediaHubActivity.class));
        navItem("◉   UPDATES","Check for signed launcher updates",this::updates);
        navItem("⚙   SETTINGS","Android TV settings",this::openSettings);
        navItem("⌂   DEFAULT","Set Aurora as your Home launcher",this::defaultHome);
        TextView foot=text("AI TV 2.0\nCINEMA EDITION",11,true);
        foot.setTextColor(0xFF779D9B);
        LinearLayout.LayoutParams footP=new LinearLayout.LayoutParams(-1,0,1);
        foot.setGravity(Gravity.BOTTOM);nav.addView(foot,footP);

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        shell.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(d(28),d(22),d(29),d(42));
        scroll.addView(content);

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView screen=text("Your cinema",27,true);
        header.addView(screen,new LinearLayout.LayoutParams(0,d(52),1));
        clock=text("",14,true);clock.setTextColor(0xFFD4DFD9);header.addView(clock);
        content.addView(header);
        hero();
        railHeader("AI MOVIE PICKS","GPT-ranked films from your verified catalog",()->open(AuroraDiscoveryActivity.class));
        railAiMovies=horizontalRail();
        railHeader("AI SERIES PICKS","GPT-ranked shows from your verified catalog",()->open(AuroraDiscoveryActivity.class));
        railAiShows=horizontalRail();
        railHeader("RECOMMENDED MOVIES","Films selected from the available catalog",()->open(AuroraDiscoveryActivity.class));
        railMovies=horizontalRail();
        railHeader("SERIES TO EXPLORE","Shows and new seasons",()->open(AuroraDiscoveryActivity.class));
        railShows=horizontalRail();
        railHeader("YOUR APPS","Open your entertainment services",()->open(ModernHomeActivity.class));
        railApps=horizontalRail();
        railHeader("CONTINUE WATCHING","Previously played media, when permitted",null);
        railContinue=horizontalRail();
        renderApps();renderContinue();renderCatalog();
        new Trending(this).refreshIfStale(picks->{if(!isFinishing()){renderCatalog();loadAiPicks();}});
        loadAiPicks();
        handler.post(clockTask);
        new Weather(this).refreshIfStale(line->{if(!isFinishing()&&weather!=null)
            weather.setText(line==null?"Weather unavailable":line);});
    }
    private ScrollView rootScroll(){
        LinearLayout shell=(LinearLayout)root.getChildAt(1);
        return (ScrollView)shell.getChildAt(1);
    }
    private void scrollTop(){rootScroll().smoothScrollTo(0,0);}
    private void hero(){
        FrameLayout card=new FrameLayout(this);
        card.setBackground(panel(0xF01B343B,0x554F8E85,22));
        LinearLayout.LayoutParams outer=new LinearLayout.LayoutParams(-1,d(226));
        outer.topMargin=d(15);content.addView(card,outer);
        featureArt=new ImageView(this);featureArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
        featureArt.setAlpha(.36f);card.addView(featureArt,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(d(25),d(22),d(24),d(16));
        card.addView(copy,new FrameLayout.LayoutParams(-1,-1));
        TextView label=text("TONIGHT'S SPOTLIGHT",12,true);label.setTextColor(GOLD);
        copy.addView(label);
        featureTitle=text("Discover something brilliant.",28,true);
        featureTitle.setMaxLines(2);
        LinearLayout.LayoutParams titleP=new LinearLayout.LayoutParams(-1,-2);
        titleP.topMargin=d(8);copy.addView(featureTitle,titleP);
        featureMeta=text("Your personalized entertainment space.",15,false);
        featureMeta.setTextColor(0xFFE0ECE8);copy.addView(featureMeta);
        LinearLayout action=new LinearLayout(this);
        LinearLayout.LayoutParams actP=new LinearLayout.LayoutParams(-1,-2);actP.topMargin=d(15);
        copy.addView(action,actP);
        TextView explore=button("EXPLORE TITLES","Open the cinematic movie and series grid",()->open(AuroraDiscoveryActivity.class));
        action.addView(explore,new LinearLayout.LayoutParams(d(168),d(49)));
        TextView media=button("MY MEDIA","Open Memo / Nuvio TV companion screen",()->open(MediaHubActivity.class));
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(d(138),d(49));mp.leftMargin=d(11);
        action.addView(media,mp);
        weather=text("Weather · loading",13,false);
        weather.setTextColor(0xFFE8C78F);
        LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(-1,-2);wp.topMargin=d(12);
        copy.addView(weather,wp);
        headline=text("Ready",13,false);headline.setVisibility(View.GONE);
        copy.addView(headline);
    }
    private void railHeader(String title,String description,Runnable action){
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,d(48));bp.topMargin=d(15);
        content.addView(bar,bp);
        TextView titleView=text(title,18,true);
        titleView.setLetterSpacing(.07f);
        bar.addView(titleView,new LinearLayout.LayoutParams(0,-2,1));
        if(action!=null){
            TextView more=button("VIEW ALL  ›",description,action);
            more.setTextSize(12);
            bar.addView(more,new LinearLayout.LayoutParams(d(116),d(38)));
        }
    }
    private LinearLayout horizontalRail(){
        HorizontalScrollView scroller=new HorizontalScrollView(this);
        scroller.setClipToPadding(false);scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams box=new LinearLayout.LayoutParams(-1,d(282));
        content.addView(scroller,box);
        LinearLayout rail=new LinearLayout(this);rail.setOrientation(LinearLayout.HORIZONTAL);
        rail.setClipChildren(false);rail.setClipToPadding(false);
        scroller.addView(rail);
        return rail;
    }
    private void renderCatalog(){
        if(railMovies==null||railShows==null)return;
        railMovies.removeAllViews();railShows.removeAllViews();
        List<Trending.Pick> catalog=new Trending(this).cached();
        Trending.Pick spotlight=null;
        int films=0,shows=0;
        for(Trending.Pick pick:catalog){
            if(pick==null||pick.title==null||pick.title.isEmpty())continue;
            if(spotlight==null)spotlight=pick;
            if("movie".equals(pick.kind)&&films<12){addPoster(railMovies,pick);films++;}
            if("tv".equals(pick.kind)&&shows<12){addPoster(railShows,pick);shows++;}
        }
        if(films==0)emptyRail(railMovies,"No movie titles cached yet. Open Discover to refresh.");
        if(shows==0)emptyRail(railShows,"No series cached yet. Open Discover to refresh.");
        if(spotlight!=null){
            featureTitle.setText(spotlight.title);
            featureMeta.setText(spotlight.meta==null?"Now streaming":spotlight.meta);
            if(spotlight.image!=null&&spotlight.image.isFile()){
                BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=3;
                featureArt.setImageBitmap(BitmapFactory.decodeFile(spotlight.image.getAbsolutePath(),options));
            }
        }
    }
    private void loadAiPicks(){
        if(railAiMovies==null||railAiShows==null)return;
        railAiMovies.removeAllViews();railAiShows.removeAllViews();
        AiTvPairingClient pairing=new AiTvPairingClient(this);
        if(!pairing.hasSession()){
            emptyRail(railAiMovies,"Connect your TV in Discover to enable GPT movie picks.");
            emptyRail(railAiShows,"Connect your TV in Discover to enable GPT series picks.");
            return;
        }
        List<Trending.Pick> catalog=new Trending(this).cached();
        if(catalog.isEmpty()){
            emptyRail(railAiMovies,"No catalog yet. Open Discover to refresh.");
            emptyRail(railAiShows,"No catalog yet. Open Discover to refresh.");
            return;
        }
        JSONArray input=new JSONArray();
        Map<String,Trending.Pick> ids=new LinkedHashMap<>();
        for(Trending.Pick pick:catalog){
            if(pick==null||pick.title==null||pick.title.isEmpty())continue;
            String id=pick.key();
            try{input.put(new JSONObject().put("id",id).put("title",pick.title)
                .put("overview",pick.meta==null?"":pick.meta)
                .put("providers",new JSONArray().put(pick.service==null?"":pick.service)));}
            catch(Exception ignored){}
            ids.put(id,pick);
            if(input.length()>=40)break;
        }
        if(input.length()==0)return;
        emptyRail(railAiMovies,"Ranking movies…");
        emptyRail(railAiShows,"Ranking series…");
        new AiRecommendationClient().recommend(
            "https://ai-tv-2-gpt-gateway.onrender.com",pairing.currentToken(),
            "Recommend a balanced mix of excellent movies and series for tonight",input,
            new AiRecommendationClient.Callback(){
                public void onSuccess(JSONArray ranked){runOnUiThread(()->{
                    if(isFinishing()||railAiMovies==null||railAiShows==null)return;
                    railAiMovies.removeAllViews();railAiShows.removeAllViews();
                    int films=0,series=0;
                    for(int i=0;i<ranked.length();i++){
                        JSONObject suggestion=ranked.optJSONObject(i);
                        Trending.Pick p=suggestion==null?null:ids.get(suggestion.optString("id"));
                        if(p!=null&&"movie".equals(p.kind)){addPoster(railAiMovies,p);films++;}
                        else if(p!=null&&"tv".equals(p.kind)){addPoster(railAiShows,p);series++;}
                    }
                    if(films==0)emptyRail(railAiMovies,"No movie picks returned. Browse recommended movies below.");
                    if(series==0)emptyRail(railAiShows,"No series picks returned. Browse recommended shows below.");
                });}
                public void onError(String message){runOnUiThread(()->{
                    if(!isFinishing()&&railAiMovies!=null&&railAiShows!=null){
                        railAiMovies.removeAllViews();railAiShows.removeAllViews();
                        emptyRail(railAiMovies,"AI ranking unavailable. Curated movies remain below.");
                        emptyRail(railAiShows,"AI ranking unavailable. Curated series remain below.");
                    }
                });}
            });
    }
    private void addPoster(LinearLayout rail,Trending.Pick pick){
        AuroraPosterCard card=new AuroraPosterCard(this,pick,142,259,()->showTitle(pick));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(d(142),d(260));
        lp.rightMargin=d(14);lp.bottomMargin=d(10);
        rail.addView(card,lp);
        AuroraImdbRatings.lookup(this,pick,rating->{if(!isFinishing())card.setImdb(rating);});
        card.setOnLongClickListener(v->{
            if(!MediaCompanions.openNuvioTitle(this,pick.id,pick.kind))open(MediaHubActivity.class);
            return true;
        });
    }
    private void showTitle(Trending.Pick pick){
        String[] options={"Open streaming provider","Open title in Nuvio","Open Discover"};
        new AlertDialog.Builder(this).setTitle(pick.title).setMessage(pick.meta)
            .setItems(options,(dialog,index)->{
                if(index==0)launchPackage(pick.pkg);
                if(index==1&&!MediaCompanions.openNuvioTitle(this,pick.id,pick.kind))
                    open(MediaHubActivity.class);
                if(index==2)open(AuroraDiscoveryActivity.class);
            }).setNegativeButton("Close",null).show();
    }
    private void emptyRail(LinearLayout row,String message){
        TextView empty=text(message,15,false);empty.setTextColor(0xFFAAC3BF);
        empty.setPadding(d(20),d(23),d(20),d(23));row.addView(empty);
    }
    private void launchPackage(String pkg){
        if(pkg==null)return;
        Intent i=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if(i==null)i=getPackageManager().getLaunchIntentForPackage(pkg);
        if(i!=null){RecentApps.note(this,pkg);startActivity(i);}
        else if(headline!=null)headline.setText("App not installed.");
    }
    private void renderApps(){
        railApps.removeAllViews();
        PackageManager pm=getPackageManager();
        Set<String> seen=new HashSet<>();
        List<ResolveInfo> found=new ArrayList<>();
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}){
            Intent probe=new Intent(Intent.ACTION_MAIN).addCategory(category);
            for(ResolveInfo entry:pm.queryIntentActivities(probe,0)){
                String pkg=entry.activityInfo.packageName;
                if(pkg.equals(getPackageName())||!seen.add(pkg))continue;
                found.add(entry);
            }
        }
        Collections.sort(found,(a,b)->a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
        int shown=0;
        for(ResolveInfo info:found){
            String pkg=info.activityInfo.packageName;
            LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);tile.setFocusable(true);
            tile.setPadding(d(8),d(8),d(8),d(8));
            tile.setBackground(panel(PANEL,0x554C7372,16));
            ImageView art=new ImageView(this);art.setScaleType(ImageView.ScaleType.FIT_CENTER);
            try{
                Drawable banner=info.activityInfo.loadBanner(pm);
                art.setImageDrawable(banner!=null?banner:info.loadIcon(pm));
                if(banner!=null)art.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }catch(Exception ignored){}
            tile.addView(art,new LinearLayout.LayoutParams(d(118),d(75)));
            TextView name=text(info.loadLabel(pm).toString(),13,true);
            name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tile.addView(name);
            tile.setOnClickListener(v->launchPackage(pkg));
            tile.setOnFocusChangeListener((v,on)->{
                tile.setBackground(panel(on?0xFF34534C:PANEL,on?FOCUS:0x554C7372,16));
                v.animate().cancel();v.animate().scaleX(on?1.045f:1f).scaleY(on?1.045f:1f)
                    .setDuration(MotionPrefs.duration(this,170)).start();
            });
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(d(151),d(126));lp.rightMargin=d(12);
            railApps.addView(tile,lp);if(++shown>=20)break;
        }
        if(shown==0)emptyRail(railApps,"Your TV apps will appear here.");
    }
    private void renderContinue(){
        railContinue.removeAllViews();
        List<Trending.Pick> history=RecentStore.load(this);
        int n=0;
        for(Trending.Pick pick:history){
            if(pick==null||pick.title==null)continue;
            addPoster(railContinue,pick);if(++n>=10)break;
        }
        if(n==0)emptyRail(railContinue,"Media appears after your TV grants compatible playback-session access.");
    }
    @Override protected void onResume(){super.onResume();
        if(railApps!=null)renderApps();
        if(railContinue!=null)renderContinue();
    }
    @Override protected void onDestroy(){handler.removeCallbacks(clockTask);super.onDestroy();}
    @Override public boolean onKeyDown(int key,KeyEvent e){
        if(key==KeyEvent.KEYCODE_SEARCH){open(AuroraDiscoveryActivity.class);return true;}
        return super.onKeyDown(key,e);
    }
}
