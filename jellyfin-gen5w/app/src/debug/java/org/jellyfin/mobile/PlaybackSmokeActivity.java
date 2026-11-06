package org.jellyfin.mobile;

import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

/** Debug-only local decode check; does not need a server or credentials. */
public final class PlaybackSmokeActivity extends AppCompatActivity {
    private SimpleExoPlayer player;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        String profiles = org.jellyfin.mobile.player.ExoPlayerFormats.INSTANCE
                .getSupportedCodecs().toJSONString();
        Log.i("Gen5WPlayback", "CODEC_PROFILE_OK=" + profiles.length());
        PlayerView view = new PlayerView(this);
        setContentView(view);
        player = new SimpleExoPlayer.Builder(this).build();
        view.setPlayer(player);
        player.setVolume(0f);
        player.addAnalyticsListener(new AnalyticsListener() {
            @Override public void onVideoDecoderInitialized(EventTime time, String name, long duration) {
                Log.i("Gen5WPlayback", "VIDEO_DECODER=" + name);
            }
            @Override public void onAudioDecoderInitialized(EventTime time, String name, long duration) {
                Log.i("Gen5WPlayback", "AUDIO_DECODER=" + name);
            }
            @Override public void onRenderedFirstFrame(EventTime time, Surface surface) {
                Log.i("Gen5WPlayback", "FIRST_FRAME");
            }
            @Override public void onPlaybackStateChanged(EventTime time, int state) {
                Log.i("Gen5WPlayback", "STATE=" + state);
                if (state == Player.STATE_ENDED) Log.i("Gen5WPlayback", "ENDED");
            }
            @Override public void onPlayerError(EventTime time, ExoPlaybackException error) {
                Log.e("Gen5WPlayback", "FAILED", error);
            }
        });
        player.setMediaSource(new ProgressiveMediaSource.Factory(
                new DefaultDataSourceFactory(this, "Jellyfin-Gen5W-smoke"))
                .createMediaSource(MediaItem.fromUri("asset:///api19-smoke.mp4")));
        player.prepare();
        player.setPlayWhenReady(true);
    }

    @Override public void onDestroy() {
        if (player != null) player.release();
        super.onDestroy();
    }
}
