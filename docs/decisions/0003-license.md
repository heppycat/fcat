# 0003. Licence: GPL-3.0-or-later

- Status: Accepted
- Date: 2026-10-28

## Context

- The leading engine candidate, MuPDF, is AGPL-3.0. An app that links it must be
  distributed under AGPL-compatible terms and may not include proprietary
  components.
- GPLv3 and AGPLv3 each allow combining code under the other licence (§13 of each).
  Apache-2.0, BSD and MIT code (AndroidX, PDFium, Kotlin) can be included in GPLv3
  works.
- The product promises no ads, no accounts and no proprietary SDKs. We want
  improvements by redistributors to stay open, which a permissive licence does not
  guarantee.

## Decision

fcat's own code is licensed **GPL-3.0-or-later**. The full text is in `LICENSE`.
Contributions are accepted under the same licence, inbound = outbound. There is no
CLA.

## Consequences

- Compatible with every engine candidate. If MuPDF is adopted, the distributed APK
  as a whole carries AGPL obligations, which we meet by publishing the complete
  source.
- Proprietary SDKs can never ship in fcat: Play Services ML Kit, Firebase,
  analytics, ads. `androidx.pdf:pdf-ocr-play-services` is therefore excluded.
- Nobody, including the maintainers, can later relicense contributors' code to
  proprietary terms without their consent. That is intended.
- Before the first public release: add a third-party licence screen, add NOTICE
  attributions for bundled native libraries, and confirm source-offer obligations
  for the final dependency set. This is not legal advice. Have it reviewed if the
  project gains an organisation behind it.
