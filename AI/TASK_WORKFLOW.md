# Task workflow

## 1. Understand

- Restate the task in one sentence. Identify the roadmap milestone it belongs to.
- Read the docs listed in [README.md](README.md) for the areas it touches, and the
  code you will change.
- If the request conflicts with a rule, a non-goal or an accepted ADR, stop and say
  so.

## 2. Research (when facts are needed)

- Use primary sources: official reference docs, library source, release notes for
  the exact version.
- Record what you verified, and where. Mark anything unverified as a hypothesis.

## 3. Plan

- For anything beyond a small fix, write a short plan before writing code:
  - files to change;
  - approach;
  - tests;
  - risks.
- Put it in the PR or issue, or in `AI/scratch/`. Not in `docs/`.
- If the plan needs a new dependency, an abstraction, or a decision that is hard to
  reverse, get agreement first. Draft an ADR if one is warranted.

## 4. Implement

- Make the smallest change that fully solves the task, following
  [DEVELOPMENT_RULES.md](DEVELOPMENT_RULES.md).
- Update the docs that own any fact you changed, in the same change.

## 5. Verify

- Run `./gradlew assembleDebug lintDebug testDebugUnitTest`.
- Add or adjust tests per [docs/development/TESTING.md](../docs/development/TESTING.md).
- For rendering, scroll or memory changes, measure per
  [docs/PERFORMANCE.md](../docs/PERFORMANCE.md), or say plainly that it was not
  measured.

## 6. Review yourself

- Read the full diff:
  - Is there anything unrelated?
  - Is there anything speculative?
  - Is there any secret or local path?
  - Are there new strings without resources?
  - Are there missing content descriptions?
- Check that the commit or PR title follows the conventions in
  [CONTRIBUTING.md](../CONTRIBUTING.md).

## 7. Report

Report briefly and factually:

- what changed, and why;
- how it was verified, with the exact commands and their results;
- what was **not** done or not verified;
- open questions or follow-ups.

Never claim a check passed if it was not run.
