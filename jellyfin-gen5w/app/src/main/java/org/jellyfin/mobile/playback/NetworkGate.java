package org.jellyfin.mobile.playback;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Process;
import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;

/** A connected Wi-Fi client and a fresh, root-owned network guard are required. */
public final class NetworkGate {
    private static final String BOOT_ID=readBootId();
    private NetworkGate() {}
    private static String readBootId() {
        try {
            BufferedReader reader=new BufferedReader(new FileReader("/proc/sys/kernel/random/boot_id"));
            try { return reader.readLine().trim(); } finally { reader.close(); }
        } catch(Exception unavailable) { return ""; }
    }
    public static boolean allows(Context context) {
        try {
            ConnectivityManager manager=(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info=manager.getActiveNetworkInfo();
            if(info==null || !info.isConnected() || info.getType()!=ConnectivityManager.TYPE_WIFI) return false;
            Properties state=new Properties();
            BufferedReader reader=new BufferedReader(new FileReader("/data/local/jellyfin-gen5w-network/state"));
            try { state.load(reader); } finally { reader.close(); }
            return NetworkStateRule.allows(state,Process.myUid(),SystemClock.elapsedRealtime(),BOOT_ID);
        } catch(Exception unavailable) { return false; }
    }
}
