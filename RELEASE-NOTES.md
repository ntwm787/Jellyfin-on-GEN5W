# Jellyfin on GEN5W - v1.0.1

Packaging update for the NEMESIS store. The APK is unchanged from the vehicle-tested v1.0.0 release; audio, audio-track selection, subtitles and Back/swipe retain the confirmed app behavior. This revised installer has been validated locally; store acceptance and a new vehicle installation have not been confirmed.

## Declared installer dependencies

- media.codecs >=2.1 - Gen5 codec patch.
- keyboard.hackers >=2.0.1 - KeyBORD / Hacker's Keyboard.
- keyboard.activate >=2.0.1 - KeyBridge activation.

The keyboard is for typing, not a runtime requirement of Jellyfin itself. The NEMESIS store installer checks these registered packages as dependencies.

## Setup order

1. Install the [Gen5 codec patch](https://github.com/KramerTech/gen5-codec-patch).
2. Install [KeyBORD](https://github.com/ntwm787/KeyBORD-Gen5W).
3. Activate the keyboard with KeyBridge from the KeyBORD repository.
4. Install Jellyfin.

Use the NEMESIS package ZIP for the publisher portal, or the USB ZIP for installation from a drive. Source and SHA256SUMS.txt are supplied separately. Quick Connect works; an existing working keyboard remains usable by the app.

Jellyfin includes a background helper installed through NEMESIS that restricts the app to Wi-Fi and blocks BlueLink modem data. The USB/store package installs both the app and helper. An APK by itself does not include the helper.

Credit belongs to the Jellyfin project and contributors. The app retains the upstream GPL v2 license, with MIT/Apache notices preserved for the API client, libraries and wrapper tooling. See CREDITS.md and THIRD-PARTY-NOTICES.md.
