---
name: "implement-feature-page"
description: "Page-level feature implementation with TDD Guard. Implements all features within one page using outside-in TDD: writes failing page tests, then for each feature writes failing feature tests and implements until green. Uses storybook page compositions as UI starting point if available. TDD Guard enforces red-green discipline at the feature level."
metadata:
  version: "1.0.0"
  tags:
    - "implement"
    - "feature"
    - "page"
    - "tdd"
    - "tdd-guard"
    - "test-first"
    - "e2e"
    - "component"
  source: "MERGED"
  prerequisites:
    files:
      - path: "_concept/2_experience/3_screens"
        gate: hard
        description: "Screen spec for this page (passed by implement-feature orchestrator)"
        min_entries: 1
      - path: "_concept/2_experience/2_features"
        gate: hard
        description: "Feature specs for this page's features"
        min_entries: 1
      - path: "package.json"
        gate: hard
        description: "Dev stack must be running with passing test baseline"
    reads:
      - path: "_concept/2_experience/4_storybook/src/pages"
        description: "Storybook page compositions as UI starting point"
      - path: "_concept/3_blueprint/3_datamodel/model.json"
        description: "Data model for entity implementation"
      - path: "_concept/3_blueprint/3_datamodel/seed.json"
        description: "Seed data for realistic test fixtures"
      - path: "_concept/1_discovery/2_brand/tokens.json"
        description: "Brand tokens for styled component implementation"
      - path: "_concept/3_blueprint/1_techstack/stack.md"
        description: "Tech stack for framework-specific implementation patterns"
---

# Implement Feature Page — Page-Level Implementation with TDD Guard

## Overview

Implements all features within one page using TDD Guard-enforced Red→Green
cycles. Receives a screen spec and associated feature specs from the
`implement-feature` orchestrator.

**Three-level flow:**
1. Write failing page-level E2E tests (page renders, core layout, states)
2. For each feature: TDD RED (failing feature tests) → TDD GREEN (implement)
3. Verify: fix until page tests pass + all prior tests still pass

**TDD Guard** blocks file edits that violate the Red→Green cycle. Declare Red
before writing tests, Green before writing prod code. See `references/tdd_guard.md`.

## When to Use

Called by `implement-feature` orchestrator — not invoked directly by users.

## Prerequisites

**Hard gates:**
- Screen spec exists for the target page
- At least one feature spec references this page
- Dev stack running (frontend + backend accessible)
- Passing test baseline (no existing failures)

---

ROLE  Page implementer — implements all features on one page using TDD Guard-enforced Red→Green cycles.

READS
  _concept/2_experience/3_screens/<group>/<screen>.md         — page spec: layout, states, routes, data
  _concept/2_experience/2_features/<group>/<feature>.md       — feature specs for features on this page
  ? _concept/2_experience/4_storybook/src/pages/              — storybook page composition (UI reference)
  _concept/3_blueprint/3_datamodel/model.json                 — data model (relevant entities)
  _concept/3_blueprint/3_datamodel/seed.json                  — seed data scenarios
  _concept/1_discovery/2_brand/tokens.json                    — brand tokens (never invent colors)
  _concept/3_blueprint/1_techstack/stack.md                   — tech stack profile to load

WRITES
  e2e/specs/pages/<group>/<page-slug>.spec.<ext>              — page-level e2e tests
  e2e/specs/features/<group>/<feature-slug>.spec.<ext>        — feature-level e2e tests
  <stack-specific page files>                                  — page + feature UI components
  <stack-specific backend files>                               — custom backend logic (if needed)

REFERENCES
  skaileup-shared/contracts/concept_structure.md  — canonical _concept/ paths
  skaileup-standards/profiles/<tech_stack_skill>/SKILL.md — stack-specific patterns
  references/tdd_guard.md           — TDD Guard state machine and CLI
  references/tdd_workflow.md        — E2E patterns, seed data, pitfalls

MUST  write failing page tests BEFORE implementing any features
MUST  write failing feature tests BEFORE implementing each feature (TDD Red→Green)
MUST  use storybook page compositions as UI starting point (when available)
MUST  add data-testid attributes to all interactive and assertable elements
MUST  use test IDs and value assertions (not screenshots)
MUST  run ALL tests after each feature to catch regressions
MUST  consume brand tokens from tokens.json — never invent visual decisions
MUST  use the component library from the tech stack profile for UI primitives
NEVER implement before tests exist
NEVER use screenshot assertions
NEVER skip regression testing after a feature
NEVER hardcode colors, fonts, or spacing
NEVER modify _concept/ files

# ── Step 0: Preflight ─────────────────────────────────────────────

STEP 0: Preflight
  - Verify backend is accessible (health check)
  - Verify frontend is accessible (health check)
  IF either not running → STOP
  - Run existing E2E tests to establish clean baseline
  IF any fail → STOP and fix before starting
  - Record baseline test count

