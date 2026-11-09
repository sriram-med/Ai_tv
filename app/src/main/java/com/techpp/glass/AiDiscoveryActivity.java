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
import org.json.JSONObject;

/** 10-foot, DPAD-first Metal UI discovery screen. */
public final class AiDiscoveryActivity extends Activity {
    private static final String GATEWAY = "https://ai-tv-2-gpt-gateway.onrender.com";
    private LinearLayout results;
    private EditText query;
    private TextView status;
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
        status=label("Connect a TV using a one-time pairing code to enable AI.",17);
        screen.addView(status);
        ScrollView scroll=new ScrollView(this);results=new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);scroll.addView(results);
        screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        addAction(results,"PAIRING STATUS",()->{
            AiTvPairingClient pairing=new AiTvPairingClient(this);
            status.setText(pairing.hasSession()?"TV paired. GPT ready when catalog is available.":"TV not paired.");
        });
        addAction(results,"ABOUT THIS BUILD",()->status.setText("Metal UI preview. Catalog integration is in progress."));
        setContentView(screen);
    }
    private void discover() {
        AiTvPairingClient pairing=new AiTvPairingClient(this);
        if (!pairing.hasSession()) {status.setText("Pair your TV first. Never enter an OpenAI API key here.");return;}
        // Catalog must come from verified providers; never invent titles to satisfy the AI.
        JSONArray catalog=new JSONArray();
        if (catalog.length()==0) {status.setText("Verified catalog adapter not yet connected.");return;}
        status.setText("Finding recommendations...");
        client.recommend(GATEWAY,pairing.currentToken(),query.getText().toString(),catalog,
            new AiRecommendationClient.Callback() {
                @Override public void onSuccess(JSONArray picks) {runOnUiThread(()->{
                    results.removeAllViews();status.setText("Recommendations");
                    for(int i=0;i<picks.length();i++) {
                        JSONObject item=picks.optJSONObject(i);
                        if(item!=null)addAction(results,item.optString("id")+" — "+item.optString("reason"),()->{});
                    }
                });}
                @Override public void onError(String reason) {runOnUiThread(()->status.setText(reason));}
            });
    }
}
