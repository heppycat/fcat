# Testing

## Layers

| Layer | Location | Runs on | Use for |
|---|---|---|---|
| Unit | `app/src/test/` | JVM, `./gradlew testDebugUnitTest` | Pure logic: layout maths, tile scheduling, position encoding, outline/page-label parsing |
| Instrumented | `app/src/androidTest/` (from M1) | Device/emulator, `./gradlew connectedDebugAndroidTest` | Engine binding, SAF access, position restore across process death, Compose UI |
| Macrobenchmark | `:benchmark` module (from M1) | Physical device | The metrics in [PERFORMANCE.md](../PERFORMANCE.md) |
| Lint | `./gradlew lintDebug` | JVM | Android API misuse, accessibility, resources |

Today only lint and the (empty) unit-test task exist. Tests arrive with the first
real code in M1. No test exists merely to make CI show a number.

## Rules

- A bug fix comes with a test that fails without the fix, unless that is
  impractical. If so, say why in the pull request.
- Logic that can be pure Kotlin should be, so it can be unit-tested without a
  device.
- Never commit test PDFs whose licence is unclear. Small, generated PDFs
  (`app/src/androidTest/assets/`) are preferred for automated tests. The large
  benchmark corpus stays outside the repo (PERFORMANCE.md §5).
- Malformed-input tests are required for anything that parses data from a PDF.
- Performance claims in pull requests need numbers recorded with the method in
  PERFORMANCE.md, not "feels faster".
