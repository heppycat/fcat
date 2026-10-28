# Roadmap

Milestones are sequential. Each one ends when its acceptance criteria are met, not
on a date. Status as of 2026-10-28: **M0 in progress.**

## M0: Foundation

Repository, documentation, build, CI.

- [x] Engine research and provisional decision (ADR 0002)
- [x] Licence decision (ADR 0003)
- [x] Gradle project with a placeholder Compose app; debug build and lint pass locally
- [x] CI workflow building, linting and unit-testing on every PR
- [ ] Public GitHub repository created (maintainer action)
- [ ] First CI run green on GitHub

## M1: Engine bake-off and first pixels

Goal: decide the engine with data, and render a real document.

- `PdfEngine` interface, plus MuPDF and PDFium implementations behind it.
- Benchmark corpus assembled (PERFORMANCE.md §2). A macrobenchmark module measures
  time to first page and scroll frame timing.
- Measurements recorded for both engines and for the AndroidX PDF baseline on the
  reference devices.
- ADR 0002 moves to Accepted. The losing implementation is deleted.
- Page canvas host chosen by measurement (ADR 0001 follow-up).

**Acceptance:** a results table exists in `docs/benchmarks/`, ADR 0002 is accepted,
and the app opens a PDF from the picker and scrolls it.

## M2: Core reader

- Continuous scroll, pinch and double-tap zoom, fit modes.
- Tiled rendering with a bounded bitmap cache. Memory stays within targets on the
  1000-page document.
- Recent documents with exact position restore, including process death.
- Opening from "Open with" and share intents. Password-protected PDFs.
- Clear error states for corrupt or unsupported files.

**Acceptance:** PERFORMANCE.md targets met on reference devices. Instrumented tests
cover position restore and the error paths.

## M3: Navigation and text

- Outline, page labels, go-to-page.
- Internal links with a back stack. External links ask before leaving the app.
- Search with result list and highlights.
- Text selection and copy.

**Acceptance:** all of the above work on the academic subset of the corpus. Search
on the 1000-page document stays responsive (results stream in, UI never blocks).

## M4: Polish and accessibility → v1.0

- Page themes (light, dark, sepia, inverted).
- Tablet two-up layout, keyboard shortcuts, mouse wheel zoom.
- TalkBack pass, font scaling, contrast, touch-target audit.
- Third-party licence screen. Release signing. F-Droid metadata.

**Acceptance:** PRODUCT.md success criteria met. Release checklist
(development/RELEASE.md) completed.

## Later (not committed)

Candidates, each needing its own proposal and proof that reading performance does
not regress:

- Highlights and notes, stored as standard PDF annotations, with atomic saves.
- Margin crop / smart zoom to the text area.
- Per-document reading statistics, stored locally.
- Reflow for scanned papers.
- EPUB, if the engine makes it nearly free.
