package org.jellyfin.mobile.playback;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import org.json.JSONObject;

/** Small command boundary. ExoPlayer itself always runs on the main thread. */
public final class NativePlaybackBridge {
    private final Context context;
    private final Uri server;
    public NativePlaybackBridge(Context context, String server) {
        this.context=context.getApplicationContext(); this.server=Uri.parse(server);
        VehicleVideoGate.start();
    }
    @JavascriptInterface public void load(String json) {
        try {
            JSONObject options=new JSONObject(json);
            Uri uri=Uri.parse(options.getString("url"));
            if (!server.getScheme().equals(uri.getScheme()) || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) ||
                    !server.getHost().equalsIgnoreCase(uri.getHost()) || port(server)!=port(uri)) {
                throw new IllegalArgumentException("Playback URL must use the configured Jellyfin origin");
            }
            context.startService(new Intent(context,NativePlaybackService.class).setAction("load").putExtra("options",json));
        } catch (Exception error) { throw new IllegalArgumentException("Invalid native playback request",error); }
    }
    private static int port(Uri uri) { return uri.getPort()!=-1?uri.getPort():("https".equals(uri.getScheme())?443:80); }
    @JavascriptInterface public void command(String command, double value) {
        if (!command.matches("play|pause|seek|stop|volume|next|previous")) return;
        context.startService(new Intent(context,NativePlaybackService.class).setAction(command).putExtra("value",value));
    }
    @JavascriptInterface public String state() { return NativePlaybackService.snapshot; }
    @JavascriptInterface public String videoPolicy() { return VehicleVideoGate.policy(); }
}
