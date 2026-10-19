---
name: external
description: "Placeholder domain for third-party and externally sourced assets that are tracked but not maintained in this repository."
type: domain
building_blocks:
  contracts: "n/a — external assets follow their own conventions"
  skills: "n/a — external assets are not locally maintained skills"
  agents: "n/a — external assets follow their own conventions"
  prompts: "n/a — external assets follow their own conventions"
  tools: "n/a — external assets follow their own conventions"
stage: alpha
---

# External

This domain is a placeholder for third-party and externally sourced assets — skills, agents, or prompts tracked for reference or integration but not maintained in this repository.

Entries here are typically registered as Library Sources via `skaile source add <path>` (after cloning the upstream repo locally) rather than stored as local files. The `external/` folder may contain symlinks, stubs, or reference notes pointing to the canonical external source.

## Usage

To explore external resources without registering them:
```bash
skaile explore https://github.com/org/external-skills
```

To register a frequently used external source:
```bash
git clone https://github.com/org/external-skills ~/skills/external-skills
skaile source add ~/skills/external-skills
```

## Conventions

- Do not copy external files into this folder — reference them via the Library (`skaile source add`)
- Track provenance (source URL, version/commit) in a `sources.md` file next to this `DOMAIN.md` if external assets are referenced directly
- External assets are not subject to the same frontmatter or structure conventions as local skills
