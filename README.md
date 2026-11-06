# Jellyfin on GEN5W

**Tested and confirmed working on a Gen5W head unit.** Stream your Jellyfin library through NEMESIS, with video, music, audio-track selection, subtitles and Quick Connect.

This is an independent Gen5W port of **Jellyfin Android v2.1.1 Libre**, made possible by the Jellyfin project and its contributors. It is not an official Jellyfin release. See [credits](CREDITS.md) and [licenses](THIRD-PARTY-NOTICES.md).

## Download

Download the **[USB installer](https://github.com/ntwm787/Jellyfin-on-GEN5W/releases/latest/download/Jellyfin-on-GEN5W-v1.0.0-USB.zip)** from [Releases](https://github.com/ntwm787/Jellyfin-on-GEN5W/releases/latest).

The USB ZIP contains only `NEMESIS.data` and `Packages/jellyfin.gen5w.libre.zip`. Source code is provided separately in this repository and as a release source archive.

## Source code

- **[Browse the source code](SOURCE-CODE.md)** - links to the application, network helper and build files.
- **[Download the complete source code ZIP](https://github.com/ntwm787/Jellyfin-on-GEN5W/releases/download/v1.0.0/Jellyfin-on-GEN5W-v1.0.0-Source.zip)**.
- [Build instructions](docs/BUILD.md).

## Requirements

A Gen5W system running Android 4.4 / API 19 with 32-bit ARM and NEMESIS installed, plus a Jellyfin server reachable through head-unit **Wi-Fi**, such as a phone hotspot.

Designed for Gen5W without a firmware-version or vehicle-model lock. Other infotainment generations are not supported. Vehicle testing confirms the tested setup, not every Gen5W variant.

## Setup order

1. Install the **[Gen5 codec patch](https://github.com/KramerTech/gen5-codec-patch)**.
2. Install **[KeyBORD](https://github.com/ntwm787/KeyBORD-Gen5W)**.
3. Activate the keyboard using **KeyBridge**, provided in the [KeyBORD repository](https://github.com/ntwm787/KeyBORD-Gen5W).
4. Install **Jellyfin on GEN5W**.

To install Jellyfin, park the vehicle and let the head unit finish booting. Extract the USB installer ZIP to the USB root. Keep `jellyfin.gen5w.libre.zip` intact inside the NEMESIS **`Packages` folder**, with `NEMESIS.data` at the root. Use one NEMESIS operation per drive. Insert the drive, confirm installation and launch **Jellyfin on GEN5W** from NEMESIS Manager.

## Keyboard and sign-in

After KeyBORD is activated with KeyBridge, you can use the keyboard in Jellyfin. If KeyBORD already works on your head unit, it will work here too; no setup again is needed.

Connect the head unit to Wi-Fi and wait for network protection to be ready. Use the keyboard to enter at least your **server address and username** when signing in with a username, plus your password if required.

**Quick Connect works!** Enter the server address and choose Quick Connect on the Jellyfin sign-in screen as an alternative to entering a username and password. The server screen also has **KeyBORD setup & sign-in** help.

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
## Network protection

Jellyfin includes a small background helper installed through NEMESIS. It restricts Jellyfin's internet access to Wi-Fi so the app cannot use the BlueLink modem.

The USB installer installs both Jellyfin and this helper. Installing the APK by itself does not include the helper.

**Install using the NEMESIS USB package. It includes the protection that blocks BlueLink modem data.**

Use head-unit Wi-Fi or a phone hotspot. Android Auto internet sharing has not been verified for this app.

## Licenses and credits

The Jellyfin Android application retains the upstream **GNU GPL v2** license. The Jellyfin API client retains its MIT license, and vendored ExoPlayer, Koin and ModernAndroidPreferences retain Apache-2.0 licenses. Original notices remain in their source trees. Installer/wrapper code retains its separate MIT notice; that does not relicense the Jellyfin app.

Read [CREDITS.md](CREDITS.md), [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md), [LICENSE](LICENSE) and [build instructions](docs/BUILD.md).
