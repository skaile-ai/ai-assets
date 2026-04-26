# Architecture

## System Overview

The concept-forge-skills pipeline transforms a user's app idea into a complete
blueprint through 7 sequential/parallel steps. Each step is a skill that reads
from earlier steps and writes to its own folder.

## Pipeline Boundaries

```
┌──────────────┐
│ discovery   │ brief.md, goals.md, comparable.md
│ (user input) │ Boundary: natural language → structured YAML frontmatter
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ discovery/2_research  │ domain.md, competitors.md, audiences.md
│ (agent)      │ Boundary: web data → structured markdown findings
└──────┬───────┘
       │
  ┌────┼────────────────┐
  ▼    ▼                ▼
┌─────┐ ┌──────┐ ┌──────────┐
│ 03  │ │  04  │ │    05    │
│feat.│ │brand │ │techstack │   Parallel. Each reads discovery/.
└──┬──┘ └──┬───┘ └────┬─────┘
   │       │          │
   ▼       │          ▼
┌─────┐    │    ┌──────────┐
│ 05b │    │    │          │
│arch.│◄───┼────┤          │   Boundary: features + stack → system architecture
│     │    │    │          │   Output: architecture.md (apps, modules, data flow, protocols)
└──┬──┘    │    └──────────┘
   │       │
   ▼       │
┌─────┐    │
│ 06  │    │
│data │    │          Boundary: features + arch → PostXL-native models + relationships
│model│    │          Output: postxl-schema.json (PostXL-native)
└──┬──┘    │
   │       │
   └───────┼──────────┘
           ▼
    ┌──────────────┐
    │  experience/screens  │   Boundary: all inputs → screen specs with component inventory
    │              │   Consumes: features, brand tokens, tech stack, architecture, data model
    └──────────────┘
```

## Data Shape Contracts

| Boundary                                                     | Input                                    | Output                                                  | Validated by                         |
| ------------------------------------------------------------ | ---------------------------------------- | ------------------------------------------------------- | ------------------------------------ |
| User → discovery                                            | Conversational answers                   | `brief.md` with YAML frontmatter                        | `shared/contracts/frontmatter.md`    |
| discovery → discovery/3_brand                                        | Approved brief + reference URLs          | `identity.md` + `tokens.json`                           | JSON schema for tokens               |
| discovery → experience/journeys                                     | Approved brief + personas                | `stories.json` with story maps and EARS criteria        | `shared/contracts/stories_schema.json` |
| experience/journeys → experience/features                                    | Approved journeys                        | Feature `.md` files with status, priority, story_refs   | `shared/contracts/frontmatter.md`    |
| experience/features + discovery/3_brand → experience/screens                          | Approved features + brand tokens         | Screen `.md` with implements[], data_entities[]         | `shared/contracts/frontmatter.md`    |
| experience/features → blueprint                                   | Approved features                        | `stack.md` with tech choices in frontmatter             | `shared/contracts/frontmatter.md`    |
| experience/features + blueprint → blueprint/architecture.md                | Approved features + stack                | `architecture.md` (apps, modules, data flow, protocols) | `shared/contracts/frontmatter.md`    |
| experience/features + blueprint + blueprint/architecture.md → blueprint/datamodel | Approved features + stack + architecture | `postxl-schema.json` (Prisma-based types)               | `shared/contracts/semantic_types.md` |

## Cross-Reference Flow

```
experience/journeys/stories.json  experience/features/*.md          experience/screens/*.md          blueprint/datamodel/feature_map.json
┌──────────────┐         ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│ downstream:  │────────►│ story_refs:  │         │ implements:  │         │ feature_map  │
│ candidate_   │         │ screens: []  │────────►│  [05_feat/…] │         │  [05_feat/…] │
│   features   │         │              │◄────────│              │         └──────────────┘
└──────────────┘         │ data_entities│◄────────┼──────────────┘              │
                         │  []          │         │ data_entities│──────────────┘
                         └──────────────┘         └──────────────┘
```

Downstream skills register back into upstream files via `shared/contracts/feedback_loop.md`.

## Module Ownership

| Folder              | Owner skill            | Can read from                                                       |
| ------------------- | ---------------------- | ------------------------------------------------------------------- |
| `discovery/`       | `concept-1-discovery-1-overview`  | —                                                                  |
| `discovery/2_research/`      | `concept-1-discovery-2-research`  | `discovery/`                                                      |
| `discovery/3_brand/`         | `concept-1-discovery-3-brand`             | `discovery/`, `discovery/2_research/`\*                                    |
| `experience/journeys/`      | `concept-2-experience-1-journeys`  | `discovery/`, `discovery/2_research/`\*                                    |
| `experience/features/`      | `concept-2-experience-2-features`  | `experience/journeys/`, `discovery/`, `discovery/2_research/`\*                    |
| `experience/screens/`       | `concept-2-experience-3-screens`   | `experience/features/`, `discovery/3_brand/`, `experience/journeys/`                        |
| `blueprint/`     | `concept-3-blueprint-1-techstack`         | `discovery/`, `experience/features/`                                      |
| `blueprint/`  | `concept-3-blueprint-2-architecture`      | `experience/features/`, `blueprint/`                                    |
| `blueprint/datamodel/`     | `concept-3-blueprint-3-datamodel`         | `experience/features/`, `blueprint/`, `blueprint/`\*              |

_\* optional_

## Refactor Checklist

- [ ] Boundary contracts (frontmatter fields) unchanged or versioned
- [ ] Ownership map still accurate
- [ ] Cross-references (screens↔features, feature_map→features) still valid
- [ ] shared/contracts/ docs updated in same change
