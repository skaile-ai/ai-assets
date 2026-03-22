# Asset Frontmatter Schemas

Canonical frontmatter definitions for the four AI asset types: **skill**, **prompt**, **agent**, **flow**.

This document covers the manifest metadata of assets in `ai-resources/`. It is distinct from `frontmatter.md`, which covers YAML fields in `_concept/` output artifacts.

---

## Common Fields

Every asset type shares these fields. All are required unless marked optional.

```yaml
name: string          # Primary identifier. kebab-case. Used by arm for install/lookup.
description: string   # One line. Used for catalog search, routing, and sidebar labels.
version: string       # Semver string: "1.0.0". Required on all new assets.
tags: [string]        # Searchable keywords. Replaces `keywords` (skills) and aligns with agent `tags`.
stage: alpha | beta | stable   # Maturity level. Moved from metadata.stage.
author: string        # Optional. Omit for assets owned by the monorepo.
license: string       # Optional. Defaults to MIT if omitted.
```

---

## Dependencies — unified `requires:`

All asset types use `requires:` for declaring dependencies. Two forms:

**Bare string** — for same-resource dependencies (contracts, sibling skills):
```yaml
requires:
  - skill-builder-contract
  - knowledge-research-contract
```

**Object** — for cross-resource dependencies with versioning (agents, skill packages):
```yaml
requires:
  - name: concept-orchestrator
    source: ../../dev-conceptualization/agents/orchestrator
    version: "1.0.0"      # optional — omit to accept any
    optional: false        # optional — default false
```

Mix both forms in one list if needed. This replaces `metadata.requires` (skills) and `dependencies:` (agents).

---

## Skill — SKILL.md

```yaml
---
name: skill-name
description: "Use when [trigger conditions]. NOT a workflow summary."
version: "1.0.0"
tags: [keyword1, keyword2]
stage: alpha | beta | stable
source: CF | SAXE | MERGED | MIGRATED   # lineage tracking; omit for net-new skills
requires:
  - contract-name           # bare strings for contract deps
do_not_invoke: false        # true for system contracts that are context-only, never triggered
user_inputs:
  dialog:
    - id: input_name
      label: "Human-readable label"
      type: text | select | multiselect | boolean | number
      required: true
      options: []           # for select / multiselect only
      default: null
      hint: "Help text shown in UI forms"
  files: []                 # _concept/ paths to pre-supply as input
reads_from: []              # _concept/ paths this skill reads
writes_to: []               # _concept/ paths this skill creates or modifies
env_vars:                   # environment variables required at runtime
  VAR_NAME: "Description of what this variable does and where to get it."
subagent: false             # true if this skill is invoked as a sub-agent (spawns its own context)
---
```

### Field notes

| Field | Required | Notes |
|---|---|---|
| `name` | yes | kebab-case; must be unique within domain |
| `description` | yes | trigger-oriented: "Use when..." — what signals this skill |
| `version` | yes | semver |
| `tags` | yes | replaces old `keywords`; at least 2–3 tags |
| `stage` | yes | replaces `metadata.stage` |
| `source` | no | CF/SAXE/MERGED/MIGRATED — omit for new skills with no lineage |
| `requires` | no | bare strings pointing to contract names in same resource |
| `do_not_invoke` | no | default false; true for shared contracts/system context |
| `user_inputs` | no | omit entirely if skill needs no user input |
| `reads_from` | no | helpful for pipeline dependency graphs; use `[]` if none |
| `writes_to` | no | helpful for pipeline dependency graphs; use `[]` if none |
| `env_vars` | no | omit if none; include any env var the skill body references |
| `subagent` | no | default false |

**Removed:** `metadata:` blob. Any domain-specific notes that don't fit the above belong in the SKILL.md body, not frontmatter.

---

## Prompt — *.prompt.md

```yaml
---
name: command-name
description: "One line: what this command does and when to invoke it."
version: "1.0.0"
tags: [tag1, tag2]
stage: alpha | beta | stable
---
```

Prompts are intentionally minimal. The body IS the prompt — there is no workflow layer to declare. When installed to `.claude/commands/`, the `name` field becomes the slash command name.

