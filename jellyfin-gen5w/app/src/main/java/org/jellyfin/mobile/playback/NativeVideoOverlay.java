package org.jellyfin.mobile.playback;

import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import org.jellyfin.mobile.R;

/** Full-screen player inside MainActivity; the browsing WebView remains mounted underneath. */
public final class NativeVideoOverlay extends FrameLayout {
    public interface CloseListener { void onClose(boolean stopPlayback); }
    private final CloseListener close;
    private final NativePlaybackService service;
    private final PlayerView playerView;
    private final android.os.Handler handler=new android.os.Handler();
    private boolean released,closing,edgeSwipe,swipeHandled;
    private float touchX,touchY;
    private androidx.appcompat.app.AlertDialog trackDialog;
    private final Runnable safetyCheck=new Runnable(){@Override public void run(){
        if(released || closing)return;
        if(service==null || !service.hasActiveVideo()){dismiss(false);return;}
        if(!VehicleVideoGate.allowsVideo()){
            playerView.setVisibility(View.GONE);dismiss(true);return;
        }
        handler.postDelayed(this,250);
    }};
    private final Player.EventListener listener=new Player.EventListener(){
        @Override public void onPlaybackStateChanged(int state){if(state==Player.STATE_ENDED)dismiss(false);}
        @Override public void onPlayerError(com.google.android.exoplayer2.ExoPlaybackException error){dismiss(false);}
    };
    public NativeVideoOverlay(Context context,CloseListener close) {
        super(context);this.close=close;service=NativePlaybackService.instance;
        setBackgroundColor(android.graphics.Color.BLACK);setClickable(true);setFocusable(true);
        playerView=new PlayerView(context);addView(playerView,new FrameLayout.LayoutParams(-1,-1));
        if(service!=null){playerView.setPlayer(service.player);service.player.addListener(listener);}
        View audio=playerView.findViewById(R.id.audio_streams_button);
        View subtitles=playerView.findViewById(R.id.subtitles_button);
        if(audio!=null)audio.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View ignored){showTracks(false);}});
        if(subtitles!=null)subtitles.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View ignored){showTracks(true);}});
        Button back=new Button(context);back.setText(R.string.video_back_to_browse);
        back.setContentDescription(context.getString(R.string.video_back_to_browse_description));
        back.setTextColor(android.graphics.Color.WHITE);back.setTextSize(16);back.setAllCaps(false);
        back.setBackgroundColor(0xcc000000);
        back.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View ignored){dismiss(true);}});
        FrameLayout.LayoutParams layout=new FrameLayout.LayoutParams(dp(112),dp(48),Gravity.TOP|Gravity.LEFT);
        layout.setMargins(dp(8),dp(8),0,0);addView(back,layout);
    }
    private void showTracks(final boolean subtitles){
        if(service==null||released||closing)return;
        if(trackDialog!=null)trackDialog.dismiss();
        final long generation=service.getGeneration();
        final java.util.ArrayList<Integer> indices=new java.util.ArrayList<Integer>();
        java.util.ArrayList<String> labels=new java.util.ArrayList<String>();
        if(subtitles){indices.add(-1);labels.add(getContext().getString(R.string.video_subtitles_off));}
        org.json.JSONArray streams=service.getStreams();int selected=-1;
        for(int i=0;i<streams.length();i++){
            org.json.JSONObject stream=streams.optJSONObject(i);
            if(stream==null||!(subtitles?"Subtitle":"Audio").equalsIgnoreCase(stream.optString("Type")))continue;
            int index=stream.optInt("Index",-2);if(index<0)continue;
            indices.add(index);String label=stream.optString("DisplayTitle");
            if(label.isEmpty())label=(stream.optString("Language","")+" "+stream.optString("Codec","")).trim();
            labels.add(label.isEmpty()?Integer.toString(index):label);
        }
        if(indices.isEmpty()){
            android.widget.Toast.makeText(getContext(),R.string.video_track_change_unavailable,android.widget.Toast.LENGTH_SHORT).show();return;
        }
        for(int i=0;i<indices.size();i++)if(indices.get(i)==service.getSelectedTrack(subtitles))selected=i;
        // Keep the existing controller visible; these are its original track buttons.
        playerView.setControllerShowTimeoutMs(0);playerView.showController();
        trackDialog=new androidx.appcompat.app.AlertDialog.Builder(getContext())
            .setTitle(subtitles?R.string.video_subtitles:R.string.playback_info_audio_streams)
            .setSingleChoiceItems(labels.toArray(new String[labels.size()]),selected,new android.content.DialogInterface.OnClickListener(){
                @Override public void onClick(android.content.DialogInterface dialog,int which){
                    if(generation!=service.getGeneration()||!service.requestTrack(subtitles,indices.get(which)))
                        android.widget.Toast.makeText(getContext(),R.string.video_track_change_unavailable,android.widget.Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }})
            .setNegativeButton(android.R.string.cancel,null).create();
        trackDialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener(){@Override public void onDismiss(android.content.DialogInterface ignored){
            if(!released)playerView.setControllerShowTimeoutMs(5000);
        }});
        trackDialog.show();
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void dismiss(boolean stopPlayback){if(!closing&&!released){closing=true;close.onClose(stopPlayback);}}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();handler.post(safetyCheck);}
    @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}
    public void pause(){if(!released && service!=null)service.player.setPlayWhenReady(false);}
    public void release(){
        if(released)return;released=true;handler.removeCallbacksAndMessages(null);
        if(trackDialog!=null)trackDialog.dismiss();
        playerView.setPlayer(null);if(service!=null)service.player.removeListener(listener);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            touchX=event.getX();touchY=event.getY();edgeSwipe=event.getPointerCount()==1&&touchX>=0&&touchX<=dp(32);swipeHandled=false;
        }else if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_POINTER_UP)edgeSwipe=false;
        if(edgeSwipe&&(action==MotionEvent.ACTION_MOVE||action==MotionEvent.ACTION_UP)){
            float dx=event.getX()-touchX,dy=Math.abs(event.getY()-touchY);
            if(event.getPointerCount()!=1||dy>dp(40)||dx < -dp(16))edgeSwipe=false;
            else if(dx>=dp(96)&&dx>2*dy){
                edgeSwipe=false;swipeHandled=true;
                MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);cancel.recycle();dismiss(true);
            }
        }
        if(swipeHandled)return true;
        boolean handled=super.dispatchTouchEvent(event);if(action==MotionEvent.ACTION_UP)edgeSwipe=false;
        return handled||edgeSwipe;
    }
}
