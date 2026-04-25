---
name: skaileup-experience
description: "Defining what users see and do — user journeys, feature specifications, behavioral specs, screen designs, and component catalogs."
type: domain
building_blocks:
  contracts: "n/a — to be populated after skill migration."
  docs: "n/a — to be populated after skill migration."
  skills: "Journeys, features, behaviors, screens, technical screen specs, and component catalog skills."
  tools: "n/a"
stage: alpha
---

# skaileup-experience

Defining what users see and do — user journeys, feature specifications, behavioral specs, screen designs, and component catalogs. Experience skills translate discovery artifacts into concrete user-facing design decisions, producing the specifications that prototype, storybook, and build skills consume.

Skills will be moved into this domain during the architecture reorganization (Phase 5.2+).

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `skills/` | Invocable skills (see below) |

## Skills (target)

| Skill | Purpose |
|-------|---------|
| `skailup-journeys/` | Maps end-to-end user journeys across the product |
| `skailup-features/` | Defines feature specifications with acceptance criteria |
| `skailup-behaviors/` | Specifies interaction behaviors, states, and edge cases |
| `skailup-screens/` | Designs screen layouts and information architecture |
| `skailup-screens-technical/` | Adds technical constraints and component bindings to screen designs |
| `skailup-components/` | Catalogs reusable UI components and their variants |

## Conventions

- Experience skills depend on `_concept/10_discovery/` artifacts; run discovery first.
- Skills within this domain can run sequentially (journeys → features → behaviors → screens → components) or selectively depending on project scope.
- Output is written to `_concept/20_experience/`; prototype, storybook, and build skills read from this path.