# ── Step 1: Write page tests ──────────────────────────────────────

STEP 1: Write page-level E2E tests
  - Read screen spec: layout, states, route, data requirements
  - Read storybook page composition (if exists): component structure reference
  - Create e2e/specs/pages/<group>/<page-slug>.spec.<ext>
  - Use isolated backend / test fixture (reset to populated scenario by default)
  - Tests cover:
    - Page loads and renders core layout (from screen spec)
    - Each screen state (default, empty, loading, error)
    - Navigation to/from this page
    - Data displays correctly from seed data
  - Use data-testid for element addressing
  - All page tests MUST FAIL at this point
  $ git commit -m "test: write page e2e tests for <page>"

PATTERNS (adapt to your stack's test runner):
  describe('<Page Name>', () => {
    beforeAll(async () => { /* reset to populated scenario */ })

    test('renders with populated data', async () => {
      // navigate to route
      // expect page-<slug> to be visible
      // assert core layout elements from screen spec
    })

    test('shows empty state', async () => {
      // reset to empty scenario
      // navigate to route
      // expect empty-state element visible
    })
  })

# ── Step 2-4: Feature TDD loop ────────────────────────────────────

STEP 2: Feature TDD cycle
  FOR EACH feature on this page (from feature specs where screens[] includes this page):

STEP 2a: TDD RED — write feature tests
  - Read feature spec: requirements, success criteria, error states
  - Create e2e/specs/features/<group>/<feature-slug>.spec.<ext>
  - Tests cover:
    - Happy path (primary success scenario)
    - Key error states from feature spec
    - Guard conditions / validation rules
  - All feature tests MUST FAIL
  $ git commit -m "test: write feature e2e tests for <feature>"

  [TDD Guard]: declare Red before writing tests

STEP 2b: TDD GREEN — implement feature
  - Load expert skills matching the tech stack:
    - Search dev-implementation-experts-* for stack-specific patterns
    - Follow their guidance for file structure, idiomatic code, component usage
  - Use storybook page composition as UI structure reference
  - Implementation order:
    1. Add route (if new page, register in router)
    2. Build page component (copy structure from storybook composition, wire to real data)
    3. Add data-testid to all interactive/assertable elements
    4. Wire data fetching using the stack's data layer (generated client, ORM, etc.)
    5. Implement forms and interactions
    6. Implement state management
  - For custom backend logic beyond standard CRUD:
    - Use the stack-appropriate mechanism (custom service, action handler, API endpoint)
    - Use in-memory/mock implementations from the infrastructure phase for external services
  - Apply brand tokens via CSS custom properties (never hardcode values)
  $ <run feature tests>
    UNTIL feature tests pass
  - Run ALL tests to catch regressions
  $ git commit -m "feat: implement <feature>"

  [TDD Guard]: declare Green before writing prod code

STEP 2c: Repeat
  UNTIL all features on this page are implemented

# ── Step 3: Verify page ───────────────────────────────────────────

STEP 3: Fix until page tests pass
  - Run page tests specifically
  - Fix layout, data flow, or state management issues
  - Re-run ALL tests to ensure no regressions
    UNTIL page tests pass AND all prior tests still pass
    $ git commit -m "feat: complete page <page-name>"

STEP 4: Verify build
  - Run build, lint, and type checks for the full project
    UNTIL all pass

STEP 5: Report completion
  EMIT [implement-feature-page] completed page=<page> features=<N> tests=<N>

CHECKLIST
  - [ ] Page e2e tests written and initially failing
  - [ ] All features on page have e2e tests
  - [ ] All feature tests pass
  - [ ] Page tests pass
  - [ ] All prior tests still pass (no regressions)
  - [ ] Build + lint + type checks pass
  - [ ] Custom backend logic implemented (if needed)
  - [ ] data-testid on all interactive elements
  - [ ] Brand tokens used throughout (no hardcoded values)

---

## Seed Data for Tests

By default, tests use the `populated` scenario.
When a test needs different data: add a custom scenario to the seed file
and reset in the test's `beforeAll`.

## Expert Skills

Before implementing, search for expert skills matching the tech stack:
- `dev-implementation-experts-js/` for JavaScript/TypeScript stacks
- `dev-implementation-experts-python/` for Python stacks
- Load relevant experts for framework-specific patterns, recipes, and component usage

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Writing code before tests | Always declare Red and write failing tests first |
| Hardcoded colors in components | Consume brand tokens via CSS custom properties |
| Screenshot assertions in tests | Use data-testid + value assertions only |
| Not running all tests after a feature | Always run full suite to catch regressions |
| Inventing UI component APIs | Load expert skills for idiomatic component usage |
