package org.jellyfin.mobile.playback;

import java.lang.ref.WeakReference;
import org.jellyfin.mobile.MainActivity;

/** In-process video presentation. Never launches an activity or invokes a system resolver. */
public final class NativeVideoHost {
    private static WeakReference<MainActivity> foreground=new WeakReference<MainActivity>(null);
    private NativeVideoHost() {}
    public static boolean isForeground(){MainActivity activity=foreground.get();return activity!=null&&!activity.isFinishing();}
    public static void selectTrack(final String kind,final int index){
        final MainActivity activity=foreground.get();
        if(activity!=null)activity.runOnUiThread(new Runnable(){@Override public void run(){
            if(foreground.get()==activity)activity.selectNativeTrack(kind,index);
        }});
    }
    public static void resume(MainActivity activity) {
        foreground=new WeakReference<MainActivity>(activity);
        NativePlaybackService service=NativePlaybackService.instance;
        if(service!=null && service.hasActiveVideo())show();
    }
    public static void pause(MainActivity activity) {
        if(foreground.get()==activity)foreground.clear();
    }
    public static void show() {
        final MainActivity activity=foreground.get();
        if(activity==null || activity.isFinishing())return;
        activity.runOnUiThread(new Runnable(){@Override public void run(){
            if(foreground.get()==activity && !activity.isFinishing())activity.showNativeVideo();
        }});
    }
    public static void hide(final boolean stopPlayback) {
        final MainActivity activity=foreground.get();
        if(activity!=null)activity.runOnUiThread(new Runnable(){@Override public void run(){activity.hideNativeVideo(stopPlayback);}});
    }
}
