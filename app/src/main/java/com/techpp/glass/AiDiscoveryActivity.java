package com.techpp.glass;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

/** 10-foot, DPAD-first Metal UI discovery screen. */
public final class AiDiscoveryActivity extends Activity {
    private static final String GATEWAY = "https://ai-tv-2-gpt-gateway.onrender.com";
    private LinearLayout results;
    private EditText query;
    private EditText pairingCode;
    private TextView status;
    private final Map<String,Trending.Pick> catalogPicks = new LinkedHashMap<>();
    private final AiRecommendationClient client = new AiRecommendationClient();

    private GradientDrawable panel(int fill, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill); d.setCornerRadius(24); d.setStroke(2,stroke); return d;
    }
    private TextView label(String value, int size) {
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);
        t.setTextColor(Color.WHITE);t.setPadding(20,14,20,14);return t;
    }
    private void addAction(LinearLayout parent, String name, Runnable action) {
        TextView item=label(name,21);item.setFocusable(true);
        item.setBackground(panel(0xFF152038,0xFF3464B2));
        item.setOnFocusChangeListener((v,focused)->{
            v.setBackground(panel(focused?0xFF2456A5:0xFF152038,
                    focused?0xFF00E5FF:0xFF3464B2));
            v.animate().scaleX(focused?1.04f:1f).scaleY(focused?1.04f:1f).setDuration(170).start();
        });
        item.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=12;
        parent.addView(item,p);
    }
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(42,24,42,24);screen.setBackgroundColor(0xFF080B15);
        TextView heading=label("AI TV 2.0  /  DISCOVER",30);heading.setTypeface(null,Typeface.BOLD);
        heading.setTextColor(0xFF00E5FF);screen.addView(heading);
        screen.addView(label("Cinematic discovery  •  GPT-ranked picks  •  Privacy-first",16));
        query=new EditText(this);query.setSingleLine(true);query.setTextColor(Color.WHITE);
        query.setHintTextColor(0xFFB3C7E6);query.setHint("What would you like to watch?");
        query.setBackground(panel(0xFF17223C,0xFF005CFF));query.setPadding(24,18,24,18);
        screen.addView(query,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        screen.addView(actions);
        TextView search=label("SEARCH WITH GPT",19);search.setFocusable(true);
        search.setBackground(panel(0xFF005CFF,0xFF00E5FF));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-2,-2);sp.setMargins(0,18,0,18);
        actions.addView(search,sp);search.setOnClickListener(v->discover());
        search.setOnFocusChangeListener((v,f)->v.animate().scaleX(f?1.06f:1f).setDuration(140).start());
        pairingCode=new EditText(this);
        pairingCode.setSingleLine(true); pairingCode.setTextColor(Color.WHITE);
        pairingCode.setHintTextColor(0xFFB3C7E6);pairingCode.setHint("One-time pairing code");
        pairingCode.setBackground(panel(0xFF17223C,0xFF005CFF));
        pairingCode.setPadding(24,12,24,12);screen.addView(pairingCode);
        TextView pairButton=label("PAIR THIS TV",19);
        pairButton.setFocusable(true);pairButton.setBackground(panel(0xFF2456A5,0xFF00E5FF));
        screen.addView(pairButton);
        pairButton.setOnClickListener(v->{
            pairButton.setEnabled(false);
            new AiTvPairingClient(this).exchange(GATEWAY,pairingCode.getText().toString(),
                new AiTvPairingClient.Callback() {
                    @Override public void onPaired() {runOnUiThread(()->{
                        status.setText("TV paired successfully.");pairButton.setEnabled(true);pairingCode.setText("");
                    });}
                    @Override public void onError(String error) {runOnUiThread(()->{
                        status.setText(error);pairButton.setEnabled(true);
                    });}
                });
        });
        status=label("Connect a TV using a one-time pairing code to enable AI.",17);
        screen.addView(status);
        ScrollView scroll=new ScrollView(this);results=new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);scroll.addView(results);
        screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        addAction(results,"PAIRING STATUS",()->{
            AiTvPairingClient pairing=new AiTvPairingClient(this);
            status.setText(pairing.hasSession()?"TV paired. GPT ready when catalog is available.":"TV not paired.");
        });
        addAction(results,"RENEW SESSION",()->new AiTvPairingClient(this).refresh(GATEWAY,
            new AiTvPairingClient.Callback() {
                @Override public void onPaired(){runOnUiThread(()->status.setText("Session renewed."));}
                @Override public void onError(String message){runOnUiThread(()->status.setText(message));}
            }));
        addAction(results,"FORGET THIS TV SESSION",()->{
            new AiTvPairingClient(this).clearSession();
            status.setText("Local session cleared. Use server revocation to invalidate remote access.");
        });
        addAction(results,"ABOUT THIS BUILD",()->status.setText("Metal UI beta: verified catalog and GPT discovery."));
        setContentView(screen);
    }
    private void discover() {
        AiTvPairingClient pairing=new AiTvPairingClient(this);
        if (!pairing.hasSession()) {status.setText("Pair your TV first. Never enter an OpenAI API key here.");return;}
        JSONArray catalog=new JSONArray();
        catalogPicks.clear();
        for (Trending.Pick pick : new Trending(this).cached()) {
            if (pick.title == null || pick.title.isEmpty() || pick.pkg == null || pick.pkg.isEmpty()) continue;
            String id=pick.key();
            if (catalogPicks.containsKey(id)) continue;
            try {
                getPackageManager().getPackageInfo(pick.pkg,0);
                catalogPicks.put(id,pick);
                catalog.put(new JSONObject().put("id",id).put("title",pick.title)
                    .put("overview",pick.meta == null ? "" : pick.meta)
                    .put("providers",new JSONArray().put(pick.service == null ? "" : pick.service)));
            } catch (Exception ignored) { }
            if (catalog.length()>=40) break;
        }
        if (catalog.length()==0) {status.setText("No verified streaming titles cached yet. Refresh Trending on Home first.");return;}
        status.setText("Finding recommendations...");
        client.recommend(GATEWAY,pairing.currentToken(),query.getText().toString(),catalog,
            new AiRecommendationClient.Callback() {
                @Override public void onSuccess(JSONArray picks) {runOnUiThread(()->{
                    results.removeAllViews();status.setText("Recommendations");
                    for(int i=0;i<picks.length();i++) {
                        JSONObject item=picks.optJSONObject(i);
                        if(item!=null) {
                            Trending.Pick pick=catalogPicks.get(item.optString("id"));
                            if(pick!=null)addAction(results,pick.title+"  •  "+item.optString("reason"),
                                ()->showSources(pick));
                        }
                    }
                });}
                @Override public void onError(String reason) {runOnUiThread(()->status.setText(reason));}
            });
    }
    private void launchProvider(Trending.Pick pick) {
        try {
            android.content.Intent intent=getPackageManager().getLeanbackLaunchIntentForPackage(pick.pkg);
            if(intent==null)intent=getPackageManager().getLaunchIntentForPackage(pick.pkg);
            if(intent!=null)startActivity(intent);
            else status.setText("Streaming app is not installed.");
        }catch(Exception e){status.setText("Unable to launch provider.");}
    }
    private void showSources(Trending.Pick pick) {
        status.setText("Resolving exact title destinations...");
        new StreamingSourceResolver().resolve(GATEWAY,new AiTvPairingClient(this).currentToken(),
            pick.id,pick.kind,Region.country(AiDiscoveryActivity.this),new StreamingSourceResolver.Callback(){
                @Override public void onSources(JSONArray sources){runOnUiThread(()->{
                    results.removeAllViews();
                    status.setText(sources.length()==0?"No verified title links for this region. Data by Watchmode.com.":"Select a streaming source • Data by Watchmode.com");
                    for(int i=0;i<sources.length();i++){
                        JSONObject source=sources.optJSONObject(i);
                        if(source==null)continue;
                        addAction(results,"WATCH ON "+source.optString("name")+" ("+source.optString("type")+")",()->{
                            if(!StreamingSourceResolver.open(AiDiscoveryActivity.this,source,
                                source.optString("name").equalsIgnoreCase(pick.service)?pick.pkg:""))
                                status.setText("No supported exact-title link. Use provider app below.");
                        });
                    }
                    addAction(results,"OPEN "+pick.service+" APP (FALLBACK)",()->launchProvider(pick));
                });}
                @Override public void onError(String message){runOnUiThread(()->{
                    results.removeAllViews();status.setText(message);
                    addAction(results,"OPEN PROVIDER APP",()->launchProvider(pick));
                });}
            });
    }

}
