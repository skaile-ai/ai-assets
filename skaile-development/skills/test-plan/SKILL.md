---
name: "test-plan"
description: "Generates a per-package test plan for any skaile-dev package. Reads CLAUDE.md (architecture, public API), the source tree, and existing tests; produces <package>/TEST_PLAN.md listing untested units, coverage gaps, integration seams, and recommended scenarios per layer (unit/integration/e2e). Used as the input for test-unit, test-integration, and test-e2e."
metadata:
  version: "1.0.0"
  tags:
    - "testing"
    - "test-plan"
    - "coverage"
    - "scenarios"
    - "scaffolding"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    files:
      - path: "package.json"
        gate: hard
        description: "Monorepo root package.json required"
    inputs_required:
      - id: target
        label: "Package path"
        type: text
        hint: "e.g. forge/project, agent-framework/runner, platform/backend"
    inputs_optional:
      - id: layers
        label: "Layers to plan (comma-separated)"
        type: text
        default: "unit,integration,e2e"
        hint: "Choose subset when some layers don't apply (libraries have no e2e)"
    reads:
      - path: "<target>/CLAUDE.md"
        description: "Package architecture + public API source of truth"
      - path: "<target>/package.json"
        description: "Scripts, entry points, exported bins"
      - path: "<target>/src"
        description: "Source tree for testable unit discovery"
      - path: "ai-assets/skaile-development/references/test_stack_map.md"
        description: "Framework + run commands for the package"
    produces:
      - path: "<target>/TEST_PLAN.md"
        description: "Structured per-layer test plan, consumed by test-unit/-integration/-e2e"
  user_inputs:
    dialog:
      - id: "target"
        label: "Package path"
        type: "text"
        required: true
      - id: "layers"
        label: "Layers (comma-separated)"
        type: "text"
        required: false
        default: "unit,integration,e2e"
    files: []
---

# Test Plan — Per-Package Coverage Plan

## Overview

Produces a structured test plan that enumerates *what* should be tested at each layer for one package. The plan is the input for the three setup/generation skills (`test-unit`, `test-integration`, `test-e2e`) and for `audit` to measure coverage gaps.

The plan is package-local (not app-wide) and is derived from:
- The package's **CLAUDE.md** — the source of truth for Architecture + public API
- The **package.json** — entry points, exported binaries, build targets
- The **source tree** — exported symbols, server routes, pages, components
- **Existing tests** — what's already covered (do not re-list)

## When to Use

- Before running `test-unit` / `test-integration` / `test-e2e` on a package that has no plan yet
- After a major refactor — regenerate the plan to capture new coverage gaps
- When onboarding a package to structured testing (most agent-framework libs)
- As prep for a release (feeds into `ready` as evidence of coverage)

## When NOT to Use

- For writing tests directly — use `test-unit`, `test-integration`, or `test-e2e`
- For running tests — use `test`
- For a multi-package coverage dashboard — use `audit scope=full`

---

ROLE  Test planner — reads a single package's CLAUDE.md + source tree and produces a layered test plan.

READS
  ! <target>/CLAUDE.md                 — Architecture + public API sections
  ! <target>/package.json              — scripts + entry points
  ! <target>/src/**                    — exported symbols, routes, components
  ? <target>/tests/** or test/**       — existing coverage to exclude
  ! ai-assets/skaile-development/references/test_stack_map.md

WRITES
  <target>/TEST_PLAN.md                — the plan (overwrites prior, backup goes to TEST_PLAN.prev.md)

MUST  read the target CLAUDE.md before listing any unit
MUST  exclude units that already have test coverage (scan existing test files)
MUST  classify every unit into exactly one layer (unit / integration / e2e)
MUST  provide a seed-data or fixture strategy for every integration and e2e scenario
MUST  respect the `layers` input — skip sections for layers not requested
NEVER invent public API that isn't in CLAUDE.md or exported from src/
NEVER list units in the wrong layer (DB-touching code is integration, browser-dependent is e2e)

EMIT [test-plan] started target=<pkg>

# ── Phase 1: Context Loading ──────────────────────────────────────

STEP 1: Validate target
  - Verify <target>/package.json exists
  - Read <target>/CLAUDE.md (required: What This Is, Architecture, Tech Stack sections)
  - Read <target>/package.json (name, main, bin, scripts)

STEP 2: Classify package category
  Using the Package Categories table from references/readiness_criteria.md:
  - app | library | internal-library | cli | docs-site | ai-domain | theme
  - This determines which layers apply (libraries have no e2e, internal types have only unit, etc.)

STEP 3: Discover existing coverage
  - Glob <target>/{tests,test}/**/*.{test,spec}.{ts,tsx}
  - For each existing test file, extract what it covers (describe blocks, file-under-test imports)
  - Build a set of already-covered source paths

# ── Phase 2: Unit Discovery ───────────────────────────────────────

