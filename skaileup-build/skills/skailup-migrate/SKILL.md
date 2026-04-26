---
name: "skailup-migrate"
description: "Generates database migrations from the data model. Reads model.dbml + model.json and stack.md, then generates migration files for the target ORM (Prisma, Drizzle, Directus, raw SQL). Translates semantic types using skaileup-shared/contracts/semantic_types.md. Run before implement-feature."
metadata:
  version: "1.0.0"
  tags:
    - "migrate"
    - "database"
    - "schema"
    - "prisma"
    - "directus"
    - "drizzle"
    - "sql"
    - "ddl"
    - "migration"
  source: "CF"
  prerequisites:
    files:
      - path: "_concept/blueprint/datamodel/model.dbml"
        gate: hard
        description: "DBML model required for entity and field definitions"
      - path: "_concept/blueprint/datamodel/model.json"
        gate: hard
        description: "JSON model required for relationship and type mapping"
      - path: "_concept/blueprint/techstack.md"
        gate: hard
        description: "Tech stack required to select target ORM and migration framework"
    produces:
      - path: "migrations"
        description: "Stack-specific migration files (Prisma schema, Drizzle migrations, SQL DDL)"
---

# Migrate — Database Migration Generator

## Overview

Reads the stack-independent data model (`model.dbml` + `model.json`) and
the chosen tech stack (`stack.md`), then generates database migration files
for the target ORM or migration framework. Translates semantic types to
stack-specific types using the translation table in
`skaileup-shared/contracts/semantic_types.md`.

## When to Use

- Data model is complete (`model.dbml` + `model.json` exist)
- Tech stack is chosen and includes a database + ORM/migration tool
- You need migration files before implementing features
- Setting up the database for the first time, or the model has changed

## When NOT to Use

- No data model yet — run `datamodel` first
- No tech stack yet — run `techstack` first
- You want seed data — use `seed` instead
- Schema-less database (MongoDB) with no migrations

## Prerequisites

**Hard gates:**
- `_concept/blueprint/datamodel/model.dbml`
- `_concept/blueprint/datamodel/model.json`
- `_concept/blueprint/techstack.md`

---

ROLE  Migration agent — generates stack-specific database migrations from model.json + model.dbml.

READS
  _concept/blueprint/datamodel/model.dbml    — human-readable model structure
  _concept/blueprint/datamodel/model.json    — relationships, enums, field metadata
  _concept/blueprint/techstack.md      — ORM + migration tool + database
  skaileup-shared/contracts/semantic_types.md         — type translation table

WRITES
  <stack-specific migration files>               — paths depend on ORM

MUST  read semantic_types.md before generating — no semantic types in output
MUST  cross-check model.dbml and model.json for consistency
MUST  search for prog-expert-* skills matching the ORM before generating
NEVER leave semantic type names in migration output
NEVER modify _concept/ files

# ── Workflow ──────────────────────────────────────────────────────

STEP 1: Read data model
  - Read model.dbml (structure) and model.json (relationships, enums, metadata)
  - If they disagree: model.dbml is authoritative for structure;
    model.json is authoritative for relationship and enum metadata
  - Extract: entities, fields (name, type, constraints), relationships, enums

STEP 2: Read tech stack
  - Read stack.md: database, ORM/migration tool, backend framework
  - Determine migration target: Prisma, Drizzle, Directus, raw SQL, etc.

STEP 3: Load type translation table
  - Read skaileup-shared/contracts/semantic_types.md
  - Map every semantic type to the target stack's concrete type

STEP 4: Search for expert skills
  - Search dev-implementation-experts-* for stack-specific migration patterns
  - Load relevant expert: prog-expert-prisma, prog-expert-drizzle, etc.
  - Follow expert guidance for file structure, naming, idiomatic patterns

STEP 5: Generate migrations
  For Prisma: generate `prisma/schema.prisma` then `prisma migrate dev --name init`
  For Drizzle: generate schema files in `src/db/schema/` then `drizzle-kit generate`
  For Directus: generate snapshot JSON or Directus API migration
  For raw SQL: generate DDL migration file in `migrations/`

  Apply these conventions always:
  - IDs: UUID with auto-generation (per expert skill's UUID syntax)
  - Timestamps: `created_at` on every entity; `updated_at` on most
  - ON DELETE: read from model.json relationship `on_delete` field; default SET NULL
  - Many-to-many: always generate junction table with two foreign keys
  - snake_case column names, mapped from field names

STEP 6: Validate output
  - Every entity in model.json has a corresponding table/model
  - Every relationship is represented (foreign keys, junction tables)
  - Every enum is defined
  - No semantic types remain in the output
  - UUIDs use auto-generation
  - ON DELETE behavior matches model.json

STEP 7: Present summary
  ```
  ## Migration Summary
  Target: <ORM> + <database>
  Entities: N tables
  Relationships: M (K foreign keys, J junction tables)
  Enums: L
  Files: <list>
  ```

EMIT [migrate] started run_id=<uuid>
EMIT [migrate] completed run_id=<uuid> tables=<N> enums=<M> files=<list>

---

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Semantic types in migration output | Always translate via semantic_types.md |
| Missing junction tables | Every m2m needs a junction table with two FKs |
| Wrong ON DELETE | Check `on_delete` in model.json relationships |
| Wrong UUID syntax | Use the expert skill for the correct syntax |
| Missing timestamps | Every entity needs created_at; most need updated_at |
| Generating for wrong stack | Always read stack.md first |

## Standalone Mode

**Gate check:** `model.dbml`, `model.json`, `stack.md`
**On completion:** Suggest running `seed` next.
