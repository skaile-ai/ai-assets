---
name: seed
description: "Generates seed scripts from seed.json. Use when migrations exist and you need seed data for development, testing, and demos. Reads seed.json scenarios (empty, single_user, populated, edge_cases) and generates executable seed scripts for the chosen stack."
keywords: seed, data, fixtures, testing, development, demo, scenarios, prisma, drizzle, sql
subagent: true
user_inputs:
  dialog: []
  files: []
---

# App Seed — Seed Script Generator

## Overview

Reads the scenario-based seed data from `_concept/06_datamodel/seed.json` and
generates executable seed scripts for the chosen tech stack. Covers all
scenarios: `empty`, `single_user`, `populated`, and `edge_cases` (plus any
custom scenarios like `permissions`). Each scenario is independently runnable
so developers can switch between data states for testing and demos.

## When to Use

- Migrations exist and the database schema is in place
- `seed.json` has been written (by `cf_concept_datamodel`)
- You need seed scripts for local development, testing, or demo environments
- You want scenario-based seeding (not just one big data dump)

## When NOT to Use

- No migrations yet — run `cf_implement_migrate` first
- No seed.json yet — run `cf_concept_datamodel` first (Step 5 writes seed data)
- You want to modify the seed data itself (edit `seed.json` directly or re-run `cf_concept_datamodel`)
- The project does not use a relational database

## Prerequisites

| Artifact | Path | Missing? Run | Gate |
|----------|------|-------------|------|
| Seed data | `_concept/06_datamodel/seed.json` | `cf_concept_datamodel` | <HARD-GATE> |
| Data model | `_concept/06_datamodel/model.json` | `cf_concept_datamodel` | <HARD-GATE> |
| Tech stack | `_concept/05_techstack/stack.md` | `cf_concept_techstack` | <HARD-GATE> |
| Migration files | varies by stack | `cf_implement_migrate` | <HARD-GATE> |

If any <HARD-GATE> artifact is missing, stop immediately and name the prerequisite skill.

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** Seed data (`06_datamodel/seed.json`), data model (`06_datamodel/model.json`), tech stack (`05_techstack/stack.md`), migration files
**If gates fail:** Run `cf_concept_datamodel`, `cf_concept_techstack`, or `cf_implement_migrate` as needed
**On completion:** Present summary, then suggest next steps.

**Verifying migrations exist:** Check for migration files in the expected
location for the stack (e.g., `prisma/migrations/`, `drizzle/`, `migrations/`).
If no migration files are found:

> "No migration files found. Run `cf_implement_migrate` first to create the database schema."

## Context Budget

| Source | Priority | Token estimate |
|--------|----------|---------------|
| `06_datamodel/seed.json` | must read | ~2000 |
| `06_datamodel/model.json` | must read (for relationships, types) | ~2000 |
| `05_techstack/stack.md` | must read | ~800 |
| `cf__shared/semantic_types.md` | reference for type handling | ~500 |
| `cf__shared/seed_data.md` | scenario format reference | ~500 |

**Total budget:** ~5800 tokens input. Seed script output varies by scenario count and entity count.

## Shared Contracts

Before starting, read:
- `cf__shared/seed_data.md` — scenario-based seed data convention
- `cf__shared/semantic_types.md` — type handling (dates, UUIDs, etc.)
- `cf__shared/concept_structure.md` — valid paths
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Workflow

### Step 1: Read Seed Data

Read `_concept/06_datamodel/seed.json`. Identify all scenarios:
- `empty` — no data (tests empty states and onboarding)
- `single_user` — one user, minimal content (first-use experience)
- `populated` — realistic usage with multiple users and mixed statuses
- `edge_cases` — stress tests for layout, validation, i18n
- Any custom scenarios (e.g., `permissions`, `large_dataset`)

### Step 2: Read Data Model

Read `_concept/06_datamodel/model.json` for:
- Entity field types (to generate correct literal values)
- Relationship structure (to determine insert order)
- Enum definitions (to validate seed enum values)
- Required fields (to ensure seeds are complete)

### Step 3: Read Tech Stack

Read `_concept/05_techstack/stack.md`. Determine the seed execution target:
- **Prisma** → TypeScript seed script using `@prisma/client`
- **Drizzle** → TypeScript seed script using Drizzle insert API
- **Directus** → REST API calls or Directus SDK script
- **Supabase** → SQL seed files or Supabase JS client
- **Raw SQL** → SQL INSERT statements

### Step 4: Search for Expert Skills

Search for `prog-expert-*` skills matching the ORM:
- `prog-expert-prisma` — Prisma seeding patterns, `prisma db seed`
- `prog-expert-drizzle` — Drizzle insert patterns, batch inserts
- `prog-expert-directus` — Directus API seeding, file uploads
- `prog-expert-supabase` — Supabase client seeding patterns

### Step 5: Determine Insert Order

Analyze relationships in `model.json` to determine the correct insert order.
Entities with no foreign keys are inserted first. Entities that depend on
others come after their dependencies.

Example order:
1. `user` (no dependencies)
2. `project` (no dependencies, or depends on `user`)
3. `task` (depends on `user` + `project`)
4. `task_tag` (junction: depends on `task` + `tag`)

### Step 6: Generate Seed Scripts

Generate one seed entry point that can run any scenario by name.

**For Prisma:**

```typescript
// prisma/seed.ts
import { PrismaClient } from '@prisma/client';
import { empty } from './seeds/empty';
import { singleUser } from './seeds/single_user';
import { populated } from './seeds/populated';
import { edgeCases } from './seeds/edge_cases';

const prisma = new PrismaClient();

const scenarios = { empty, singleUser, populated, edgeCases };

async function main() {
  const scenario = process.argv[2] || 'populated';
  // Clear existing data (reverse dependency order)
  await prisma.task.deleteMany();
  await prisma.user.deleteMany();
  // Seed with chosen scenario
  await scenarios[scenario](prisma);
  console.log(`Seeded with scenario: ${scenario}`);
}

main()
  .catch(console.error)
  .finally(() => prisma.$disconnect());
```

