---
name: standards-contract
description: "Shared contract for all dev-standards skills. Describes _standards/ folder layout, profile format, standards injection protocol, and how profiles map to tech-stack choices."
metadata:
  type: system
  do_not_invoke: true
  stage: alpha
  requires: []
---

# Standards Domain — Shared Contract

**Do not invoke directly.** This is a dependency contract — all `dev-standards` skills read this before operating.

## Scope

Standards skills discover, manage, and inject coding conventions into implementation workflows. This contract defines folder structures and profile formats.

## Skills Overview

| Skill | Trigger | Output |
|-------|---------|--------|
| `discover` | existing codebase, user wants convention extraction | `_standards/` populated |
| `inject` | implementation skill requests stack-specific guidance | filtered standards subset |
| `sync` | standards profile updated, codebase drifted | bidirectional repair |

## _standards/ Folder Layout

```
_standards/
├── index.yml             ← registry: which standards files exist + their scope
├── api/                  ← API conventions (REST, error handling, auth)
├── database/             ← DB conventions (naming, migrations, seeds)
├── ui/                   ← Component conventions (naming, props, slots)
├── naming/               ← File naming, variable naming, module structure
├── testing/              ← Test conventions (unit, integration, e2e)
└── architecture/         ← Layer boundaries, import rules, module structure
```

## index.yml Format

```yaml
schema_version: "1.0"
app: <slug>
discovered_at: YYYY-MM-DD
profile: <profile-name>   # resolved tech-stack profile
standards:
  - path: api/rest.md
    scope: backend
    auto_discovered: true
  - path: ui/components.md
    scope: frontend
    auto_discovered: true
```

## Profile Format

Profiles live in `profiles/<profile-name>/SKILL.md` and define stack-specific conventions:

```yaml
---
name: <profile-name>
description: "Tech stack preset for <stack>"
metadata:
  type: profile
  stack: [nuxt, primevue, directus]
  inherits: []        # parent profiles
  skip_steps: []      # pipeline steps to skip for this stack
---
```

## Injection Protocol

When a skill requests standards injection:
1. Read `_concept/3_blueprint/1_techstack/stack.md` for declared stack
2. Match declared stack → profile name
3. Load `profiles/<profile>/SKILL.md` + matching `_standards/` files
4. Return only the subset relevant to the requesting skill's domain (ui, api, etc.)
