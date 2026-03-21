---
name: migrate
description: "Generates database migrations from the data model. Use when model.dbml and stack.md exist and you need migration files for the chosen stack (Prisma, Directus, Drizzle, or raw SQL). Translates semantic types using cf__shared/semantic_types.md."
keywords: migrate, database, schema, prisma, directus, drizzle, sql, ddl, migration
subagent: true
user_inputs:
  dialog: []
  files: []
metadata:
  stage: alpha
  requires:
  - implementation-contract
---

# App Migrate — Database Migration Generator

## Overview

Reads the stack-independent data model (`model.dbml` + `model.json`) and the
chosen tech stack (`stack.md`), then generates database migration files for the
target ORM or migration framework. Translates semantic types to stack-specific
types using the translation table in `cf__shared/semantic_types.md`.

## When to Use

- Data model is complete and approved (`model.dbml` + `model.json` exist)
- Tech stack is chosen and includes a database + ORM/migration tool
- You need migration files before implementing features
- You are setting up the database for the first time, or the model has changed

## When NOT to Use

- No data model yet — run `cf_concept_datamodel` first
- No tech stack yet — run `cf_concept_techstack` first
- You want to design the data model (that is `cf_concept_datamodel`)
- You want seed data (that is `cf_implement_seed`)
- The project uses a schema-less database (MongoDB, etc.) with no migrations

## Prerequisites

| Artifact | Path | Missing? Run | Gate |
|----------|------|-------------|------|
| Data model (DBML) | `_concept/06_datamodel/model.dbml` | `cf_concept_datamodel` | <HARD-GATE> |
| Data model (JSON) | `_concept/06_datamodel/model.json` | `cf_concept_datamodel` | <HARD-GATE> |
| Tech stack | `_concept/05_techstack/stack.md` | `cf_concept_techstack` | <HARD-GATE> |

If any <HARD-GATE> artifact is missing, stop immediately and name the prerequisite skill.

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** Data model DBML (`06_datamodel/model.dbml`), data model JSON (`06_datamodel/model.json`), tech stack (`05_techstack/stack.md`)
**If gates fail:** Run `cf_concept_datamodel` or `cf_concept_techstack` as needed
**On completion:** Present summary, then suggest next steps.

## Context Budget

| Source | Priority | Token estimate |
|--------|----------|---------------|
| `06_datamodel/model.dbml` | must read | ~1500 |
| `06_datamodel/model.json` | must read | ~2000 |
| `05_techstack/stack.md` | must read | ~800 |
| `cf__shared/semantic_types.md` | must read | ~1000 |

**Total budget:** ~5300 tokens input. Migration output varies by entity count.

## Shared Contracts

Before starting, read:
- `cf__shared/semantic_types.md` — the complete type translation table
- `cf__shared/concept_structure.md` — valid paths
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Workflow

### Step 1: Read Data Model

Read both `model.dbml` (for human-readable structure) and `model.json`
(for relationships, enums, and field metadata). Cross-reference to ensure
consistency. If they disagree, treat `model.dbml` as authoritative for
structure and `model.json` as authoritative for editor metadata (positions,
colors, icons).

Extract:
- All entities and their fields (name, type, constraints)
- All relationships (type, from, to, on_delete)
- All enums (id, values)

### Step 2: Read Tech Stack

Read `_concept/05_techstack/stack.md`. Extract:
- **Database:** PostgreSQL, SQLite, MySQL, etc.
- **ORM / migration tool:** Prisma, Drizzle, Directus, raw SQL, etc.
- **Backend framework:** determines file locations

### Step 3: Load Translation Table

Read `cf__shared/semantic_types.md`. Use the Stack Translation Table to map
each semantic type to the target stack's concrete type:

| Semantic | Directus | Prisma | Supabase | Raw SQL |
|----------|----------|--------|----------|---------|
| `string` | `string` | `String` | `text` | `VARCHAR(255)` |
| `uuid` | `uuid` | `String @id @default(uuid())` | `uuid` | `UUID` |
| ... | ... | ... | ... | ... |

### Step 4: Search for Expert Skills

Search for `prog-expert-*` skills matching the ORM/migration tool:
- Prisma → `prog-expert-prisma`
- Drizzle → `prog-expert-drizzle`
- Directus → `prog-expert-directus`
- Supabase → `prog-expert-supabase`

Load the relevant expert skill for migration conventions, file structure,
and idiomatic patterns.

### Step 5: Generate Migrations

**For Prisma:**

Generate `prisma/schema.prisma` with all models, enums, and relations:

```prisma
model User {
  id          String   @id @default(uuid())
  email       String   @unique
  displayName String   @map("display_name")
  role        UserRole @default(member)
  createdAt   DateTime @default(now()) @map("created_at")
  tasks       Task[]   @relation("assigned_tasks")

  @@map("user")
}

enum UserRole {
  admin
  member
}
```

