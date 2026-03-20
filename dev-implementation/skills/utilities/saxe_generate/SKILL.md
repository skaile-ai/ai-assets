---
name: implement-generate
description: "PostXL code generation and conflict resolution. This skill should be used when the user asks to 'generate code', 'run generators', 'regenerate', 'sync schema', 'resolve conflicts', or when the data model has changed and generated code needs updating. Runs PostXL generators from postxl-schema.json, auto-resolves merge conflicts, and verifies the build."
---

ROLE  Code Generation agent — runs PostXL generators, resolves conflicts, and verifies the build.

READS
  _concept/3_blueprint/3_datamodel/postxl-schema.json   — authoritative concept schema (for sync check)
  postxl-schema.json                         — project-root schema consumed by generators
  postxl-lock.json                           — file-state tracking (generated/ejected/custom)

WRITES
  postxl-schema.json                         — updated from concept when syncing
  generated backend + frontend code          — via pnpm run generate
  prisma migrations                          — via prisma migrate dev

REFERENCES
  shared/contracts/prerequisites.md                   — tool prerequisite checks
  shared/contracts/verification.md                    — Level 1 build verification
  shared/contracts/implementation_structure.md        — tracking and progress
  references/conflict_resolution.md          — ejection system, merge cascade, common patterns

REQUIRES
  hard: pnpm, @postxl/cli
  state: postxl-schema.json exists (scaffolded PostXL project)

# ─── Workflow ───────────────────────────────────────────────────────────

EMIT  [implement-generate] started run_id=<uuid>

STEP 1: Pre-flight checks
  - Verify postxl-schema.json exists and is valid JSON
  - Check postxl-lock.json exists (if absent, this is first-time generation)
  - Warn if uncommitted changes in working tree
  IF project-root postxl-schema.json differs from _concept/3_blueprint/3_datamodel/postxl-schema.json
    - Ask user which version to use (concept version is authoritative unless intentionally diverged)
  ELSE
    - Continue with current project-root schema

STEP 2: Schema sync (if needed)
  IF called after a concept schema update
    - Copy _concept/3_blueprint/3_datamodel/postxl-schema.json to project root
    - Diff old and new schemas
    - Report changes: new models, modified fields, removed entities

STEP 3: Run generators
  - $ pnpm run generate
  - Capture output and categorise files:
    - Created (new)
    - Updated (regenerated)
    - Skipped (ejected — user-modified, tracked in postxl-lock.json)
    - Conflicted (both user-modified and generator-modified)

STEP 4: Resolve conflicts
  - Follow the four-level conflict resolution cascade (see references/conflict_resolution.md):
    Level 1 — Auto-resolve: generated-only files overwritten automatically
    Level 2 — Preserve custom blocks: verify <<<<<<< Custom / >>>>>>> Custom markers survived
    Level 3 — Intelligent merge (ejected files):
      - $ pnpm run generate --diff
      - Accept generator structural changes (imports, type definitions)
      - Preserve user business logic (function bodies, custom methods)
      - Attempt semantic merge when both sides modified the same section
      - If conflict is feature-level, suggest refining the concept instead
    Level 4 — Escalate: present both versions with a recommendation for genuine design decisions

STEP 5: Run Prisma migration
  IF schema changed
    - $ pnpm prisma migrate dev --name <descriptive-name>
    - Name describes the change: add_deployment_model, update_app_status_enum, etc.
    IF migration fails due to data loss
      - Dev database: $ pnpm prisma migrate reset then re-seed
      - Production-like: escalate to user

STEP 6: Verify build
  - $ pnpm run build
  - $ pnpm run lint
  - $ pnpm run test:types
  IF build fails after generation
    - Analyse errors (usually type mismatches from schema changes)
    - Fix type errors in custom code to match new generated types
    - Re-run build verification
  UNTIL build passes

STEP 7: Commit and report
  - $ git add -A
  - $ git commit -m "generate: run PostXL generators (<summary of changes>)"

OUTPUT generation_report
  Generation complete.
  Schema: N models (X new, Y updated, Z unchanged)
  Files: A created, B updated, C ejected (preserved), D conflicts resolved
  Migration: <migration-name> applied
  Build: passing

EMIT  [implement-generate] completed run_id=<uuid> models=<N> files_created=<A> files_updated=<B> conflicts_resolved=<D> build=passed

# ─── Re-generation Triggers ────────────────────────────────────────────
# Re-invoke this skill when:
#   1. Schema update — new models or field changes in concept data model
#   2. Post-feature conflicts — feature implementation conflicts with generated code
#   3. Generator upgrade — PostXL generators updated with new capabilities
#   4. Final pass — before full verification, to ensure generated code is current

# ─── Constraints ────────────────────────────────────────────────────────

MUST  run Level 1 verification (build + lint + types) after every generation
MUST  preserve custom blocks — never delete code between <<<<<<< Custom / >>>>>>> Custom markers
MUST  commit generation results as a separate commit (not mixed with feature code)
NEVER  overwrite ejected files without attempting intelligent merge first
NEVER  use --force flag without exhausting merge options
NEVER  leave unresolved conflict markers in committed code
NEVER  modify _concept/ files (read-only)

CHECKLIST
  - [ ] postxl-schema.json is valid and in sync with concept
  - [ ] pnpm run generate completed without errors
  - [ ] All custom blocks survived regeneration
  - [ ] Ejected file conflicts resolved (Level 3+)
  - [ ] Prisma migration applied (if schema changed)
  - [ ] Build passes (pnpm run build + lint + test:types)
  - [ ] Generation committed as a standalone commit
