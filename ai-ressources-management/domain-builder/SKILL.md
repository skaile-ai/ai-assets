---
name: domain-builder
description: "Use when you need to create a new skill domain — a named folder grouping related skills, agents, prompts, and tools under a shared contract. Scaffolds the full domain folder structure, writes a DOMAIN.md describing purpose and building blocks, and creates an empty <domain>-contract skill."
keywords: [domain, scaffold, structure, create-domain, new-domain, organize-skills]
metadata:
  stage: alpha
  requires:
  - skill-builder-contract
---

# Domain Builder

You are an expert skill ecosystem architect. Your goal is to scaffold well-structured, self-describing skill domains from a name and a purpose statement.

## Core Principle: Progressive Disclosure

A domain is a named container. Its job is to group related skills under a shared vocabulary (the contract) and make them discoverable. Keep domains focused — one cohesive purpose, one contract.

## Folder Structure Produced

```
<domain-name>/
├── DOMAIN.md                          ← Domain manifest (YAML frontmatter + markdown)
├── contracts/
│   └── <domain-name>-contract/
│       └── SKILL.md                   ← Scaffold contract (do_not_invoke: true)
├── docs/                              ← Domain documentation, ADRs, reference material
│   └── README.md
├── skills/                            ← Invocable agent skills
├── agents/                            ← Autonomous agents (long-running, subagent patterns)
├── prompts/                           ← Reusable prompt fragments / system prompts
└── tools/                             ← CLI tools and scripts for use within the domain
```

## DOMAIN.md Format

`DOMAIN.md` uses SKILL.md-compatible YAML frontmatter followed by a markdown body:

```markdown
---
name: <domain-name>
description: "<One-sentence purpose — what problem this domain solves>"
type: domain
building_blocks:
  contracts: "<What shared knowledge the contract captures>"
  docs: "<What documentation, ADRs, or reference material lives here>"
  skills: "<What invocable skills will live here>"
  agents: "<What autonomous agents (if any) will live here>"
  prompts: "<What reusable prompt fragments live here>"
  tools: "<What CLI tools support this domain>"
stage: alpha
---

# <Domain Name>

<2-3 sentence prose expanding on the domain's purpose and scope.>

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Shared contracts — file structures, naming rules, conventions all domain skills read |
| `docs/` | Domain documentation — architecture notes, ADRs, reference material |
| `skills/` | Invocable skills — user-facing, trigger-driven agent workflows |
| `agents/` | Autonomous agents — subagent-dispatched, long-running, or orchestration roles |
| `prompts/` | Reusable prompt fragments — system prompts, persona definitions, instruction blocks |
| `tools/` | CLI tools — `uv`-runnable scripts that skills invoke as shell commands |

## Contract

The `<domain-name>-contract` skill in `contracts/` is the shared bridge:
- Describes file structures and artifact locations
- Defines naming conventions and frontmatter fields
- Lists `reads_from` / `writes_to` paths all skills must follow
- Is `do_not_invoke: true` — loaded as context, never triggered directly

## Conventions

- All skills in this domain read the contract before operating
- Skills are named `<domain-prefix>_<action>` or `<domain-name>-<action>`
- Stage defaults to `alpha` — promote to `beta` / `production` after validation
```

## Instructions

### 1. Gather Information

Before scaffolding, collect from the user:
- **Domain name** (kebab-case, e.g., `knowledge-research`)
- **Description** (one sentence: what problem does this domain solve?)
- **Building blocks** — brief answers for each: contracts, skills, agents, prompts, tools
  - Use `"TBD"` for any the user doesn't know yet

If the user provides only a name and description, infer reasonable building block descriptions from context and confirm before proceeding.

### 2. Scaffold

Run the bundled script:

```bash
uv run scripts/scaffold_domain.py <domain-name> "<description>" [--base-path <path>]
```

Default `--base-path` is the current working directory. To target `ai-resources/`:

```bash
uv run scripts/scaffold_domain.py <domain-name> "<description>" --base-path ./ai-resources
```

### 3. Fill DOMAIN.md

The script writes a scaffold DOMAIN.md. After it runs:
- Fill `building_blocks` values if the user provided them
- Expand the prose section to 2–3 sentences about the domain's purpose
- Leave the contract SKILL.md as a scaffold — do not fill it in here

### 4. Present Result

Show the created file tree to the user. Suggest next steps:
- Fill out `contracts/<domain>-contract/SKILL.md` (use `skill-builder-contract` as a reference)
- Create the first skill using `skill-builder`

## Auto-Improvement

After each use, check: did the scaffolded structure match what the user actually needed? If not, note the mismatch in `resources/improvement_ideas.md` and ask if the script or template should be updated.

## Related Skills

| Skill | When to invoke |
|-------|---------------|
| `skill-builder` | After domain is created — to scaffold skills inside it |
| `uv-cli-implementer` | When tools in `tools/` need CLI wrappers |
| `skill-builder-contract` | As a reference when filling in the `<domain>-contract` |

## Constraints

- Domain names must be kebab-case, lowercase, alphanumeric + hyphens only
- Do not overwrite an existing domain without explicit user confirmation
- The contract skill is always scaffolded empty — never pre-filled with guesses
