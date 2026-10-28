# Releases

No release has been made. This is the intended process, to be completed during M4.

## Versioning

- `versionName`: semantic versioning (`MAJOR.MINOR.PATCH`). Pre-1.0 releases may
  break anything.
- `versionCode`: strictly increasing integer, bumped on every published build.
- Git tag `vX.Y.Z` on the release commit on `main`.

## Signing

- Release keys are **never** stored in the repository, CI logs, or issue tracker.
  `.gitignore` excludes `*.jks`, `*.keystore` and `keystore.properties`, but review
  every commit anyway.
- The signing setup (local only, or CI secrets) is decided before the first release
  and documented here. It is not decided yet.
- Losing the key means users cannot update. Keep an offline backup.

## Checklist

1. `main` is green in CI. No open release-blocking issues.
2. PERFORMANCE.md targets re-measured on reference devices. Results committed under
   `docs/benchmarks/`.
3. Update `CHANGELOG.md` (created with the first release) and bump
   `versionCode`/`versionName`.
4. Third-party licence screen and NOTICE are up to date with the dependency list.
   Source-offer obligations are met (ADR 0003).
5. Build `./gradlew assembleRelease bundleRelease` from a clean checkout of the tag.
6. Install the release build on the reference devices. Smoke-test opening, scroll,
   search and position restore.
7. Publish: GitHub release with APK and checksums. Store listings as decided.
   F-Droid builds from source, so keep the build reproducible and free of
   proprietary dependencies.
