# Contributing to fcat

Thanks for helping. fcat is young: the best contributions right now are careful
research, benchmarks on real devices, and focused pull requests against the
[roadmap](docs/ROADMAP.md).

## Before you start

- Read [PRODUCT.md](docs/PRODUCT.md), especially the non-goals, and
  [ARCHITECTURE.md](docs/ARCHITECTURE.md).
- For anything bigger than a small fix, **open an issue first** and agree on the
  approach. Pull requests that add scope outside the roadmap will probably be
  declined, however good the code.
- Setup: [docs/development/SETUP.md](docs/development/SETUP.md).

## Workflow

1. Fork, or branch from `main`. Use a short, descriptive branch name with a type
   prefix: `feat/outline-panel`, `fix/zoom-anchor`, `docs/perf-method`,
   `perf/tile-cache`, `chore/agp-9.5`.
2. Make one focused change. Don't mix refactors with behaviour changes.
3. Run the CI checks locally:
   `./gradlew assembleDebug lintDebug testDebugUnitTest`.
4. Open a pull request against `main` and fill in the template.
5. A maintainer reviews it. `main` is merged by **squash**, so the PR title becomes
   the commit message.

## Commit and PR titles

Use [Conventional Commits](https://www.conventionalcommits.org/):
`type(scope): summary`. Write the summary in the imperative, under about 72
characters.

Types: `feat`, `fix`, `perf`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`.

Examples:

```text
feat(reader): restore zoom with reading position
perf(engine): reuse tile bitmaps from pool
docs(adr): accept MuPDF as rendering engine
```

## Quality bar

- **No new dependency without justification** in the PR: what it does, why
  platform/AndroidX code is not enough, its licence (must be GPL-3.0-compatible and
  non-proprietary), and its size cost.
- **No speculative abstraction.** Add an interface when there are two real
  implementations or a test genuinely needs it.
- **Performance-sensitive changes** (rendering, layout, scrolling, caching) include
  before/after numbers measured as described in
  [PERFORMANCE.md](docs/PERFORMANCE.md).
- **Tests** as described in [TESTING.md](docs/development/TESTING.md). A bug fix
  includes a regression test where practical.
- **Accessibility:** new UI has content descriptions, 48 dp targets, and works with
  TalkBack and font scaling.
- **Docs:** if behaviour, architecture or a decision changes, update the matching
  document in the same PR. Hard-to-reverse decisions get an
  [ADR](docs/decisions/README.md).
- **No secrets or personal data:** no keys, keystores, tokens, local paths or
  personal documents in commits. Test PDFs must have a clear redistributable
  licence.

## AI-assisted contributions

Allowed. You are responsible for every line: understand it, test it, and make sure
it follows [AI/DEVELOPMENT_RULES.md](AI/DEVELOPMENT_RULES.md). Say in the PR if a
substantial part was generated.

## Licence

By contributing, you agree that your contribution is licensed under the project's
licence, [GPL-3.0-or-later](LICENSE). There is no CLA.

## Conduct

Everyone taking part follows the [Code of Conduct](CODE_OF_CONDUCT.md).
