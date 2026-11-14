package com.techpp.glass;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Aurora Discovery: works offline with cached recommendations. GPT is optional, paired and server-side only. */
public final class AuroraDiscoveryActivity extends Activity {
    private static final String GATEWAY="https://ai-tv-2-gpt-gateway.onrender.com";
    private static final int PANEL=0xE523343B, MINT=0xFF9FE0CC, GOLD=0xFFE8BE83;
    private LinearLayout list;
    private TextView status;
    private EditText query;
    private final Map<String,Trending.Pick> lookup=new LinkedHashMap<>();
    private final AiRecommendationClient ai=new AiRecommendationClient();
    private int density(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int fill,int stroke) {
        GradientDrawable g=new GradientDrawable();g.setColor(fill);
        g.setCornerRadius(density(17));g.setStroke(density(1),stroke);return g;
    }
    private TextView text(String value,int size,boolean strong) {
        TextView t=new TextView(this);t.setText(value);t.setTextColor(Color.WHITE);
        t.setTextSize(size);if(strong)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private TextView button(String text,String hint,Runnable action) {
        TextView t=text(text,15,true);t.setTag(hint);
        t.setGravity(Gravity.CENTER);t.setPadding(density(14),density(11),density(14),density(11));
        t.setBackground(bg(PANEL,0xFF48636B));t.setFocusable(true);
        t.setOnFocusChangeListener((v,on)->{
            v.setBackground(bg(on?0xFF355D58:PANEL,on?MINT:0xFF48636B));
            v.animate().cancel();
            v.animate().scaleX(on?1.035f:1f).scaleY(on?1.035f:1f)
                .setDuration(MotionPrefs.duration(this,160)).start();
            if(on && status!=null && v.getTag() instanceof String)status.setText((String)v.getTag());
        });
        t.setOnClickListener(v->action.run());return t;
    }
    private void addChip(LinearLayout row,String label,String prompt) {
        TextView c=button(label,"Suggested search: "+prompt,()->{query.setText(prompt);search(false);});
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,density(47));
        p.rightMargin=density(9);row.addView(c,p);
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        FrameLayout frame=new FrameLayout(this);
        frame.addView(new AuroraBackdrop(this),new FrameLayout.LayoutParams(-1,-1));
        LinearLayout screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(density(45),density(25),density(45),density(24));
        frame.addView(screen,new FrameLayout.LayoutParams(-1,-1));setContentView(frame);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading=text("AURORA  /  DISCOVER",27,true);heading.setTextColor(MINT);
        bar.addView(heading,new LinearLayout.LayoutParams(0,density(52),1));
        bar.addView(button("BACK TO HOME","Return to your library",this::finish));
        screen.addView(bar);
        TextView hero=text("Find something worth watching.",30,true);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=density(14);
        screen.addView(hero,hp);
        TextView lead=text("Explore your available catalog. AI-enhanced ranking is optional; no API key is entered on the TV.",16,false);
        lead.setTextColor(0xFFC0D4D3);screen.addView(lead);
        LinearLayout searchRow=new LinearLayout(this);searchRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams sr=new LinearLayout.LayoutParams(-1,-2);sr.topMargin=density(19);screen.addView(searchRow,sr);
        query=new EditText(this);query.setSingleLine(true);query.setTextColor(Color.WHITE);
        query.setHintTextColor(0xFFB6CECE);query.setHint("Mood, genre, title, or actor...");
        query.setPadding(density(18),density(9),density(18),density(9));query.setBackground(bg(0xDD213740,0xFF4B7774));
        query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        query.setOnEditorActionListener((v,id,event)->{search(false);return true;});
        searchRow.addView(query,new LinearLayout.LayoutParams(0,density(55),1));
        TextView go=button("SEARCH","Search the available catalog",()->search(false));
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(density(124),density(55));gp.leftMargin=density(12);
        searchRow.addView(go,gp);
        TextView aiButton=button("AI RANK","Use paired AI gateway when available",()->search(true));
        LinearLayout.LayoutParams aip=new LinearLayout.LayoutParams(density(124),density(55));aip.leftMargin=density(12);
        searchRow.addView(aiButton,aip);
        HorizontalScrollView chipsScroll=new HorizontalScrollView(this);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips=new LinearLayout(this);chips.setOrientation(LinearLayout.HORIZONTAL);
        addChip(chips,"THRILLERS","thriller");
        addChip(chips,"COMEDY","comedy");
        addChip(chips,"SCI-FI","science fiction");
        addChip(chips,"ROMANCE","romance");
        addChip(chips,"FAMILY","family");
        addChip(chips,"ACTION","action");
        addChip(chips,"DRAMA","drama");
        chipsScroll.addView(chips);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,density(58));cp.topMargin=density(13);
        screen.addView(chipsScroll,cp);
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=text("RECOMMENDATIONS",19,true);
        row.addView(label,new LinearLayout.LayoutParams(0,density(48),1));
        row.addView(button("REFRESH CATALOG","Fetch available titles from streaming sources",this::refreshCatalog));
        TextView companions=button("MEMO / NUVIO","Open installed media companions",()->
                startActivity(new Intent(this,MediaHubActivity.class)));
        LinearLayout.LayoutParams companionsP=new LinearLayout.LayoutParams(-2,-2);
        companionsP.leftMargin=density(10);
        row.addView(companions,companionsP);
        TextView pair=button("PAIR AI","Optional secure device pairing",this::pairDialog);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-2,-2);pp.leftMargin=density(12);
        row.addView(pair,pp);
        LinearLayout.LayoutParams rowp=new LinearLayout.LayoutParams(-1,-2);
        rowp.topMargin=density(8);screen.addView(row,rowp);
        status=text("Loading available titles...",14,false);
        status.setTextColor(GOLD);screen.addView(status);
        ScrollView scroll=new ScrollView(this);
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1);
        lp.topMargin=density(8);screen.addView(scroll,lp);
        search(false);
        new Trending(this).refreshIfStale(picks ->{
            if(!isFinishing() && query!=null && list!=null && list.getChildCount()<=14) search(false);
        });
        query.requestFocus();
    }
    private void refreshCatalog() {
        status.setText("Refreshing streaming titles. This requires an internet connection and a supported catalog source.");
        new Trending(this).refreshIfStale(picks ->{
            if(isFinishing())return;
            if(picks.isEmpty())status.setText("No catalog returned. Local app browsing is still available.");
            else search(false);
        });
    }
    private void installedProviders() {
        java.util.Set<String> seen=new java.util.HashSet<>();
        android.content.pm.PackageManager pm=getPackageManager();
        Intent tv=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<android.content.pm.ResolveInfo> installed=pm.queryIntentActivities(tv,0);
        int shown=0;
        for(android.content.pm.ResolveInfo app:installed){
            String pkg=app.activityInfo.packageName;
            if(pkg.equals(getPackageName())||!seen.add(pkg))continue;
            String name=app.loadLabel(pm).toString();
            String lower=name.toLowerCase(Locale.ROOT);
            if(!lower.matches(".*(netflix|prime|youtube|hotstar|zee5|sony|disney|hulu|apple tv|crunchyroll|mubi|mx player|stream|video|plex|jio|tubi|pluto|stremio).*"))continue;
            TextView provider=button("OPEN "+name.toUpperCase(Locale.ROOT),
                    "Browse "+name+" directly",()->{
                        Intent intent=pm.getLeanbackLaunchIntentForPackage(pkg);
                        if(intent==null)intent=pm.getLaunchIntentForPackage(pkg);
                        if(intent!=null)startActivity(intent);
                    });
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,density(56));
            p.bottomMargin=density(9);list.addView(provider,p);
            if(++shown>=12)break;
        }
        if(shown==0)list.addView(text("No streaming applications detected. Install a streaming service or refresh the catalog.",17,false));
        TextView companionHub=button("OPEN MEDIA COMPANIONS","Memo and Nuvio TV library setup",()->
                startActivity(new Intent(this,MediaHubActivity.class)));
        LinearLayout.LayoutParams hubP=new LinearLayout.LayoutParams(-1,density(55));
        hubP.topMargin=density(11);list.addView(companionHub,hubP);
    }
    private void pairDialog() {
        AiTvPairingClient session=new AiTvPairingClient(this);
        if(session.hasSession()){
            new AlertDialog.Builder(this).setTitle("Secure AI gateway")
                .setMessage("This TV has a paired session. AI ranking is available when the gateway and streaming catalog are online.")
                .setPositiveButton("Refresh session",(d,w)->session.refresh(GATEWAY,new AiTvPairingClient.Callback(){
                    public void onPaired(){runOnUiThread(()->status.setText("AI session refreshed."));}
                    public void onError(String reason){runOnUiThread(()->status.setText(reason));}
                })).setNeutralButton("Disconnect",(d,w)->{session.clearSession();status.setText("Device session cleared.");})
                .setNegativeButton("Cancel",null).show();return;
        }
        EditText code=new EditText(this);code.setSingleLine(true);
        code.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        code.setHint("10-character pairing code");
        new AlertDialog.Builder(this).setTitle("Connect optional AI gateway")
            .setMessage("Enter the one-time pairing code issued by your gateway administrator. Never paste an OpenAI API key into the TV.")
            .setView(code).setPositiveButton("Pair",(d,w)->{
                status.setText("Pairing...");
                session.exchange(GATEWAY,code.getText().toString().trim(),new AiTvPairingClient.Callback(){
                    public void onPaired(){runOnUiThread(()->status.setText("TV paired. Select AI RANK for GPT ranking."));}
                    public void onError(String reason){runOnUiThread(()->status.setText(reason));}
                });
            }).setNegativeButton("Cancel",null).show();
    }
    private void search(boolean useAi) {
        list.removeAllViews();lookup.clear();
        List<Trending.Pick> all=new ArrayList<>();
        String q=query.getText().toString().trim().toLowerCase(Locale.ROOT);
        try {
            for(Trending.Pick pick:new Trending(this).cached()) {
                if(pick==null||pick.title==null||pick.title.trim().isEmpty())continue;
                String searchable=(pick.title+" "+pick.meta+" "+pick.service).toLowerCase(Locale.ROOT);
                if(!q.isEmpty()&&!searchable.contains(q))continue;
                String id=pick.key();if(lookup.containsKey(id))continue;
                lookup.put(id,pick);all.add(pick);
                if(all.size()>=70)break;
            }
        } catch(Exception ignored){}
        if(all.isEmpty()){
            status.setText("No matching titles are cached. You can still open installed streaming apps from Home. AI ranking requires a populated catalog.");
            TextView empty=text("No catalog matches yet. Try a different term or return to Home to refresh recommendations.",19,false);
            empty.setPadding(density(22),density(26),density(22),density(26));
            empty.setBackground(bg(PANEL,0xFF48636B));list.addView(empty);
            installedProviders();
            return;
        }
        status.setText(all.size()+" catalog results"+(useAi?"  •  Checking AI availability":"  •  Available offline"));
        for(Trending.Pick p:all)result(p,null);
        if(!useAi)return;
        AiTvPairingClient pairing=new AiTvPairingClient(this);
        if(!pairing.hasSession()){status.setText("Showing local results. For optional AI ranking, select PAIR AI and enter a one-time device code.");return;}
        JSONArray catalog=new JSONArray();
        for(Trending.Pick pick:all) {
            try{catalog.put(new JSONObject().put("id",pick.key()).put("title",pick.title)
                .put("overview",pick.meta==null?"":pick.meta)
                .put("providers",new JSONArray().put(pick.service==null?"":pick.service)));}
            catch(Exception ignored){}
            if(catalog.length()>=40)break;
        }
        ai.recommend(GATEWAY,pairing.currentToken(),query.getText().toString(),catalog,new AiRecommendationClient.Callback(){
            public void onSuccess(JSONArray picks){runOnUiThread(()->{
                list.removeAllViews();
                status.setText("AI-ranked recommendations  •  "+picks.length()+" picks");
                for(int i=0;i<picks.length();i++){
                    JSONObject item=picks.optJSONObject(i);
                    if(item==null)continue;
                    Trending.Pick pick=lookup.get(item.optString("id"));
                    if(pick!=null)result(pick,item.optString("reason"));
                }
            });}
            public void onError(String reason){runOnUiThread(()->status.setText("Local results remain available. AI gateway: "+reason));}
        });
    }
    private void result(Trending.Pick pick,String reason) {
        LinearLayout item=new LinearLayout(this);item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(density(22),density(12),density(22),density(12));
        item.setBackground(bg(PANEL,0xFF415F64));
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);
        TextView name=text(pick.title,19,true);copy.addView(name);
        TextView info=text(reason!=null&&!reason.isEmpty()?reason:(pick.meta==null?"":pick.meta),14,false);
        info.setTextColor(0xFFAFCAC7);info.setMaxLines(2);copy.addView(info);
        item.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        TextView launch=button("OPEN PROVIDER","Open streaming service",()->openProvider(pick));
        item.addView(launch,new LinearLayout.LayoutParams(density(160),density(49)));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin=density(10);list.addView(item,p);
    }
    private void openProvider(Trending.Pick pick){
        if(pick.pkg==null||pick.pkg.isEmpty()){status.setText("Provider not available for this title.");return;}
        try{
            Intent intent=getPackageManager().getLeanbackLaunchIntentForPackage(pick.pkg);
            if(intent==null)intent=getPackageManager().getLaunchIntentForPackage(pick.pkg);
            if(intent!=null)startActivity(intent);
            else status.setText("Provider app not installed.");
        }catch(Exception ex){status.setText("Unable to open provider.");}
    }
    @Override public boolean onKeyDown(int key,KeyEvent event){
        if(key==KeyEvent.KEYCODE_SEARCH){query.requestFocus();return true;}
        return super.onKeyDown(key,event);
    }
}
