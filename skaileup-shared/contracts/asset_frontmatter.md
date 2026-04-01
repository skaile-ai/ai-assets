# Asset Frontmatter Schemas

Canonical frontmatter definitions for the four AI asset types: **skill**, **prompt**, **agent**, **flow**.

This document covers the manifest metadata of assets in `ai-assets/`. It is distinct from `frontmatter.md`, which covers YAML fields in `_concept/` output artifacts.

## Design Principles

Skills and prompts follow the [Agent Skills Specification](https://agentskills.io/specification) for maximum interoperability — `name` and `description` at root, everything else in `metadata:`.

Agents follow the [GitAgent Specification](https://www.gitagent.sh/) — a git-native standard with its own root-level field structure. The `metadata:` block holds skaile-specific extensions.

Flows use a custom schema optimised for the agent-flow-engine.

---

## Root Fields — agentskills.io Compatible

These two fields are required on **every** asset type and follow the [Agent Skills spec](https://agentskills.io/specification):

```yaml
name: string          # Required. kebab-case. 1–64 chars. a-z, 0-9, hyphens only.
                      # Must not start/end with hyphen or contain consecutive hyphens.
                      # Must match the parent directory name (skills) or filename stem (prompts, flows).
description: string   # Required. 1–1024 chars. Describes what the asset does AND when to use it.
                      # Skills: start with "Use when..." for trigger-based routing.
```

---

## Skill — SKILL.md

Follows the [Agent Skills Specification](https://agentskills.io/specification). Root fields are spec-defined; `metadata:` holds skaile extensions.

```yaml
---
name: skill-name
description: "Use when [trigger conditions]. Specific keywords for agent routing."
license: MIT                            # optional — agentskills.io field
compatibility: "Requires Python 3.12+"  # optional — agentskills.io field; environment requirements
allowed-tools: Bash(git:*) Read Edit    # optional — agentskills.io experimental; pre-approved tools
metadata:
  version: "1.0.0"
  author: skaile                        # optional — omit for monorepo-owned assets
  tags: [keyword1, keyword2]            # searchable; used by skaile search and routing
  stage: alpha | beta | stable
  source: CF | SAXE | MERGED | MIGRATED # lineage tracking; omit for new skills
  requires:                             # dependency declarations
    - contract-name                     # bare string for same-resource contracts
  do_not_invoke: false                  # true for system contracts (context-only, never triggered)
  subagent: false                       # true if skill runs as a sub-agent
  prerequisites:                        # skill dependencies and inputs (preferred over user_inputs)
    files:                              # files/dirs that must exist before skill runs
      - path: "_concept/path/to/file"
        gate: hard | soft               # hard = block execution, soft = warn and continue
        description: "Why this file is needed"
        min_entries: 1                  # for directories: minimum entry count
    inputs_required:                    # user inputs that MUST be collected before skill runs
      - id: input_name
        label: "Human-readable label"
        type: text | textarea | select | multiselect | boolean | number
        options: []                     # for select/multiselect
        default: null
        hint: "Help text"
        schema: {}                      # optional JSON Schema for validation
    inputs_optional:                    # user inputs that MAY be collected
      - id: input_name
        label: "Human-readable label"
        type: text
    reads:                              # optional data sources (never blocks execution)
      - path: "_concept/path/to/optional/source"
        description: "What this provides"
    produces:                           # what this skill creates
      - path: "_concept/path/to/output"
        description: "What is produced"
  user_inputs:                          # DEPRECATED — use metadata.prerequisites instead
    dialog:
      - id: input_name
        label: "Human-readable label"
        type: text | select | multiselect | boolean | number
        required: true
        options: []                     # for select/multiselect only
        default: null
        hint: "Help text shown in UI forms"
    files: []                           # _concept/ paths to pre-supply as input
  reads_from: []                        # _concept/ paths this skill reads
  writes_to: []                         # _concept/ paths this skill creates/modifies
  env_vars:                             # environment variables required at runtime
    VAR_NAME: "Description and where to get it."
---
```

### Skill field reference

| Field | Location | Required | Notes |
|---|---|---|---|
| `name` | root | yes | kebab-case; unique within domain; must match directory name |
| `description` | root | yes | trigger-oriented: "Use when..." |
| `license` | root | no | agentskills.io field; default MIT |
| `compatibility` | root | no | agentskills.io field; environment requirements |
| `allowed-tools` | root | no | agentskills.io experimental; space-delimited tool list |
| `metadata.version` | metadata | yes | semver |
| `metadata.author` | metadata | no | omit for monorepo-owned |
| `metadata.tags` | metadata | yes | at least 2–3 tags |
| `metadata.stage` | metadata | yes | alpha / beta / stable |
| `metadata.source` | metadata | no | CF / SAXE / MERGED / MIGRATED |
| `metadata.requires` | metadata | no | bare strings or `{name, source, version?, optional?}` objects |
| `metadata.do_not_invoke` | metadata | no | default false; true for shared contracts |
| `metadata.subagent` | metadata | no | default false |
| `metadata.prerequisites` | metadata | no | preferred; replaces `user_inputs`, `reads_from`, `writes_to` |
| `metadata.prerequisites.files` | metadata | no | file/dir gates; each entry has `path`, `gate`, `description`, optional `min_entries` |
| `metadata.prerequisites.inputs_required` | metadata | no | user inputs that block skill start until collected |
| `metadata.prerequisites.inputs_optional` | metadata | no | user inputs collected opportunistically |
| `metadata.prerequisites.reads` | metadata | no | optional data sources; never blocks execution |
| `metadata.prerequisites.produces` | metadata | no | output paths this skill creates |
| `metadata.user_inputs` | metadata | no | **DEPRECATED** — use `metadata.prerequisites` instead |
| `metadata.reads_from` | metadata | no | **DEPRECATED** — use `metadata.prerequisites.reads` instead |
| `metadata.writes_to` | metadata | no | **DEPRECATED** — use `metadata.prerequisites.produces` instead |
| `metadata.env_vars` | metadata | no | omit if none |

### Skill directory structure (agentskills.io)

```
skill-name/
├── SKILL.md          # Required: metadata + instructions
├── scripts/          # Optional: executable code (PEP 723 / bash / node)
├── references/       # Optional: detailed docs loaded on demand
├── assets/           # Optional: templates, schemas, static resources
└── examples/         # Optional: calibration / few-shot examples
```

### Progressive disclosure

1. **Metadata** (~100 tokens): `name` + `description` loaded at startup for all skills
2. **Instructions** (<5000 tokens): Full SKILL.md body loaded when skill activates. Keep under 500 lines.
3. **Resources** (as needed): Files in `scripts/`, `references/`, `assets/` loaded only when required

---

## Prompt — *.prompt.md

Minimal variant of the skill format. The body IS the prompt text — no workflow layer.

```yaml
---
name: command-name
description: "What this command does and when to invoke it."
metadata:
  version: "1.0.0"
  tags: [tag1, tag2]
  stage: alpha | beta | stable
---
```

When installed to `.claude/commands/`, the `name` field becomes the slash command name (e.g. `commit` → `/commit`).

---

## Agent — agent.yaml

Follows the [GitAgent Specification](https://www.gitagent.sh/). Root fields are gitagent-native; `metadata:` holds skaile extensions.

```yaml
spec_version: "0.1.0"                  # gitagent spec version
name: agent-name                        # kebab-case identifier
version: "1.0.0"                        # semver
description: "What this agent does."
author: skaile
license: MIT
tags: [tag1, tag2]                      # gitagent puts tags at root

extends: ../path/to/base-agent/agent.yaml  # optional inheritance

model:
  preferred: claude-opus-4-6
  fallback:
    - claude-sonnet-4-6
    - claude-haiku-4-5-20251001
  constraints:
    temperature: 0.2
    max_tokens: 8192

skills: []                              # skill names this agent can invoke
delegation:
  mode: explicit | router | auto

runtime:
  max_turns: 100
  timeout: 1800                         # seconds

requires:                               # agent dependencies (gitagent: dependencies → requires)
  - name: concept-orchestrator
    source: ../../skaileup-conceptualization/agents/orchestrator
    version: "1.0.0"
    optional: false
    mount: agents/conceptualization     # where to mount sub-agent

compliance:                             # optional — gitagent regulatory metadata
  risk_tier: low | medium | high
  supervision:
    human_in_the_loop: never | on_risk | always
    kill_switch: false

metadata:                               # skaile extensions
  stage: alpha | beta | stable
  domain: domain-name
  produces: "What this agent writes"
  consumes: "What this agent reads"
```

### GitAgent directory structure

```
agent-name/
├── agent.yaml          # Required: manifest with version, model, compliance
├── SOUL.md             # Required: identity, personality, communication style
├── RULES.md            # Hard constraints and safety boundaries
├── DUTIES.md           # Segregation of duties and role policies
├── AGENTS.md           # Framework-agnostic fallback instructions
├── skills/             # Reusable capability modules
├── tools/              # MCP-compatible tool schemas
├── workflows/          # Multi-step YAML procedures
├── knowledge/          # Reference documents (sorted by index.yaml priority)
│   └── index.yaml      # Priority ordering for knowledge files
├── memory/runtime/     # Live agent state (gitignored)
├── hooks/              # Lifecycle handlers (bootstrap, teardown)
├── config/             # Environment-specific overrides
├── compliance/         # Regulatory artifacts and audit logs
├── agents/             # Sub-agent definitions (recursive composition)
└── examples/           # Calibration interactions (few-shot)
```

### Imprint assembly order

`buildAgentImprint()` in agent-runner assembles the system prompt:

1. `SOUL.md` — agent identity and values
2. `RULES.md` — behavioral constraints
3. `knowledge/*.md` — sorted by `index.yaml` priority (default: 99)
4. Project `CLAUDE.md` overlay — optional

Parts joined with `\n\n---\n\n`.

### Export adapters (gitagent CLI)

| Format | Target |
|---|---|
| `system-prompt` | Plain text for any LLM |
| `claude-code` | Claude Code CLAUDE.md |
| `openai` | OpenAI Agents SDK |
| `crewai` | CrewAI YAML |
| `github` | GitHub Actions |

---

## Flow — *.flow.yaml

Custom schema for agent-flow-engine. Uses `id` as the machine identifier (distinct from display `name`).

```yaml
id: flow-id                             # machine identifier; used by `skaile run <id>`
name: Human Readable Name
version: "1.0"
description: >-
  What this flow does.

metadata:
  tags: [tag1, tag2]
  stage: alpha | beta | stable
  icon: i-heroicons-rocket-launch       # UI icon class
  category: full-stack | prototype | concept | cli | maintenance
  onboarding:                           # optional structured onboarding form
    input_style: structured | repo
    fields: [app_name, problem, audience]

globals:
  research_depth: skip | light | moderate | deep
  approval_mode: auto_approve | checkpoint
  auto_review: false
  subagent_mode: true
  verbosity: brief | standard | detailed
  cli_mode: false

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

entry: node-id
nodes: [...]
edges: [...]
```

### Flow field notes

Flows keep `id`, `version`, `name`, `description` at root because the flow-engine and runner parse them directly. The `metadata:` block holds catalog/UI fields (`tags`, `stage`, `icon`, `category`, `onboarding`) — previously split between root and a `meta:` block.

---

## Domain — DOMAIN.md

Domain manifest for directories under `ai-assets/`. Not managed by arm but read by agents and documentation.

```yaml
---
name: domain-name
description: "One-line summary of this domain."
metadata:
  stage: alpha | beta | stable
  type: domain
---
```

---

## Migration from Previous Schema

### Skill files

| Old | New | Action |
|---|---|---|
| `keywords: [...]` | `metadata.tags: [...]` | Move into metadata, rename |
| `metadata.stage: alpha` | `metadata.stage: alpha` | Keep in metadata (no change) |
| `metadata.requires: [...]` | `metadata.requires: [...]` | Keep in metadata (no change) |
| `metadata.do_not_invoke: true` | `metadata.do_not_invoke: true` | Keep in metadata (no change) |
| `metadata.type: system` | remove | Redundant with `do_not_invoke` |
| `source: CF` (root) | `metadata.source: CF` | Move into metadata |
| `version: "1.0.0"` (root) | `metadata.version: "1.0.0"` | Move into metadata |
| `reads_from: []` (root) | `metadata.reads_from: []` | Move into metadata |
| `writes_to: []` (root) | `metadata.writes_to: []` | Move into metadata |
| `user_inputs:` (root) | `metadata.user_inputs:` | Move into metadata |
| `subagent: true` (root) | `metadata.subagent: true` | Move into metadata |
| `env_vars:` (root) | `metadata.env_vars:` | Move into metadata |
| `risk: safe` (root) | remove | Not in spec |
| `metadata.user_inputs` | `metadata.prerequisites` | Deprecated; migrate to prerequisites schema |
| `metadata.reads_from` | `metadata.prerequisites.reads` | Deprecated; migrate to prerequisites.reads |
| `metadata.writes_to` | `metadata.prerequisites.produces` | Deprecated; migrate to prerequisites.produces |

**Migration note — prerequisites schema:** New skills should use `metadata.prerequisites` exclusively. It consolidates file gate checks (`files`), user input collection (`inputs_required`, `inputs_optional`), optional reads (`reads`), and declared outputs (`produces`) into a single block. Existing skills using the deprecated fields continue to work but should be migrated when next touched.

### Agent files

| Old | New | Action |
|---|---|---|
| `dependencies: [...]` | `requires: [...]` | Rename at root |
| `tags: [...]` | no change | Already at root (gitagent) |
| add `metadata.stage` | | New field |

### Flow files

| Old | New | Action |
|---|---|---|
| `meta:` block | `metadata:` block | Rename; absorb `icon`, `category`, `tags`, `onboarding` |
| add `metadata.stage` | | New field |

### Prompt files

| Old | New | Action |
|---|---|---|
| `version:` (root) | `metadata.version:` | Move into metadata |
| `tags:` (root) | `metadata.tags:` | Move into metadata |
| `stage:` (root) | `metadata.stage:` | Move into metadata |

---

## Version Bump Rules

All assets use [semantic versioning](https://semver.org/) (MAJOR.MINOR.PATCH). Developers bump versions manually; tooling validates that bumps happen.

### Skills and Prompts

| Bump | When |
|------|------|
| **Major** | Renamed/removed `inputs_required`/`inputs_optional` fields; changed `produces` paths; renamed/removed the asset; behavioral change breaking existing flow pins |
| **Minor** | Added new inputs; added new `produces` outputs; new behavior not affecting existing consumers; added `references/` or `scripts/` |
| **Patch** | Fixed typos, improved prompt wording; bug fixes in `validator.py`/`scripts/`; updated `references/` content; changed `tags`/`stage`/metadata |

### Agents

| Bump | When |
|------|------|
| **Major** | Changed `requires`/`delegation`/`skills` in a breaking way; removed capabilities |
| **Minor** | Added new capabilities, new sub-agents, expanded `skills` list |
| **Patch** | Prompt tuning in SOUL.md/RULES.md; metadata changes |

### Flows

| Bump | When |
|------|------|
| **Major** | Removed/reordered required nodes; changed entry point; removed edges |
| **Minor** | Added optional nodes; added new edges; new modes |
| **Patch** | Parameter tweaks; label changes; position adjustments |

### Flow Version Pinning

Flow nodes can optionally pin a skill version range:

```yaml
nodes:
  - id: "overview"
    type: skill
    data:
      skill: "overview"
      version: "^1.0.0"    # optional — semver range
```

When present, the runner validates at flow startup that the installed skill version satisfies the range. If absent, no version check is performed.

### Per-Domain Changelogs

Each domain with versioned assets maintains a `CHANGELOG.md`:

```markdown
# Changelog — <domain-name>

## [Unreleased]

## YYYY-MM-DD

### <asset-name> <old-version> → <new-version>
- Description of change
- BREAKING: Description of breaking change
```

Update the changelog in the same commit as the version bump.
