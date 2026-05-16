# skaile-powers — Configuration

> **Every skaile-powers skill reads this file first.** skaile-powers skills are written to be **project-agnostic** — the workflow techniques apply to any codebase. This file is the **single project adapter**: it binds the generic skills to one project's paths, commands, and conventions. To use skaile-powers in another project, rewrite the **Project Bindings** section; everything above it is the skaile-powers method and rarely changes.

## Upstream

Workflow scaffold forked from **obra/superpowers v5.0.6**. Grilling, ubiquitous-language, and ADR techniques adapted from **mattpocock/skills**.

## Artifact Layout

All workflow artifacts live under one root — the **artifact root** (see Project Bindings):

```
<artifact-root>/
├── glossary/
│   └── glossary.md                  ← ubiquitous language (one global glossary)
├── decisions/                       ← ADRs, organized by category
│   ├── architecture/NNNN-<slug>.md
│   ├── design/NNNN-<slug>.md
│   └── code-style/NNNN-<slug>.md
├── specs/
│   └── NNNN-<topic>.md              ← design spec, sequential number
└── plans/
    └── NNNN-<topic>/                ← plan directory, SAME number as its spec
        ├── overview.md              ← links + dependency graph of the task beads
        ├── NNNN.1-<slug>.md         ← task "bead"
        └── NNNN.2-<slug>.md
```

## Numbering

- **Specs** carry a sequential 4-digit prefix: `0001-`, `0002-`, … Scan `specs/` for the highest number and increment.
- An **implementation plan** reuses its spec's number. Spec `0007-foo.md` → plan directory `plans/0007-foo/`.
- **Task beads** inside a plan use sub-numbers: `0007.1-`, `0007.2-`, … assigned in dependency order.
- **Decisions** are numbered per category — `architecture/0001`, `design/0001`, `code-style/0001` are independent sequences. Reference a decision as `<category>/NNNN` (e.g. `architecture/0003`).

## Glossary (Ubiquitous Language)

One global glossary at `<artifact-root>/glossary/glossary.md`. It is the project's **ubiquitous language** — the single canonical term for every domain concept.

**Every skill that explores the codebase reads the glossary first** and uses its vocabulary in all output: spec text, bead titles, test names, commit messages, refactor proposals. If the glossary does not exist yet, the first skill to resolve a domain term creates it lazily.

Format:

```md
# Glossary

## <cluster heading>

| Term | Definition (one sentence — what it IS) | Aliases to avoid |
|---|---|---|
| **Order** | A customer's request to purchase one or more items | purchase, transaction |

## Relationships

- An **Order** produces one or more **Invoices**

## Flagged ambiguities

- "account" was used for both **Customer** and **User** — resolved: distinct concepts.
```

Rules: be opinionated (pick one canonical term, list the rest as aliases-to-avoid); one-sentence definitions; only project-specific domain concepts, not generic programming terms; flag conflicts explicitly. The glossary is a glossary — never a spec, scratchpad, or decision log.

## Decisions (ADRs)

Architecture Decision Records live under `<artifact-root>/decisions/<category>/`, where category is one of `architecture`, `design`, or `code-style`.

**Offer an ADR only when all three are true:**

1. **Hard to reverse** — changing your mind later is costly.
2. **Surprising without context** — a future reader will look at the result and wonder "why this way?"
3. **A real trade-off** — genuine alternatives existed, and one was chosen for specific reasons.

If any one is missing, skip the ADR.

Format — a single paragraph is enough:

```md
# <short title of the decision>

<1-3 sentences: the context, what was decided, and why.>

<optional: Considered Options / Consequences — only when they add genuine value>
```

**Decision-log integration:** when an ADR is accepted, the commit that lands the related work also records the decision through the project's decision-log mechanism (see Project Bindings). The ADR file holds the full rationale; the decision log holds the index entry. The two stay in sync.

## Task Bead Frontmatter

Every task bead (`plans/NNNN-<topic>/NNNN.M-<slug>.md`) starts with this frontmatter:

```yaml
---
id: NNNN.M
spec: NNNN
title: <short imperative title>
depends_on: [NNNN.M, ...]   # beads that must finish first; [] if none
status: pending             # pending | in-progress | done
type: AFK                   # AFK = agent finishes unattended; HITL = needs the user
---
```

