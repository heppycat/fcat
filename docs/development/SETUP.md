# Development setup

## Requirements

- **A JDK, version 17 or newer** (a full JDK with `javac`, not a JRE). AGP 9.x
  needs 17 or newer. CI uses Temurin 17. The build has also been run locally on
  JDK 27 with Gradle 9.8.2. The bytecode target is fixed at 17 whichever JDK runs
  Gradle.
- **Android SDK** with platform `android-37.0`. Android Studio installs it, or use
  `sdkmanager "platforms;android-37.0"`.
- **NDK and CMake**, at the versions `ndk` and `cmake` in
  `gradle/libs.versions.toml`, for the PDFium JNI layer in `app/src/main/cpp/`.
  For example `sdkmanager "ndk;30.0.16248370" "cmake;4.1.2"`.
- Git. Any editor. Android Studio gives the best Compose preview and profiler
  support.

Gradle itself is fetched by the wrapper (`./gradlew`). Its download is pinned by
SHA-256 in `gradle/wrapper/gradle-wrapper.properties`.

The first build also downloads prebuilt PDFium for each ABI from
`github.com/bblanchon/pdfium-binaries` into `app/build/pdfium/`. The release tag and
each archive's SHA-256 are pinned in the version catalog, and the build fails on a
mismatch.

## Build

```sh
git clone <repository-url> fcat && cd fcat
echo "sdk.dir=/path/to/Android/Sdk" > local.properties   # or set ANDROID_HOME
./gradlew assembleDebug          # app/build/outputs/apk/debug/
./gradlew installDebug           # onto a connected device or emulator
```

The same checks CI runs:

```sh
./gradlew assembleDebug lintDebug testDebugUnitTest
```

## Networks that block Google's Maven or Gradle services

Configure a proxy **outside the repository**, in `~/.gradle/gradle.properties`:

```properties
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=8080
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=8080
```

Or pass the same values as `-D` flags on the command line. Never commit proxy,
mirror or credential settings to `gradle.properties` in the repository.

## Project layout

```text
app/                 the Android application (single module, see ADR 0001)
gradle/              version catalog (libs.versions.toml) and wrapper
docs/                public documentation
AI/                  instructions for AI coding agents
.github/             issue templates, PR template, CI
```

Dependency versions live only in `gradle/libs.versions.toml`.
