---
name: writing-plans
description: Use when you have an approved spec and need an implementation plan. Produces a plan overview plus a set of small, dependency-tracked task "beads" — one file per vertical slice.
---

> Read `skaile-powers/references/config.md` before proceeding.

# Writing Plans

## Overview

Turn an approved spec into an implementation plan: an **overview** that maps the work, plus a set of small **task beads** — one file per task, strung on a numbered thread, each declaring its dependencies and status.

Assume the engineer (or subagent) executing a bead is skilled but knows almost nothing about this codebase. Each bead must be self-contained: complete file paths, complete code, exact commands. DRY, YAGNI, TDD, frequent commits.

**Announce at start:** "I'm using the writing-plans skill to create the implementation plan."

## The Numbering Thread

A plan reuses its spec's number (see `config.md` → Numbering). Spec `0007-foo.md` produces:

```
<artifact-root>/plans/0007-foo/
├── overview.md          ← maps the work, lists the bead dependency graph
├── 0007.1-<slug>.md     ← task bead
├── 0007.2-<slug>.md
└── 0007.3-<slug>.md
```

Beads are numbered `0007.1`, `0007.2`, … in dependency order.

## Step 1 — Scope Check

If the spec covers multiple independent subsystems, it should have been decomposed during brainstorming. If it wasn't, stop and suggest one plan per subsystem. Each plan must produce working, testable software on its own.

## Step 2 — Map the Files

Before defining beads, map which files will be created or modified and what each is responsible for. This locks in decomposition decisions.

- Design units with clear boundaries and one responsibility each. Favor **deep modules** — a lot of behavior behind a small, stable interface.
- Files that change together live together. Split by responsibility, not by technical layer.
- In existing codebases, follow established patterns. Use the glossary's vocabulary for module and concept names.

## Step 3 — Decompose Into Beads (Vertical Slices)

Break the plan into **tracer-bullet** beads. Each bead is a thin **vertical slice** that cuts through every layer end-to-end (schema → logic → API → UI → tests) — NOT a horizontal slice of one layer.

```
Bead rules:
- Each bead delivers a narrow but COMPLETE path through every layer it touches
- A completed bead is independently demoable or verifiable
- Prefer many thin beads over few thick ones
- Each bead is sized at roughly one focused work session
```

**Beads are vertical slices.** Each bead should be independently shippable —
a thin slice through all the layers it needs (data, logic, interface), not a
horizontal layer that only becomes useful once a later bead lands. If a bead
cannot be demoed or tested on its own, re-slice it.

Classify each bead:

- **AFK** — an agent can implement and complete it unattended.
- **HITL** — needs the user (an architectural decision, a design review, a credentials step). Prefer AFK; mark HITL only when human interaction is genuinely required.

Assign `depends_on` honestly — a bead lists every other bead that must finish before it can start.

## Step 4 — Write the Overview

Save `overview.md` in the plan directory:

````markdown
# <Topic> — Implementation Plan

> Spec: `../../specs/NNNN-<topic>.md`
> Execute with `skaile-powers:subagent-driven-development` (default) or `skaile-powers:dispatching-parallel-agents` for parallel sessions.

**Goal:** <one sentence — what this builds>

**Architecture:** <2-3 sentences — the approach>

## Beads

| Bead | Title | Depends on | Type | Status |
|---|---|---|---|---|
| NNNN.1 | <title> | — | AFK | pending |
| NNNN.2 | <title> | NNNN.1 | AFK | pending |
| NNNN.3 | <title> | NNNN.1 | HITL | pending |

## Dependency graph

```
NNNN.1 ──┬──► NNNN.2
         └──► NNNN.3
```

## File map

- `path/to/file` — created by NNNN.1, extended by NNNN.2
- `path/to/other` — created by NNNN.2
````

The `Status` column mirrors each bead's frontmatter `status`; execution skills keep it current.

## Step 5 — Write Each Bead

One file per bead: `NNNN.M-<slug>.md`. Frontmatter schema is in `config.md`.

````markdown
---
id: NNNN.M
spec: NNNN
title: <short imperative title>
depends_on: [NNNN.1]
status: pending
type: AFK
---

# <title>

## What to build

A concise description of this vertical slice — the end-to-end behavior, not a
layer-by-layer implementation log. Use the glossary's vocabulary.

## Files

- Create: `exact/path/to/file`
- Modify: `exact/path/to/existing` (lines if known)
- Test: `exact/path/to/test`

## Steps

- [ ] **Write the failing test**

```
<actual test code>
```

- [ ] **Run the test, verify it fails**

Run: `<exact test command — see config.md>`
Expected: FAIL with `<expected message>`

- [ ] **Write the minimal implementation**

```
<actual implementation code>
```

- [ ] **Run the test, verify it passes**

Run: `<exact test command>`
Expected: PASS

- [ ] **Commit** using the project commit format (see `config.md`)

## Acceptance criteria

- [ ] <observable criterion 1>
- [ ] <observable criterion 2>
````

Each step is one action (2-5 minutes). Write the failing test → run it → implement → run it → commit.

## No Placeholders

Every step must contain the actual content an engineer needs. These are **plan failures** — never write them:

- "TBD", "TODO", "implement later", "fill in details"
- "Add appropriate error handling" / "add validation" / "handle edge cases"
- "Write tests for the above" (without actual test code)
- "Similar to bead N" — repeat the code; beads may be executed out of order
- Steps that describe what to do without showing how
- References to types, functions, or methods not defined in any bead

## Self-Review

After writing every bead, review the whole plan with fresh eyes:

1. **Spec coverage** — skim each spec requirement. Can you point to a bead that implements it? Add beads for any gaps.
2. **Placeholder scan** — search for the red flags above. Fix them.
3. **Type consistency** — do types, signatures, and names match across beads? `clearLayers()` in one bead and `clearFullLayers()` in another is a bug.
4. **Dependency sanity** — does the `depends_on` graph have no cycles? Can the AFK beads actually run unattended?
5. **Language check** — do bead titles and descriptions use the glossary's canonical terms?

Fix issues inline.

## Execution Handoff

After saving the overview and all beads, append an entry to `devlog/DEVLOG.md` (see `config.md` → Devlog) noting the plan and bead count.

> "Plan complete — overview and N beads saved to `<plan-dir>/`. Recommended: execute with `skaile-powers:subagent-driven-development` (fresh subagent per bead, review between beads). For independent beads across parallel sessions, use `skaile-powers:dispatching-parallel-agents`. Which approach?"

**REQUIRED SUB-SKILL:** `skaile-powers:subagent-driven-development` is the default execution mode.
