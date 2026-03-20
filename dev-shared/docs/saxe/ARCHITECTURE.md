# Architecture

## System Overview

The concept-forge-skills pipeline transforms a user's app idea into a complete
blueprint through 7 sequential/parallel steps. Each step is a skill that reads
from earlier steps and writes to its own folder.

## Pipeline Boundaries

```
┌──────────────┐
│ 1_discovery/1_overview   │ brief.md, goals.md, comparable.md
│ (user input) │ Boundary: natural language → structured YAML frontmatter
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ 1_discovery/2_research  │ domain.md, competitors.md, audiences.md
│ (agent)      │ Boundary: web data → structured markdown findings
└──────┬───────┘
       │
  ┌────┼────────────────┐
  ▼    ▼                ▼
┌─────┐ ┌──────┐ ┌──────────┐
│ 03  │ │  04  │ │    05    │
│feat.│ │brand │ │techstack │   Parallel. Each reads 1_discovery/1_overview/.
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
    │  2_experience/3_screens  │   Boundary: all inputs → screen specs with component inventory
    │              │   Consumes: features, brand tokens, tech stack, architecture, data model
    └──────────────┘
```

## Data Shape Contracts

| Boundary                                                     | Input                                    | Output                                                  | Validated by                         |
| ------------------------------------------------------------ | ---------------------------------------- | ------------------------------------------------------- | ------------------------------------ |
| User → 1_discovery/1_overview                                            | Conversational answers                   | `brief.md` with YAML frontmatter                        | `shared/contracts/frontmatter.md`    |
| 1_discovery/1_overview → 1_discovery/3_brand                                        | Approved brief + reference URLs          | `identity.md` + `tokens.json`                           | JSON schema for tokens               |
| 1_discovery/1_overview → 2_experience/1_journeys                                     | Approved brief + personas                | `stories.json` with story maps and EARS criteria        | `shared/contracts/stories_schema.json` |
| 2_experience/1_journeys → 2_experience/2_features                                    | Approved journeys                        | Feature `.md` files with status, priority, story_refs   | `shared/contracts/frontmatter.md`    |
| 2_experience/2_features + 1_discovery/3_brand → 2_experience/3_screens                          | Approved features + brand tokens         | Screen `.md` with implements[], data_entities[]         | `shared/contracts/frontmatter.md`    |
| 2_experience/2_features → 3_blueprint/1_techstack                                   | Approved features                        | `stack.md` with tech choices in frontmatter             | `shared/contracts/frontmatter.md`    |
| 2_experience/2_features + 3_blueprint/1_techstack → 3_blueprint/2_architecture                | Approved features + stack                | `architecture.md` (apps, modules, data flow, protocols) | `shared/contracts/frontmatter.md`    |
| 2_experience/2_features + 3_blueprint/1_techstack + 3_blueprint/2_architecture → 3_blueprint/3_datamodel | Approved features + stack + architecture | `postxl-schema.json` (Prisma-based types)               | `shared/contracts/semantic_types.md` |

## Cross-Reference Flow

```
2_experience/1_journeys/stories.json  2_experience/2_features/*.md          2_experience/3_screens/*.md          3_blueprint/3_datamodel/feature_map.json
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
| `1_discovery/1_overview/`       | `concept-1-discovery-1-overview`  | —                                                                  |
| `1_discovery/2_research/`      | `concept-1-discovery-2-research`  | `1_discovery/1_overview/`                                                      |
| `1_discovery/3_brand/`         | `concept-1-discovery-3-brand`             | `1_discovery/1_overview/`, `1_discovery/2_research/`\*                                    |
| `2_experience/1_journeys/`      | `concept-2-experience-1-journeys`  | `1_discovery/1_overview/`, `1_discovery/2_research/`\*                                    |
| `2_experience/2_features/`      | `concept-2-experience-2-features`  | `2_experience/1_journeys/`, `1_discovery/1_overview/`, `1_discovery/2_research/`\*                    |
| `2_experience/3_screens/`       | `concept-2-experience-3-screens`   | `2_experience/2_features/`, `1_discovery/3_brand/`, `2_experience/1_journeys/`                        |
| `3_blueprint/1_techstack/`     | `concept-3-blueprint-1-techstack`         | `1_discovery/1_overview/`, `2_experience/2_features/`                                      |
| `3_blueprint/2_architecture/`  | `concept-3-blueprint-2-architecture`      | `2_experience/2_features/`, `3_blueprint/1_techstack/`                                    |
| `3_blueprint/3_datamodel/`     | `concept-3-blueprint-3-datamodel`         | `2_experience/2_features/`, `3_blueprint/1_techstack/`, `3_blueprint/2_architecture/`\*              |

_\* optional_

## Refactor Checklist

- [ ] Boundary contracts (frontmatter fields) unchanged or versioned
- [ ] Ownership map still accurate
- [ ] Cross-references (screens↔features, feature_map→features) still valid
- [ ] shared/contracts/ docs updated in same change
