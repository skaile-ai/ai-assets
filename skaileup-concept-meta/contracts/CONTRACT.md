---
name: "meta-concept-contract"
description: "Shared contract for multi-product umbrella concepts. Defines the _concept/ structure for projects composed of multiple independent subsystems, each with its own codebase, tech stack, and deployment model."
metadata:
  stage: "alpha"
  do_not_invoke: true
---

# Meta-Concept Contract — Multi-Product Umbrella Projects

**Do not invoke directly.** This is a dependency contract — `skaileup-project-concept` skills read this before operating.

## When to Use This Format

Use the meta-concept format when a project has **two or more** of the following:

- Multiple independent codebases (repos, submodules, or monorepo packages)
- Multiple tech stacks (e.g., NestJS + Nuxt + Astro)
- Multiple deployable products serving different audiences
- A shared identity / brand but independent feature sets
- Per-subsystem `_concept/` directories that already exist

If the project is a single app with one tech stack, use the standard `concept_structure.md` contract from `skaileup-shared/contracts/` instead.

## Relationship to Single-App Concepts

The meta-concept is a **routing layer**, not a replacement:

```
meta _concept/          ← this contract (umbrella view)
  └── references →
    subsystem-a/_concept/   ← standard concept_structure.md
    subsystem-b/_concept/   ← standard concept_structure.md
```

The meta-concept **never duplicates** detail that exists in a subsystem's own `_concept/`.
It describes:
- What the ecosystem is and why it exists (discovery)
- What subsystems compose it and how they relate (subsystem map)
- How they connect, share code, and deploy together (integration)
- What the current state and roadmap look like (plans)

Individual subsystem concepts describe their own features, screens, data models, and tech stacks.

---

## _concept/ Directory Structure

```
_concept/
├── discovery/
│   ├── 1_overview/
│   │   ├── brief.md              ← ecosystem elevator pitch
│   │   ├── goals.md              ← unified success criteria, scope, constraints
│   │   └── comparable.md         ← competitive positioning as a whole
│   └── 2_brand/
│       └── identity.md           ← shared brand identity (or reference)
│
├── 2_subsystems/
│   ├── index.md                  ← subsystem map: what exists, maturity, audiences
│   └── <subsystem_name>.md       ← one file per major subsystem
│
├── 3_integration/
│   ├── architecture.md           ← how subsystems connect (repos, packages, protocols)
│   ├── deployment.md             ← deployment topology per subsystem
│   └── shared_contracts.md       ← shared types, packages, conventions across subsystems
│
└── PLANS.md                      ← ecosystem roadmap and status
```

### What is NOT included (delegated to subsystem concepts)

| Artifact | Why excluded |
|---|---|
| `experience/features/` | Each subsystem defines its own features |
| `experience/screens/` | Each subsystem defines its own screens |
| `blueprint/` | Each subsystem has its own stack |
| `blueprint/datamodel/` | Each subsystem has its own data model |
| `experience/journeys/` | Journeys span subsystems but are authored per-product |
| `_grounding/` | Research is per-subsystem or lives in the shell repo's `_devlog/` |

---

## Frontmatter Schemas

### Universal Fields

Every markdown file in the meta `_concept/`:

```yaml
---
last_updated: YYYY-MM-DD
---
```

### discovery/brief.md

```yaml
---
elevator_pitch: "One sentence describing the whole ecosystem"
audience: "All user personas across all subsystems"
problem: "The unified problem the ecosystem solves"
hero_flow: "The most important cross-product user journey"
comparable_products: [product1, product2]
subsystem_count: N
last_updated: YYYY-MM-DD
---
```

### discovery/goals.md

Standard format — same as `skaileup-shared/contracts/frontmatter.md`.

### discovery/comparable.md

Standard format — same as `skaileup-shared/contracts/frontmatter.md`.

### discovery/brand/identity.md

Standard format — same as `skaileup-shared/contracts/frontmatter.md`.
If the brand identity lives in a subsystem's `_concept/`, this file can be a
reference pointer:

```yaml
---
reference: platform/_concept/.snapshots/full_concept_approved/discovery/3_brand/identity.md
last_updated: YYYY-MM-DD
---

# Brand Identity

See [platform brand identity](../../platform/_concept/.snapshots/full_concept_approved/discovery/3_brand/identity.md).
```

### 2_subsystems/index.md

```yaml
---
subsystem_count: N
last_updated: YYYY-MM-DD
---
```

### 2_subsystems/<subsystem_name>.md

```yaml
---
name: "Human-readable subsystem name"
repo: "github-org/repo-name"
path: "path/in/shell-repo"
type: library | application | content | website
audience: "Who uses this subsystem"
tech_stack: "Primary tech (e.g. NestJS + React + PostgreSQL)"
maturity: concept | prototype | alpha | beta | production
has_concept: true | false
concept_ref: "path/to/_concept/"
last_updated: YYYY-MM-DD
---
```

