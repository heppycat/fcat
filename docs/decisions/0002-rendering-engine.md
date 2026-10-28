# 0002. Rendering engine

- Status: **Proposed**. To be decided at the end of the M1 bake-off.
- Date: 2026-10-28

## Context

Full evidence is in [RESEARCH.md §1](../RESEARCH.md#1-rendering-engines). In
summary:

- **Framework `PdfRenderer`.** Zero APK cost. But the engine version depends on the
  device and its mainline updates, only one page can be open at a time behind one
  lock, text and search need API 35 (or 30+ with S-ext 13), and the reference shows
  no document outline API.
- **AndroidX PDF 1.0.0-beta01.** Wraps the framework renderer and provides a
  complete viewer UI. Its Compose `PdfViewer` is still `@ExperimentalPdfApi`. Using
  it means giving up control of scroll, zoom, tiling and caching, which are the
  product's differentiators.
- **PDFium, bundled.** Permissive licence and full feature coverage. But no
  maintained Kotlin binding exists, so we would own the JNI layer and the security
  updates.
- **MuPDF 1.28.5.** Full feature coverage, display lists and cloneable contexts
  that suit tiled multi-threaded rendering, and an official Android artifact. It is
  AGPL-3.0, which forbids proprietary components in the app.

No benchmark we found compares these engines on Android under a reader workload.

## Decision (provisional)

- MuPDF is the **leading candidate**. Bundled PDFium is the **challenger**. The
  framework renderer via AndroidX PDF is the **baseline**.
- In M1, implement the minimal `PdfEngine` interface (open, page count, page size,
  render a region at a scale, outline, text for a page) for MuPDF and PDFium. Run
  the protocol in [PERFORMANCE.md](../PERFORMANCE.md) on the reference devices and
  the corpus.
- Pick the engine that meets the M1 targets with the lowest maintenance and security
  burden. If the two are within measurement noise, prefer MuPDF for feature
  coverage. Record the result by updating this ADR to Accepted, or by superseding
  it.
- Do not use AndroidX PDF's viewer UI as the app's reader, whatever the outcome.

## Consequences

- Until M1 ends, nothing outside the engine package may depend on engine-specific
  types.
- After the decision, keep the interface only if it still pays for itself, for
  example for tests. Do not keep it out of habit.
- If PDFium wins, licensing stays as in ADR 0003 (GPL-3.0-or-later). Nothing forces
  a relicence.
