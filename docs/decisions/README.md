# Architecture decision records

Each significant, hard-to-reverse decision gets a short record here. Number them in
order and never reuse a number. To change a decision, write a new record that
supersedes the old one, and update the old record's status line.

Status values:

- **Proposed**: a direction, not yet settled. Usually waiting on a prototype.
- **Accepted**: in force.
- **Superseded by NNNN**: replaced by a later record.

| # | Decision | Status |
|---|---|---|
| [0001](0001-native-kotlin-compose.md) | Native Android: Kotlin + Jetpack Compose, single app module | Accepted |
| [0002](0002-rendering-engine.md) | Rendering engine: MuPDF leading, PDFium challenger, decided by M1 bake-off | **Proposed** |
| [0003](0003-license.md) | Licence: GPL-3.0-or-later | Accepted |
| [0004](0004-platform-baseline.md) | minSdk 28, compileSdk/targetSdk 37, provisional applicationId | Accepted (applicationId provisional) |

## Template

```markdown
# NNNN. Title

- Status: Proposed | Accepted | Superseded by NNNN
- Date: YYYY-MM-DD

## Context
What forces are at play. Link evidence (docs/RESEARCH.md, benchmarks).

## Decision
What we will do.

## Consequences
What becomes easier, what becomes harder, what we must now watch.
```
