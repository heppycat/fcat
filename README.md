<p align="center">
  <img src="docs/assets/icon.svg" width="96" alt="fcat icon: a calico cat face">
</p>

<h1 align="center">fcat</h1>

<p align="center"><b>A native Android PDF reader.</b><br><i>Read smoothly. Stay curious.</i></p>

---

fcat is a free and open-source PDF reader for Android, built for people who read
long documents: textbooks, papers, lecture notes. It focuses on **reading quality**:

- fast opening;
- smooth scrolling and zoom;
- exact position restore;
- good navigation of long documents.

It has no accounts, no ads, no trackers and no network access.

## Status

**Pre-alpha. Not usable yet.** The repository holds the project foundation
(milestone M0): research, decisions, documentation, and a placeholder app that
builds. The rendering engine is chosen by benchmark in M1. See the
[roadmap](docs/ROADMAP.md).

There are no screenshots, because there is nothing to show yet.

## Planned for v1.0

Open from the file picker or "Open with". Continuous scroll, pinch and double-tap
zoom. Outline, page jump, links with a back stack. Search, text selection, recent
documents with position restore. Light, dark and sepia page themes.
Password-protected files.

Not planned for v1.0: accounts, sync, AI, scanning, conversion, editing. Details are
in [docs/PRODUCT.md](docs/PRODUCT.md).

## Building

You need JDK 17, the Android SDK with platform `android-37.0`, and the NDK and
CMake versions pinned in `gradle/libs.versions.toml`.

```sh
./gradlew assembleDebug
```

See [docs/development/SETUP.md](docs/development/SETUP.md).

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) first. The
project follows the [Code of Conduct](CODE_OF_CONDUCT.md). Report security issues
as described in [SECURITY.md](SECURITY.md), not in public issues.

## Documentation

- [Product](docs/PRODUCT.md)
- [Roadmap](docs/ROADMAP.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Design principles](docs/DESIGN_PRINCIPLES.md)
- [Performance](docs/PERFORMANCE.md)
- [Research](docs/RESEARCH.md)
- [Decisions](docs/decisions/README.md)

## License

fcat is free software, licensed under the
[GNU General Public License v3.0 or later](LICENSE). See
[ADR 0003](docs/decisions/0003-license.md) for why. Third-party components keep
their own licences.
