---
name: skaileup-discovery
description: "Understanding the problem space — project brief, goals, comparable analysis, and brand identity. The foundation artifacts that all downstream skills depend on."
type: domain
building_blocks:
  contracts: "n/a — to be populated after skill migration."
  docs: "n/a — to be populated after skill migration."
  skills: "Project overview, visual brand identity, and behavioral brand identity skills."
  tools: "n/a"
stage: alpha
---

# skaileup-discovery

Understanding the problem space — project brief, goals, comparable analysis, and brand identity. The foundation artifacts that all downstream skills depend on. Discovery establishes the shared vocabulary and constraints that keep every subsequent skill grounded in the same project reality.

Skills will be moved into this domain during the architecture reorganization (Phase 5.2+).

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `skills/` | Invocable skills (see below) |

## Skills (target)

| Skill | Purpose |
|-------|---------|
| `skailup-overview/` | Produces the project brief: goals, audience, comparables, and positioning |
| `skailup-brand-visual/` | Defines the visual brand identity — color, typography, and visual language |
| `skailup-brand-behavioral/` | Defines the behavioral brand identity — tone, voice, and interaction principles |

## Conventions

- Discovery skills must run before experience, prototype, storybook, or blueprint skills.
- `skailup-overview` is the single required prerequisite; brand skills are optional but recommended before experience design begins.
- All discovery output is written to `_concept/10_discovery/`; downstream skills read from this path.
