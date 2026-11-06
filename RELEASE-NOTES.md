# Jellyfin on GEN5W - v2.1.1-gen5w.17

**Tested and confirmed working on a Gen5W head unit.**

- Audio is fully working.
- In-player audio-track and subtitle selection work.
- Video and Back/button swipe navigation work.
- The factory media-app screen appears only when exiting Jellyfin, reported as normal head-unit behavior.
- Quick Connect works.

Download the USB ZIP and extract it to USB root. Keep the inner ZIP intact inside Packages. Complete source is provided separately in the repository and Source ZIP. SHA256SUMS.txt covers both ZIPs.

Install KeyBORD first, then activate it with KeyBORD Activator before typing the server address and username. An already-working KeyBORD works in Jellyfin. Quick Connect is an alternative to username/password sign-in after entering the server address.

Requires NEMESIS, Gen5W Android API19/ARM, the separate Gen5 codec patch and Wi-Fi. BlueLink modem data and downloads are blocked. Video requires confirmed stationary-in-Park status. Streaming adapts to connection speed up to 1080p. There is no firmware-version or model lock; every Gen5W variant has not been tested.

This independent port is based on Jellyfin Android v2.1.1 Libre. Credit goes to the Jellyfin project and contributors. The application retains its upstream GPL v2 license. MIT/Apache licenses and original copyright notices are preserved; see CREDITS.md and THIRD-PARTY-NOTICES.md. NEMESIS, KeyBORD and the codec patch are separate projects.

The released APK is the exact vehicle-tested binary. Release preparation changes documentation, credits and package notices only. Uninstall existing copies through NEMESIS before USB installation; saved sign-in/settings will be removed.
