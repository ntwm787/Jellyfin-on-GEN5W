package org.jellyfin.mobile.playback;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.Log;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.source.*;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.*;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import org.jellyfin.mobile.MainActivity;
import org.jellyfin.mobile.R;
import org.json.JSONObject;
import org.json.JSONArray;

/** Native decoder, audio focus and foreground lifetime shared by music and video. */
public final class NativePlaybackService extends Service {
    public static volatile String snapshot="{\"state\":\"idle\",\"generation\":0}";
    public static NativePlaybackService instance;
    public SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private JSONArray streams=new JSONArray();
    private int audioStreamIndex=-1,subtitleStreamIndex=-1;
    private boolean audioApplied;
    private String playMethod="";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private MediaSessionCompat session;
    private long generation, actionRevision, durationHint;
    private String status="idle", request="", title="Jellyfin", error="";
    private boolean video, foreground;
    private boolean explicitlyStopped=true,videoFocus,videoFocusRequested;
    private long stoppedPosition,stoppedDuration;
    private android.media.AudioManager audioManager;
    private final android.media.AudioManager.OnAudioFocusChangeListener videoFocusListener=new android.media.AudioManager.OnAudioFocusChangeListener(){
        @Override public void onAudioFocusChange(int focus){
            if(focus==android.media.AudioManager.AUDIOFOCUS_GAIN){
                if(!explicitlyStopped&&video&&videoFocusRequested)videoFocus=true;
                return; // Never auto-resume on a system focus change.
            }
            if(focus==android.media.AudioManager.AUDIOFOCUS_LOSS || focus==android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
                if(focus==android.media.AudioManager.AUDIOFOCUS_LOSS)videoFocusRequested=false;
                videoFocus=false;if(video && player!=null){player.setPlayWhenReady(false);publish();}
                if(explicitlyStopped&&!videoFocusRequested)stopSelf();
            }
        }
    };
    private final BroadcastReceiver noisy=new BroadcastReceiver() {
        @Override public void onReceive(Context c,Intent i) { player.setPlayWhenReady(false); publish(); }
    };
    private final Runnable ticker=new Runnable() {
        @Override public void run() {
            if (("ready".equals(status)||"buffering".equals(status)) && !NetworkGate.allows(NativePlaybackService.this)) blockNetwork();
            if (player.getVideoFormat()!=null) video=true;
            if (video && !"error".equals(status) && !"stopped".equals(status) && !"ended".equals(status) && !VehicleVideoGate.allowsVideo()) blockVideo();
            publish(); handler.postDelayed(this,250);
        }
    };
    @Override public void onCreate() {
        super.onCreate(); instance=this;
        trackSelector=new DefaultTrackSelector(this);
        player=new SimpleExoPlayer.Builder(this).setTrackSelector(trackSelector).build();
        audioManager=(android.media.AudioManager)getSystemService(AUDIO_SERVICE);
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.CONTENT_TYPE_MUSIC).build(),false);
        player.setWakeMode(C.WAKE_MODE_NETWORK);
        player.addListener(new Player.EventListener() {
            @Override public void onTracksChanged(TrackGroupArray groups,com.google.android.exoplayer2.trackselection.TrackSelectionArray selections) {
                if(groups.length==0 || player.getCurrentTrackGroups().length==0)return;
                MappedTrackInfo info=trackSelector.getCurrentMappedTrackInfo();
                if(info!=null)for(int renderer=0;renderer<info.getRendererCount();renderer++){
                    if(info.getRendererType(renderer)==C.TRACK_TYPE_TEXT && !trackSelector.getParameters().getRendererDisabled(renderer))
                        trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(renderer,true));
                }
                applySelectedAudio();
            }
            @Override public void onPlaybackStateChanged(int state) {
                if(explicitlyStopped){publish();return;}
                if (state==Player.STATE_ENDED) { status="ended";session.setActive(false);releaseVideoFocusUnlessBrowsing();Log.i("Gen5WNative","ENDED generation="+generation); }
                else if (state==Player.STATE_READY) status="ready";
                else if (state==Player.STATE_BUFFERING) status="buffering";
                publish(); updateNotification();
            }
            @Override public void onPlayWhenReadyChanged(boolean ready,int reason) {
                if(ready && (explicitlyStopped || (video&&!acquireVideoFocus())))player.setPlayWhenReady(false);
                publish();updateNotification();
            }
            @Override public void onPlayerError(ExoPlaybackException e) {
                if(explicitlyStopped)return;
                status="error"; error=e.type+":"+e.getMessage(); Log.e("Gen5WNative","PLAYER_ERROR type="+e.type); publish(); updateNotification();
                session.setActive(false);releaseVideoFocusUnlessBrowsing();
            }
        });
        player.addAnalyticsListener(new AnalyticsListener() {
            @Override public void onAudioDecoderInitialized(EventTime time,String name,long elapsed) { Log.i("Gen5WNative","AUDIO_DECODER="+name); }
            @Override public void onVideoDecoderInitialized(EventTime time,String name,long elapsed) { Log.i("Gen5WNative","VIDEO_DECODER="+name); }
            @Override public void onRenderedFirstFrame(EventTime time,android.view.Surface surface) { Log.i("Gen5WNative","FIRST_FRAME"); }
            @Override public void onVideoSizeChanged(EventTime time,int width,int height,int rotation,float ratio) {
                Log.i("Gen5WNative","VIDEO_SIZE="+width+"x"+height);
            }
        });
        ComponentName mediaButtons=new ComponentName(this,NativeMediaButtonReceiver.class);
        PendingIntent mediaButtonIntent=PendingIntent.getBroadcast(this,0,
                new Intent(Intent.ACTION_MEDIA_BUTTON).setComponent(mediaButtons),PendingIntent.FLAG_UPDATE_CURRENT);
        session=new MediaSessionCompat(this,"Gen5WNativePlayback",mediaButtons,mediaButtonIntent);
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS|MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSessionCompat.Callback() {
            @Override public void onPlay() { if(!video)playIfAllowed(); }
            @Override public void onPause() { player.setPlayWhenReady(false); }
            @Override public void onStop() { stopPlayback(); }
            @Override public void onSeekTo(long pos) { player.seekTo(Math.max(0,pos)); }
            @Override public void onSkipToNext() { if(!explicitlyStopped&&!video){request="next"; actionRevision++; publish();} }
            @Override public void onSkipToPrevious() { if(!explicitlyStopped&&!video){request="previous"; actionRevision++; publish();} }
        });
        session.setActive(false);
        registerReceiver(noisy,new IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        if (Build.VERSION.SDK_INT>=26) ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(
                new NotificationChannel("gen5w-native","Jellyfin on GEN5W playback",NotificationManager.IMPORTANCE_LOW));
        handler.post(ticker);
    }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if (intent==null) return START_NOT_STICKY;
        String command=intent.getAction();
        if(Intent.ACTION_MEDIA_BUTTON.equals(command)) {
            if(explicitlyStopped || video)return START_NOT_STICKY;
            androidx.media.session.MediaButtonReceiver.handleIntent(session,intent);
            return START_NOT_STICKY;
        }
        double value=intent.getDoubleExtra("value",0);
        if ("load".equals(command)) {
            try { load(new JSONObject(intent.getStringExtra("options"))); }
            catch (Exception e) { status="error"; error="Invalid media source"; publish(); }
        } else if ("play".equals(command)) playIfAllowed();
        else if ("pause".equals(command)) player.setPlayWhenReady(false);
        else if ("seek".equals(command)) { player.seekTo(Math.max(0,(long)value)); Log.i("Gen5WNative","SEEK_MS="+(long)value); }
        else if ("volume".equals(command)) player.setVolume(Math.max(0,Math.min(1,(float)value/100)));
        else if ("next".equals(command)||"previous".equals(command)) { request=command; actionRevision++; }
        else if ("stop".equals(command)) stopPlayback();
        publish(); updateNotification();
        return START_NOT_STICKY;
    }
    private void load(JSONObject options) throws Exception {
        explicitlyStopped=false;
        generation=options.getLong("generation"); error=""; status="buffering";
        actionRevision=0; request="";
        streams=options.optJSONArray("streams");if(streams==null)streams=new JSONArray();
        audioStreamIndex=options.optInt("audioStreamIndex",-1);
        subtitleStreamIndex=options.optInt("subtitleStreamIndex",-1);
        playMethod=options.optString("playMethod");audioApplied=true;
        trackSelector.setParameters(trackSelector.buildUponParameters().clearSelectionOverrides().setDisabledTextTrackSelectionFlags(~0));
        video=options.optBoolean("video"); title=options.optString("title","Jellyfin");
        if (!NetworkGate.allows(this)) { blockNetwork(); return; }
        if (video && !VehicleVideoGate.allowsVideo()) { blockVideo(); return; }
        if(video){
            session.setActive(false);
            session.setPlaybackState(new PlaybackStateCompat.Builder().setActions(0).setState(PlaybackStateCompat.STATE_STOPPED,0,0).build());
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.CONTENT_TYPE_MOVIE).build(),false);
            if(!acquireVideoFocus()){status="error";error="Video audio focus is unavailable";publish();return;}
        }else{
            releaseVideoFocus();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.CONTENT_TYPE_MUSIC).build(),true);
        }
        durationHint=options.optLong("durationMs");
        String url=options.getString("url");
        DefaultDataSourceFactory data=new DefaultDataSourceFactory(this,
                new DefaultHttpDataSourceFactory("Jellyfin-Gen5W/"+org.jellyfin.mobile.BuildConfig.VERSION_NAME));
        MediaItem item=MediaItem.fromUri(url);
        MediaSource source=url.contains(".m3u8") ? new HlsMediaSource.Factory(data).createMediaSource(item) :
                new ProgressiveMediaSource.Factory(data,new DefaultExtractorsFactory().setMp3ExtractorFlags(
                        Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)).createMediaSource(item);
        player.setMediaSource(source,Math.max(0,options.optLong("positionMs")));
        audioApplied=false;
        player.prepare(); player.setPlayWhenReady(!options.optBoolean("startPaused"));
        if(!video){
            session.setMetadata(new MediaMetadataCompat.Builder().putString(MediaMetadataCompat.METADATA_KEY_TITLE,title)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,durationHint).build());
            session.setActive(true);
        }
        updateNotification();
        if (video) NativeVideoHost.show();
        Log.i("Gen5WNative","LOAD generation="+generation+" video="+video);
    }
    private void applySelectedAudio() {
        if(audioApplied || audioStreamIndex<0)return;
        MappedTrackInfo info=trackSelector.getCurrentMappedTrackInfo();if(info==null)return;
        int ordinal=0;boolean found=false;
        for(int i=0;i<streams.length();i++){
            JSONObject stream=streams.optJSONObject(i);if(stream==null || !"Audio".equalsIgnoreCase(stream.optString("Type")))continue;
            if(stream.optInt("Index",-1)==audioStreamIndex){found=true;break;}ordinal++;
        }
        if(!found)return;
        for(int renderer=0;renderer<info.getRendererCount();renderer++){
            if(info.getRendererType(renderer)!=C.TRACK_TYPE_AUDIO)continue;
            TrackGroupArray groups=info.getTrackGroups(renderer);int total=0;
            for(int group=0;group<groups.length;group++)total+=groups.get(group).length;
            if(total==0)return;
            // A transcoded/remuxed source may contain only the server-selected audio stream.
            int target=total==1?0:ordinal;
            if(target>=total){status="error";error="Selected audio track is unavailable";player.stop();publish();return;}
            for(int group=0;group<groups.length;group++){
                if(target<groups.get(group).length){
                    audioApplied=true;
                    trackSelector.setParameters(trackSelector.buildUponParameters().setSelectionOverride(renderer,groups,
                            new DefaultTrackSelector.SelectionOverride(group,target)));
                    return;
                }
                target-=groups.get(group).length;
            }
        }
    }
    public void stopPlayback() {
        if(!explicitlyStopped){
            stoppedPosition=player.getCurrentPosition();stoppedDuration=player.getDuration();
            if(stoppedDuration==C.TIME_UNSET)stoppedDuration=durationHint;
        }
        explicitlyStopped=true;status="stopped";request="";
        session.setActive(false);
        player.setPlayWhenReady(false);player.stop(true);player.clearMediaItems();publish();
        releaseVideoFocusUnlessBrowsing();
        NativeVideoHost.hide(false);
        stopForeground(true); foreground=false;
        final long stoppedGeneration=generation;
        handler.postDelayed(new Runnable(){@Override public void run(){
            if("stopped".equals(status)&&generation==stoppedGeneration&&!videoFocusRequested)stopSelf();
        }},3000);
    }
    private void playIfAllowed() {
        if(explicitlyStopped || !("ready".equals(status)||"buffering".equals(status)))return;
        if (!NetworkGate.allows(this)) { blockNetwork(); return; }
        if (video && !VehicleVideoGate.allowsVideo()) { blockVideo(); return; }
        if(video&&!acquireVideoFocus())return;
        player.setPlayWhenReady(true);
    }
    private boolean acquireVideoFocus(){
        if(explicitlyStopped)return false;
        if(videoFocus)return true;
        videoFocus=audioManager.requestAudioFocus(videoFocusListener,android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)==android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        if(videoFocus)videoFocusRequested=true;
        return videoFocus;
    }
    private void releaseVideoFocus(){if(videoFocusRequested){videoFocus=false;videoFocusRequested=false;audioManager.abandonAudioFocus(videoFocusListener);}}
    private void releaseVideoFocusUnlessBrowsing(){if(!NativeVideoHost.isForeground())releaseVideoFocus();}
    public void leaveForeground(){
        releaseVideoFocus();
        if(video&&!hasActiveVideo())stopSelf();
    }
    public JSONArray getStreams(){try{return new JSONArray(streams.toString());}catch(Exception ignored){return new JSONArray();}}
    public int getSelectedTrack(boolean subtitles){return subtitles?subtitleStreamIndex:audioStreamIndex;}
    public long getGeneration(){return generation;}
    public boolean requestTrack(boolean subtitles,int index){
        if(!hasActiveVideo()||!NativeVideoHost.isForeground()||!NetworkGate.allows(this)||!VehicleVideoGate.allowsVideo())return false;
        boolean valid=subtitles&&index==-1;
        for(int i=0;i<streams.length();i++){
            JSONObject stream=streams.optJSONObject(i);
            if(stream!=null && stream.optInt("Index",-2)==index && (subtitles?"Subtitle":"Audio").equalsIgnoreCase(stream.optString("Type")))valid=true;
        }
        if(!valid)return false;
        NativeVideoHost.selectTrack(subtitles?"subtitle":"audio",index);return true;
    }
    private void blockVideo() {
        player.setPlayWhenReady(false); player.stop(true);
        status="error"; error="Video requires confirmed stationary vehicle in Park";
        Log.i("Gen5WNative","VIDEO_BLOCKED generation="+generation);
        publish(); updateNotification();
        session.setActive(false);releaseVideoFocusUnlessBrowsing();
    }
    private void blockNetwork() {
        player.setPlayWhenReady(false); player.stop(true);
        status="error"; error=getString(R.string.gen5w_network_required);
        Log.i("Gen5WNative","NETWORK_BLOCKED generation="+generation);
        publish(); updateNotification();
        session.setActive(false);releaseVideoFocusUnlessBrowsing();
    }
    private void publish() {
        if (player==null) return;
        try {
            long duration=player.getDuration(); if (duration==C.TIME_UNSET) duration=durationHint;
            if(explicitlyStopped)duration=stoppedDuration;
            long position=explicitlyStopped?stoppedPosition:player.getCurrentPosition();
            JSONObject state=new JSONObject().put("generation",generation).put("state",status)
                    .put("position",position).put("duration",Math.max(0,duration))
                    .put("paused",!player.getPlayWhenReady()).put("seekable",player.isCurrentWindowSeekable())
                    .put("buffered",player.getBufferedPosition()).put("volume",Math.round(player.getVolume()*100))
                    .put("video",video).put("error",error).put("actionRevision",actionRevision).put("action",request)
                    .put("audioStreamIndex",audioStreamIndex).put("subtitleStreamIndex",subtitleStreamIndex);
            state.put("playMethod",playMethod);
            Format audio=player.getAudioFormat();
            if(audio!=null)state.put("audioLanguage",audio.language==null?"":audio.language);
            Format format=player.getVideoFormat();
            if(format!=null)state.put("videoWidth",format.width).put("videoHeight",format.height);
            snapshot=state.toString();
            if (session!=null && !video) {
                boolean terminal=explicitlyStopped||"idle".equals(status)||"ended".equals(status)||"error".equals(status);
                session.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setActions(terminal?0:PlaybackStateCompat.ACTION_PLAY|PlaybackStateCompat.ACTION_PAUSE|PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_STOP|
                            PlaybackStateCompat.ACTION_SEEK_TO|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                    .setState(terminal?PlaybackStateCompat.STATE_STOPPED:
                                    (player.getPlayWhenReady()?PlaybackStateCompat.STATE_PLAYING:PlaybackStateCompat.STATE_PAUSED),
                            position,!terminal&&player.getPlayWhenReady()?1:0).build());
            }
        } catch (Exception ignored) { /* JSONObject holds primitive values only. */ }
    }
    private void updateNotification() {
        if (session==null||"idle".equals(status)||"stopped".equals(status)) return;
        if("ended".equals(status)||"error".equals(status)){stopForeground(true);foreground=false;return;}
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT);
        if(video){
            startForeground(86,new NotificationCompat.Builder(this,"gen5w-native").setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle(title).setContentText("Jellyfin on GEN5W").setContentIntent(open).setOnlyAlertOnce(true).build());
            foreground=true;return;
        }
        NotificationCompat.Builder b=new NotificationCompat.Builder(this,"gen5w-native")
                .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(title).setContentText("Jellyfin on GEN5W")
                .setContentIntent(open).setOnlyAlertOnce(true).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(android.R.drawable.ic_media_previous,"Previous",action("previous",1))
                .addAction(player.getPlayWhenReady()?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,
                        player.getPlayWhenReady()?"Pause":"Play",action(player.getPlayWhenReady()?"pause":"play",2))
                .addAction(android.R.drawable.ic_media_next,"Next",action("next",3))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1,2));
        startForeground(86,b.build()); foreground=true;
    }
    private PendingIntent action(String name,int id) {
        return PendingIntent.getService(this,id,new Intent(this,NativePlaybackService.class).setAction(name),PendingIntent.FLAG_UPDATE_CURRENT);
    }
    public static boolean playing() {
        return instance!=null&&instance.player!=null&&instance.player.getPlayWhenReady()&&
                !"stopped".equals(instance.status)&&!"ended".equals(instance.status)&&!"idle".equals(instance.status);
    }
    public boolean hasActiveVideo() {
        return video && ("buffering".equals(status)||"ready".equals(status));
    }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onDestroy() {
        explicitlyStopped=true;session.setActive(false);releaseVideoFocus();
        NativeVideoHost.hide(false);
        handler.removeCallbacksAndMessages(null); unregisterReceiver(noisy);
        player.release(); session.release(); instance=null; snapshot="{\"state\":\"idle\",\"generation\":0}";
        super.onDestroy();
    }
}
