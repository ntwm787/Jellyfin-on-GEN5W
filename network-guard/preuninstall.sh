#!/system/bin/sh
# NEMESIS stops this package's services before running preuninstall.
HERE="${0%/*}"
am force-stop org.jellyfin.gen5w.debug || exit 1
exec /system/bin/sh "$HERE/netguard.sh" cleanup
