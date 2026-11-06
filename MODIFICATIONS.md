# Gen5W modifications

Modified for Jellyfin on GEN5W, November 5, 2026. Base: Jellyfin Android v2.1.1 Libre.

The app source contains the API 19/ARM backport, native ExoPlayer playback service and bridge, in-app player overlay/Back handling, audio/subtitle controls, simpler WebView UI, download blocking, read-only vehicle status gate, network readiness gate and renamed app/sign-in guidance.

Vendored Jellyfin API client, ExoPlayer, Koin and ModernAndroidPreferences source is adapted to the API 19 build and local project layout. Original copyright/license notices remain. SOURCE-PROVENANCE.json records upstream archive hashes; CREDITS.md links their upstream versions.

network-guard/, packaging/ and tools/ provide the NEMESIS installer, app-scoped Wi-Fi/IPv4/IPv6/DNS service and build/package tooling. The app retains its upstream GPL license; wrapper and third-party licenses remain separate.

The APK is the exact vehicle-tested gen5w.17 binary. Release preparation adds public docs and package license/credit files around the unchanged payload. Unused upstream CI files, test-media fixtures and the prebuilt ktlint utility are excluded; app resources and build-consumed source are retained.