Optional field for prompts with an explicit command name different from the filename:
```yaml
command: explicit-name    # only needed if name differs from the desired /command slug
```

---

## Agent — agent.yaml

```yaml
spec_version: "0.1.0"
name: agent-name
version: "1.0.0"
description: "One line: what this agent does and when it is invoked."
tags: [tag1, tag2]
stage: alpha | beta | stable
author: skaile
license: MIT

extends: ../path/to/base-agent/agent.yaml    # optional inheritance

model:
  preferred: claude-opus-4-6
  fallback:
    - claude-sonnet-4-6
    - claude-haiku-4-5-20251001
  constraints:
    temperature: 0.2
    max_tokens: 8192

delegation:
  mode: explicit | router | auto

runtime:
  max_turns: 100
  timeout: 1800          # seconds

requires:                # replaces old `dependencies:` array
  - name: concept-orchestrator
    source: ../../dev-conceptualization/agents/orchestrator
    version: "1.0.0"
    optional: false

metadata:                # domain-specific context — keep minimal; prefer dedicated fields above
  domain: domain-name
  produces: "What this agent writes"
  consumes: "What this agent reads"
```

### Field notes

The top-level `tags` replaces the old bare `tags:` array (no change in value, just clarified as part of the common schema). The `dependencies:` key is renamed to `requires:` — same object format, just a consistent name.

`metadata:` may remain for domain-specific documentation fields (`produces`, `consumes`, `phases`, `resume`, `expert_routing`) that are informational only, not parsed by tooling.

---

## Flow — *.flow.yaml

```yaml
id: flow-id             # machine identifier; kebab-case; used by skaile run <id>
name: Human Name        # display name
version: "1.0"
description: >-
  One paragraph describing what this flow does.
tags: [tag1, tag2]      # top-level tags for catalog search
stage: alpha | beta | stable

meta:                   # UI / presentation metadata (not parsed by runner)
  icon: i-heroicons-rocket-launch
  category: full-stack | prototype | concept | cli | maintenance
  tags: [tag1, tag2]    # duplicates top-level tags for UI consumers — keep both for now
  onboarding:           # optional — for flows with a structured onboarding form
    input_style: structured | repo
    fields: [app_name, problem, audience]

globals:
  research_depth: skip | light | moderate | deep
  approval_mode: auto_approve | checkpoint
  auto_review: false
  subagent_mode: true
  verbosity: brief | standard | detailed
  cli_mode: false       # optional; true for CLI-only flows

modes:
  research:
    enabled: false
    skill: research
    triggers: []
    parameters: {}
  standards:
    enabled: false
    skill: standards-discover
    inject_skill: standards-inject
    trigger_after: scaffold
    parameters: {}

entry: node-id          # first node to execute
```

### Field notes

Flows already have `id` at root (distinct from `name`). The new `tags` field at top-level enables catalog indexing. `meta.tags` is kept for UI consumers but `tags` is the canonical catalog field. `stage` is new for flows — add for all new flows; existing flows default to `stable`.

---

## Migration Notes

### For skill files (bulk update needed)

| Old field | New field | Action |
|---|---|---|
| `keywords: []` | `tags: []` | Rename |
| `metadata.stage: alpha` | `stage: alpha` | Promote to top-level |
| `metadata.requires: [...]` | `requires: [...]` | Promote to top-level |
| `metadata.do_not_invoke: true` | `do_not_invoke: true` | Promote to top-level |
| `metadata.type: system` | remove | Redundant with `do_not_invoke` |
| `metadata: {}` (empty) | remove | Delete empty blocks |
| `metadata:` (domain-only fields) | keep | `phases`, `produces`, `consumes`, `resume` stay in metadata |

### For agent.yaml files

| Old field | New field | Action |
|---|---|---|
| `dependencies: [...]` | `requires: [...]` | Rename; keep object format |
| `tags: [...]` | no change | Already correct |

### For flow.yaml files

| Action | |
|---|---|
| Add `tags: [...]` at top-level | Copy from `meta.tags` |
| Add `stage: stable` | New field; existing flows are stable |

### For prompt files

| Action | |
|---|---|
| Add `tags: [...]` | New field |
| Add `stage: alpha` | New field |
