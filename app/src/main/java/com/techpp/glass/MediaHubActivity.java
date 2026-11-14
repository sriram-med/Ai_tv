package com.techpp.glass;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Aurora hub for explicitly installed, separately maintained personal-media companions. */
public final class MediaHubActivity extends Activity {
    private LinearLayout content;
    private TextView status;
    private int d(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable panel(int color){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);g.setCornerRadius(d(18));g.setStroke(d(1),0xFF426767);return g;
    }
    private TextView text(String value,int size,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextColor(0xFFF2F5EF);t.setTextSize(size);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private TextView button(String label,Runnable click){
        TextView t=text(label,16,true);t.setGravity(Gravity.CENTER);
        t.setPadding(d(17),d(13),d(17),d(13));
        t.setBackground(panel(0xDD263E43));t.setFocusable(true);
        t.setOnFocusChangeListener((v,on)->{
            v.setBackground(panel(on?0xFF3E6964:0xDD263E43));
            v.animate().cancel();v.animate().scaleX(on?1.025f:1f).scaleY(on?1.025f:1f)
                .setDuration(MotionPrefs.duration(this,160)).start();
        });
        t.setOnClickListener(v->click.run());return t;
    }
    private void row(String name,String description,boolean installed,Runnable launch,Runnable download){
        LinearLayout tile=new LinearLayout(this);tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setBackground(panel(0xDC1A3039));tile.setPadding(d(20),d(17),d(20),d(17));
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);
        TextView title=text(name+(installed?"  •  INSTALLED":"  •  NOT INSTALLED"),22,true);
        title.setTextColor(installed?0xFFA6E9D6:0xFFF0C28F);copy.addView(title);
        TextView desc=text(description,15,false);desc.setTextColor(0xFFB7D1CF);
        LinearLayout.LayoutParams descP=new LinearLayout.LayoutParams(-1,-2);descP.topMargin=d(7);
        copy.addView(desc,descP);
        tile.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        TextView action=button(installed?"OPEN":"RELEASES",installed?launch:download);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(d(152),d(54));ap.leftMargin=d(15);
        tile.addView(action,ap);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);
        rp.bottomMargin=d(15);content.addView(tile,rp);
    }
    private void refresh(){
        content.removeAllViews();
        row("MEMO  /  LUMORA",
            "Independent media client for IPTV, M3U, Xtream, Stalker, Jellyfin and Plex. Sign in and add your own sources inside Memo.",
            MediaCompanions.installed(this,MediaCompanions.MEMO),
            ()->{if(!MediaCompanions.launchMemo(this))status.setText("Unable to open Memo. Check whether it is installed.");},
            ()->MediaCompanions.openDownload(this,true));
        row("NUVIO TV",
            "Independent Android TV media browser. Configure your own legal sources, addons and account inside Nuvio.",
            MediaCompanions.installedNuvio(this)!=null,
            ()->{if(!MediaCompanions.launchNuvio(this))status.setText("Unable to open Nuvio TV. Check whether it is installed.");},
            ()->MediaCompanions.openDownload(this,false));
        TextView info=text("Compatibility mode  •  Companion apps remain separate and update independently. Aurora does not copy their playback engines, settings or private credentials.",15,false);
        info.setTextColor(0xFFA8C1C3);
        content.addView(info);
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        FrameLayout background=new FrameLayout(this);
        background.addView(new AuroraBackdrop(this),new FrameLayout.LayoutParams(-1,-1));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(d(48),d(30),d(48),d(30));
        background.addView(root,new FrameLayout.LayoutParams(-1,-1));setContentView(background);
        LinearLayout toolbar=new LinearLayout(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("AURORA  /  MEDIA COMPANIONS",26,true);title.setTextColor(0xFFA6E9D6);
        toolbar.addView(title,new LinearLayout.LayoutParams(0,d(60),1));
        toolbar.addView(button("REFRESH",this::refresh));
        TextView back=button("BACK",this::finish);
        LinearLayout.LayoutParams backP=new LinearLayout.LayoutParams(-2,-2);backP.leftMargin=d(13);
        toolbar.addView(back,backP);
        root.addView(toolbar);
        status=text("Connect your personal libraries through installed companion applications.",16,false);
        status.setTextColor(0xFFD7C7A3);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.bottomMargin=d(22);
        root.addView(status,sp);
        ScrollView scroll=new ScrollView(this);
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        refresh();
    }
}
