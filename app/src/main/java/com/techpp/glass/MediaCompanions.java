package com.techpp.glass;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

/**
 * Compatibility bridge: launch independently installed media applications.
 * No code, player engine, login tokens or copyrighted streams are embedded.
 */
final class MediaCompanions {
    static final String MEMO = "com.lumora";
    static final String[] NUVIO = {"com.nuvio.tv","com.nuvio.app"};
    static final String MEMO_DOWNLOAD = "https://github.com/sriram-med/memo/releases";
    static final String NUVIO_DOWNLOAD = "https://github.com/NuvioMedia/NuvioTV/releases";
    private MediaCompanions(){}

    static String installedNuvio(Context context){
        for(String pkg:NUVIO)if(installed(context,pkg))return pkg;
        return null;
    }
    static boolean installed(Context context,String pkg){
        PackageManager pm=context.getPackageManager();
        return pm.getLeanbackLaunchIntentForPackage(pkg)!=null ||
               pm.getLaunchIntentForPackage(pkg)!=null;
    }
    static boolean launch(Context context,String pkg){
        PackageManager pm=context.getPackageManager();
        Intent i=pm.getLeanbackLaunchIntentForPackage(pkg);
        if(i==null)i=pm.getLaunchIntentForPackage(pkg);
        if(i==null)return false;
        try{
            if(!(context instanceof Activity))i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
            RecentApps.note(context,pkg);
            return true;
        }catch(ActivityNotFoundException|SecurityException ignored){return false;}
    }
    static boolean launchMemo(Context c){return launch(c,MEMO);}
    static boolean launchNuvio(Context c){
        String pkg=installedNuvio(c);return pkg!=null && launch(c,pkg);
    }
    static void openDownload(Context context,boolean memo){
        String url=memo?MEMO_DOWNLOAD:NUVIO_DOWNLOAD;
        try{
            Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse(url));
            if(!(context instanceof Activity))intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }catch(Exception ignored){}
    }
}
