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