`status` is updated by the execution skills as work progresses. Beads discovered mid-implementation are appended with the next free sub-number and `status: pending`, and linked into `overview.md`.

---

## Project Bindings

> **Rewrite this section to use skaile-powers in another project.** Everything above is the skaile-powers method.

| Binding | This project (skaile-dev) |
|---|---|
| Artifact root | `docs/devlog/` |
| Package manager | Bun (`bun@1.3.9`) — never npm or yarn |
| Unit test command | `bun x --bun vitest run` |
| E2E test | Playwright |
| Format command | `bun run format` (Biome; **never** run Biome on `platform/`) |
| Quality gate | `quality mode=quick` |
| Deep code review | `skaile skill run audit scope=diff` (after a bead) or `scope=package target=<pkg>` (major feature / pre-merge) |
| Commit format | Skaile commit-spec (below) |
| Decision-log mechanism | Accepted ADRs are mirrored into the commit `decisions:` YAML block; a GitHub Action extracts them to the repo-root `decisions/` index on merge to main |
| Repo structure | Submodule monorepo — when work spans the shell repo + a submodule, commit the submodule first, then bump its pointer in the shell repo as a separate commit |

### Skaile Commit-Spec Format

```
type(scope): title (max 72 chars)

Human description: 1-3 sentences — what changed and why.

---agent---
scope: [<package-paths>]
type: feat|fix|refactor|docs|test|chore|perf|build
breaking: true|false
affects: [<downstream-packages>]

changes:
- <imperative description of each discrete change>

decisions:                  # omit if no ADR-worthy choices; else mirror accepted ADRs
- <decision summary>
  reason: <why>
  alternatives: [<rejected options>]
  revisit_when: <condition>
```

Types: `feat` (new feature), `fix` (bug fix), `refactor`, `docs`, `test`, `chore`, `perf`, `build`.

### Skill Authoring

> Consumed by `writing-skills`. Project-specific — rewrite for another skill host.

**Skill location:** skills live at `ai-assets/<domain>/skills/<skill-name>/`. Each skill directory contains:

- `SKILL.md` — required; YAML frontmatter + markdown body
- `CLI.md` — optional; CLI invocation docs
- `references/` — optional; reference material
- `validator.py` — optional; output validation

Skill names are flat (no group prefix) when referenced in `DOMAIN.md` or invoked via the `Skill` tool, even when the filesystem uses group subdirectories.

**SKILL.md frontmatter** — max 1024 characters total:

| Field | Required | Purpose |
|---|---|---|
| `name` | yes | kebab-case identifier (letters, numbers, hyphens) |
| `description` | yes | third-person, starts with "Use when…" — triggering conditions only |
| `source` | project | lineage: `CF` \| `SAXE` \| `MERGED` \| `MIGRATED` |
| `version` | project | semantic version string |
| `keywords` | project | array of discovery terms |
| `user_inputs` | project | array of `{key, prompt, required}` objects |
| `reads_from` / `writes_to` | project | arrays of file/path patterns |

**Registration:** add a new skill to its domain's `DOMAIN.md` inventory table.

---

## Skill Inventory

15 skills, by flat name — use these names when invoking via the `Skill` tool or skaile CLI.

| Skill | Group | Path |
|---|---|---|
| `using-skaile-powers` | meta | `skills/meta/using-skaile-powers/` |
| `brainstorming` | planning | `skills/planning/brainstorming/` |
| `grill-me` | planning | `skills/planning/grill-me/` |
| `writing-plans` | planning | `skills/planning/writing-plans/` |
| `writing-skills` | planning | `skills/planning/writing-skills/` |
| `systematic-debugging` | execution | `skills/execution/systematic-debugging/` |
| `test-driven-development` | execution | `skills/execution/test-driven-development/` |
| `executing-plans` | execution | `skills/execution/executing-plans/` |
| `dispatching-parallel-agents` | execution | `skills/execution/dispatching-parallel-agents/` |
| `subagent-driven-development` | execution | `skills/execution/subagent-driven-development/` |
| `using-git-worktrees` | execution | `skills/execution/using-git-worktrees/` |
| `requesting-code-review` | review | `skills/review/requesting-code-review/` |
| `receiving-code-review` | review | `skills/review/receiving-code-review/` |
| `verification-before-completion` | review | `skills/review/verification-before-completion/` |
| `finishing-a-development-branch` | review | `skills/review/finishing-a-development-branch/` |
