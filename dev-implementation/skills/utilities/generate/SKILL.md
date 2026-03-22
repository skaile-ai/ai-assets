---
name: "generate"
description: "PostXL code generation and conflict resolution. Runs PostXL generators from postxl-schema.json, auto-resolves merge conflicts using a four-level cascade, and verifies the build. Only applicable when using the PostXL tech stack. Re-run after any schema change or custom action addition."
metadata:
  version: "1.0.0"
  tags:
    - "generate"
    - "codegen"
    - "postxl"
    - "schema"
    - "conflicts"
    - "prisma"
    - "regenerate"
    - "sync"
  source: "SAXE"
  prerequisites:
    files:
      - path: "postxl-schema.json"
        gate: hard
        description: "PostXL schema file required — this skill is PostXL-specific"
    reads:
      - path: "_concept/3_blueprint/3_datamodel/model.json"
        description: "Data model for sync-check between concept and PostXL schema"
      - path: "postxl-lock.json"
        description: "Lock file to detect schema drift and guide conflict resolution"
    produces:
      - path: "src"
        description: "Generated backend and frontend code from PostXL schema"
---

# Generate — PostXL Code Generation

> **Stack requirement:** This skill is specific to the **PostXL** tech stack.
> It requires `@postxl/cli` and a `postxl-schema.json` in the project root.
> If your project uses a different stack, use your stack's equivalent tooling.

## Overview

Runs PostXL generators, resolves merge conflicts using a four-level cascade,
and verifies the build. Keeps generated code synchronized with
`postxl-schema.json` while preserving user customizations.

## When to Use

- Schema changed (`postxl-schema.json` modified — new models, field changes)
- Custom action added (requires regeneration to get routes + placeholders)
- After feature implementation conflicts with generated code
- Final pass before verification (ensure generated code is current)

## When NOT to Use

- Using a non-PostXL stack — this skill is PostXL-specific
- No `postxl-schema.json` exists — scaffold first

---

ROLE  Code generation agent — runs PostXL generators, resolves conflicts, verifies build.

READS
  _concept/3_blueprint/3_datamodel/model.json   — authoritative concept schema (for sync check)
  postxl-schema.json                            — project-root schema consumed by generators
  postxl-lock.json                              — file-state tracking (generated/ejected/custom)

WRITES
  postxl-schema.json                            — updated from concept when syncing
  generated backend + frontend code             — via pnpm run generate
  prisma migrations                             — via prisma migrate dev

MUST  run Level 1 verification after every generation
MUST  preserve custom blocks — never delete code between <<<<<<< Custom / >>>>>>> Custom markers
MUST  commit generation results as a standalone commit
NEVER overwrite ejected files without attempting intelligent merge first
NEVER use --force without exhausting merge options
NEVER leave unresolved conflict markers in committed code
NEVER modify _concept/ files

EMIT [generate] started run_id=<uuid>

# ── Workflow ──────────────────────────────────────────────────────

STEP 1: Pre-flight
  - Verify postxl-schema.json exists and is valid JSON
  - Check postxl-lock.json (absent = first-time generation)
  - Warn if uncommitted changes in working tree
  IF project-root schema differs from _concept/3_blueprint/3_datamodel/model.json
    - Ask user which version to use (concept is authoritative unless intentionally diverged)

STEP 2: Schema sync (if needed)
  IF called after a concept schema update
    - Copy concept model.json fields to project-root postxl-schema.json
    - Diff old and new schemas: report new models, modified fields, removed entities

STEP 3: Run generators
  $ pnpm run generate
  - Categorize files: Created | Updated | Skipped (ejected) | Conflicted

STEP 4: Resolve conflicts (four-level cascade)
  Level 1 — Auto-resolve: generated-only files overwritten automatically
  Level 2 — Preserve custom blocks: verify <<<<<<< Custom / >>>>>>> Custom markers survived
  Level 3 — Intelligent merge (ejected files):
    $ pnpm run generate --diff
    - Accept generator structural changes (imports, type definitions)
    - Preserve user business logic (function bodies, custom methods)
  Level 4 — Escalate: present both versions for genuine design decisions

STEP 5: Run Prisma migration (if schema changed)
  $ pnpm prisma migrate dev --name <descriptive-name>
  IF data loss risk
    - Dev database: $ pnpm prisma migrate reset then re-seed
    - Production-like: escalate to user

STEP 6: Verify build
  $ pnpm run build && pnpm run lint && pnpm run test:types
    UNTIL build passes

STEP 7: Commit
  $ git add -A
  $ git commit -m "generate: run PostXL generators (<summary>)"

  ```
  Generation complete.
  Schema: N models (X new, Y updated, Z unchanged)
  Files: A created, B updated, C ejected (preserved), D conflicts resolved
  Migration: <migration-name> applied
  Build: passing
  ```

EMIT [generate] completed run_id=<uuid> models=<N> files_created=<A> files_updated=<B> conflicts_resolved=<D>

CHECKLIST
  - [ ] postxl-schema.json valid and in sync with concept
  - [ ] pnpm run generate completed without errors
  - [ ] Custom blocks survived regeneration
  - [ ] Ejected file conflicts resolved
  - [ ] Prisma migration applied (if schema changed)
  - [ ] Build passes (build + lint + types)
  - [ ] Generation committed as standalone commit
