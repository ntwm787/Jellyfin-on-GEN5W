#!/system/bin/sh
# Read-only collection to the USB working directory; leaves playback running.
export PATH=/system/bin:/system/xbin:/sbin:/vendor/bin
export LD_LIBRARY_PATH=/vendor/lib:/system/lib
umask 077
BASE="$(pwd)/Jellyfin-logs"
mkdir -p "$BASE" || exit 1
N=1
while [ -e "$BASE/run-$N" ]; do
    N=$((N + 1))
done
OUT="$BASE/run-$N"
mkdir "$OUT" || exit 1
{
    echo "Jellyfin Gen5W native playback report"
    echo "COLLECTOR_VERSION=2.1.1-gen5w.17"
    echo "SDK=$(getprop ro.build.version.sdk)"
    echo "ANDROID=$(getprop ro.build.version.release)"
    echo "ABI=$(getprop ro.product.cpu.abi)"
    echo "DEVICE=$(getprop ro.product.device)"
    echo "BUILD=$(getprop ro.build.display.id)"
    echo "BOOT_COMPLETED=$(getprop sys.boot_completed)"
    echo "APK_PATH:"
    pm path org.jellyfin.gen5w.debug
    echo "NEMESIS_PACKAGES:"
    if [ -x /system/xbin/nemesis ]; then
        /system/xbin/nemesis pkg-list
    fi
} > "$OUT/status.txt" 2>&1
logcat -d -v threadtime -s Gen5WNative:V AndroidRuntime:V MediaCodec:V MediaCodecList:V OMXClient:V ACodec:V OMXCodec:V AudioTrack:V '*:S' > "$OUT/playback-log.txt" 2>&1
LOG_RESULT=$?
dumpsys audio > "$OUT/audio-state.txt" 2>&1
{
    echo "NETWORK_GUARD_STATE"
    cat /data/local/jellyfin-gen5w-network/state
    echo "IPv4 app filter"
    iptables -L JF_G5W_UID -n -v
    iptables -L JF_G5W_OUT -n -v
    echo "IPv6 app filter"
    ip6tables -L JF_G5W_UID -n -v
    ip6tables -L JF_G5W_OUT -n -v
} > "$OUT/network-guard.txt" 2>&1
if [ -f /data/nemesis/media.codecs.log ]; then
    cp /data/nemesis/media.codecs.log "$OUT/codec-patch-log.txt"
fi
echo "LOGCAT_EXIT=$LOG_RESULT" >> "$OUT/status.txt"
sync
echo "Jellyfin report saved to $OUT"
exit "$LOG_RESULT"
