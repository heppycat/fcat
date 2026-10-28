# Architecture

Status: **M1 in progress**. `engine/` holds `PdfEngine` and its MuPDF
implementation. The app opens a PDF from the system picker and shows its first page.
This document describes the intended shape. Sections marked *planned* do not exist
in code yet.

## Boundaries

```text
┌──────────────────────────── fcat (single :app module) ───────────────────────────┐
│                                                                                  │
│  ui/            Compose screens: library, reader chrome, outline, search         │
│   │                                                                              │
│  reader/        Viewport state, layout of pages, gesture → zoom/scroll,          │
│   │             tile scheduling, bitmap cache        (planned, M1–M2)             │
│   │                                                                              │
│  engine/        PdfEngine interface + one implementation (MuPDF or PDFium)       │
│   │             All native/JNI code lives behind this line (MuPDF, PDFium; M1)   │
│   │                                                                              │
│  library/       Recent documents, reading positions (local storage)  (planned)   │
│                                                                                  │
└────────────────┬──────────────────────────────────────┬──────────────────────────┘
                 │ Storage Access Framework             │ No network
                 ▼                                      ▼
          content:// URIs from the                (no INTERNET permission)
          system document picker / intents
```

Dependencies point downwards only: `ui → reader → engine`, and `ui → library`.
`engine` knows nothing about Compose. `reader` knows nothing about which engine is in
use.

## Key decisions

| Topic | Decision | Record |
|---|---|---|
| Platform and UI | Kotlin, Jetpack Compose, Material 3, single module | [ADR 0001](decisions/0001-native-kotlin-compose.md) |
| Rendering engine | MuPDF leading, PDFium challenger, decided by M1 measurement | [ADR 0002](decisions/0002-rendering-engine.md) (Proposed) |
| Licence | GPL-3.0-or-later | [ADR 0003](decisions/0003-license.md) |
| SDK levels and app ID | minSdk 28, target 37 | [ADR 0004](decisions/0004-platform-baseline.md) |

## Components (planned)

### `engine`: `PdfEngine`

A deliberately small interface. It exists because M1 runs two implementations side
by side:

- open a document from a file descriptor, with an optional password;
- get the page count and page sizes;
- render a rectangular region of a page at a given scale into a caller-supplied
  `Bitmap`;
- read the outline, page labels and links;
- extract text, with positions, for one page.

Rules:

- Calls are blocking and run on engine-owned threads. The interface never exposes
  native handles.
- Every native call must be cancellable or quick. Long work (search) is page-by-page
  so it can stream and be cancelled.

### `reader`: viewport and tiles

- Page layout is computed from page sizes only (cheap, done at open).
- The visible region plus a prefetch margin is split into tiles at the current zoom
  level. A low-resolution full-page pass renders first, then sharp tiles replace it.
- One bounded bitmap pool and an LRU tile cache, sized from
  `ActivityManager.getMemoryClass()`. Bitmaps are reused, never allocated per frame.
- Rendering is cancelled when a tile leaves the viewport before it finishes.

### `library`: positions and recents

- Persist the URI permission (`takePersistableUriPermission`), display name, size,
  and a content fingerprint to re-identify moved files.
- Position = page index + offset within the page + zoom. Written on pause and
  debounced while scrolling.
- Storage: start with the simplest durable option that supports atomic updates
  (DataStore or SQLite via Room). Pick in M2 with a short ADR.

## Data and privacy

- Documents are read through the Storage Access Framework. No broad storage
  permission.
- No network permission. Nothing leaves the device.
- Documents are never modified in v1.0. Later write features must use write-to-temp
  then atomic replace, and must never truncate the original first.

## Dependencies

Present:

- AndroidX Core, Activity Compose, Lifecycle, Compose BOM and Material 3;
- kotlinx-coroutines (already pulled in by Compose, now declared because the app
  uses it directly);
- MuPDF `com.artifex.mupdf:fitz`, a bake-off candidate until ADR 0002 is accepted;
- PDFium, the other bake-off candidate: the prebuilt non-V8 Android build from
  `bblanchon/pdfium-binaries`, pinned by tag and SHA-256, behind our own JNI layer
  (`app/src/main/cpp/`). Built with the NDK and CMake;
- JUnit 4 for unit tests, AndroidX Test (runner, ext-junit) for instrumented tests.

Planned:

- removal of the losing engine (M1);
- AndroidX Benchmark / Macrobenchmark and Baseline Profiles (M1);
- a persistence library (M2).

Any dependency beyond these needs a justification in its pull request (see
[AI/DEVELOPMENT_RULES.md](../AI/DEVELOPMENT_RULES.md) and
[CONTRIBUTING.md](../CONTRIBUTING.md)).
