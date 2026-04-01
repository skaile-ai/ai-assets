---
name: skaileup-standards
description: "Codebase convention discovery, injection, and synchronization — ensuring skills generate code that matches the existing patterns of the target project."
type: domain
building_blocks:
  contracts: "Standards file structure, index.yml schema, and naming conventions for the _standards/ artifact folder."
  docs: "Standards discovery workflow, injection protocol, and sync strategy for keeping standards up to date."
  skills: "Three skills: discover (scan codebase → _standards/), inject (match standards to requesting skill), sync (bidirectional standards ↔ profiles update)."
  agents: "TBD"
  prompts: "TBD"
  tools: "TBD"
stage: alpha
---

# Dev Standards

This domain handles codebase convention discovery and standards injection — the mechanism by which skills learn to write code that fits naturally into an existing project's style, architecture, and tooling choices.

When pointed at an existing codebase, `discover` extracts conventions into a `_standards/` artifact folder. When implementation skills need guidance, `inject` matches the relevant standards to the requesting skill's context. `sync` keeps the `_standards/` folder and reusable profiles in bidirectional alignment.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | `_standards/` folder structure, `index.yml` schema, category conventions (api/, database/, ui/, naming/, testing/, architecture/) |
| `docs/` | Discovery workflow, injection protocol, sync strategy |
| `skills/` | `discover`, `inject`, `sync` |
| `profiles/` | Reusable tech-stack presets (nuxt-ui, nextjs-shadcn, postxl, etc.) that pre-populate `_standards/` for known stacks |

## Skills

| Skill | Purpose |
|-------|---------|
| `discover` | Scan an existing codebase and extract conventions into `_standards/` (runs in parallel/research mode) |
| `inject` | Given a requesting skill and its context, return the relevant standards sections |
| `sync` | Bidirectional sync between `_standards/` and `profiles/` — promotes discovered conventions to reusable presets |

## Conventions

- `discover` runs as a parallel mode (like `cf_research`) — it does not block the main pipeline
- `_standards/` is unnumbered and readable by all skills across all domains
- Standards are organized by category, not by technology — categories are stable across stacks
- Profiles inherit and override each other; a project profile extends a stack preset
