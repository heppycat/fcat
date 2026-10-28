# Development rules

Hard rules. If a task seems to require breaking one, stop and ask.

## Scope

- Do what the task asks. **No unrelated refactoring**, renaming, reformatting or
  "while I'm here" changes.
- Stay inside the [roadmap](../docs/ROADMAP.md) and the
  [non-goals](../docs/PRODUCT.md#non-goals-v10). Never add network use,
  accounts, analytics, ads or AI services.

## Dependencies

- Prefer the platform and the AndroidX libraries already in the catalog.
- A new dependency needs a written justification in the PR covering:
  - purpose;
  - why existing code is not enough;
  - licence (GPL-3.0-compatible, not proprietary);
  - size.
- Versions go only in `gradle/libs.versions.toml`. Never use dynamic versions
  (`+`, `latest.release`).
- Never add repositories, mirrors or proxies to the build files.

## Abstractions

- No interface, base class, DI framework or module split without a concrete
  present need: two real implementations, or a test that cannot work otherwise.
- `PdfEngine` is the one sanctioned seam, because of the M1 bake-off (ADR 0002).

## APIs and facts

- **Never assume an API exists or behaves a certain way.** Check the official
  reference or the library source for the exact version in the catalog. Respect
  `minSdk` 28: guard newer APIs with `Build.VERSION.SDK_INT`, or with
  `SdkExtensions` where the reference requires it.
- Do not present guesses as facts in code comments, docs or reports. Label
  hypotheses as hypotheses.
- Never invent benchmark numbers, test results, contact addresses, usernames or
  URLs.

## Code conventions

- Kotlin official code style. `.editorconfig` is authoritative. Lines are at most
  120 characters.
- Packages are by feature (`ui`, `reader`, `engine`, `library`), with dependencies
  pointing `ui → reader → engine` only.
- Coroutines for async work. No blocking calls on the main thread. Engine calls run
  on engine-owned dispatchers.
- Compose:
  - state hoisted;
  - side effects in effect handlers;
  - stable parameters on hot paths;
  - no allocation per frame in drawing code.
- Bitmaps are pooled and recycled through the cache, never allocated per frame.
- Comments explain *why*, not *what*. Match the density of the surrounding code.
- UI strings go in `res/values/strings.xml`. Every control has a content
  description.

## Safety

- Never commit secrets, keystores, `local.properties`, personal paths or personal
  documents.
- Never weaken `.gitignore`, the wrapper checksum, or CI checks to make something
  pass.
- Never delete or overwrite user files outside the task. Never force-push shared
  branches.
