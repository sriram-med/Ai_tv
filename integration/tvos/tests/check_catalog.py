"""Execute real catalog loader against controlled HTTP failures and an offline cache."""
from pathlib import Path
import subprocess,tempfile,os
jar=os.environ.get('JSON_JAR','/tmp/ai-tv-check/json.jar')
assert Path(jar).is_file(),'Set JSON_JAR to org.json 20240303 jar'
stubs={
'android/content/Context.java':'''package android.content; public class Context { final java.io.File dir; public Context(java.io.File d){dir=d;} public java.io.File getCacheDir(){return dir;} }''',
'android/util/Log.java':'''package android.util; public class Log {public static int w(String t,String s){return 0;}}''',
'com/techpp/glass/Search.java':'''package com.techpp.glass; import java.util.*; class Search {
static class Apps {} static class Offer {}
static class Title {String title="",type="",year="",overview="",genres="",poster="",backdrop="",imdb="";double rating;List<Offer> offers=new ArrayList<>();}
static List<Title> titles(android.content.Context c,String q,Apps a)throws Exception{throw new java.io.IOException("primary search down");}
static void addStremio(android.content.Context c,Title t){}
}''',
'com/techpp/glass/CatalogRegression.java':r'''package com.techpp.glass;
import java.net.*;import java.io.*;import java.util.*;import java.nio.charset.StandardCharsets;
public class CatalogRegression {
static volatile boolean offline=false;static int calls=0;
static String fixture="{\"metas\":[{\"id\":\"tt1\",\"name\":\"Thriller one\",\"releaseInfo\":\"2025\",\"genres\":[\"Thriller\"],\"poster\":\"https://example.org/a.jpg\"},{\"id\":\"tt2\",\"name\":\"Family one\",\"genres\":[\"Family\"]},{\"id\":\"invalid\",\"name\":\"Not a film\"}]}";
static void check(boolean test,String message){if(!test)throw new AssertionError(message);}
public static void main(String[] args)throws Exception {
 Thread.setDefaultUncaughtExceptionHandler((thread,error)->{error.printStackTrace();System.exit(1);});
 URL.setURLStreamHandlerFactory(protocol->protocol.equals("https")?new URLStreamHandler(){protected URLConnection openConnection(URL u){return new HttpURLConnection(u){
 public void disconnect(){}public boolean usingProxy(){return false;}public void connect(){}
 public int getResponseCode(){synchronized(CatalogRegression.class){calls++;}return offline||u.getHost().startsWith("v3-")?403:200;}
 public InputStream getInputStream(){return new ByteArrayInputStream(fixture.getBytes(StandardCharsets.UTF_8));}
 };}}:null);
 File dir=new File(args[0]);dir.mkdirs();android.content.Context context=new android.content.Context(dir);
 AiCatalog.Result initial=AiCatalog.load(context,"","",new Search.Apps());
 check(initial.titles.size()==4,"No default results when Home providers are absent");
 check(initial.titles.stream().allMatch(t->t.offers.isEmpty()),"Invented provider offers");
 check(calls==4,"Primary-source failures did not fall back for both kinds");
 offline=true;int previous=calls;
 AiCatalog.Result mood=AiCatalog.load(context,"suspense","",new Search.Apps());
 check(mood.titles.size()==2,"Genre filtering failed");
 check(mood.titles.stream().allMatch(t->t.genres.contains("Thriller")),"Off-genre results");
 check(calls==previous,"Fresh cached catalog should work without network");
 for(File file:dir.listFiles())file.setLastModified(1);
 check(AiCatalog.load(context,"","",new Search.Apps()).titles.size()==4,"Offline stale cache failed");
 for(File file:dir.listFiles())file.delete();
 AiCatalog.Result failure=AiCatalog.load(context,"","",new Search.Apps());
 check(failure.titles.isEmpty()&&failure.message.contains("unavailable"),"Empty source falsely reported success");
 System.out.println("PASS: fallback metadata hosts, no-provider defaults, genre filters, fresh/stale offline cache and explicit empty-source failure");
 System.exit(0);
}}
'''
}
with tempfile.TemporaryDirectory() as tmp:
 files=[]
 for name,code in stubs.items():
  p=Path(tmp)/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(code);files.append(str(p))
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-cp',jar,'-d',tmp,
                 'integration/tvos/AiCatalog.java',*files],check=True)
 subprocess.run(['java','-cp',os.pathsep.join([tmp,jar]),'com.techpp.glass.CatalogRegression',tmp+'/cache'],check=True,timeout=30)
