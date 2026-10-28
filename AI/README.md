# AI agent guide

This folder holds **stable rules for AI coding agents**. Public, human-facing facts
live in `docs/`. Link to them; never copy them here.

## Read before any change

1. [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md): what fcat is and where it stands now.
2. [DEVELOPMENT_RULES.md](DEVELOPMENT_RULES.md): conventions and hard constraints.
3. [TASK_WORKFLOW.md](TASK_WORKFLOW.md): how to carry a task from request to report.
4. The `docs/` files relevant to the task:

| Task touches | Read |
|---|---|
| Features, scope | [docs/PRODUCT.md](../docs/PRODUCT.md), [docs/ROADMAP.md](../docs/ROADMAP.md) |
| Code structure, dependencies | [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md), [docs/decisions/](../docs/decisions/README.md) |
| UI | [docs/DESIGN_PRINCIPLES.md](../docs/DESIGN_PRINCIPLES.md) |
| Rendering, scrolling, memory | [docs/PERFORMANCE.md](../docs/PERFORMANCE.md) |
| Engines, licences | [docs/RESEARCH.md](../docs/RESEARCH.md) |
| Build, tests, release | [docs/development/](../docs/development/) |

## Stable rules vs task context

- **Here and in `docs/`:** rules and facts that outlive a single task. Change them
  only as a deliberate, reviewed edit.
- **Task-specific notes** (plans, scratch findings, hand-off notes): put them in the
  issue or PR description, or in `AI/scratch/`, which is git-ignored. Do not commit
  task notes into this folder or into `docs/`.
- When a stable fact changes, for example a decision is accepted, update the single
  place that owns it. `PROJECT_CONTEXT.md` only links to it.
