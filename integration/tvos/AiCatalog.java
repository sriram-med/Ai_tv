// AI TV independent recommendation catalog. Metadata does not imply streaming availability.
package com.techpp.glass;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

final class AiCatalog {
    private static final ExecutorService FETCH=Executors.newFixedThreadPool(2);
    private static final long FRESH=6*3600_000L;
    static final class Result {
        final List<Search.Title> titles;
        final String message;
        Result(List<Search.Title> titles,String message){this.titles=titles;this.message=message;}
    }
    static String genre(String prompt) {
        String q=(prompt==null?"":prompt).toLowerCase(Locale.ROOT);
        String[][] groups={{"Sci-Fi","science fiction","sci-fi","scifi","space"},
            {"Thriller","thriller","suspense"},{"Comedy","comedy","funny","laugh"},
            {"Family","family","children","kids"},{"Horror","horror","scary"},
            {"Romance","romance","romantic"},{"Action","action"},{"Drama","drama"},
            {"Animation","animation","animated"},{"Documentary","documentary"}};
        for(String[] group:groups)for(int i=1;i<group.length;i++)
            if(q.matches(".*\\b"+java.util.regex.Pattern.quote(group[i])+"\\b.*"))return group[0];
        return "";
    }
    static Result load(Context c,String prompt,String kind,Search.Apps apps) {
        List<Search.Title> all=new ArrayList<>();
        List<Future<List<Search.Title>>> futures=new ArrayList<>();
        for(String type:new String[]{"movie","series"}) {
            if("movie".equals(kind)&&type.equals("series") || "tv".equals(kind)&&type.equals("movie"))continue;
            futures.add(FETCH.submit(()->catalog(c,type)));
        }
        int unavailable=0;
        for(Future<List<Search.Title>> f:futures)try {all.addAll(f.get(28,TimeUnit.SECONDS));}
        catch(Exception e){f.cancel(true);unavailable++;android.util.Log.w("AiCatalog","Catalog unavailable: "+e.getClass().getSimpleName());}
        String category=genre(prompt);
        // Exact title lookup supplements the broad pool. Mood phrases are not sent as literal film titles.
        String q=prompt==null?"":prompt.trim();
        if(category.isEmpty()&&!q.isEmpty()&&!q.matches("(?i)(movies?|series|tonight|popular|binge.*)")) {
            try {all.addAll(0,Search.titles(c,q,apps));}
            catch(Exception e){android.util.Log.w("AiCatalog","Title lookup unavailable; using catalog");}
        }
        List<Search.Title> out=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Search.Title t:all) {
            if(t==null||t.title.isEmpty()||(!kind.isEmpty()&&!kind.equals(t.type)))continue;
            if(!category.isEmpty()&&!t.genres.toLowerCase(Locale.ROOT).contains(category.toLowerCase(Locale.ROOT)))continue;
            String id=t.type+":"+(t.imdb.isEmpty()?t.title.toLowerCase(Locale.ROOT)+t.year:t.imdb);
            if(seen.add(id)) {if(t.offers.isEmpty())Search.addStremio(c,t);out.add(t);}
        }
        String label=category.isEmpty()?"Popular catalog":category+" catalog";
        if(out.isEmpty())label=all.isEmpty()?"Catalog unavailable. Check the TV internet connection and select RETRY.":"No matching "+(category.isEmpty()?"titles":category)+" titles. Try another mood or browse all.";
        else if(unavailable>0)label+=" • One source unavailable";
        return new Result(out,label);
    }
    private static List<Search.Title> catalog(Context c,String kind) throws Exception {
        File cache=new File(c.getCacheDir(),"ai_catalog_v2_"+kind+".json");
        String saved=null;
        try {saved=read(new FileInputStream(cache));if(System.currentTimeMillis()-cache.lastModified()<FRESH){List<Search.Title> p=parse(saved,kind);if(!p.isEmpty())return p;}}
        catch(Exception ignored){}
        String[] urls={"https://v3-cinemeta.strem.io/catalog/"+kind+"/top/skip=0.json",
            "https://cinemeta-catalogs.strem.io/top/catalog/"+kind+"/top.json"};
        for(String url:urls)try {
            String json=get(url);List<Search.Title> parsed=parse(json,kind);
            if(parsed.isEmpty())continue;
            File part=new File(cache.getPath()+".part");
            try(OutputStream out=new FileOutputStream(part)){out.write(json.getBytes(StandardCharsets.UTF_8));}
            part.renameTo(cache);return parsed;
        }catch(Exception e){android.util.Log.w("AiCatalog","Metadata source failed: "+e.getClass().getSimpleName());}
        if(saved!=null){List<Search.Title> p=parse(saved,kind);if(!p.isEmpty())return p;}
        throw new IOException("No catalog or offline cache");
    }
    static List<Search.Title> parse(String json,String kind) throws Exception {
        JSONArray metas=new JSONObject(json).optJSONArray("metas");List<Search.Title> out=new ArrayList<>();
        for(int i=0;metas!=null&&i<metas.length()&&out.size()<80;i++) {
            JSONObject m=metas.optJSONObject(i);if(m==null)continue;
            String id=m.optString("id",m.optString("imdb_id"));String name=m.optString("name").trim();
            if(!id.matches("tt[0-9]+")||name.isEmpty())continue;
            Search.Title t=new Search.Title();t.imdb=id;t.title=name;t.type=kind.equals("series")?"tv":"movie";
            t.overview=m.optString("description");t.poster=m.optString("poster");t.backdrop=m.optString("background");
            String year=m.optString("releaseInfo",m.optString("year"));t.year=year.length()>=4?year.substring(0,4):"";
            t.rating=m.optDouble("imdbRating",0);if(Double.isNaN(t.rating)||t.rating<0||t.rating>10)t.rating=0;
            JSONArray genres=m.optJSONArray("genres");List<String> names=new ArrayList<>();
            for(int j=0;genres!=null&&j<genres.length();j++)names.add(genres.optString(j));
            t.genres=String.join(", ",names);out.add(t);
        }
        return out;
    }
    private static String get(String url) throws Exception {
        HttpURLConnection h=(HttpURLConnection)new URL(url).openConnection();
        h.setConnectTimeout(4000);h.setReadTimeout(7000);h.setRequestProperty("User-Agent","AI-TV/1.8.5");h.setRequestProperty("Accept","application/json");
        try {int status=h.getResponseCode();if(status!=200)throw new IOException("HTTP "+status);return read(h.getInputStream());}finally{h.disconnect();}
    }
    private static String read(InputStream input) throws Exception {
        try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1){if(out.size()+n>4*1024*1024)throw new IOException("Catalog too large");out.write(buffer,0,n);}
            return out.toString("UTF-8");
        }
    }
}
