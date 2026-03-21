---
title: dev-standards
description: Codebase convention discovery, injection, and synchronization — ensuring skills generate code that matches existing project patterns.
---

Handles codebase convention discovery and standards injection — the mechanism by which skills learn to write code that fits naturally into an existing project's style, architecture, and tooling choices.

When pointed at an existing codebase, `discover` extracts conventions into a `_standards/` artifact folder. When implementation skills need guidance, `inject` matches the relevant standards to the requesting skill's context. `sync` keeps the `_standards/` folder and reusable profiles in bidirectional alignment.

**Stage:** alpha

## Building Blocks

| Folder | Purpose |
|---|---|
| `contracts/` | `_standards/` folder structure, `index.yml` schema, category conventions |
| `docs/` | Discovery workflow, injection protocol, sync strategy |
| `skills/` | `standards-discover`, `standards-inject`, `standards-sync` |
| `profiles/` | Reusable tech-stack presets that pre-populate `_standards/` for known stacks |

## Skills

| Skill | When to use |
|---|---|
| `standards-discover` | Point at an existing codebase → extracts conventions into `_standards/` (api/, database/, ui/, naming/, testing/, architecture/) |
| `standards-inject` | Called by other skills — matches discovered standards to the requesting skill's context and injects them as additional guidance |
| `standards-sync` | Bidirectional sync between `_standards/` and reusable profiles — run after adding/changing standards to keep presets up to date |

## `_standards/` Structure

```
_standards/
├── index.yml          ← Category index, last discovered timestamp
├── api/               ← API design conventions
├── database/          ← Schema and migration patterns
├── ui/                ← Component and styling conventions
├── naming/            ← Naming rules for files, variables, routes
├── testing/           ← Test structure and coverage expectations
└── architecture/      ← Layer boundaries, module organization
```

## Profiles

`profiles/` contains reusable presets for known tech stacks (e.g. `nuxt-ui`, `nextjs-shadcn`). Instead of running `discover` on a new project, `sync` can populate `_standards/` from a profile. Profiles can be customized per project and pushed back to the shared registry.
