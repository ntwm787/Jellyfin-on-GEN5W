# Jellyfin on GEN5W

**Tested and confirmed working on a Gen5W head unit.** Stream your Jellyfin library through NEMESIS, with video, music, audio-track selection, subtitles and Quick Connect.

This is an independent Gen5W port of **Jellyfin Android v2.1.1 Libre**, made possible by the Jellyfin project and its contributors. It is not an official Jellyfin release. See [credits](CREDITS.md) and [licenses](THIRD-PARTY-NOTICES.md).

## Download

Download the **[USB installer](https://github.com/ntwm787/Jellyfin-on-GEN5W/releases/latest/download/Jellyfin-on-GEN5W-v1.0.0-USB.zip)** from [Releases](https://github.com/ntwm787/Jellyfin-on-GEN5W/releases/latest).

The USB ZIP contains only `NEMESIS.data` and `Packages/jellyfin.gen5w.libre.zip`. Source code is provided separately in this repository and as a release source archive.

## Requirements

- A Gen5W system running Android 4.4 / API 19 with 32-bit ARM, with NEMESIS installed.
- The separate [Gen5 codec patch](https://github.com/KramerTech/gen5-codec-patch) for native video decoding.
- A Jellyfin server reachable through head-unit **Wi-Fi**, such as a phone hotspot.
- **[KeyBORD](https://github.com/ntwm787/KeyBORD-Gen5W)** installed and activated for typing.

Designed for Gen5W without a firmware-version or vehicle-model lock. Other infotainment generations are not supported. Vehicle testing confirms the tested setup, not every Gen5W variant.

## Keyboard and sign-in

1. Install **KeyBORD** first, then activate it with **KeyBORD Activator**.
2. You can then use the keyboard in Jellyfin. If KeyBORD already works on your head unit, it will work here too; no setup again is needed.
3. Use the keyboard to enter at least your **server address and username** when signing in with a username, plus your password if required.

**Quick Connect works!** Enter the server address and choose Quick Connect on the Jellyfin sign-in screen as an alternative to entering a username and password. The server screen also has **KeyBORD setup & sign-in** help.

## Install

1. Park the vehicle and let the head unit finish booting.
2. If an earlier copy is installed, uninstall it through NEMESIS first. This removes its saved sign-in/settings.
3. Extract the USB installer ZIP to the USB root. Keep `jellyfin.gen5w.libre.zip` intact inside the NEMESIS **`Packages` folder**, with `NEMESIS.data` at the root. Use one NEMESIS operation per drive.
4. Insert the drive and confirm installation. Launch **Jellyfin on GEN5W** from NEMESIS Manager.
5. Connect the head unit to Wi-Fi, wait for network protection to be ready, enter your server address and sign in normally or with Quick Connect.

## Confirmed behavior

- Video and audio playback work; audio is fully restored in this release.
- In-player **audio-track and subtitle selection work**. Track changes may reload the stream at the current position.
- The Back button and left-edge swipe stop video and return to the previous Jellyfin screen.
- The factory media-app screen appears only when exiting the app, reported by the tester as normal head-unit behavior.
- Quick Connect works.

See the [testing record](docs/TESTED.md) for the confirmation and its scope.

## Playback and network rules

- Video requires fresh factory confirmation that the vehicle is stationary in Park. Unknown vehicle status blocks video.
- Streaming adapts to connection speed, up to **1080p**.
- Content downloads are disabled.
- **BlueLink modem data is blocked.** Use Wi-Fi or a phone hotspot. Android Auto internet sharing has not been verified for this app.
- A root network service protects this app's IPv4, IPv6 and DNS traffic. Install the full NEMESIS package; an APK alone is not the supported installation.

## Source and licenses

The Jellyfin Android application retains the upstream **GNU GPL v2** license. The Jellyfin API client retains its MIT license, and vendored ExoPlayer, Koin and ModernAndroidPreferences retain Apache-2.0 licenses. Original notices remain in their source trees. Installer/wrapper code retains its separate MIT notice; that does not relicense the Jellyfin app.

Read [CREDITS.md](CREDITS.md), [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md), [LICENSE](LICENSE) and [build instructions](docs/BUILD.md).
