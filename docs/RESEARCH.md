# Research

Research behind fcat's foundational decisions. Last reviewed **2026-10-28**.

Evidence labels used throughout:

- **[Doc]**: stated in official documentation or the project's own repository.
- **[Inspected]**: verified by inspecting a published artifact (POM, AAR manifest, bytecode).
- **[Reported]**: claimed by a third party (store listing, user reviews, community repos).
- **[Hypothesis]**: our expectation. Not evidence. Must be tested before it is relied on.

No fcat benchmark has been run yet. Every performance statement below is either
documented by the vendor or a hypothesis.

## 1. Rendering engines

### 1.1 Android `PdfRenderer` (framework)

- In the framework since API 21. Backed by PDFium inside the platform. [Doc]
- Before API 35 the API is bitmap rendering only: page count, page size, `render()`. [Doc]
- API 35 (Android 15) added password-protected loading, text contents
  (`getTextContents`), `searchText`, `selectContent`, link and goto-link contents,
  form widgets, and `write()`. [Doc]
- `PdfRendererPreV` exposes the same V-era API on API 30–34 when the device has
  **S Extensions level 13**. The extension is delivered through Google Play system
  updates (mainline). [Doc]
- Page annotation APIs (`addPageAnnotation`, `getPageAnnotations`, …) are marked
  "Added in version 36.1". [Doc]
- "Only a single page opened at any given time." The class is thread-safe, but
  "only one thread will be executed at a time as the access is synchronized by
  internal locking." [Doc]
- The reference shows per-page goto links/bookmarks, but no document-level outline
  (table of contents) API. [Doc, absence noted 2026-10-28]

**Implications.** No native code and no APK cost. But three problems weigh against it
as the primary engine:

- The engine version, and so its bugs and performance, depends on the device and its
  mainline updates rather than on us.
- Rendering is serialised: one page at a time, behind one lock.
- There is no document outline, and textbooks need one.

[Hypothesis] The serialisation limits how fast tiles can fill in during fast flings
and zoom changes on multi-page layouts.

### 1.2 AndroidX PDF (`androidx.pdf:*`)

- Latest: **1.0.0-beta01, 2026-08-26**. Public APIs are Beta, except those marked
  `@ExperimentalPdfApi`, which includes the Compose `PdfViewer`, `PdfViewerState`,
  `EditablePdfViewerFragment` and `OcrProvider`. [Doc]
- Artifacts: `pdf-core`, `pdf-viewer`, `pdf-viewer-fragment`, `pdf-compose`,
  `pdf-document-service`, `pdf-ink`, `pdf-ocr-play-services`. [Inspected: POM]
- `minSdkVersion="28"` in the AAR manifests. [Inspected]
- Ships **no native libraries**. [Inspected: no `.so` in the AARs] It chooses an
  adapter at runtime:
  - framework `PdfRenderer` on API ≥ 35;
  - `PdfRendererPreV` on API ≥ 30 with S-extension ≥ 13;
  - otherwise a compat adapter over the legacy renderer.

  [Inspected: `PdfDocumentRendererFactoryImpl` bytecode]
- The release notes for 1.0.0-alpha18 say read and rendering were "backported to
  support android devices down to minSdk = 28 and sdk-extension < 13". [Doc] Which
  features (text, search, links) work on that compat path is **not documented**;
  see open questions.
- Feature gates found in `SdkUtils` bytecode (S-extension levels):
  - level 13: core APIs, form filling, linearization status;
  - level 18: annotations, signatures;
  - level 19: render form-content mode, top-object lookup, image selection.

  [Inspected]
- Features: search, selection, zoom and scroll, keyboard and mouse input (alpha14),
  form filling and ink annotations via `EditablePdfViewerFragment` (alpha13+), and
  OCR via an `OcrProvider` whose default implementation uses ML Kit / Play
  Services. [Doc]
- The Compose artifact depends on Compose 1.7.8 and requires AGP ≥ 9.2 because of
  compileSdk 37 (alpha17 note). [Doc + Inspected]
- License: Apache-2.0. [Doc: AndroidX]

**Implications.** This is the fastest path to a working viewer, but it is a
**finished UI component**. The things we want to differentiate on live inside it,
and we cannot tune them:

- scroll and zoom physics;
- tiling and bitmap caching;
- page layout modes;
- reading-position semantics.

It inherits every limit of the platform renderer. Its default OCR provider pulls in
Play Services, which conflicts with an F-Droid-friendly, offline build.

It is valuable as a **baseline** to benchmark against.

### 1.3 PDFium, bundled

