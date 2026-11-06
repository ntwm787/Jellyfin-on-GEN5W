# Jellyfin Gen5W network guard

This NEMESIS root service accompanies gen5w.6. The APK alone intentionally stays
offline. Its production interface is `wlan0`, verified in the supplied Wi-Fi
capture. The app separately requires Android's connected Wi-Fi client state;
the head unit broadcasting a hotspot is insufficient. No Android Auto sharing
interface is currently allowlisted.

The service discovers the installed app's UID, installs private IPv4/IPv6 filter
chains, and allows its traffic only on the approved output interface. Other UIDs
return to their existing firewall policy. It does not disable the modem, modify
the main default route or change the global default DNS settings.

DNS needs additional handling: Android 4.4's `dnsproxyd` issues queries under its
system UID. The service uses the legacy per-UID resolver and fwmark controls to
reserve the logical `jfg5wnet` domain and obtain an app-private DNS socket mark.
It converts that mark before stock mangle rules clear it, then subjects those
queries to the same interface filter. The logical domain is not a new physical
network adapter. Failure to establish this state leaves the APK offline.

A root-owned heartbeat under `/data/local/jellyfin-gen5w-network` authorizes the
APK only while fresh and matching its UID and kernel boot ID. A lock permits one service instance.
Normal termination leaves kernel restrictions installed; NEMESIS uninstall first
stops the service, then force-stops the app and runs scoped cleanup. Cleanup never
flushes a stock table, clears all resolver mappings or removes another domain.
If netd's private state cannot be recovered safely after a daemon restart, the
service fails closed; restarting the parked head unit may be needed.

`lab-eth0` is accepted only when `ro.kernel.qemu=1`; it is used to simulate a
permitted interface in the owned emulator. The NEMESIS service passes no lab
option, and the APK still refuses an eth0 heartbeat. The tests use controlled
local endpoints and never connect to a vehicle modem.

The scripts are original wrapper code under the repository's MIT license.
The Android app retains Jellyfin's GPL-2.0 license. API behavior was checked
against these Android 4.4.2 primary sources:

- [netd resolver command handling](https://android.googlesource.com/platform/system/netd/+/android-4.4.2_r1/CommandListener.cpp)
- [DNS proxy UID handling](https://android.googlesource.com/platform/system/netd/+/android-4.4.2_r1/DnsProxyListener.cpp)
- [Private UID marks](https://android.googlesource.com/platform/system/netd/+/android-4.4.2_r1/SecondaryTableController.cpp)
- [Resolver socket marks](https://android.googlesource.com/platform/bionic/+/android-4.4.2_r1/libc/netbsd/resolv/res_send.c)

Local tests are not vehicle validation. The package remains a parked-vehicle
test candidate until the actual head unit's guard startup and Wi-Fi loss/reconnect
have been checked.
