# Performance

Status: **no measurements yet.** This document defines what will be measured and
how, so the M1 engine bake-off and later regressions are judged the same way.
Targets below are **initial goals, not results**. Revise them only with recorded
evidence.

## 1. Metrics

| Metric | Definition | How measured |
|---|---|---|
| **TTFP**: time to first page | From the document `Intent` reaching the activity to the first page frame containing rendered content (low-resolution pass allowed) | Macrobenchmark with a custom trace section ending at first page draw; Perfetto |
| **TTSP**: time to sharp page | From the same start to the first visible page drawn at full resolution for the current zoom | Trace section |
| **Scroll jank** | Share of janky frames and P50/P90/P99 frame duration during a scripted fling over 50 pages | `FrameTimingMetric` |
| **Blank-tile time** | Total time any visible region shows no content (not even low resolution) during the scripted fling | Custom trace counter |
| **Zoom re-render latency** | Pinch end to sharp tiles for the visible region | Trace section |
| **Memory** | Peak PSS and Java/native heap during open + fling + zoom on the largest document | `MemoryUsageMetric` / `dumpsys meminfo` |
| **Search** | Time to first result and total time for a whole-document search | Trace section |
| **APK size** | Per-ABI download size of the release build | `apkanalyzer` |

Cold start = process killed. Warm start = process alive, activity recreated. Report
both for TTFP.

## 2. Corpus

Stored outside the repository (see §5). Each entry is recorded with its source URL,
licence, page count and file size. Only freely redistributable documents go in the
shared list.

| Id | Kind | Why it is here |
|---|---|---|
| C1 | Short text paper (≈10 pages, LaTeX) | Baseline, typical arXiv paper |
| C2 | Long textbook (≥ 800 pages, text + figures) | Layout cost, memory, outline depth |
| C3 | Huge document (≥ 2000 pages) | Open time must not scale with page count |
| C4 | Heavy vector figures (plots, CAD-like pages) | Worst-case render time per tile |
| C5 | Scanned book (image pages, ± OCR layer) | Image decode cost, memory |
| C6 | Large page sizes (posters, A0) | Tile scheduling at high zoom |
| C7 | Malformed / truncated PDFs | Must fail gracefully |
| C8 | Password-protected | Open path correctness |
| C9 | Type 3 fonts, unusual encodings | Fidelity |

## 3. Reference devices

At least three devices, each recorded with model, Android version, SoC and RAM:

- **Low:** ≤ 4 GB RAM, older mid-range SoC.
- **Mid:** 6–8 GB RAM, recent mid-range.
- **High:** recent flagship, or a tablet for large-screen layouts.

Emulators are fine for correctness, but not for performance numbers.

## 4. Initial targets (goals, unvalidated)

On the **mid** device unless noted:

| Metric | Target |
|---|---|
| TTFP, warm, C1 | ≤ 300 ms |
| TTFP, cold, C2/C3 | ≤ 800 ms, and must not grow with page count |
| Scroll jank, C1/C2 | < 1 % janky frames; P99 frame ≤ 2× frame budget |
| Blank-tile time, C2 fling | 0 ms at normal fling speed on mid/high; low device reported, not gated |
| Zoom re-render, C1 | ≤ 150 ms to sharp |
| Peak memory, C3 | Bounded; does not grow with pages scrolled (no leak) |
| Search, C2 | First result ≤ 500 ms; UI never blocks |

## 5. Method

- Release build with R8 and Baseline Profile, never debug, for any reported number.
- Device rules:
  - Plugged in, screen brightness fixed, airplane mode.
  - Thermal state recorded.
  - Wait for idle before each run.
- At least 10 iterations per scenario. Report median and P90, never a single run.
- Results go in `docs/benchmarks/YYYY-MM-DD-<topic>.md`. Each file states:
  - device and OS;
  - app commit;
  - engine and version;
  - corpus ids;
  - raw numbers, plus a link to traces where possible.
- A change that touches rendering, layout or the bitmap cache must include
  before/after numbers for the affected metrics.

The corpus files themselves are not committed (size, licences). Keep them under
`benchmark-corpus/local/`, which is git-ignored, and commit only the manifest.
