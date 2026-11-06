#!/system/bin/sh
# Read-only checks; installation does not alter the vehicle's modem settings.
export PATH=/system/bin:/system/xbin:/sbin:/vendor/bin
case "$(id)" in uid=0\(*) ;; *) echo "Jellyfin network protection requires NEMESIS root installation."; exit 1 ;; esac
for TOOL in iptables ip6tables ndc ip ps chmod mv dumpsys mkdir rmdir rm sleep cat getprop; do
    command -v "$TOOL" >/dev/null 2>&1 || { echo "Required network tool is missing: $TOOL"; exit 1; }
done
for FAMILY in ip ip6; do
    OWNER=0
    MARK=0
    FILE="/proc/net/${FAMILY}_tables_matches"
    [ -r "$FILE" ] || exit 1
    while IFS= read -r LINE; do
        [ "$LINE" = owner ] && OWNER=1
        [ "$LINE" = mark ] && MARK=1
    done < "$FILE"
    [ "$OWNER" = 1 ] && [ "$MARK" = 1 ] || { echo "Per-app network filtering is unavailable."; exit 1; }
done
ANSWER="$(ndc interface fwmark get mark 0 2>&1)" || exit 1
case "$ANSWER" in 2??\ *) ;; *) echo "This head unit lacks the required per-app resolver controls."; exit 1 ;; esac
exit 0
