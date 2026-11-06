package org.jellyfin.mobile.playback;

import android.net.Uri;
import java.util.Locale;

/** Smaller browser artwork, independent of media stream resolution and bitrate. */
public final class MenuArtworkPolicy {
    private MenuArtworkPolicy() {}
    public static boolean isArtwork(Uri uri) {
        String path=uri.getPath();
        return path!=null && path.toLowerCase(Locale.ROOT).matches(
                ".*/(?:items|users)/[^/]+/images/(?:primary|thumb|logo|banner|backdrop)(?:/[0-9]+)?/?");
    }
    public static String requestUrl(String url) {
        Uri uri=Uri.parse(url);
        if(!isArtwork(uri))return url;
        Uri.Builder result=uri.buildUpon().clearQuery();
        int width=320,height=480,quality=65;
        for(String key:uri.getQueryParameterNames()) {
            String lower=key.toLowerCase(Locale.ROOT);
            if(lower.equals("maxwidth") || lower.equals("width") || lower.equals("fillwidth")) {
                width=smaller(width,uri.getQueryParameter(key));continue;
            }
            if(lower.equals("maxheight") || lower.equals("height") || lower.equals("fillheight")) {
                height=smaller(height,uri.getQueryParameter(key));continue;
            }
            if(lower.equals("quality")) {quality=smaller(quality,uri.getQueryParameter(key));continue;}
            // Minimum dimensions conflict with the upper bounds. Let the server preserve aspect ratio.
            if(lower.equals("minwidth") || lower.equals("minheight") || lower.equals("minscale"))continue;
            for(String value:uri.getQueryParameters(key))result.appendQueryParameter(key,value);
        }
        return result.appendQueryParameter("maxWidth",Integer.toString(width))
                .appendQueryParameter("maxHeight",Integer.toString(height))
                .appendQueryParameter("quality",Integer.toString(quality)).build().toString();
    }
    private static int smaller(int bound,String value) {
        try {int parsed=Integer.parseInt(value);return parsed>0?Math.min(bound,parsed):bound;}
        catch(Exception invalid){return bound;}
    }
}
