package com.techpp.glass;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;

/** Original cinematic poster card; uses images already cached by Trending. */
final class AuroraPosterCard extends LinearLayout {
    private final Trending.Pick pick;
    private final TextView score;
    private final int dp(float v) { return (int)(getResources().getDisplayMetrics().density*v+.5f); }
    private GradientDrawable surface(int color,int border) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(18));d.setStroke(dp(2),border);return d;
    }
    AuroraPosterCard(Context context,Trending.Pick p,int widthDp,int heightDp,Runnable open) {
        super(context);pick=p;setOrientation(VERTICAL);setFocusable(true);setClickable(true);
        setPadding(dp(7),dp(7),dp(7),dp(7));
        setBackground(surface(0xE91A2B31,0x554A7474));
        FrameLayout posterArea=new FrameLayout(context);
        ImageView art=new ImageView(context);art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        File file=p.poster!=null && p.poster.isFile()?p.poster:p.image;
        if(file!=null && file.isFile()){
            BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
            BitmapFactory.decodeFile(file.getAbsolutePath(),bounds);
            int sample=1;while(bounds.outWidth/sample>widthDp*3 || bounds.outHeight/sample>heightDp*3)sample*=2;
            BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=Math.max(1,sample);
            try{Bitmap bitmap=BitmapFactory.decodeFile(file.getAbsolutePath(),opts);if(bitmap!=null)art.setImageBitmap(bitmap);}
            catch(Exception ignored){}
        }
        posterArea.addView(art,new FrameLayout.LayoutParams(-1,-1));
        TextView type=new TextView(context);type.setText("tv".equals(p.kind)?" SERIES ":" MOVIE ");
        type.setTextColor(0xFFF2F8F5);type.setTextSize(11);type.setTypeface(null,Typeface.BOLD);
        type.setBackground(surface(0xD308191C,0x22416462));
        type.setPadding(dp(7),dp(5),dp(7),dp(5));
        FrameLayout.LayoutParams typeP=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START);
        typeP.setMargins(dp(9),dp(9),0,0);posterArea.addView(type,typeP);
        score=new TextView(context);score.setText("IMDb  —");score.setTextColor(0xFFFFD99A);
        score.setTextSize(12);score.setTypeface(null,Typeface.BOLD);
        score.setPadding(dp(8),dp(5),dp(8),dp(5));score.setBackground(surface(0xDE0E191D,0x33687B78));
        FrameLayout.LayoutParams badgeP=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.END);
        badgeP.setMargins(0,0,dp(8),dp(8));posterArea.addView(score,badgeP);
        LinearLayout.LayoutParams areaP=new LinearLayout.LayoutParams(-1,0,1);
        addView(posterArea,areaP);
        TextView name=new TextView(context);name.setText(p.title);
        name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setTextColor(Color.WHITE);name.setTypeface(null,Typeface.BOLD);name.setTextSize(15);
        LinearLayout.LayoutParams nameP=new LinearLayout.LayoutParams(-1,dp(30));nameP.topMargin=dp(8);
        addView(name,nameP);
        TextView provider=new TextView(context);provider.setText(p.service==null?"Explore title":p.service);
        provider.setSingleLine(true);provider.setTextSize(12);provider.setTextColor(0xFFACC4C1);
        addView(provider,new LinearLayout.LayoutParams(-1,dp(23)));
        setOnClickListener(v->open.run());
        setOnFocusChangeListener((v,focused)->{
            v.animate().cancel();setBackground(surface(focused?0xFF324D4D:0xE91A2B31,focused?0xFF99E7D3:0x554A7474));
            v.animate().scaleX(focused?1.045f:1f).scaleY(focused?1.045f:1f)
                .translationZ(focused?dp(18):0).setDuration(MotionPrefs.duration(context,190))
                .setInterpolator(new DecelerateInterpolator(1.5f)).start();
        });
        setTag(p.title);
        // IMDb badge stays blank unless an external IMDb-rating lookup succeeds.
    }
    void setImdb(Double value) {
        if(value!=null && value>=0 && value<=10)score.setText(String.format(java.util.Locale.US,"IMDb  %.1f",value));
        else score.setText("IMDb  —");
    }
}
