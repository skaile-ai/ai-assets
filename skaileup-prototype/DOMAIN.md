---
name: skaileup-prototype
description: "Simple tangible prototypes — text-based mockup descriptions that visualize screen designs without requiring a full Storybook setup."
type: domain
building_blocks:
  contracts: "n/a — to be populated after skill migration."
  docs: "n/a — to be populated after skill migration."
  skills: "Text-based screen mockup skill."
  tools: "n/a"
stage: alpha
---

# skaileup-prototype

Simple tangible prototypes — text-based mockup descriptions that visualize screen designs without requiring a full Storybook setup. This domain provides a lightweight path to stakeholder review: produce readable, structured mockup documents from experience artifacts without spinning up a code environment.

Skills will be moved into this domain during the architecture reorganization (Phase 5.2+).

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `skills/` | Invocable skills (see below) |

## Skills (target)

| Skill | Purpose |
|-------|---------|
| `skailup-mock/` | Generates text-based mockup descriptions from screen design artifacts |

## Conventions

- This domain is an alternative to skaileup-storybook for teams that do not need a runnable UI prototype.
- Depends on `_concept/20_experience/` artifacts from skaileup-experience; run experience skills first.
- Output is written to `_concept/25_prototype/`; suitable for stakeholder review without any build tooling.
