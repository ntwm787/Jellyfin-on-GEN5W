# Licensing and third-party notices

This repository contains modified Jellyfin Android code and separately licensed libraries and tooling. Original copyright/license files remain authoritative. Redistribution must preserve their notices and applicable source obligations.

| Component | Version | License | Included license |
|---|---|---|---|
| Jellyfin Android and this app port | v2.1.1 / gen5w.17 | GNU GPL v2, as upstream | [LICENSE](LICENSE) |
| Jellyfin API client | v0.7.7 | MIT; Jellyfin and retained Emby copyrights | [license](licenses/Jellyfin-API-client-MIT.txt) |
| ExoPlayer | r2.12.0 | Apache-2.0 | [license](licenses/ExoPlayer-Apache-2.0.txt) |
| Koin | 2.1.6 | Apache-2.0 | [license](licenses/Koin-Apache-2.0.txt) |
| ModernAndroidPreferences | 1.1.0-alpha3 | Apache-2.0 | [license](licenses/ModernAndroidPreferences-Apache-2.0.txt) |
| Installer/wrapper tooling | Gen5W port | MIT; separate from the application | [notice](licenses/Wrapper-MIT.txt) |

Original upstream README files, source comments and notices remain in `jellyfin-gen5w/` and its vendored `third_party/` trees. Other dependencies declared by the build retain their upstream licenses. NEMESIS, KeyBORD, codec patches, a Jellyfin server and the server-supplied web client are separate installations; this package does not bundle them.

## Corresponding source

The release tag and `Source.zip` provide the app's corresponding source, modified vendored library source, resources, Gradle configuration, installer scripts, network guard, build/packaging scripts and notices. The USB package includes license/credit files. The released APK matches the vehicle-tested gen5w.17 APK exactly.

Unused upstream test-media fixtures, the prebuilt ktlint developer utility and upstream CI configurations are omitted from the curated publication. They are not inputs to this APK. No proprietary head-unit firmware, collected reports, credentials, private keys, build caches or local server data are included.
