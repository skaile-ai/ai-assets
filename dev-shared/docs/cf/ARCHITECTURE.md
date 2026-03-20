# Architecture

## System Overview

The concept-forge-skills pipeline transforms a user's app idea into a complete
blueprint through sequential/parallel steps. Each step is a skill that reads
from earlier steps and writes to its own folder. Skills run standalone or
orchestrated — file existence is the only gate between steps.

## Pipeline Boundaries

```
┌──────────────┐
│ 01_project   │ brief.md, goals.md, comparable.md
│ (user input) │ Boundary: natural language → structured YAML frontmatter
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ _research    │ general/, {step}/, findings/
│ (research)   │ Boundary: web data → structured markdown findings + user_input.json
└──────┬───────┘
       │
  ┌────┼────────────────┐
  ▼    ▼                ▼
┌─────┐ ┌──────┐ ┌──────────┐
│ 03  │ │  04  │ │    05    │
│feat.│ │brand │ │techstack │   Parallel. Each reads 01_project/.
└──┬──┘ └──┬───┘ └────┬─────┘
   │       │          │
   ▼       │          ▼
┌─────┐    │    ┌──────────┐
│ 05b │    │    │          │
│arch.│◄───┼────┤          │   Boundary: features + stack → system architecture
│     │    │    │          │   Output: architecture.md (apps, data flow, protocols)
└──┬──┘    │    └──────────┘
   │       │
   ▼       │
┌─────┐    │
│ 06  │    │
│data │    │          Boundary: features + arch → semantic entities + relationships
│model│    │          Output: model.dbml, model.json (stack-independent)
└──┬──┘    │
   │       │
   └───────┼──────────┘
           ▼
    ┌──────────────┐
    │  07_screens  │   Boundary: all inputs → screen specs with component inventory
    │              │   Consumes: features, brand tokens, tech stack, architecture, data model
    └──────────────┘
```

## Orchestrator

```
┌──────────────┐
│ Orchestrator │──── user communication (direct)
│  (controller)│
└──────┬───────┘
       │ dispatches
       ▼
┌──────────────┐
│  Skill (as   │──► _concept/ artifacts
│  subagent)   │
└──────────────┘
```

The orchestrator handles pipeline management AND user communication directly.
Skills can also run standalone — checking their own hard_gates and collecting
their own inputs. After standalone completion, the orchestrator can suggest
next steps.

## Special Folders

### _research/ — Research & User Input Layer
Written by `cf_research` (parallel mode) and skills saving user inputs. Read by ALL skills.
Step subfolders hold per-step research and `user_input.json` files; `general/` holds
cross-cutting topics (domain, competitors, audiences, etc.).

### _standards/ — Discovered Codebase Standards
Written by `cf_discover_standards` (parallel mode). Read by ALL skills via
`cf_standards_inject` matching. Contains conventions extracted from existing
codebases, organized by domain (api/, database/, ui/, naming/, testing/, architecture/).

## Data Shape Contracts

| Boundary | Input | Output | Validated by |
|----------|-------|--------|-------------|
| User → 01_project | Conversational answers | `brief.md` with YAML frontmatter | `cf__shared/frontmatter.md` |
| 01_project → 03_features | Brief exists | Feature `.md` files with priority, roles | `cf__shared/frontmatter.md` |
| 01_project → 04_brand | Brief exists + reference URLs | `identity.md` + `tokens.json` | JSON schema for tokens |
| 01_project → 05_techstack | Brief exists | `stack.md` with tech choices in frontmatter | `cf__shared/frontmatter.md` |
| 03_features + 05_techstack → 05b_architecture | Features + stack exist | `architecture.md` (apps, data flow, protocols) | `cf__shared/frontmatter.md` |
| 03_features + 05_techstack + 05b_architecture → 06_datamodel | Features + stack + architecture exist | `model.json` (TypeBox-validated) | `cf__shared/semantic_types.md` |
| All → 07_screens | All artifacts exist | Screen `.md` with implements[], data_entities[] | `cf__shared/frontmatter.md` |

## Gates

File existence is the only gate between pipeline steps. There is no status
lifecycle or approval mechanism — if the required files exist, a skill can proceed.

## Cross-Reference Flow

```
03_features/*.md          07_screens/*.md          06_datamodel/model.json
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│ screens: []  │────────►│ implements:  │         │ from_features│
│              │◄────────│  [03_feat/…] │         │  [03_feat/…] │
│ data_entities│◄────────┼──────────────┘         └──────────────┘
│  []          │         │ data_entities│──────────────────┘
└──────────────┘         └──────────────┘
```

Downstream skills register back into upstream files via `cf__shared/feedback_loop.md`.

## Module Ownership

| Folder | Owner skill | Can read from |
|--------|------------|---------------|
| `01_project/` | `cf_concept_overview` | — |
| `_research/` | `cf_research` | — (special, all skills can read) |
| `_standards/` | `cf_discover_standards` | — (special, all skills can read) |
| `03_features/` | `cf_concept_functionality_features` | `01_project/`, `_research/`* |
| `04_brand/` | `cf_concept_brand_visual` | `01_project/`, `_research/`* |
| `05_techstack/` | `cf_concept_techstack` | `01_project/`, `03_features/` |
| `05b_architecture/` | `cf_concept_architecture` | `01_project/`, `03_features/`, `03b_behavior/`*, `05_techstack/` |
| `06_datamodel/` | `cf_concept_datamodel` | `01_project/`, `03_features/`, `05_techstack/`, `05b_architecture/` |
| `07_screens/` | `cf_concept_ui_screens` | all above |

*\* optional*

## Profiles

Reusable configuration presets stored in `cf__shared/profiles.json`. Profiles
define route, complexity, research depth, and standards injection settings.
Resolution order: project override (`_concept/profile.json`) > selected profile > default.

## Refactor Checklist

- [ ] Boundary contracts (frontmatter fields) unchanged or versioned
- [ ] Ownership map still accurate
- [ ] Cross-references (screens↔features, model→features) still valid
- [ ] cf__shared/ docs updated in same change