STEP 4: Enumerate testable units per layer

  Unit layer (pure logic, no I/O):
    - Exported functions in src/ that don't import fs, DB, network
    - Composables (Nuxt / Vue) — test return values + reactivity
    - Utility modules, validators, pure stores
    - Type guards, parsers, formatters

  Integration layer (I/O with bounded scope):
    - API route handlers (server/api/*.ts, controllers in NestJS, apps in Nuxt server/)
    - Database repositories / services (drizzle queries, Prisma services)
    - Agent framework: session state machines, flow-engine transitions, bridge driver integration
    - External-service clients (with mocked network but real parser)

  E2E layer (full-stack, browser or CLI end-to-end):
    - User journeys through the app (forge apps, platform frontend)
    - CLI command end-to-end flows (agent-framework/cli)
    - Cross-package integration (e.g. agent-framework/runner ↔ bridge ↔ connectors)

  Skip: generated files, `dist/`, `node_modules`, test files themselves, `.config.ts` files.

# ── Phase 3: Scenario Generation ──────────────────────────────────

STEP 5: For each unit, specify scenarios

  Unit scenarios:
    - Happy path — correct inputs → expected output
    - Edge inputs — empty, max, min, special chars
    - Error inputs — invalid types, bad shapes → throw/reject

  Integration scenarios:
    - Request → response for every handler, with auth variations
    - DB writes → verify row state + constraints
    - DB reads → empty state, populated state, filters, pagination
    - Cascade / transaction behavior
    - Rate limits / timeouts if applicable

  E2E scenarios:
    - One journey per user-facing feature (from README or CLAUDE.md Architecture)
    - Happy path + one error path per journey
    - Responsive breakpoints for frontend pages (375, 768, 1440)
    - CLI: one invocation per public command + error (missing flag, bad argument)

# ── Phase 4: Fixture & Seed Strategy ──────────────────────────────

STEP 6: Specify fixture/seed per scenario

  Forge (SQLite + drizzle): temp-file DB via `fs.mkdtempSync()`; seed JSON loaded in beforeAll
  Platform backend (PostgreSQL + Prisma): separate test DB, truncate in afterEach
  Agent framework: temp directories, in-memory state, mock adapters
  Platform frontend: mock API via MSW (or fetch stub); render with TanStack providers

# ── Phase 5: Write Plan ───────────────────────────────────────────

STEP 7: Write <target>/TEST_PLAN.md

  OUTPUT <target>/TEST_PLAN.md
  ---
  last_updated: <YYYY-MM-DD>
  package: <target>
  category: <category>
  layers: [unit, integration, e2e]   # only included layers
  unit_count: N
  integration_count: N
  e2e_count: N
  existing_coverage: <N files / N scenarios>
  ---

  # Test Plan — <target>

  ## Coverage Summary

  | Layer | Scenarios | Already Covered | To Add |
  |---|---|---|---|
  | Unit | 42 | 18 | 24 |
  | Integration | 12 | 0 | 12 |
  | E2E | 5 | 0 | 5 |

  ## Unit Layer

  ### Module: `src/lib/skill-manifest.ts`
  - [ ] **parseManifest(yaml: string)** — parses SKILL.md frontmatter
    - Happy: valid frontmatter → typed result
    - Edge: missing optional fields → defaults applied
    - Error: invalid YAML → throws ParseError
  - [ ] **resolveSkillDependencies(manifest)** — resolves transitive deps
    - Happy: single dep
    - Edge: circular — throws
    - Edge: unknown dep — throws

  [repeat for each module]

  ## Integration Layer

  ### Handler: `server/api/sessions/start.post.ts`
  Fixture: temp SQLite DB, drizzle migrations applied, seed `empty`.
  - [ ] POST with valid body → 200, session row created in DB
  - [ ] POST with missing workspaceId → 400
  - [ ] POST without auth → 401
  - [ ] Concurrent POSTs from same user → second returns existing session id

  [repeat for each handler / service]

  ## E2E Layer

  ### Journey: Start a workspace session
  Route: /workspace/:id
  Seed: populated (`test/e2e/fixtures.ts` — default user + one workspace)
  - [ ] User opens /workspace/w1 → workspace loads, session starts
  - [ ] User sends chat message → bot reply appears, DB session row updated
  - [ ] User reloads page → session persists
  - [ ] 375px viewport — layout renders without overflow

  [repeat for each journey]

  ## Fixtures

  | Name | Used By | Seed |
  |---|---|---|
  | empty | unit + integration | no seed |
  | populated | integration + e2e | 1 user, 2 workspaces, 3 sessions |
  | edge_cases | integration | 1 workspace with 1000 sessions |

  ## Out of Scope

  - src/generated/**/* — schema-generated, not worth testing in isolation
  - Deprecated exports (see CLAUDE.md "Deprecated")

STEP 8: Backup previous plan
  IF <target>/TEST_PLAN.md existed → rename to TEST_PLAN.prev.md before overwriting

STEP 9: Report
  [test-plan] target=<pkg> category=<cat> → N unit + N integration + N e2e scenarios
  Existing: N covered | To add: N
  Plan written to <target>/TEST_PLAN.md
  Next: run `test-unit target=<pkg>` to generate unit tests from the plan.

EMIT [test-plan] completed target=<pkg> unit=<N> integration=<N> e2e=<N>

CHECKLIST
  - [ ] CLAUDE.md read before unit discovery
  - [ ] Package category classified
  - [ ] Existing coverage scanned and excluded
  - [ ] Every unit classified into exactly one layer
  - [ ] Every integration/e2e scenario has a fixture strategy
  - [ ] Prior TEST_PLAN.md backed up if it existed
  - [ ] Layers input respected (skipped layers have no section)

---

## Integration

- **Called by:** `test-unit`, `test-integration`, `test-e2e`, `ready` (as coverage evidence), `quality`
- **Reads:** `<target>/CLAUDE.md`, `<target>/src/**`, `references/test_stack_map.md`
- **Writes:** `<target>/TEST_PLAN.md`
