package com.techpp.glass;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Resolves a TMDB title to verified provider destinations; never synthesizes provider IDs. */
public final class StreamingSourceResolver {
    public interface Callback {
        void onSources(JSONArray sources);
        void onError(String message);
    }
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    public void resolve(String gateway,String token,int tmdbId,String kind,String region,Callback cb) {
        worker.execute(()->{
            HttpURLConnection conn=null;
            try {
                if(tmdbId<=0 || !("movie".equals(kind)||"tv".equals(kind)))
                    throw new IllegalArgumentException("Invalid TMDB identity");
                URL url=new URL(gateway+"/v1/sources?tmdbId="+tmdbId+"&kind="+kind+
                    "&region="+URLEncoder.encode(region,"UTF-8"));
                if(!"https".equalsIgnoreCase(url.getProtocol()))throw new IllegalArgumentException("HTTPS required");
                conn=(HttpURLConnection)url.openConnection();
                conn.setRequestProperty("Authorization","Bearer "+token);
                conn.setConnectTimeout(7000);conn.setReadTimeout(14000);
                if(conn.getResponseCode()!=200)throw new IllegalStateException("Source lookup unavailable");
                try(InputStream in=conn.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] bytes=new byte[2048];int n;
                    while((n=in.read(bytes))!=-1){if(out.size()+n>65536)throw new IllegalStateException("Response too large");out.write(bytes,0,n);}
                    cb.onSources(new JSONObject(out.toString("UTF-8")).getJSONArray("sources"));
                }
            } catch(Exception e){cb.onError("Title links unavailable; open the provider app instead.");}
            finally {if(conn!=null)conn.disconnect();}
        });
    }
    public static boolean open(Activity activity,JSONObject source,String expectedPackage) {
        for(String field:new String[]{"android_tv_url","android_url","web_url"}){
            String value=source.optString(field,"");
            if(!(value.startsWith("https://")||value.startsWith("http://")))continue;
            try {
                Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse(value));
                if(expectedPackage!=null&&!expectedPackage.isEmpty()){
                    Intent restricted=new Intent(intent).setPackage(expectedPackage);
                    if(restricted.resolveActivity(activity.getPackageManager())!=null){
                        activity.startActivity(restricted);return true;
                    }
                }
                if(intent.resolveActivity(activity.getPackageManager())!=null){
                    activity.startActivity(intent);return true;
                }
            }catch(Exception ignored){}
        }
        return false;
    }
}
