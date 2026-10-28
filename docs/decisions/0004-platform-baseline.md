# 0004. Platform baseline and application ID

- Status: Accepted. The `applicationId` is provisional.
- Date: 2026-10-28

## Decision

| Setting | Value | Why |
|---|---|---|
| `minSdk` | 28 (Android 9) | Same floor as AndroidX PDF, our benchmark baseline, so it can run on every device we test. MuPDF and PDFium support far lower, so this is not an engine limit. Revisit if user demand for API 26–27 appears. |
| `compileSdk` / `targetSdk` | 37 | Current platform. Compose libraries released in 2026 require compileSdk 37 and AGP ≥ 9.2. |
| JVM target / JDK | bytecode 17; build JDK ≥ 17 | Supported by AGP 9.x. CI builds with JDK 17. |
| `applicationId` | `io.github.heppycat.fcat` | Reverse-DNS of the GitHub Pages domain of the account currently hosting the code. A common, verifiable pattern for open-source apps without a domain (accepted by F-Droid). |

## Consequences

- The `applicationId` **cannot change after the first public release** without
  orphaning installs. It must be confirmed by the maintainer before the first
  release (RESEARCH.md Q7). Changing it before then is free.
- The minimum Android version is a product decision as much as a technical one.
  Raising it later is easy; lowering it may not be.
