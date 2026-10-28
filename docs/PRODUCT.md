# Product

> **fcat**: a native Android PDF reader. *Read smoothly. Stay curious.*

## Audience

People who read long, dense PDFs on Android phones and tablets:

- students with textbooks and lecture notes;
- researchers with papers;
- anyone working through technical manuals.

They read for hours, jump between sections, follow citations and figures, and come
back to the same documents across days.

## Problem

Popular readers have drifted away from reading:

- Reading-adjacent features move behind subscriptions.
- Apps add cloud, AI and scanning features the reader did not ask for.
- Some ask for an account or carry trackers.

Open-source alternatives respect the user, but many have dated or non-native UIs,
are e-ink-first, or are no longer actively developed (see
[RESEARCH.md §2](RESEARCH.md#2-competitors-and-existing-open-source-readers)).

## Differentiators

1. **Reading quality.** Fast first page, smooth scrolling and zoom, no blank tiles
   in normal use, sharp text at any zoom. Targets are measured, not claimed (see
   [PERFORMANCE.md](PERFORMANCE.md)).
2. **Never lose your place.** Position restore is exact: page, offset and zoom. It
   survives process death, reboots and file moves where Android allows it.
3. **Navigation for long documents.** Outline, page labels (roman-numeral front
   matter), and a back stack for internal links, so you can follow a citation and
   come straight back. Fast page jumps.
4. **Native and accessible.** Material 3, system document picker, predictive back,
   keyboard and mouse, large screens, TalkBack.
5. **Yours.** Offline, no account, no ads, no trackers, no network permission in the
   initial scope. Free software (GPL-3.0-or-later).

## Initial scope (v1.0)

- Open PDFs from the system picker and from "Open with" / share intents.
- Continuous vertical scroll. Pinch zoom and double-tap zoom. Fit-width and
  fit-page modes.
- Outline (table of contents), page-number jump, internal and external links with a
  back stack.
- Text search, with results highlighted and listed.
- Text selection and copy, where the PDF has a text layer.
- Recent documents, with exact position restore.
- Light, dark and sepia page themes (page-colour inversion for night reading).
- Password-protected PDFs.

## Non-goals (v1.0)

- Accounts, sync, a backend, or any network use.
- Ads, analytics, telemetry, proprietary SDKs.
- AI features.
- Scanning, conversion, PDF editing or creation.
- Formats other than PDF. MuPDF might make EPUB cheap later, but it is out of scope
  now.
- Annotation. It is planned after v1.0 (see [ROADMAP.md](ROADMAP.md)) and must not
  slow down reading.

## Success criteria

Measured as defined in [PERFORMANCE.md](PERFORMANCE.md), on the reference devices:

- Time to first rendered page meets the targets for every document in the corpus.
- Scrolling and zooming meet the frame-time and blank-tile targets.
- Opening a recent document restores the exact position, verified by instrumented
  tests including process death.
- Zero crashes on the corpus, including malformed and huge files. Malformed files
  produce a clear error, never a crash.
- No accessibility-scanner errors on the main screens. Full TalkBack navigation of
  library, outline and search.
- The app declares no `INTERNET` permission.