- Chromium's PDF engine. BSD-3-Clause. [Doc: `pdfium.googlesource.com` LICENSE]
- It bundles third-party code (FreeType, libjpeg-turbo, OpenJPEG, lcms2, zlib,
  abseil, …), each under its own permissive licence. A full audit is outstanding.
- `bblanchon/pdfium-binaries` publishes prebuilt Android binaries (arm, arm64, x86,
  x64) automatically, weekly. It is not affiliated with Google. Release `chromium/8125`
  (PDFium 158.0.8125.0) was published 2026-10-26. [Doc]
  - Releases are immutable, and GitHub shows a SHA-256 digest per asset. [Doc]
  - Each archive ships the `args.gn` it was built with, the public headers, and the
    third-party licence texts (`licenses/`). [Checked: `chromium/8125` arm archive]
  - The non-V8 variant has `pdf_enable_v8 = false` (no JavaScript) and
    `pdf_enable_xfa = false`. [Checked: its `args.gn`]
  - Its `libpdfium.so` needs only libc, libm and libdl, so no shared C++ runtime.
    [Checked: `readelf -d`]
- Android wrappers:
  - `barteksc/AndroidPdfViewer`, now `DImuthuUpe/AndroidPdfViewer`: Apache-2.0,
    PdfiumAndroid-based. Its README says "looking for contributors to bring the
    project back to track" and it still references jcenter. [Doc]
  - `io.legere:pdfiumandroid` (`johngray1965/PdfiumAndroidKt`) **is** a maintained
    Kotlin binding. It is Apache-2.0 and on Maven Central; 2.0.3 is stable (2026-07-26)
    and 3.0.x is in alpha. Its API has coroutines, opening from a
    `ParcelFileDescriptor`, and rendering into a `Bitmap`. [Doc; checked: 2.0.3 AAR]
    - It bundles a prebuilt `libpdfium.so` whose source and version are not
      documented. The file has no version string, was last updated 2026-02-11, and
      matches none of bblanchon's arm64 builds `chromium/7643`–`7678`. [Checked]
    - It also brings Guava and its own global lock. [Checked: POM]
    - For these reasons M1 uses our own small JNI layer on a pinned bblanchon build
      instead. That decision was made in M1b, 2026-10-28.
- PDFium's public API (`fpdfview.h`, `fpdf_text.h`, `fpdf_doc.h`, `fpdf_annot.h`)
  covers rendering, text, search, bookmarks/outline and annotations. [Doc]
- `fpdfview.h`: "None of the PDFium APIs are thread-safe. They expect to be called
  from a single thread." Embedders must make sure only one PDFium call runs at a
  time. [Doc: `public/fpdfview.h`, main, 2026-10-28]
- F-Droid accepts library dependencies only if they are "built from source or provided
  in a trusted Maven repo". A tarball from GitHub releases is neither, so shipping
  PDFium on F-Droid would need a PDFium source build. [Doc: F-Droid Inclusion Policy]

**Implications.** Permissive licensing and full control. The cost is real: we would
own the JNI surface, the build or binary sourcing, and **security updates** for a
large C++ parser handling untrusted files. The APK grows by a native library per ABI
(size to be measured).

### 1.4 MuPDF

- Artifex. Dual licensed: **AGPL-3.0** or commercial. [Doc: mupdf.readthedocs.io]
- The Android guide says an open-source app under the AGPL "may *not* use any
  proprietary closed source libraries or components in your app". It names Google
  Play Services as an example. [Doc]
- Official prebuilt Android library `com.artifex.mupdf:fitz`, latest **1.28.5**, on
  `maven.ghostscript.com`. [Inspected: maven-metadata.xml]
- Docs require minSdk ≥ 16. [Doc]
- The core repository is actively developed (last commit 2026-10-27). [Inspected:
  GitHub API]
- Capabilities: rendering, structured text extraction, search, outline, links,
  annotations and forms, plus EPUB, XPS, CBZ and images. It has display lists, so a
  page can be parsed once and rendered many times at different scales or tiles, and
  contexts can be cloned for multi-threaded rendering. [Doc]
- Used by mature open-source readers: Librera, Document Viewer, Artifex's own
  viewer. [Doc]

**Implications.** It is the most complete engine for a reader. Display lists and
cloneable contexts map directly onto tiled, multi-threaded rendering. The price is
the licence:

- The distributed app must be AGPL-compatible.
- No proprietary SDKs, ever. This is already a non-goal for us.
- Any future proprietary relicensing would need an Artifex commercial licence.

### 1.5 Comparison

