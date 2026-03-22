---
name: skaile-dev
description: "Meta-tooling for developing the skaile-dev monorepo itself: devlog authoring, commit workflows, and repository hygiene."
type: domain
stage: alpha
---

# skaile-dev

Developer tooling for working *on* the skaile-dev repository. Skills in this domain assume the working directory is the `skaile-dev` monorepo root and are aware of its conventions: `_devlog/` format, package structure, git workflow, and Starlight docs site.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `skills/` | Invocable developer workflow skills |

## Skills

| Skill | Purpose |
|-------|---------|
| `commit` | Write a devlog entry summarising the session's changes and commit everything |

## Conventions

- All skills assume `cwd` = `skaile-dev/` monorepo root
- Devlog files live at `_devlog/<YYYY-MM-DD>_<slug>.md`
- Commit messages follow: `<type>(<scope>): <description>` with `Co-Authored-By` trailer
