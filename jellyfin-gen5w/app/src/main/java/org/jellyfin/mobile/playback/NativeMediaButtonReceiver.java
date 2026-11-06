package org.jellyfin.mobile.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Explicit legacy receiver required by MediaSessionCompat on API 19. */
public final class NativeMediaButtonReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        if(Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            context.startService(new Intent(context,NativePlaybackService.class)
                    .setAction(Intent.ACTION_MEDIA_BUTTON).putExtras(intent));
        }
    }
}