| | PdfRenderer | AndroidX PDF | PDFium (bundled) | MuPDF |
|---|---|---|---|---|
| Licence | AOSP (Apache-2.0) | Apache-2.0 | BSD-3 (+ third-party) | AGPL-3.0 / commercial |
| Native code in APK | none | none | yes, per ABI | yes, per ABI |
| Engine version controlled by us | no (device/mainline) | no | yes | yes |
| Text / search | API 35+, or 30+ with ext 13 | same as platform | yes | yes |
| Outline / TOC | not in reference | not documented | yes | yes |
| Annotations | 36.1+ | ext 18+, beta | yes | yes |
| Concurrency | one page, one lock | inherits | caller-serialised | cloneable contexts |
| Owns viewer UI | no | **yes** (fragment / composable) | no | no (core) |
| Maturity | stable, limited | beta, experimental Compose | mature engine, weak wrappers | mature |
| Our maintenance load | low | low | **high** (JNI + security) | medium (JNI exists) |

### 1.6 Recommendation (provisional, see ADR 0002)

1. Do **not** build fcat on AndroidX PDF's viewer UI. Reading quality is the
   product, and that UI is a component we cannot shape. Keep it, together with the
   framework renderer, as the **baseline** in benchmarks.
2. **MuPDF is the leading candidate.** It is the only option that today combines
   outline, text, search, annotations, display-list re-rendering and an official
   Android artifact without us writing a JNI layer from scratch. Its licence is
   compatible with the licence we chose (ADR 0003).
3. **Bundled PDFium is the challenger.** If MuPDF loses on measured performance or
   on rendering fidelity for our corpus, PDFium is the permissive fallback, at the
   cost of owning JNI and security updates.
4. **The decision is deferred to the M1 prototype bake-off** defined in
   [PERFORMANCE.md](PERFORMANCE.md). Research has not settled which engine is
   faster on real devices for our documents. No published benchmark we found
   compares them on Android under a reader workload.

To keep the bake-off cheap, the app talks to the engine through one small interface
(`PdfEngine`, see ARCHITECTURE.md). That interface exists because we will genuinely
run two or three implementations side by side, not for abstraction's sake.

## 2. Competitors and existing open-source readers

| App | Licence | Engine | Notes |
|---|---|---|---|
| Xodo (Apryse) | proprietary | Apryse SDK | 10M+ installs. Reader, annotation, forms, scanner, conversion, cloud sync. Subscription plans; "1 free action per day" on web tools. [Doc/Reported] |
| GrapheneOS PdfViewer | MIT / Apache-2.0 files present | pdf.js in a sandboxed WebView | Security-first, needs no permissions. Minimal reading features. [Doc] |
| Librera | GPL-3.0 | MuPDF (AGPL) | Feature-rich, multi-format. README: development "frozen for an unpredictable time". [Doc] |
| Document Viewer | GPL-3.0+ | MuPDF, DjVu | EBookDroid fork. [Doc] |
| KOReader | AGPL-3.0 | (not stated on README) | E-ink first, reflow via k2pdfopt. Strong reading features, non-native UI. [Doc] |
| MuPDF viewer (Artifex) | AGPL-3.0 | MuPDF | Reference viewer, minimal UI. [Doc] |

**What Xodo users complain about.** [Reported: Google Play reviews visible
2026-10-28]

- Features moving behind a subscription, including the margin-crop tool readers used
  for comfortable reading.
- Daily usage limits on annotation.
- A cloud-sync failure that left a 0-byte file and lost a student's handwritten
  notes.

The Play listing's data-safety section says the app may collect location and
personal info and share data types with third parties.

**Opportunities for fcat.** These are hypotheses until tested with users.

- **Reading features that are never paywalled.** Margin crop, night and sepia modes,
  reflow-free fit modes, and fast position restore.
- **Never lose the user's work.** Atomic saves: write to a temp file, then replace.
  Never write to the original until a save has succeeded.
- **Native Material UI** with good large-screen and keyboard support. Most
  open-source readers have dated, non-native, or e-ink-first UIs.
- **Academic-specific navigation.**
  - Follow a citation or figure link and come back (a back stack for internal links).
  - Fast outline and page-label navigation, honouring roman-numeral front matter.
  - Two-up layout for textbooks on tablets.
- **Privacy as a feature.** No network permission at all in the initial scope.

**Things we deliberately do not copy:** scanning, file conversion, cloud sync,
accounts, AI summaries, and Office formats.

## 3. Licensing research (see ADR 0003)