```typescript
// prisma/seeds/populated.ts
import { PrismaClient } from '@prisma/client';

export async function populated(prisma: PrismaClient) {
  const maria = await prisma.user.create({
    data: {
      id: 'u1',
      email: 'maria.schmidt@example.com',
      displayName: 'Maria Schmidt',
      role: 'admin',
    },
  });
  // ... remaining entities in dependency order
}
```

**For Drizzle:**

```typescript
// src/db/seed.ts
import { db } from './index';
import { user, task } from './schema';

const scenarios = {
  populated: async () => {
    await db.insert(user).values([
      { id: 'u1', email: 'maria.schmidt@example.com', displayName: 'Maria Schmidt', role: 'admin' },
      // ...
    ]);
    await db.insert(task).values([
      { id: 't1', title: 'Design landing page', status: 'done', assignedTo: 'u1' },
      // ...
    ]);
  },
  // ... other scenarios
};
```

**For raw SQL:**

```sql
-- seeds/populated.sql
INSERT INTO "user" (id, email, display_name, role) VALUES
  ('u1', 'maria.schmidt@example.com', 'Maria Schmidt', 'admin'),
  ('u2', 'jean-pierre.dubois@example.com', 'Jean-Pierre Dubois', 'member');

INSERT INTO task (id, title, status, assigned_to) VALUES
  ('t1', 'Design landing page', 'done', 'u1'),
  ('t2', 'Set up CI pipeline', 'in_progress', 'u2');
```

### Step 7: Validate Seeds

After generating, verify:
- All entity IDs from `seed.json` are preserved (relational integrity)
- Foreign key references resolve (e.g., `assigned_to: "u1"` → user `u1` exists)
- Enum values match the model's enum definitions
- Required fields are populated in every scenario
- The `empty` scenario truly inserts no data (only clears)
- The `edge_cases` scenario includes special characters, long strings, null optionals
- Insert order respects foreign key constraints

### Step 8: Present Seed Summary

```
## Seed Script Summary

Target: Prisma + PostgreSQL
Scenarios: 4 (empty, single_user, populated, edge_cases)
Entities seeded: user (3), task (3), project (2) [populated scenario]

Files generated:
- prisma/seed.ts (entry point)
- prisma/seeds/empty.ts
- prisma/seeds/single_user.ts
- prisma/seeds/populated.ts
- prisma/seeds/edge_cases.ts

Usage:
  npx prisma db seed                    # default: populated
  npx prisma db seed -- populated       # explicit scenario
  npx prisma db seed -- edge_cases      # edge case testing
```

### Step 9: Emit Events

```
[cf_implement_seed] started
  run_id: <uuid>
  reads: 06_datamodel/seed.json, 06_datamodel/model.json, 05_techstack/stack.md

[cf_implement_seed] checkpoint phase=scenarios_parsed
  scenarios: [empty, single_user, populated, edge_cases]
  entities: N

[cf_implement_seed] checkpoint phase=scripts_generated
  files: [list]
  target: prisma

[cf_implement_seed] completed
  run_id: <uuid>
  seed_files: [list]
  scenarios: 4
```

## Outputs

| Output | Location | Depends on stack |
|--------|----------|-----------------|
| Prisma seed entry | `prisma/seed.ts` | Prisma |
| Prisma seed scenarios | `prisma/seeds/<scenario>.ts` | Prisma |
| Drizzle seed script | `src/db/seed.ts` | Drizzle |
| Drizzle seed scenarios | `src/db/seeds/<scenario>.ts` | Drizzle |
| Directus seed script | `scripts/seed.ts` | Directus |
| Raw SQL seeds | `seeds/<scenario>.sql` | Raw SQL |

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

> "Seed scripts generated. Next steps:
> - Apply seeds: `npx prisma db seed` (or equivalent for your stack)
> - Run `cf_implement_feature` to start building features
> - Use `edge_cases` scenario for testing, `populated` for development"

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Wrong insert order | Not analyzing foreign keys | Build dependency graph from `model.json` relationships, insert parents first |
| Broken ID references | Typos in foreign key values | Cross-validate all IDs: every foreign key value must exist as a primary key |
| Missing the empty scenario | Treating empty as "do nothing" | Empty must actively CLEAR all data (truncate/delete), not just skip inserts |
| Hardcoded UUIDs that clash | Using `uuid()` instead of deterministic IDs | Use the short IDs from `seed.json` (u1, t1) for deterministic, reproducible seeds |
| Ignoring enum validation | Inserting values not in the enum | Validate every enum field value against `model.json` enum definitions |
| Single monolithic seed file | Putting all scenarios in one file | One file per scenario for independent execution and clear organization |
| Not clearing before seeding | Appending to existing data | Always truncate/delete all tables (in reverse dependency order) before inserting |

## Integration

- **Upstream:** reads from `06_datamodel/` (seed.json, model.json), `05_techstack/` (stack.md)
- **Depends on:** `cf_implement_migrate` (migration files must exist)
- **Downstream:** consumed by `cf_implement_feature` (for test fixtures), `cf_test_e2e` (for test data)
- **Phase:** implementation (infrastructure setup)
- **Pipeline position:** after `cf_implement_migrate`, before `cf_implement_feature`
- **Subagent:** yes — searches for `prog-expert-*` skills for ORM-specific seeding patterns
- **Called by:** orchestrator or standalone
- **Feedback loop:** none (does not modify concept files)
