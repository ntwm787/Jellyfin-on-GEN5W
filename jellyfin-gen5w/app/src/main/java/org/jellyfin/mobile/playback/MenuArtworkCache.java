package org.jellyfin.mobile.playback;

import android.content.Context;
import java.io.File;
import okhttp3.Cache;

/** One cache owner per process, also when browse fragments are recreated. */
public final class MenuArtworkCache {
    private static Cache cache;
    private MenuArtworkCache() {}
    public static synchronized Cache get(Context context) {
        if(cache==null)cache=new Cache(new File(context.getApplicationContext().getCacheDir(),"menu-artwork"),16L*1024*1024);
        return cache;
    }
}
