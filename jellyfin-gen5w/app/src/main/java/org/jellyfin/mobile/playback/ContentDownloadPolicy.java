package org.jellyfin.mobile.playback;

import java.net.URI;
import java.util.Locale;

/** Distinguish explicit saves from the normal streams and artwork the app needs. */
public final class ContentDownloadPolicy {
    private ContentDownloadPolicy() {}
    public static boolean blocks(String address) {
        try {
            URI uri = new URI(address);
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            String query = uri.getQuery() == null ? "" : uri.getQuery().toLowerCase(Locale.ROOT);
            return path.matches(".*/(?:items|videos|audio)/[^/]+/(?:download|file)(?:/.*)?") ||
                    query.matches("(?:.*&)?download=(?:true|1)(?:&.*)?");
        } catch (Exception malformed) { return true; }
    }
}
