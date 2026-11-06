package org.jellyfin.mobile.playback;

/** Conservative, independently testable decision. Unknown or stale means blocked. */
public final class StationaryRule {
    private StationaryRule() {}
    public static boolean allows(boolean known, int park, float speed, boolean moving,
                                 int restriction, long ageMs) {
        return known && park == 1 && speed == 0f && !moving && restriction == 0 &&
                ageMs >= 0 && ageMs <= 1000;
    }
}
