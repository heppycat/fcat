# Project context

**fcat** is a native Android PDF reader. Its core value is reading quality:

- fast first page;
- smooth scroll and zoom;
- exact position restore;
- navigation of long documents.

It is aimed at people who read academic PDFs and textbooks. It is offline, with no
account and no ads, and licensed GPL-3.0-or-later.

## Current state

The authoritative status is the top of [docs/ROADMAP.md](../docs/ROADMAP.md). Read
it there; this line is not updated on every change.

- Milestone **M0, Foundation**: docs, build, CI. The app is a placeholder screen.
- The rendering engine is **not chosen**. [ADR 0002](../docs/decisions/0002-rendering-engine.md)
  is *Proposed*: MuPDF is leading and PDFium is the challenger, to be decided by the
  M1 benchmark. Do not write code that assumes either engine outside `engine/`.

## Stack

- Kotlin, Jetpack Compose with Material 3, single `:app` module.
- Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`).
- minSdk 28, compile/target SDK 37, JDK 17.
- Package `io.github.heppycat.fcat`. The `applicationId` is provisional
  (ADR 0004).

## Hard product constraints

- No `INTERNET` permission. No backend, accounts, sync, analytics, ads or AI APIs.
- No proprietary dependencies, including Play Services (licence: ADR 0003).
- Never modify the user's document in v1.0.
