---
title: Skills
description: What a skill is, how it's structured, how agents consume it, and how the runner resolves it.
---

A **skill** is the atomic unit of agent instruction. It is a directory containing a `SKILL.md` file — YAML frontmatter describing metadata and I/O, plus a Markdown prompt body the agent reads and follows when the skill is activated.

## Directory Structure

```
concept_overview/
├── SKILL.md          ← Required. Frontmatter + agent prompt body.
├── CLI.md            ← Optional. Slash-command / CLI invocation interface.
├── resources/        ← Optional. Reference docs loaded on demand by the agent.
├── examples/         ← Optional. Worked examples for few-shot prompting.
└── scripts/          ← Optional. Supporting uv-runnable Python tools.
```

The **directory name** is the skill's identity — it is what the runner searches for and what `skaile skill show` matches against.

## SKILL.md Format

```yaml
---
name: concept_overview
description: Generate project brief, goals, and comparable analysis
keywords: [concept, brief, overview, project]
source: CF                  # CF | SAXE | MERGED | MIGRATED
version: "2.1.0"
user_inputs:
  - name: project_description
    description: What is this project?
    required: true
reads_from:
  - _grounding/general/
writes_to:
  - _concept/01_project/
metadata:
  phase: discovery
  domain: dev-conceptualization
---

## ROLE
...

## READS
...

## WRITES
...

## REFERENCES
...

## MUST
...

## NEVER
...

## STEPS
1. ...

## CHECKLIST
- [ ] ...
```

## Frontmatter Fields

| Field | Required | Description |
|---|---|---|
| `name` | Yes | Unique identifier (matches directory name) |
| `description` | Yes | One-line description shown by `skaile skill list` |
| `keywords` | No | Tags for discovery and search |
| `source` | No | Origin: `CF`, `SAXE`, `MERGED`, or `MIGRATED` |
| `version` | No | Semver string |
| `user_inputs` | No | Inputs the skill collects before starting |
| `reads_from` | No | `_concept/` or `_grounding/` paths this skill reads |
| `writes_to` | No | Paths this skill creates or updates |
| `metadata` | No | Freeform — phase, domain, pipeline position, etc. |

### source Values

| Value | Meaning |
|---|---|
| `CF` | Originated in the Concept Forge ecosystem |
| `SAXE` | Originated in the Skaile/SAXE ecosystem |
| `MERGED` | Unified CF + SAXE variant |
| `MIGRATED` | Moved from a deprecated location; treated as current |

## Prompt Body Sections

| Section | Purpose |
|---|---|
| `ROLE` | What the agent is being asked to be for this task |
| `READS` | Files and paths to read before starting |
| `WRITES` | Files and paths to create or update |
| `REFERENCES` | Shared contracts and reference material to consult |
| `MUST` | Absolute requirements — mechanical rules, iron laws |
| `NEVER` | Hard prohibitions |
| `EMIT` | Structured observability events to emit at key steps |
| `STEPS` | Numbered procedure |
| `CHECKLIST` | Self-verification before declaring complete |

Not all sections are required. Minimal skills need at least `ROLE` and `STEPS`.

## Optional Files

| File | Purpose |
|---|---|
| `CLI.md` | Slash command interface — how to invoke from Claude Code |
| `validator.py` | Python validation script for skill output |
| `resources/*.md` | Reference material loaded by the agent on demand |
| `examples/` | Worked examples for few-shot prompting |

## How the Runner Resolves a Skill

When executing a flow node, the runner searches for `SKILL.md` in this order:

1. `<projectDir>/.claude/skills/<skillId>/SKILL.md`
2. `<projectDir>/.omp/skills/<skillId>/SKILL.md`
3. Walk up from `projectDir` (max 6 levels) to find `ai-resources/`:
   - For each domain: scan `<domain>/skills/` recursively
   - Match directory named **exactly** `<skillId>` or `cf_<skillId>`
4. Fallback string — `"Execute skill: <skillId>"` — never hard-fails

The `cf_` prefix is a compatibility alias: a directory named `cf_concept_overview` matches a flow node with `skill: "concept_overview"`.

## How Agents Consume Skills

The runner reads the raw `SKILL.md` text and sends it as the prompt to `driver.prompt()`. The agent receives the full file — frontmatter and body — as its instruction. There is no template rendering or preprocessing; what's in `SKILL.md` is exactly what the agent reads.

## Creating a New Skill

**Recommended**: use the `skill-builder` meta-skill — it creates the directory structure, frontmatter, and body sections interactively.

**Manual**:
1. Create `<domain>/skills/<skill-name>/SKILL.md`
2. Add YAML frontmatter (`name`, `description`, `source`, `writes_to` at minimum)
3. Write the prompt body (`ROLE`, `STEPS`, `CHECKLIST` at minimum)
4. Register the skill in `DOMAIN.md` under the appropriate section
