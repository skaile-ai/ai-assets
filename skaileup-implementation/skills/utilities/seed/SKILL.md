---
name: "seed"
description: "Generates seed scripts from seed.json. Reads seed.json scenarios (empty, single_user, populated, edge_cases) and generates executable seed scripts for the chosen stack (Prisma, Drizzle, raw SQL, etc.). Each scenario is independently runnable. Run after migrate."
metadata:
  version: "1.0.0"
  tags:
    - "seed"
    - "data"
    - "fixtures"
    - "testing"
    - "development"
    - "demo"
    - "scenarios"
    - "prisma"
    - "drizzle"
    - "sql"
  source: "CF"
  prerequisites:
    files:
      - path: "_concept/3_blueprint/3_datamodel/seed.json"
        gate: hard
        description: "Seed data scenarios required to generate seed scripts"
      - path: "_concept/3_blueprint/3_datamodel/model.json"
        gate: hard
        description: "Data model required for entity type validation and relationship seeding"
      - path: "_concept/3_blueprint/1_techstack/stack.md"
        gate: hard
        description: "Tech stack required to select seed script format (Prisma, Drizzle, raw SQL)"
      - path: "migrations"
        gate: hard
        description: "Migrations must exist before seed scripts can be run"
    produces:
      - path: "scripts/seed"
        description: "Executable seed scripts per scenario (empty, single_user, populated, edge_cases)"
---

# Seed — Seed Script Generator

## Overview

Reads the scenario-based seed data from `_concept/3_blueprint/3_datamodel/seed.json`
and generates executable seed scripts for the chosen tech stack. Covers all
scenarios: `empty`, `single_user`, `populated`, and `edge_cases` (plus any
custom scenarios). Each scenario is independently runnable for switching
between data states during development, testing, and demos.

## When to Use

- Migration files exist and the database schema is in place
- `seed.json` has been written (by `datamodel` skill)
- You need seed scripts for local development, testing, or demo environments
- You want scenario-based seeding (not just one data dump)

## When NOT to Use

- No migrations yet — run `migrate` first
- No seed.json yet — run `datamodel` first
- The project does not use a relational database

## Prerequisites

**Hard gates:**
- `_concept/3_blueprint/3_datamodel/seed.json`
- `_concept/3_blueprint/3_datamodel/model.json`
- `_concept/3_blueprint/1_techstack/stack.md`
- Migration files exist (varies by stack)

---

ROLE  Seed agent — generates stack-specific seed scripts from seed.json scenarios.

READS
  _concept/3_blueprint/3_datamodel/seed.json     — all scenarios (empty, single_user, populated, edge_cases)
  _concept/3_blueprint/3_datamodel/model.json    — entity relationships, types, enums
  _concept/3_blueprint/1_techstack/stack.md      — seed execution target
  skaileup-shared/contracts/seed_data.md              — scenario format reference
  skaileup-shared/contracts/semantic_types.md         — type handling (dates, UUIDs, etc.)

WRITES
  <stack-specific seed scripts>                  — one file per scenario + entry point

MUST  respect entity insert order (parents before children)
MUST  preserve all IDs from seed.json (relational integrity)
MUST  include an empty scenario that actively CLEARS all data
MUST  validate enum values against model.json definitions
MUST  search for prog-expert-* skills for ORM-specific patterns
NEVER leave dangling foreign key references
NEVER modify _concept/ files

# ── Workflow ──────────────────────────────────────────────────────

STEP 1: Read seed data
  - Read seed.json: enumerate all scenarios
  - Standard scenarios: empty, single_user, populated, edge_cases
  - Plus any custom scenarios

STEP 2: Read data model
  - Read model.json: field types, relationships, enums, required fields
  - Build entity insert order (dependency graph: parents before children)

STEP 3: Read tech stack
  - Read stack.md: ORM + database → determine seed execution target

STEP 4: Search for expert skills
  - Search dev-implementation-experts-* for ORM-specific seeding patterns

STEP 5: Determine insert order
  - Analyze relationships: entities with no foreign keys first
  - Entities with foreign keys after their dependencies
  - Generate reverse order for cleanup (delete/truncate)

STEP 6: Generate seed scripts
  - One entry point that accepts a scenario name
  - One file per scenario (clear separation for independent execution)

  For Prisma:
  - `prisma/seed.ts` (entry point)
  - `prisma/seeds/<scenario>.ts` (per scenario)

  For Drizzle:
  - `src/db/seed.ts` (entry point)
  - `src/db/seeds/<scenario>.ts`

  For raw SQL:
  - `seeds/<scenario>.sql` (per scenario)

  Entry point:
  - Clear all tables (reverse dependency order)
  - Seed chosen scenario
  - Accept scenario as CLI argument (default: populated)

STEP 7: Validate seeds
  - All entity IDs from seed.json preserved
  - Foreign key references resolve (every FK value exists as a PK)
  - Enum values match model.json definitions
  - Required fields populated in every scenario
  - Empty scenario actively clears all data
  - Edge_cases includes special characters, long strings, null optionals

STEP 8: Present summary
  ```
  ## Seed Script Summary
  Target: <ORM> + <database>
  Scenarios: N (empty, single_user, populated, edge_cases)
  Entities seeded: <list with counts> [populated scenario]
  Files: <list>
  Usage: <run command> [scenario]
  ```

EMIT [seed] started run_id=<uuid>
EMIT [seed] completed run_id=<uuid> scenarios=<N> files=<list>

---

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Wrong insert order | Build dependency graph; parents before children |
| Broken FK references | Cross-validate: every FK value must exist as a PK |
| Empty scenario does nothing | Empty must actively CLEAR all data |
| Hardcoded UUIDs that clash | Use short IDs from seed.json (u1, t1) for deterministic seeds |
| Missing enum validation | Validate every enum field against model.json |
| Single monolithic seed file | One file per scenario for independent execution |
| Not clearing before seeding | Always truncate/delete first (reverse dependency order) |

## Standalone Mode

**Gate check:** `seed.json`, `model.json`, `stack.md`, migration files
**On completion:** Suggest running `implement-feature` next.
