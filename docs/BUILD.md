# Build and package

Use JDK 11, Python 3.10+ and an Android SDK with platform 30 and build-tools 29.0.3. Accept the SDK license, set JAVA_HOME to JDK 11 and ANDROID_HOME (or ANDROID_SDK_ROOT) to the SDK. Internet access is needed for the included Gradle 6.6.1 wrapper and Maven dependencies on first build.

From the repository root:

    python tools/build.py

The APK appears in jellyfin-gen5w/app/build/outputs/apk/libre/debug/. The internal debug app ID is retained for install/network-guard compatibility. The distributed APK is development signed and debuggable. Publication does not change the tested payload or migrate signing keys.

To create USB-only and source ZIPs plus checksums:

    python tools/package.py --apk path/to/jellyfin-android-v2.1.1-gen5w.17-libre-debug.apk

Outputs go to dist/. The packager uses the editable descriptor/scripts in packaging/, the root network guard, complete curated source and license/credit files. No old build artifacts, collected firmware or private reports are needed.

Run the player regression:

    node tests/manual-stop-regression.cjs jellyfin-gen5w/app/src/main/assets/gen5w-client.js

Lint must have zero NewApi findings. Five existing errors remain in voice-search handlers and formatting; upstream abortOnError=false remains. Local checks are separate from the user-confirmed vehicle result in TESTED.md.

Source provenance is in jellyfin-gen5w/SOURCE-PROVENANCE.json. Source changes are described in MODIFICATIONS.md; original notices remain in vendored trees. Excluded test media and the prebuilt ktlint utility are not build inputs.

The published USB APK must match docs/RELEASE-MANIFEST.json. Rebuilds can differ because of environment/debug signing. A newly rebuilt APK must not be called vehicle-tested without confirmation.

The first public release is v1.0.0. Its NEMESIS package version is 1.0.0. The unchanged vehicle-tested APK retains internal version 2.1.1-gen5w.17; see RELEASE-MANIFEST.json for its SHA-256.
