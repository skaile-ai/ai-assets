---
title: Developer Guide
description: Repository structure, skill resolution algorithm, contract inheritance, merge strategy, and adding a domain.
---

## Repository Structure

```
ai-assets/
├── ai-asset-management/    ← meta-skills + root orchestrator agent
│   ├── agents/skaile/         ← root router agent
│   ├── skills/                ← skill-builder, domain-builder, etc.
│   └── DOMAIN.md
├── skaileup-conceptualization/
│   ├── agents/orchestrator/   ← concept pipeline agent
│   ├── contracts/             ← domain-specific contracts
│   ├── flows/                 ← *.json flow definitions
│   ├── skills/                ← organized by phase (10_, 20_, 30_)
│   └── DOMAIN.md
├── skaileup-implementation/        ← same structure as skaileup-conceptualization
├── dev-implementation-experts-js/
│   └── prog-expert-*/         ← each is a skill directory (no skills/ subdir)
├── skaileup-evaluate/
│   └── skills/
├── skaileup-shared/
│   ├── contracts/             ← shared specs read by all domains
│   └── flow.schema.json
├── skaileup-standards/
│   └── skills/
├── knowledge-research/        ← skills live directly as top-level dirs
├── knowledge-writing/
├── use/                       ← skills live as top-level dirs (use-*)
├── pichi/
│   └── agent/                 ← default GitAgent (SOUL.md, RULES.md, knowledge/)
└── packages/                  ← shared npm packages
```

## Skill Resolution Algorithm

When the runner looks for a skill by ID, it searches:

1. `<projectDir>/.claude/skills/<id>/SKILL.md`
2. `<projectDir>/.omp/skills/<id>/SKILL.md`
3. Walk up from `projectDir` (max 6 levels) to find `ai-assets/`
4. For each domain directory in `ai-assets/`:
   - Scan `<domain>/skills/` recursively
   - Match: directory name `=== id` OR `=== 'cf_' + id`
5. Fallback string (always succeeds)

**Implication**: the `cf_` prefix is a compatibility alias. A skill directory named `cf_concept_overview` matches a flow node with `skill: "concept_overview"` (or `skill: "cf_concept_overview"`).

## Contract Inheritance

`skaileup-shared/contracts/` contains authoritative definitions that all domain skills reference. The reading chain is:

```
SKILL.md  →  ## REFERENCES  →  skaileup-shared/contracts/*.md
```

Skills list which contracts they follow in their `REFERENCES:` section. This is a documentation/agent convention, not enforced by tooling at build time.

When a contract is updated, all skills that reference it are implicitly affected on the next run — no explicit sync required.

## Merge Strategy (CF / SAXE)

Skills have a `source` frontmatter field:

| Value | Meaning |
|---|---|
| `CF` | Concept Forge origin — may have `cf/` subdirectory variant |
| `SAXE` | Skaile/SAXE origin |
| `MERGED` | Unified variant combining CF and SAXE |
| `MIGRATED` | Moved from deprecated location |

CF and SAXE variants **coexist** under the same skill directory until they are manually merged. The `MERGED` variant is the target state. The merge process involves reconciling different STEPS/CHECKLIST sections and updating `source: MERGED`.

## Adding a Domain

1. Create `ai-assets/<domain-name>/`
2. Add `DOMAIN.md` (name, purpose, key skills)
3. Add `skills/` subdirectory with at least one skill
4. Add `package.yaml` for dependency declarations (optional)
5. If the domain has flows, add `flows/*.json`
6. If the domain has agents, add `agents/<name>/` with `agent.yaml` + `SOUL.md`
7. Register the domain in the root `ai-assets/README.md` domains table
