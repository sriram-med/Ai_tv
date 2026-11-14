package com.techpp.glass;
import java.util.Locale;
final class TvMediaPolicy {
    static final String NUVIO_PACKAGE="com.nuvio.tv";
    static String nuvioUri(String type,String id) {
        if(id==null||!id.matches("tt[0-9]+"))return "";
        return "nuvio://detail/"+("tv".equals(type)||"series".equals(type)?"series":"movie")+"/"+id;
    }
    static boolean ticketOffer(String monetization,String service,String tech,String url) {
        String mode=monetization==null?"":monetization.toUpperCase(Locale.ROOT);
        String text=((service==null?"":service)+" "+(tech==null?"":tech)+" "+(url==null?"":url)).toLowerCase(Locale.ROOT);
        return mode.equals("CINEMA")||mode.equals("THEATRICAL")||text.contains("bookmyshow")||text.contains("ticketnew")||text.contains("paytm.com/movies")||text.contains("district.in/movies");
    }
}