- MuPDF is AGPL-3.0. Linking it makes the distributed app subject to the AGPL.
- GPL-3.0 §13 and AGPL-3.0 §13 explicitly allow combining GPLv3 code with AGPLv3
  code. The combined work is distributed under the AGPL for the MuPDF part.
- Apache-2.0 and BSD code (AndroidX, PDFium, Kotlin) can be incorporated into GPLv3
  works.
- Therefore **GPL-3.0-or-later** for fcat's own code is compatible with every
  candidate engine:
  - It keeps derivatives open, unlike Apache or MIT.
  - It does not force network-use clauses on our own code.
- If MuPDF is adopted, the distributed APK as a whole is effectively governed by the
  AGPL. That is acceptable: fcat has no server component.
- Consequence: fcat can never include proprietary SDKs. That means no Play Services
  ML Kit, no Firebase, no ad SDKs. This matches the product's non-goals.

Not legal advice. Before the first public release, confirm the distribution notices
(NOTICE file, third-party licence screen, source offer) with the final dependency
list.

## 4. Open questions

| # | Question | How it gets answered |
|---|---|---|
| Q1 | MuPDF vs PDFium vs platform: time to first page, scroll jank, zoom re-render latency, and memory on our corpus | M1 bake-off ([PERFORMANCE.md](PERFORMANCE.md)) |
| Q2 | APK size cost per ABI of MuPDF vs PDFium | M1: build both, measure with `apkanalyzer` |
| Q3 | Which `androidx.pdf` features work on the compat path (API 28–29, or ext < 13)? | Prototype on an API 28 emulator |
| Q4 | Rendering fidelity on tricky academic PDFs (Type 3 fonts, large vector figures, scanned + OCR layers) | Visual diff in the bake-off corpus |
| Q5 | Is MuPDF's official Maven artifact usable in CI and reproducible builds (hosting, checksums)? Or should we vendor or build it from source for F-Droid? | M1 + F-Droid inclusion policy review |
| Q6 | PDFium thread-safety specifics, and whether one document instance per worker is viable for memory | Partly answered: no PDFium call may run concurrently (`fpdfview.h`), so M1 uses one engine thread. Per-process isolation only if PDFium wins |
| Q7 | Final `applicationId` and repository owner | Maintainer decision before first release (ADR 0004) |

## Sources

- AndroidX PDF release notes: <https://developer.android.com/jetpack/androidx/releases/pdf>
- Android PDF viewer overview: <https://developer.android.com/develop/ui/views/layout/pdf/pdf-viewer>
- `PdfRenderer`: <https://developer.android.com/reference/android/graphics/pdf/PdfRenderer>
- `PdfRenderer.Page`: <https://developer.android.com/reference/android/graphics/pdf/PdfRenderer.Page>
- `PdfRendererPreV`: <https://developer.android.com/reference/android/graphics/pdf/PdfRendererPreV>
- Google Maven, `androidx.pdf` 1.0.0-beta01 POMs and AARs (inspected 2026-10-28)
- MuPDF licence: <https://mupdf.readthedocs.io/en/latest/license.html>
- MuPDF on Android: <https://mupdf.readthedocs.io/en/latest/guide/using-with-android.html>
- MuPDF Maven metadata: <https://maven.ghostscript.com/com/artifex/mupdf/fitz/maven-metadata.xml>
- Artifex MuPDF Android viewer: <https://github.com/ArtifexSoftware/mupdf-android-viewer>
- PDFium: <https://pdfium.googlesource.com/pdfium/>
- pdfium-binaries: <https://github.com/bblanchon/pdfium-binaries>
- AndroidPdfViewer: <https://github.com/barteksc/AndroidPdfViewer>
- PdfiumAndroidKt: <https://github.com/johngray1965/PdfiumAndroidKt>, and the
  `io.legere:pdfiumandroid` 2.0.3 artifacts on Maven Central (inspected 2026-10-28)
- PDFium threading note: <https://pdfium.googlesource.com/pdfium/+/refs/heads/main/public/fpdfview.h>
- F-Droid Inclusion Policy: <https://f-droid.org/docs/Inclusion_Policy/>
- GrapheneOS PdfViewer: <https://github.com/GrapheneOS/PdfViewer>
- Librera: <https://github.com/foobnix/LibreraReader>
- Document Viewer: <https://github.com/SufficientlySecure/document-viewer>
- KOReader: <https://github.com/koreader/koreader>
- Xodo on Google Play: <https://play.google.com/store/apps/details?id=com.xodo.pdf.reader>
- Xodo pricing: <https://xodo.com/pricing>
- GNU licence compatibility: <https://www.gnu.org/licenses/gpl-faq.html#AGPLv3>
