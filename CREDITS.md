# Credits

## Jellyfin

This port is based on **[Jellyfin Android v2.1.1](https://github.com/jellyfin/jellyfin-android/tree/v2.1.1)**, Libre flavor. Credit belongs to the Jellyfin Android developers, translators, designers and contributors, and the wider **[Jellyfin project](https://jellyfin.org/)**. The app connects to [Jellyfin Server](https://github.com/jellyfin/jellyfin) and integrates its [web client](https://github.com/jellyfin/jellyfin-web).

The original app is licensed under the GNU General Public License, version 2; the complete upstream license is retained in [LICENSE](LICENSE) and [jellyfin-gen5w/LICENSE.md](jellyfin-gen5w/LICENSE.md). This independent port does not imply Jellyfin endorsement.

Gen5W changes include Android API 19 compatibility, native ExoPlayer playback, in-app video and Back navigation, audio/subtitle controls, lighter UI settings, disabled downloads, a read-only Park/motion gate, and app-scoped Wi-Fi/modem protection. Jellyfin branding and original authorship remain credited.

## Libraries

- [Jellyfin API client / SDK v0.7.7](https://github.com/jellyfin/jellyfin-apiclient-java/tree/v0.7.7): Jellyfin contributors and the retained Emby copyright; MIT.
- [ExoPlayer r2.12.0](https://github.com/google/ExoPlayer/tree/r2.12.0): Google and ExoPlayer contributors; Apache-2.0.
- [Koin 2.1.6](https://github.com/InsertKoinIO/koin/tree/2.1.6): Koin contributors; Apache-2.0.
- [ModernAndroidPreferences 1.1.0-alpha3](https://github.com/Maxr1998/ModernAndroidPreferences/tree/1.1.0-alpha3): Maxr1998 and contributors; Apache-2.0.
- Other upstream Android/JVM dependencies retain their respective licenses and original source notices.

## Gen5W ecosystem

Thanks to [NEMESIS](https://github.com/xwtk/hmg_nemesis) and its contributors for package installation, [KramerTech's Gen5 codec patch](https://github.com/KramerTech/gen5-codec-patch) for platform codec enablement, and [KeyBORD](https://github.com/ntwm787/KeyBORD-Gen5W) and its underlying keyboard/activation contributors for text input. These are separate projects and are not bundled or relicensed by this release.

The Gen5W port is maintained in [ntwm787/Jellyfin-on-GEN5W](https://github.com/ntwm787/Jellyfin-on-GEN5W). Vehicle confirmation was provided by the head-unit user on November 5, 2026.
