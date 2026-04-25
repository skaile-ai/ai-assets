---
name: skaileup-storybook
description: "Living Storybook prototypes — full interactive prototypes with component stories, page stories, journey stories, and type definitions. An alternative to simple mockups for teams that want a runnable UI."
type: domain
building_blocks:
  contracts: "n/a — to be populated after skill migration."
  docs: "n/a — to be populated after skill migration."
  skills: "Storybook setup, type generation, component stories, page stories, journey stories, and orchestrator skills."
  tools: "n/a"
stage: alpha
---

# skaileup-storybook

Living Storybook prototypes — full interactive prototypes with component stories, page stories, journey stories, and type definitions. An alternative to simple mockups for teams that want a runnable UI. This domain takes experience artifacts and produces a fully wired Storybook environment that designers and developers can interact with before implementation begins.

Skills will be moved into this domain during the architecture reorganization (Phase 5.2+).

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `skills/` | Invocable skills (see below) |

## Skills (target)

| Skill | Purpose |
|-------|---------|
| `skailup-storybook/` | Orchestrates the full Storybook prototype pipeline |
| `skailup-storybook-setup/` | Initializes the Storybook environment and configuration |
| `skailup-storybook-types/` | Generates TypeScript type definitions from concept artifacts |
| `skailup-storybook-components/` | Produces component stories from the component catalog |
| `skailup-storybook-pages/` | Produces page stories from screen design artifacts |
| `skailup-storybook-journeys/` | Produces journey stories that wire pages into user flow sequences |

## Conventions

- This domain is an alternative to skaileup-prototype; use when a runnable, interactive UI is required before implementation.
- Run `skailup-storybook-setup` first; subsequent skills within this domain depend on the initialized environment.
- Depends on `_concept/20_experience/` artifacts; run skaileup-experience skills before invoking this domain.
