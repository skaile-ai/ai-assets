---
title: Skill Anatomy
description: SKILL.md frontmatter reference, body sections, optional files, and skill resolution search path.
---

## SKILL.md Frontmatter Fields

```yaml
---
name: cf_concept_overview
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
  phase: conceptualization
  domain: dev-conceptualization
---
```

| Field | Required | Description |
|---|---|---|
| `name` | Yes | Unique skill identifier (matches directory name) |
| `description` | Yes | One-line description for `skill list` |
| `keywords` | No | Tags for discovery and search |
| `source` | No | `CF` (Concept Forge origin), `SAXE` (Skaile origin), `MERGED` (both), `MIGRATED` (moved from deprecated) |
| `version` | No | Semver string |
| `user_inputs` | No | Inputs the skill collects from the user |
| `reads_from` | No | `_concept/` paths this skill reads |
| `writes_to` | No | `_concept/` paths this skill writes |
| `metadata` | No | Freeform — phase, domain, pipeline position, etc. |
| `metadata.prerequisites` | No | Structured requirements — file gates, inputs, reads, produces (see below) |

## metadata.prerequisites

Skills can declare their dependencies and outputs in `metadata.prerequisites`. This block is read by `skaile-agent-resolver` to validate requirements before execution.

```yaml
metadata:
  prerequisites:
    files:                              # files/dirs that must exist
      - path: "_concept/1_discovery/1_overview/brief.md"
        gate: hard                      # hard = block, soft = warn
        description: "Project brief"
        min_entries: 1                  # for directories only
    inputs_required:                    # user inputs that MUST be collected
      - id: scope
        label: "Feature scope"
        type: select                    # text | textarea | select | multiselect | boolean | number
        options: ["must-have-only", "all-features"]
        default: "all-features"
        hint: "How broad?"
        schema: {}                      # optional JSON Schema
    inputs_optional:                    # user inputs that MAY be collected
      - id: extra_context
        label: "Extra context"
        type: textarea
    reads:                              # optional data sources (never blocks)
      - path: "_concept/_grounding/general/domain.md"
        description: "Domain research"
    produces:                           # what this skill creates
      - path: "_concept/2_experience/2_features/"
        description: "Feature specs"
```

| Section | Purpose |
|---|---|
| `files` | File/directory existence gates. `hard` blocks execution; `soft` warns. |
| `inputs_required` | User inputs that must be collected before the skill runs. |
| `inputs_optional` | User inputs that improve output but are not mandatory. |
| `reads` | Optional data sources the skill checks for additional context. |
| `produces` | Output paths created by the skill (used for flow dependency hints). |

Collected inputs are stored at `_concept/_grounding/{skillId}/input.json`.

> **Migration:** The older `user_inputs` / `reads_from` / `writes_to` fields are still supported by the parser for backward compatibility, but new skills should use `metadata.prerequisites`.

## Skill Body Sections

Well-formed skills follow this structure (not all sections required):

```markdown
## ROLE
What the agent is being asked to be.

## READS
Files/paths the agent must read before starting.

## WRITES
Files/paths the agent will create or update.

## REFERENCES
Shared contracts and reference material to consult.

## MUST
Absolute requirements (mechanical rules, iron laws).

## NEVER
What the agent must not do under any circumstances.

## EMIT
Structured events to emit at key steps.

## STEPS
Numbered procedure — what to do in what order.

## CHECKLIST
Self-verification before declaring complete.
```

## Optional Files

| File | Purpose |
|---|---|
| `CLI.md` | Slash command interface definition — how to invoke from Claude Code |
| `validator.py` | Python validation script for skill output |
| `resources/*.md` | Reference material loaded by the agent on demand |
| `examples/` | Worked examples for few-shot prompting |

## source Values

| Value | Meaning |
|---|---|
| `CF` | Originated in the Concept Forge ecosystem |
| `SAXE` | Originated in the Skaile/SAXE ecosystem |
| `MERGED` | Represents the combined CF + SAXE variant |
| `MIGRATED` | Moved from a deprecated location; treated as current |

## Skill Resolution Search Path

When the runner executes a skill node, it searches for `SKILL.md` in this order:

1. `<projectDir>/.claude/skills/<skillId>/SKILL.md`
2. `<projectDir>/.omp/skills/<skillId>/SKILL.md`
3. Walk up from `projectDir` (max 6 levels) to find `ai-resources/`:
   - For each domain in `ai-resources/`:
     - Scan `<domain>/skills/` recursively
     - Match directory named `<skillId>` or `cf_<skillId>`
4. Fallback: `"Execute skill: <skillId>"` (always succeeds)

## Creating a New Skill

**Recommended**: use the `skill-builder` meta-skill — it creates the directory structure, frontmatter, and body sections interactively.

**Manual**:
1. Create `<domain>/skills/<skill-name>/SKILL.md`
2. Add YAML frontmatter (name, description, source, writes_to at minimum)
3. Write the prompt body (ROLE, STEPS, CHECKLIST at minimum)
4. Register in `DOMAIN.md` under the appropriate section
