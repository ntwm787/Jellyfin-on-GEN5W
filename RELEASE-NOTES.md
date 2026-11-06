# Jellyfin on GEN5W - v1.0.0

First public release.

**Tested and confirmed working on a Gen5W head unit.**

- Audio is fully working.
- In-player audio-track and subtitle selection work.
- Video and Back/button swipe navigation work.
- The factory media-app screen appears only when exiting Jellyfin, reported as normal head-unit behavior.
- Quick Connect works.

## Setup order

1. Install the **[Gen5 codec patch](https://github.com/KramerTech/gen5-codec-patch)**.
2. Install **[KeyBORD](https://github.com/ntwm787/KeyBORD-Gen5W)**.
3. Activate the keyboard using **KeyBridge**, provided in the [KeyBORD repository](https://github.com/ntwm787/KeyBORD-Gen5W).
4. Install **Jellyfin on GEN5W**.

Download the USB ZIP and extract it to USB root. Keep the inner ZIP intact inside the NEMESIS **Packages** folder. Complete source is provided separately in the repository and Source ZIP. SHA256SUMS.txt covers both ZIPs.

After activation with KeyBridge, use KeyBORD to enter your server address and username. An already-working KeyBORD works in Jellyfin. **Quick Connect works** as an alternative to username/password sign-in after entering the server address.

Requires NEMESIS, Gen5W Android API19/ARM, the separate Gen5 codec patch and Wi-Fi. BlueLink modem data and downloads are blocked. Video requires confirmed stationary-in-Park status. Streaming adapts to connection speed up to 1080p. There is no firmware-version or model lock; every Gen5W variant has not been tested.

## Network protection

Jellyfin includes a small background helper installed through NEMESIS. It restricts Jellyfin's internet access to Wi-Fi so the app cannot use the BlueLink modem.

The USB installer installs both Jellyfin and this helper. Installing the APK by itself does not include the helper.

**Install using the NEMESIS USB package. It includes the protection that blocks BlueLink modem data.**

This independent port is based on Jellyfin Android v2.1.1 Libre. Credit goes to the Jellyfin project and contributors. The application retains its upstream GPL v2 license. MIT/Apache licenses and original copyright notices are preserved; see CREDITS.md and THIRD-PARTY-NOTICES.md. NEMESIS, KeyBORD and the codec patch are separate projects.

The released APK is the exact vehicle-tested binary. Release preparation changes documentation, credits and package notices only.
