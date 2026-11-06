package org.jellyfin.mobile.playback;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import dalvik.system.PathClassLoader;
import org.json.JSONObject;

/** Read-only factory-service queries. No broadcasts, overrides or guessed defaults. */
public final class VehicleVideoGate {
    private static volatile Reading reading = new Reading();
    private static Handler worker;
    private static ClassLoader carLoader, hcanLoader;
    private static final class Reading {
        boolean known, moving;
        int park, restriction;
        float speed;
        long at;
    }
    private VehicleVideoGate() {}
    public static synchronized void start() {
        if (worker != null) return;
        HandlerThread thread = new HandlerThread("Gen5W-vehicle-read");
        thread.start();
        worker = new Handler(thread.getLooper());
        worker.post(new Runnable() {
            @Override public void run() {
                Reading next = new Reading();
                try {
                    if (carLoader == null) carLoader = new PathClassLoader(
                            "/system/framework/automotive.jar", VehicleVideoGate.class.getClassLoader());
                    if (hcanLoader == null) hcanLoader = new PathClassLoader(
                            "/system/framework/com.hkmc.ext.hcan.jar", carLoader);
                    Object system = service(carLoader, "com.hkmc.Service", "SYSTEM_SERVICE",
                            "com.hkmc.micom.ISystemService");
                    Object hscan = service(hcanLoader, "com.hkmc.telematics.hscan.tm_interface.TM_HSCANAdapter",
                            "TM_HSCAN_SERVICE", "com.hkmc.telematics.hscan.tm_interface.ITM_HSCANService");
                    Object regulation = service(carLoader, "com.hkmc.Service", "REGULATION_SERVICE",
                            "com.hkmc.regulation.IRegulationManager");
                    next.park = ((Integer) call(carLoader, "com.hkmc.micom.ISystemService", system, "getGearPStatus")).intValue();
                    Class<?> hscanType = hcanLoader.loadClass("com.hkmc.telematics.hscan.tm_interface.ITM_HSCANService");
                    next.speed = ((Float) hscanType.getMethod("getCarSpeed", byte.class).invoke(hscan, (byte) 0)).floatValue();
                    next.moving = ((Boolean) hscanType.getMethod("IsCarMoving").invoke(hscan)).booleanValue();
                    next.restriction = ((Integer) call(carLoader, "com.hkmc.regulation.IRegulationManager", regulation, "getStatus")).intValue();
                    next.known = true;
                    next.at = SystemClock.elapsedRealtime();
                } catch (Exception unavailable) {
                    // Missing services, permissions and malformed readings all fail closed.
                } catch (LinkageError incompatible) {
                    // A different firmware API must never be treated as stationary.
                }
                reading = next;
                worker.postDelayed(this, 250);
            }
        });
    }
    private static Object service(ClassLoader loader, String constants, String field, String api) throws Exception {
        String name = (String) loader.loadClass(constants).getField(field).get(null);
        IBinder binder = (IBinder) Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class).invoke(null, name);
        if (binder == null || !binder.isBinderAlive()) throw new IllegalStateException("Vehicle service unavailable");
        return loader.loadClass(api + "$Stub").getMethod("asInterface", IBinder.class).invoke(null, binder);
    }
    private static Object call(ClassLoader loader, String api, Object target, String method) throws Exception {
        return loader.loadClass(api).getMethod(method).invoke(target);
    }
    public static boolean allowsVideo() {
        start();
        Reading value = reading;
        return StationaryRule.allows(value.known, value.park, value.speed, value.moving,
                value.restriction, SystemClock.elapsedRealtime() - value.at);
    }
    public static String policy() {
        try {
            return new JSONObject().put("allowed", allowsVideo()).put("message",
                    "Video is available only when the car is confirmed stationary in Park.").toString();
        } catch (Exception impossible) { return "{\"allowed\":false}"; }
    }
}
