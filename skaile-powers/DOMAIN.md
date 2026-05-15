---
name: skaile-powers
description: Project-agnostic development workflow skills — a fork of obra/superpowers v5.0.6 with mattpocock/skills techniques (grilling, ubiquitous language, ADRs) integrated. Covers the full arc from grilling an idea through Domain-Driven design, numbered specs, bead-based plans, execution, and review.
type: domain
building_blocks:
  references: 'config.md — the single project adapter: artifact layout, numbering, glossary/decision formats, project bindings, skill inventory'
  skills: 'using-skaile-powers, brainstorming, grill-me, writing-plans, writing-skills, systematic-debugging, test-driven-development, executing-plans, dispatching-parallel-agents, subagent-driven-development, using-git-worktrees, requesting-code-review, receiving-code-review, verification-before-completion, finishing-a-development-branch'
stage: beta
upstream: obra/superpowers v5.0.6 + mattpocock/skills
---

# Skaile Powers

A development workflow skill set: 15 composable skills covering the full arc from idea to merged branch. Forked from [obra/superpowers](https://github.com/obra/superpowers) v5.0.6, with the grilling, ubiquitous-language, and ADR techniques from [mattpocock/skills](https://github.com/mattpocock/skills) integrated.

**The skills are project-agnostic.** The workflow techniques apply to any codebase. A single file — `references/config.md` — is the **project adapter**: it binds the generic skills to one project's paths, commands, and conventions. To use skaile-powers elsewhere, rewrite the *Project Bindings* section of `config.md`; nothing in the skills themselves is hard-coded to a project.

The workflow is oriented around **Domain-Driven Design**: a shared ubiquitous language (the glossary) underpins every skill, and hard-to-reverse decisions are captured as categorized ADRs.

---

## The Artifact Model

skaile-powers produces four kinds of durable artifact, all under one **artifact root** (set in `config.md`):

```
<artifact-root>/
├── glossary/glossary.md             ← ubiquitous language — one global glossary
├── decisions/                       ← ADRs, by category
│   ├── architecture/NNNN-<slug>.md
│   ├── design/NNNN-<slug>.md
│   └── code-style/NNNN-<slug>.md
├── specs/NNNN-<topic>.md            ← design spec, sequential 4-digit number
└── plans/NNNN-<topic>/              ← plan dir, SAME number as its spec
    ├── overview.md                  ← links + dependency graph of the beads
    ├── NNNN.1-<slug>.md             ← task "bead"
    └── NNNN.2-<slug>.md
```

**The numbering thread.** A spec gets a sequential number (`0007`). Its implementation plan reuses that number (`plans/0007-…/`). The plan's tasks are **beads** strung on that thread — `0007.1`, `0007.2`, … Each bead is a small file with frontmatter declaring `depends_on` and `status`. Work discovered mid-implementation becomes a new follow-up bead on the same thread.

**The living documents.** `brainstorming` and `grill-me` maintain the **glossary** (the ubiquitous language) and the **decisions** (ADRs) inline as designs crystallize. Every other skill reads them: the glossary supplies canonical vocabulary; ADRs record decisions not to re-litigate.

Full formats, numbering rules, and bead frontmatter schema live in `config.md`.

---

## The Full Workflow

Every meaningful session follows this arc. Start at `using-skaile-powers`, identify the work, and follow the path through to `finishing-a-development-branch`.

```
  ┌──────────────────────────────────────────┐
  │  Session start — using-skaile-powers     │
  └─────────────────┬────────────────────────┘
                    │
        ┌───────────▼────────────┐
        │  What kind of work?    │
        └──┬────┬────┬───────┬───┘
           │    │    │       │
     new idea  bug  just    authoring
   /feature  /fail  talking  a skill
           │    │   it through │
           ▼    ▼      │        ▼
   brainstorming │   grill-me   writing-skills ──► done
   (grill →      │   (grill →
    numbered     │    glossary +
    spec)        │    ADRs, no spec)
           │     ▼
           │  systematic-debugging
           │     │
           │  test-driven-development
           ▼     │  (fix, commit)
   writing-plans │
   (overview +   │
    bead files)  │
           │     │
           ▼     │
  ┌──────────────────────────────────────────────────┐
  │  subagent-driven-development  (default)           │
  │   • one fresh subagent per bead                   │
  │   • each bead uses test-driven-development        │
  │   • bead status frontmatter updated as it lands   │
  │   • new work → follow-up bead on the thread       │
  │  — dispatching-parallel-agents for parallel runs —│
  └─────────────────┬─────────────────────────────────┘
                    ▼
  ┌──────────────────────────────────────────┐
  │  Review loop                             │
  │  verification-before-completion          │
  │     pass │      │ fail → fix → repeat    │
  │  requesting-code-review                  │
  │  receiving-code-review                   │
  └─────────────────┬────────────────────────┘
                    ▼
          finishing-a-development-branch
          (merge / PR / keep / discard)
```

Throughout, `brainstorming` and `grill-me` keep the **glossary** and **ADRs** current — those feed back into every skill's vocabulary and decisions.

---

## Grilling: brainstorming vs. grill-me

Both skills run a **grilling session** — relentless one-question-at-a-time interrogation, a recommended answer for every question, explore-the-codebase-instead-of-asking, and inline glossary + ADR maintenance. They differ in commitment:

```
        Do you intend to build this?
                  │
        yes ──────┴────── no
         │                │
   brainstorming        grill-me
   → grills the idea    → grills a plan / a fuzzy
   → numbered spec        concept / an assumption
   → hands off to        → updates glossary + ADRs
     writing-plans       → produces NO spec, no handoff
```

Use `grill-me` to think something through or sharpen terminology without committing. Use `brainstorming` when you intend to build and need an approved spec.

---

## Choosing an Execution Mode

Once you have a plan, always execute its beads with subagents — never inline.

```
  Have a plan (overview + beads)?
         │
    no ──┴──► write one with writing-plans
         │
        yes
         │
    Need parallel runs across sessions?
         │
    yes ─┴──► dispatching-parallel-agents
    │         (one ready bead per parallel agent)
    │
    no ──────► subagent-driven-development   ← default
               (fresh subagent per bead, same session,
                spec + code-quality review per bead)
```

`subagent-driven-development` is the default. `executing-plans` (inline, no subagents) exists as a fallback for environments without subagent support. Each bead, in any mode, follows `test-driven-development`.

---

## Isolation: Set Up a Worktree First

Before non-trivial implementation work, create an isolated git worktree so the main checkout stays clean.

```
  starting a feature?
          │
          ▼
  using-git-worktrees      ← isolated workspace
          │
  brainstorming → writing-plans → subagent-driven-development
          │
          ▼
  finishing-a-development-branch   ← merge back, clean up worktree
```

For submodule-aware repos (see `config.md` → Project Bindings): commit the submodule first, then bump its pointer in the shell repo as a separate commit. `finishing-a-development-branch` handles both.

---

## Review Loop Detail

```
  implementation complete
          │
          ▼
  verification-before-completion
  ┌───────────────────────────────────────┐
  │  run the project gates (config.md):   │
  │  • test command — 0 failures          │
  │  • format command — exit 0            │
  │  • quality gate — no blockers         │
  │  • no regressions                     │
  └───────────────┬───────────────────────┘
        pass │    │ fail → fix → re-verify
             ▼
  requesting-code-review
  ┌───────────────────────────────────────┐
  │  dispatch a code-reviewer subagent;    │
  │  escalate large/high-risk diffs to the │
  │  project's deep-review tool (config.md)│
  └───────────────┬────────────────────────┘
             ▼
  receiving-code-review
  ┌───────────────────────────────────────┐
  │  per issue: acknowledge → fix (TDD) →  │
  │  commit (project commit format) →      │
  │  respond with the resolution           │
  └───────────────┬────────────────────────┘
             ▼
  finishing-a-development-branch
  (merge locally / open PR / keep / discard)
```

---

## Common Quick Paths

### New feature
```
using-skaile-powers → brainstorming (grill → numbered spec)
  → writing-plans (overview + beads)
  → using-git-worktrees
  → subagent-driven-development
  → verification-before-completion
  → finishing-a-development-branch
```

### Bug fix
```
using-skaile-powers → systematic-debugging (find root cause)
  → test-driven-development (failing test → fix → pass)
  → verification-before-completion
  → finishing-a-development-branch
```

### Think something through
```
using-skaile-powers → grill-me
  → (glossary + ADRs updated; no spec)
```

### Write a new skill
```
using-skaile-powers → brainstorming (design the skill)
  → writing-skills (author SKILL.md)
  → verification-before-completion
```

### Execute an existing plan
```
using-skaile-powers → subagent-driven-development
  → verification-before-completion
  → finishing-a-development-branch
```

---

## Skill Reference

| Skill | Group | When to Use |
|---|---|---|
| `using-skaile-powers` | meta | Session start — establishes available skills and invocation rules |
| `brainstorming` | planning | Grilling session that turns an idea into an approved, numbered spec |
| `grill-me` | planning | Grilling discussion that hardens terminology + decisions; produces no spec |
| `writing-plans` | planning | Turn a spec into a plan overview + dependency-tracked task beads |
| `writing-skills` | planning | When creating or editing skills in ai-assets |
| `systematic-debugging` | execution | On any bug, test failure, or unexpected behavior |
| `test-driven-development` | execution | When implementing features or fixing bugs |
| `subagent-driven-development` | execution | **Default execution mode** — fresh subagent per bead + spec + code-quality review |
| `dispatching-parallel-agents` | execution | Independent beads run in parallel across sessions |
| `executing-plans` | execution | Inline bead execution — fallback for no-subagent environments |
| `using-git-worktrees` | execution | Before starting isolated feature work |
| `requesting-code-review` | review | After completing beads or features |
| `receiving-code-review` | review | When responding to code review feedback |
| `verification-before-completion` | review | Before claiming work is complete |
| `finishing-a-development-branch` | review | When implementation is reviewed and ready to land |

---

## Building Blocks

| Path | Skill | Purpose |
|---|---|---|
| `references/config.md` | — | The single project adapter: artifact layout, numbering, glossary/ADR formats, project bindings, skill inventory |
| `skills/meta/using-skaile-powers/` | `using-skaile-powers` | Session-start skill |
| `skills/planning/brainstorming/` | `brainstorming` | Grill an idea → numbered spec; maintains glossary + ADRs |
| `skills/planning/grill-me/` | `grill-me` | Grill a plan/concept → glossary + ADR updates, no spec |
| `skills/planning/writing-plans/` | `writing-plans` | Plan overview + bead task files |
| `skills/planning/writing-skills/` | `writing-skills` | New skill authoring |
| `skills/execution/systematic-debugging/` | `systematic-debugging` | Root-cause-first debugging |
| `skills/execution/test-driven-development/` | `test-driven-development` | Red-green-refactor TDD |
| `skills/execution/subagent-driven-development/` | `subagent-driven-development` | **Default execution** — fresh subagent per bead with spec + quality review |
| `skills/execution/dispatching-parallel-agents/` | `dispatching-parallel-agents` | Parallel bead dispatch across sessions |
| `skills/execution/executing-plans/` | `executing-plans` | Inline bead execution (no-subagent fallback) |
| `skills/execution/using-git-worktrees/` | `using-git-worktrees` | Isolated worktree setup |
| `skills/review/requesting-code-review/` | `requesting-code-review` | Code review dispatch |
| `skills/review/receiving-code-review/` | `receiving-code-review` | Code review response |
| `skills/review/verification-before-completion/` | `verification-before-completion` | Verification gate before completion claims |
| `skills/review/finishing-a-development-branch/` | `finishing-a-development-branch` | Branch completion: merge/PR/keep/discard |

---

## The Project Adapter (`config.md`)

All 15 skills read `references/config.md` at the start of every invocation. It is the **only** project-specific file — everything else is generic. It defines:

- **Artifact layout & numbering** — the `docs/devlog`-style tree, sequential spec numbers, bead sub-numbers
- **Glossary format** — the ubiquitous-language table structure
- **Decision (ADR) format** — categories, the 3-criteria offer test, the decision-log integration
- **Bead frontmatter schema** — `id`, `spec`, `depends_on`, `status`, `type`
- **Project Bindings** — package manager, test/format/quality commands, commit format, repo structure. *This is the section you rewrite to use skaile-powers in another project.*
- **Skill inventory** — the 15 flat skill names

To change a path or convention, edit `config.md` only — all skills pick it up.

---

## Upstream Sync

Workflow scaffold from `obra/superpowers v5.0.6`; grilling, ubiquitous-language, and ADR techniques from `mattpocock/skills`. To sync:

1. Check upstream release notes for the relevant skill
2. `diff` the adapted SKILL.md against the new upstream version
3. Cherry-pick changes that don't conflict with skaile-powers adaptations
4. Update the `upstream` field in this DOMAIN.md and in `config.md`

---

## Relationship to Other Domains

- **superpowers plugin** (`obra/superpowers`): generic predecessor. During migration both coexist; `skaile-powers:*` takes priority, superpowers is the fallback for anything not yet in this domain.
- **skaile-development**: monorepo operation skills (`implement`, `git`, `audit`, `devlog`). `skaile-powers` is the meta-workflow layer — how to grill, design, plan, and review. The two complement each other: skaile-powers orchestrates the workflow, skaile-development performs the project-specific operations the workflow's `config.md` bindings point at.