### 3_integration/architecture.md

```yaml
---
repo_topology: monorepo | multi-repo | shell-repo
package_manager: "bun | pnpm | npm"
shared_scope: "@skaile/*"
protocols: [workspace, npm, git-submodule]
last_updated: YYYY-MM-DD
---
```

### 3_integration/deployment.md

```yaml
---
deployment_models: N
last_updated: YYYY-MM-DD
---
```

### 3_integration/shared_contracts.md

```yaml
---
shared_package_count: N
last_updated: YYYY-MM-DD
---
```

### PLANS.md

```yaml
---
last_updated: YYYY-MM-DD
---
```

---

## Naming Rules

- Subsystem file names are `lowercase_snake_case` matching their directory name in the shell repo
- If a subsystem lives in a subdirectory (e.g., `forge/concept`), use an underscore join: `forge_concept.md`
- No spaces in any path
- Phase folder names use single-digit numbers: `discovery/`, `2_subsystems/`, `3_integration/`

---

## Read Direction

The meta-concept follows a simplified read direction:

| Section | May read from |
|---|---|
| `discovery/` | Nothing (root of the tree) |
| `2_subsystems/` | `discovery/` |
| `3_integration/` | `discovery/`, `2_subsystems/` |
| `PLANS.md` | All sections |

Each subsystem file in `2_subsystems/` may reference its own `_concept/` via `concept_ref`,
but the meta-concept never reads INTO a subsystem's `_concept/` for its own content.

---

## Cross-Product Journeys

When a user journey spans multiple subsystems (e.g., "developer creates a skill in ai-assets,
tests it in forge/concept, deploys it to the platform"), document it in `PLANS.md` under a
`### Cross-Product Journeys` section — not in a separate journeys folder. These are lightweight
narrative descriptions, not the full stories.json format.

---

## Subsystem Maturity Levels

| Level | Meaning |
|---|---|
| `concept` | `_concept/` exists but no implementation |
| `prototype` | Working code, not production-ready |
| `alpha` | Feature-incomplete, internal use only |
| `beta` | Feature-complete, limited external use |
| `production` | Deployed, maintained, externally used |

---

## PLANS.md Format

```markdown
# Plans

## Ecosystem: <Project Name>

### Scope

<One paragraph describing the ecosystem's current state and direction>

### Subsystem Status

| Subsystem | Maturity | Active work | Next milestone |
|---|---|---|---|
| <name> | <level> | <current focus> | <next goal> |

### Cross-Product Journeys

1. **<Journey name>**: <subsystem A> → <subsystem B> → <subsystem C>
   - Status: <working | planned | blocked>
   - <notes>

### Roadmap

- [ ] <milestone> — <target date or "TBD">
- [x] <completed milestone> — <completion date>

### Decisions

- YYYY-MM-DD: <decision and reasoning>

### Open Questions

- <question that affects multiple subsystems>
```

---

## Comparison with Single-App Concept

| Aspect | Single-app (`concept_structure.md`) | Meta-concept (this contract) |
|---|---|---|
| Scope | One application | Multiple products/repos |
| Features | `experience/features/` with groups | Delegated to subsystem `_concept/` |
| Screens | `experience/screens/` with groups | Delegated to subsystem `_concept/` |
| Data model | `blueprint/datamodel/` | Delegated to subsystem `_concept/` |
| Tech stack | `blueprint/techstack.md` | Per-subsystem in `2_subsystems/*.md` |
| Architecture | Intra-app modules and services | Inter-repo connections and protocols |
| Brand | `discovery/brand/` | Shared or referenced from a subsystem |
| Journeys | `experience/journeys/stories.json` | Lightweight cross-product narratives in PLANS.md |
| Permissions | Per-feature role matrix | Per-subsystem audience in subsystem files |
| Progress | Pipeline step checkboxes in PLANS.md | Subsystem maturity + roadmap milestones |

---

## When to Update the Meta-Concept

| Event | What to update |
|---|---|
| New subsystem added | `2_subsystems/index.md` + new `<subsystem>.md` |
| Subsystem reaches new maturity level | `2_subsystems/<subsystem>.md` maturity field + PLANS.md |
| New shared package or protocol | `3_integration/shared_contracts.md` |
| Deployment model changes | `3_integration/deployment.md` |
| New cross-product journey | `PLANS.md` Cross-Product Journeys section |
| Brand refresh | `discovery/brand/identity.md` |
| Competitive landscape shift | `discovery/comparable.md` |
| Major architectural decision | `3_integration/architecture.md` + PLANS.md Decisions |
