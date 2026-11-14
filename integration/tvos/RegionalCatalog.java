// Regional catalogues use Wikidata's P364 original-language claims (CC0 metadata).
package com.techpp.glass;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

final class RegionalCatalog {
    static final String[] CODES={"","te","hi","ta","ml","kn","bn","mr","pa","en"};
    static final String[] NAMES={"All languages","Telugu","Hindi","Tamil","Malayalam","Kannada","Bengali","Marathi","Punjabi","English"};
    static final String[] ENTITIES={"","Q8097","Q1568","Q5885","Q36236","Q33673","Q9610","Q1571","Q58635","Q1860"};
    private static final ExecutorService REFRESH=Executors.newFixedThreadPool(2);
    private static final Set<String> ACTIVE=Collections.newSetFromMap(new ConcurrentHashMap<>());
    static int index(String code){for(int i=0;i<CODES.length;i++)if(CODES[i].equals(code))return i;return 0;}
    static String name(String code){return NAMES[index(code)];}
    static List<Search.Title> catalog(Context c,String language,String kind) throws Exception {
        int index=index(language);if(index==0)throw new IllegalArgumentException("Unknown regional language");
        String key=language+"_"+kind;
        File cache=new File(c.getCacheDir(),"regional_v1_"+key+".json");
        List<Search.Title> saved=new ArrayList<>();
        try {saved=parse(AiCatalog.read(new FileInputStream(cache)),language,kind);}catch(Exception ignored){}
        if(!saved.isEmpty()&&System.currentTimeMillis()-cache.lastModified()<24*3600_000L)return saved;
        if(ACTIVE.add(key))REFRESH.execute(()->{
            try {
                String raw=fetch(language,kind);String normalized=normalize(raw,language).toString();
                if(!parse(normalized,language,kind).isEmpty()) {
                    File part=new File(cache.getPath()+".part");
                    try(OutputStream out=new FileOutputStream(part)){out.write(normalized.getBytes(StandardCharsets.UTF_8));}
                    part.renameTo(cache);
                }
            }catch(Exception e){android.util.Log.w("RegionalCatalog","Refresh unavailable; retaining saved catalogue");}
            finally{ACTIVE.remove(key);}
        });
        if(!saved.isEmpty())return saved;
        JSONObject bundle=new JSONObject(AiCatalog.read(new java.util.zip.GZIPInputStream(c.getAssets().open("regional_catalogs.json.gz"))));
        JSONObject data=bundle.getJSONObject("catalogs").optJSONObject(key);
        return data==null?new ArrayList<>():parse(data.toString(),language,kind);
    }
    static List<Search.Title> parse(String json,String language,String kind)throws Exception {
        JSONArray source=new JSONObject(json).optJSONArray("metas"),verified=new JSONArray();
        for(int i=0;source!=null&&i<source.length();i++) {
            JSONObject m=source.optJSONObject(i);
            if(m!=null&&language.equals(m.optString("originalLanguage")))verified.put(m);
        }
        return AiCatalog.parse(new JSONObject().put("metas",verified).toString(),kind);
    }
    static JSONObject normalize(String json,String language)throws Exception {
        JSONArray rows=new JSONObject(json).getJSONObject("results").getJSONArray("bindings");
        Map<String,JSONObject> titles=new LinkedHashMap<>();
        for(int i=0;i<rows.length();i++) {
            JSONObject row=rows.getJSONObject(i);String id=value(row,"imdb"),name=value(row,"filmLabel");
            if(!id.matches("tt[0-9]+")||name.isEmpty()||name.matches("Q[0-9]+"))continue;
            JSONObject m=titles.get(id);
            if(m==null) {
                m=new JSONObject().put("id",id).put("name",name).put("description",value(row,"filmDescription"))
                    .put("originalLanguage",language).put("languageSource","Wikidata P364")
                    .put("poster","https://images.metahub.space/poster/medium/"+id+"/img")
                    .put("background","https://images.metahub.space/background/medium/"+id+"/img")
                    .put("releaseInfo","").put("genres",new JSONArray());titles.put(id,m);
            }
            String date=value(row,"date");String year=date.length()>=4?date.substring(0,4):"";
            if(year.matches("[0-9]{4}")&&(m.optString("releaseInfo").isEmpty()||year.compareTo(m.optString("releaseInfo"))<0))m.put("releaseInfo",year);
            String genre=value(row,"genreLabel").replace(" film","").replace(" television series","");
            JSONArray genres=m.getJSONArray("genres");boolean found=false;
            for(int j=0;j<genres.length();j++)if(genre.equals(genres.optString(j)))found=true;
            if(!genre.isEmpty()&&!found)genres.put(genre);
        }
        JSONArray metas=new JSONArray();for(JSONObject m:titles.values())metas.put(m);
        return new JSONObject().put("metas",metas);
    }
    private static String value(JSONObject row,String key){JSONObject field=row.optJSONObject(key);return field==null?"":field.optString("value");}
    private static String fetch(String language,String kind)throws Exception {
        String cls=kind.equals("series")?"Q5398426":"Q11424";
        String query="SELECT ?film ?filmLabel ?filmDescription ?imdb ?date ?genreLabel WHERE { { SELECT DISTINCT ?film ?imdb ?links WHERE { ?film wdt:P31 wd:"+cls+"; wdt:P364 wd:"+ENTITIES[index(language)]+"; wdt:P345 ?imdb; wikibase:sitelinks ?links. } ORDER BY DESC(?links) LIMIT 40 } OPTIONAL { ?film wdt:P577 ?date. } OPTIONAL { ?film wdt:P136 ?genre. } SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\". } }";
        HttpURLConnection h=(HttpURLConnection)new URL("https://query.wikidata.org/sparql?format=json&query="+URLEncoder.encode(query,"UTF-8")).openConnection();
        h.setConnectTimeout(5000);h.setReadTimeout(18000);
        h.setRequestProperty("User-Agent","AI-TV/1.8.7 (https://github.com/sriram-med/Ai_tv)");h.setRequestProperty("Accept","application/sparql-results+json");
        try {if(h.getResponseCode()!=200)throw new IOException("Regional source unavailable");return AiCatalog.read(h.getInputStream());}finally{h.disconnect();}
    }
}
