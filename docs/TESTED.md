# Tested and confirmed working

Public release: v1.0.0, using the unchanged internal gen5w.17 APK. Vehicle confirmation supplied by the head-unit user on November 5, 2026.

The release APK was confirmed working on a Gen5W head unit:

- Audio is fully working again.
- In-player audio-track and subtitle selection work.
- Video playback and Back/button swipe navigation work.
- The factory media-app screen appears only when exiting Jellyfin; the tester reports this as normal behavior.
- Quick Connect was confirmed working earlier in the same testing session.

This confirmation applies to the tested setup. There is no firmware or vehicle-model lock; every Gen5W variant has not been tested. Track changes rebuild the stream at its current position and can reload the video.

The released APK is byte-for-byte the vehicle-tested gen5w.17 APK. Release preparation updates documentation, attribution and package license files only.

Local validation also covered APK build, zero NewApi findings, navigation/selection regressions, the known playback-source baseline, ZIP integrity and NEMESIS metadata. These checks are separate from the user's vehicle confirmation. Five existing lint errors remain in voice-search handling and string formatting.
