# Benchmark corpus manifest

The documents for the M1 bake-off and later regression runs. The ids and their purpose
are defined in [PERFORMANCE.md §2](../docs/PERFORMANCE.md#2-corpus).

The files themselves are **not** in the repository (PERFORMANCE.md §5). Put them in
`benchmark-corpus/local/`, which is git-ignored, named `<id>.pdf`.

## Rules for an entry

- Only documents whose licence allows redistribution go in this list, so anyone can
  rebuild the same corpus. Link the licence as stated by the source.
- Record the exact file: source URL, SHA-256, size in bytes, page count. If the source
  changes the file, the hash tells us, and the entry is updated in its own commit.
- Page count from `pdfinfo`, or from the engine under test if they disagree (note it).
- Generated documents (C7, C8) give the recipe instead of a URL.

## Entries

Status: **no external document selected yet.** Rows marked *open* need a candidate
that meets the rules above.

| Id | Kind | Source | Licence | Pages | Bytes | SHA-256 | Status |
|---|---|---|---|---|---|---|---|
| C1 | Short text paper (≈10 pages, LaTeX) | | | | | | open |
| C2 | Long textbook (≥ 800 pages, text + figures) | | | | | | open |
| C3 | Huge document (≥ 2000 pages) | | | | | | open |
| C4 | Heavy vector figures | | | | | | open |
| C5 | Scanned book (image pages, ± OCR layer) | | | | | | open |
| C6 | Large page sizes (posters, A0) | | | | | | open |
| C7 | Malformed / truncated PDFs | Generated, see below | n/a | varies | varies | per run | recipe |
| C8 | Password-protected | Generated, see below | n/a | per source | varies | per run | recipe |
| C9 | Type 3 fonts, unusual encodings | | | | | | open |

## Recipes for generated entries

These need `qpdf`. Run them on a selected C1 document so the results are comparable
with the plain one.

- **C7a, truncated:** the first half of C1's bytes.
  `head -c $(( $(stat -c %s C1.pdf) / 2 )) C1.pdf > C7a.pdf`
- **C7b, broken xref:** C1 with its `startxref` offset pointing at byte 0.
  `perl -0pe 's/startxref\s+\K\d+(?=\s+%%EOF\s*$)/0/' C1.pdf > C7b.pdf`
- **C8, encrypted:** C1 encrypted with AES-256, user password `fcat`.
  `qpdf --encrypt fcat owner 256 -- C1.pdf C8.pdf`

AES-256 encryption uses random salts, so C8's hash differs on every run. Record the
hash of the file actually used.
