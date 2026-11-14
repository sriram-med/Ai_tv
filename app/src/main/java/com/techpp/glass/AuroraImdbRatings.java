package com.techpp.glass;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optional IMDb score resolver: TMDB external IDs -> Agregarr proxy of IMDb noncommercial ratings dataset.
 * No guessed scores, HTML scraping or bundled API secrets. Cached for 7 days; missing key -> unknown score.
 */
final class AuroraImdbRatings {
    interface Listener { void onRating(Double score); }
    private static final long TTL=7L*24*60*60*1000;
    private static final ExecutorService work=Executors.newFixedThreadPool(2);
    private static final Handler main=new Handler(Looper.getMainLooper());
    private AuroraImdbRatings(){}

    static void lookup(Context context,Trending.Pick pick,Listener done){
        Context c=context.getApplicationContext();
        if(pick==null||pick.id<=0||(!"movie".equals(pick.kind)&&!"tv".equals(pick.kind))){
            main.post(()->done.onRating(null));return;
        }
        String id=pick.kind+":"+pick.id;
        SharedPreferences prefs=c.getSharedPreferences("aurora_imdb_ratings",Context.MODE_PRIVATE);
        if(System.currentTimeMillis()-prefs.getLong(id+":time",0)<TTL){
            float saved=prefs.getFloat(id+":score",-1);
            main.post(()->done.onRating(saved>=0?(double)saved:null));return;
        }
        work.execute(()->{
            Double rating=null;
            try {
                File root=c.getExternalFilesDir(null);
                if(root!=null){
                    String key;
                    try(InputStream in=new FileInputStream(new File(root,"tmdb_key.txt"))){
                        byte[] b=new byte[256];int n=in.read(b);
                        key=n>0?new String(b,0,n,StandardCharsets.UTF_8).trim():"";
                    }
                    if(!key.isEmpty() && key.length()<256) {
                        String path="movie".equals(pick.kind)?"movie":"tv";
                        JSONObject ids=new JSONObject(fetch("https://api.themoviedb.org/3/"+path+"/"+pick.id+
                            "/external_ids?api_key="+URLEncoder.encode(key,"UTF-8")));
                        String imdb=ids.optString("imdb_id","");
                        if(imdb.matches("tt[0-9]{7,10}")){
                            JSONArray scores=new JSONArray(fetch("https://api.agregarr.org/api/ratings?id="+imdb));
                            if(scores.length()>0) {
                                JSONObject value=scores.optJSONObject(0);
                                if(value!=null&&!value.isNull("rating")){
                                    double n=value.optDouble("rating",-1);
                                    if(n>=0&&n<=10)rating=n;
                                }
                            }
                        }
                    }
                }
            }catch(Exception ignored){}
            Double finalScore=rating;
            // Cache both known and missing scores to avoid repeated network calls.
            prefs.edit().putLong(id+":time",System.currentTimeMillis())
                .putFloat(id+":score",rating==null?-1:rating.floatValue()).apply();
            main.post(()->done.onRating(finalScore));
        });
    }
    private static String fetch(String urlText)throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(urlText).openConnection();
        try {
            connection.setConnectTimeout(4500);connection.setReadTimeout(4500);
            connection.setRequestProperty("User-Agent","Aurora-AI-TV/1.6");
            if(connection.getResponseCode()!=200)throw new Exception("http");
            try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] b=new byte[4096];int n;
                while((n=in.read(b))!=-1){if(out.size()+n>65536)throw new Exception("oversize");out.write(b,0,n);}
                return out.toString("UTF-8");
            }
        }finally {connection.disconnect();}
    }
}
