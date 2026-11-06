package org.jellyfin.mobile.playback;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

/** Keep the web client's playlist controller running while the service plays. */
public final class PlaybackWebView extends WebView {
    public PlaybackWebView(Context context,AttributeSet attrs){super(context,attrs);}
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(NativePlaybackService.playing()?View.VISIBLE:visibility);
    }
}
