# 0001. Native Android with Kotlin and Jetpack Compose, one app module

- Status: Accepted
- Date: 2026-10-28

## Context

fcat competes on reading quality: fast first page, smooth scroll and zoom, and a
native-feeling UI. Cross-platform UI toolkits add a rendering layer between the
page bitmaps and the screen, and they do not use platform conventions such as
predictive back, the system document picker, large-screen and keyboard behaviour,
or TalkBack semantics.

Compose is Android's recommended UI toolkit. The page canvas, however, is a
performance-critical custom surface. Whether it is best drawn in Compose
(`Canvas`/`Modifier.drawWithCache`) or in a custom `View` hosted with `AndroidView`
is **not yet known**. We decide that in M1 by measurement.

## Decision

- Kotlin only. C/C++ only where the rendering engine needs it.
- Jetpack Compose with Material 3 for all application UI.
- The page canvas is built in M1 with whichever host, Compose or a `View`, wins the
  frame-time measurements. Record the result in a follow-up ADR.
- Start with a **single `:app` module**. Split modules only when there is a concrete
  benefit, for example isolating native engine code or build times we have actually
  measured. Package boundaries inside `:app` come first.

## Consequences

- One build, one place to look. Contributors are not met with a module graph.
- Package-level boundaries must be kept by review, since the compiler will not
  enforce them until modules exist.
- Revisit the single-module choice when the engine binding lands (M1).