Then run `prisma migrate dev --name init` (or generate the SQL migration file).

**For Drizzle:**

Generate schema files in `src/db/schema/`:

```typescript
// src/db/schema/user.ts
export const user = pgTable('user', {
  id: uuid('id').primaryKey().defaultRandom(),
  email: varchar('email', { length: 255 }).unique().notNull(),
  displayName: varchar('display_name', { length: 255 }).notNull(),
  role: userRoleEnum('role').default('member'),
  createdAt: timestamp('created_at', { withTimezone: true }).defaultNow().notNull(),
});
```

Generate migration SQL via `drizzle-kit generate`.

**For Directus:**

Generate a snapshot JSON or a Directus schema migration that creates
collections, fields, and relations using the Directus API format.

**For raw SQL:**

Generate a PostgreSQL DDL migration file:

```sql
-- migrations/001_init.sql

CREATE TYPE user_role AS ENUM ('admin', 'member');

CREATE TABLE "user" (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  display_name VARCHAR(255) NOT NULL,
  role user_role DEFAULT 'member',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE task (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title VARCHAR(255) NOT NULL,
  description TEXT,
  status task_status DEFAULT 'todo',
  assigned_to UUID REFERENCES "user"(id) ON DELETE SET NULL,
  due_date DATE
);
```

### Step 6: Validate Migrations

After generating, verify:
- Every entity in `model.json` has a corresponding table/model
- Every relationship is represented (foreign keys, junction tables for m2m)
- Every enum is defined
- Every field type was translated correctly (no semantic types remain)
- `id` fields use UUID with auto-generation
- `created_at` / `updated_at` timestamps are present where needed
- ON DELETE behavior matches `model.json` relationship `on_delete` values

### Step 7: Present Migration Summary

```
## Migration Summary

Target: Prisma + PostgreSQL
Entities: N tables
Relationships: M (K foreign keys, J junction tables)
Enums: L

Files generated:
- prisma/schema.prisma
- prisma/migrations/YYYYMMDD_init/migration.sql

Type translations applied:
- uuid → String @id @default(uuid())
- email → String (with @unique where specified)
- richtext → String
- enum → Prisma enum
- relation → @relation with foreign key
```

### Step 8: Emit Events

```
[cf_implement_migrate] started
  run_id: <uuid>
  reads: 06_datamodel/model.dbml, 06_datamodel/model.json, 05_techstack/stack.md

[cf_implement_migrate] checkpoint phase=types_translated
  entities: N
  enums: M
  target: prisma

[cf_implement_migrate] checkpoint phase=migrations_generated
  files: [list]
  tables: N
  relationships: M

[cf_implement_migrate] completed
  run_id: <uuid>
  migration_files: [list]
  tables: N
  enums: M
```

## Outputs

| Output | Location | Depends on stack |
|--------|----------|-----------------|
| Prisma schema | `prisma/schema.prisma` | Prisma |
| Prisma migration | `prisma/migrations/<timestamp>_init/migration.sql` | Prisma |
| Drizzle schema | `src/db/schema/*.ts` | Drizzle |
| Drizzle migration | `drizzle/<timestamp>_init.sql` | Drizzle |
| Directus snapshot | `directus/snapshot.json` | Directus |
| Raw SQL migration | `migrations/001_init.sql` | Raw SQL |

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

> "Migration files generated. Next steps:
> - Run `cf_implement_seed` to generate seed scripts for test data
> - Run `cf_implement_feature` to start building features
> - The migrations should be applied to the database before feature implementation."

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Using semantic types in migration output | Forgetting to translate | Always translate via `cf__shared/semantic_types.md` — no semantic types in output |
| Missing junction tables for m2m | Only creating the two main tables | Every m2m relationship needs a junction table with two foreign keys |
| Forgetting ON DELETE behavior | Not reading relationship metadata | Check `on_delete` in `model.json` relationships — default to SET NULL if unspecified |
| Wrong UUID generation | Stack-specific UUID syntax varies | Use the expert skill for the correct UUID default syntax |
| Missing timestamps | Not adding created_at/updated_at | Every entity should have `created_at`; most should have `updated_at` |
| Generating for wrong stack | Not reading stack.md | Always read `stack.md` first — never assume the target |

## Integration

- **Upstream:** reads from `06_datamodel/` (model.dbml, model.json), `05_techstack/` (stack.md)
- **Downstream:** consumed by `cf_implement_seed`, `cf_implement_feature`
- **Phase:** implementation (infrastructure setup)
- **Pipeline position:** after `cf_implement` plan, before `cf_implement_feature`
- **Subagent:** yes — searches for `prog-expert-*` skills for ORM-specific patterns
- **Called by:** orchestrator or standalone
- **Feedback loop:** none (does not modify concept files)
