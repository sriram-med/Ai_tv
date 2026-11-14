package com.techpp.glass;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
    private TextView subtitle;
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
        Collections.sort(apps,(a,b)->String.CASE_INSENSITIVE_ORDER.compare(
                a.loadLabel(getPackageManager()).toString(),b.loadLabel(getPackageManager()).toString()));
        int count=0;
        for(ResolveInfo info:apps) {
            String pkg=info.activityInfo.packageName;
            LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);tile.setPadding(dp(12),dp(12),dp(12),dp(12));
            tile.setBackground(shape(PANEL,0xFF364568,18));
            ImageView icon=new ImageView(this);
            try {icon.setImageDrawable(info.loadIcon(getPackageManager()));}catch(Exception ignored){}
            tile.addView(icon,new LinearLayout.LayoutParams(dp(64),dp(64)));
            TextView title=text(info.loadLabel(getPackageManager()).toString(),15,true);
            title.setGravity(Gravity.CENTER);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams tl=new LinearLayout.LayoutParams(-1,dp(36));tl.topMargin=dp(8);
            tile.addView(title,tl);tile.setTag(title.getText().toString());focus(tile);
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
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(44),dp(24),dp(44),dp(32));
        scroll.addView(body);setContentView(scroll);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo=text("✦  AI TV 2.0",30,true);logo.setTextColor(CYAN);
        top.addView(logo,new LinearLayout.LayoutParams(0,dp(64),1));
        TextView discover=action("✦  AI DISCOVERY","Search for movies and shows with AI",
                ()->launch(new Intent(this,AiDiscoveryActivity.class)));
        top.addView(discover);TextView settings=action("⚙  MOTION","Change animation and Telugu reactions",this::showMotion);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-2,-2);sp.leftMargin=dp(14);top.addView(settings,sp);
        body.addView(top);
        TextView hero=text("Your entertainment, beautifully arranged.",31,true);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(32);body.addView(hero,hp);
        subtitle=text("Select an app with your remote. Press AI DISCOVERY for recommendations.",17,false);
        subtitle.setTextColor(0xFFB7C6E8);body.addView(subtitle);
        TextView banner=text("✦  DISCOVER SOMETHING NEW     సినిమాల సందడి! ✨",22,true);
        banner.setGravity(Gravity.CENTER_VERTICAL);banner.setPadding(dp(28),dp(16),dp(28),dp(16));
        banner.setBackground(shape(0xFF252052,ACCENT,24));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(108));bp.topMargin=dp(26);
        body.addView(banner,bp);banner.setFocusable(true);banner.setTag("Open AI-powered discovery");
        focus(banner);banner.setOnClickListener(v->launch(new Intent(this,AiDiscoveryActivity.class)));
        TextView heading=text("YOUR APPS",18,true);
        LinearLayout.LayoutParams h=new LinearLayout.LayoutParams(-1,-2);h.topMargin=dp(24);body.addView(heading,h);
        HorizontalScrollView hs=new HorizontalScrollView(this);hs.setClipToPadding(false);hs.setHorizontalScrollBarEnabled(false);
        appRow=new LinearLayout(this);appRow.setOrientation(LinearLayout.HORIZONTAL);appRow.setClipChildren(false);
        hs.addView(appRow);body.addView(hs);addApps();
        TextView tip=text("AI TV 2.0  •  Remote-first  •  Original glass-inspired design",14,false);
        tip.setTextColor(0xFF8E9BBF);body.addView(tip);
    }
    private void showMotion() {
        final String[] options={"Full 3D","Subtle","Off"};
        int level=MotionPrefs.level(this);
        int selected=level==2?0:level==1?1:2;
        new android.app.AlertDialog.Builder(this).setTitle("Motion & animations")
                .setSingleChoiceItems(options,selected,(dialog,which)->{
                    MotionPrefs.setLevel(this,which==0?2:which==1?1:0);
                    dialog.dismiss();subtitle.setText("Motion: "+options[which]);
                }).setNeutralButton("Telugu reactions: "+(MotionPrefs.telugu(this)?"ON":"OFF"),(dialog,which)->{
                    MotionPrefs.setTelugu(this,!MotionPrefs.telugu(this));
                    subtitle.setText("Telugu reactions "+(MotionPrefs.telugu(this)?"enabled":"disabled"));
                }).setNegativeButton("Cancel",null).show();
    }
    @Override public boolean onKeyDown(int key,KeyEvent event) {
        if(key==KeyEvent.KEYCODE_SEARCH) {
            launch(new Intent(this,AiDiscoveryActivity.class));return true;
        }
        return super.onKeyDown(key,event);
    }
}
