#!/system/bin/sh
# Install only on the API/ABI investigated for this package.
SDK=$(getprop ro.build.version.sdk)
ABI=$(getprop ro.product.cpu.abi)
BOOT=$(getprop sys.boot_completed)
if [ "$SDK" != "19" ]; then
    echo "Jellyfin on GEN5W: this package requires Android API 19; found $SDK."
    exit 1
fi
case "$ABI" in
    armeabi-v7a|armeabi) ;;
    *) echo "Jellyfin on GEN5W: this package requires 32-bit ARM; found $ABI."; exit 1 ;;
esac
if [ "$BOOT" != "1" ]; then
    echo "Jellyfin on GEN5W: wait for Android to finish booting, then retry."
    exit 1
fi
INSTALLED=$(pm path org.jellyfin.gen5w.debug 2>/dev/null)
case "$INSTALLED" in
    package:*)
        echo "Jellyfin on GEN5W is already installed. Uninstall the previous package through NEMESIS before installing this version."
        echo "Uninstall the existing copy yourself if you intend to replace it."
        exit 1
        ;;
esac
echo "Jellyfin on GEN5W API 19 / ARM preflight passed. Installing native playback APK."
HERE="${0%/*}"
exec /system/bin/sh "$HERE/../guard/preflight.sh"
