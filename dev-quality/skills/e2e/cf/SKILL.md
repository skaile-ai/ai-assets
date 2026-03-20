---
name: e2e
description: "Use when the app is running and user wants end-to-end browser testing. Also when user says 'test the app', 'E2E tests', 'browser testing', 'test the user journey'."
disable-model-invocation: true
keywords: [testing, e2e, browser, screenshots, validation, journey, playwright]
user_inputs:
  dialog: []
  files: []
---

# Test E2E — End-to-End Browser Testing

## Overview

The **cf_test_e2e** skill runs end-to-end browser tests against a running application.
It reads screen specs, features, and data model from `_concept/`, uses the
**browser** skill to test every user journey, takes screenshots, and validates
database records.

**Phase:** testing / e2e
**Writes to:** `e2e-screenshots/`, test report, feature status updates

> **Tip:** Run **audit** before this skill for static analysis, and **ready**
> to verify all features have complete specs.

## When to Use

- The app is running and the user wants end-to-end browser testing
- The user says "test the app", "E2E tests", "browser testing", "test the user journey"
- After implementation to verify features work as specified
- The orchestrator dispatches this after implementation is complete

## When NOT to Use

- The app is not running or cannot be started — fix that first
- No source code exists — nothing to test
- You want static code analysis only — use **audit** instead
- You want to check concept readiness — use **ready** instead

## Prerequisites

### HARD-GATE

Both must be satisfied:
1. Source code must exist (package.json, nuxt.config.ts, or equivalent)
2. App must be runnable (dev server can be started)

If not:

> "Cannot run E2E tests: [source code not found / app cannot start]."

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** Source code must exist, app must be runnable
**If gates fail:** Run implementation skills first (`cf_implement`, `cf_implement_feature`)
**On completion:** Present summary, then suggest next steps.

### Shared Contracts

Before starting, read:
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

This skill reads from the _concept/ pipeline:
- `_concept/01_project/brief.md` — app overview
- `_concept/03_features/**/*.md` — feature specs and requirements
- `_concept/06_datamodel/model.json` — entity definitions for DB validation
- `_concept/06_datamodel/seed.json` — scenario-based template data (see `cf__shared/seed_data.md`)
- `_concept/07_screens/**/*.md` — screen specs with routes, components, states

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | `_concept/01_project/brief.md` | Yes |
| **Must read** | `_concept/03_features/**/*.md` | Yes |
| **Must read** | `_concept/06_datamodel/model.json` | Yes |
| **Must read** | `_concept/06_datamodel/seed.json` | Yes |
| **Must read** | `_concept/07_screens/**/*.md` | Yes |
| **Must read** | `package.json` (or equivalent) | Yes |
| **Optional** | `.env.example` | No (for auth and DB connection) |
| **Never load** | `_concept/_research/`, `_concept/04_brand/` | — |

## Workflow

### Pre-flight Check

#### 1. Platform Check

```bash
uname -s
```
- `Linux` or `Darwin`: proceed
- Anything else: stop: "The **browser** skill only supports Linux, WSL, and macOS."

#### 2. Frontend Check

Verify the app has a browser-accessible frontend (package.json with dev script,
pages/, index.html, etc.). Stop if no frontend found.

#### 3. Browser Installation

```bash
agent-browser --version || (npm install -g agent-browser && agent-browser install --with-deps)
```

### Phase 1: Parallel Research

Launch two sub-agents simultaneously:

#### Sub-agent 1: Concept and User Journeys

Read `_concept/` pipeline:
1. `_concept/01_project/brief.md` — app name, purpose
2. `_concept/03_features/**/*.md` — every feature, requirements, success criteria
3. `_concept/07_screens/**/*.md` — every screen spec: route, components, template data, states
4. `package.json` — dev server command, port, URL
5. Auth info from `.env.example` or feature docs

Synthesize into:
- Startup guide (exact commands)
- User journey list with steps, interactions, expected outcomes

#### Sub-agent 2: Database Schema and Data Flows

Read `_concept/06_datamodel/model.json` for entities, relationships, and field types.
Cross-reference with `.env.example` for connection details.

Return:
- Database type and connection
- Entity to table mapping
- Data flows per user action
- Validation queries per flow

### Phase 2: Start Application

1. Install dependencies
2. Start dev server in background
3. Wait for ready
4. `agent-browser open <url>` and confirm
5. Screenshot: `e2e-screenshots/00-initial-load.png`

### Phase 3: Test User Journeys

For each journey from Sub-agent 1:

#### Browser Testing
Use agent-browser commands. Use **scenario-based data from `_concept/06_datamodel/seed.json`**:
- `populated` scenario for core journey form inputs
- `empty` scenario for first-use / onboarding flow tests
- `edge_cases` scenario for validation and layout stress tests
- `permissions` scenario for role-based access tests (if present)

Screenshot every step to `e2e-screenshots/<journey>/`.
Analyze screenshots with Read tool. Check `agent-browser console` for errors.

#### Database Validation
After data-modifying interactions, query the database to verify records match
the expected state from `model.json` entity definitions.

#### Issue Handling
Document, fix, re-test, screenshot the fix.

#### Responsive Testing
Test key pages at 375x812 (mobile), 768x1024 (tablet), 1440x900 (desktop).

### Phase 4: Cleanup

Stop dev server, close browser session.

### Phase 5: Report

#### Update Feature Status (Feedback Loop)

For every successfully tested journey, find the corresponding feature in
`_concept/03_features/` and set `impl_status: tested` in frontmatter.

```
[cf_test_e2e] feedback_loop updated 03_features/01_user_auth/login.md
  set impl_status: tested
```

#### Summary

```
## E2E Testing Complete

Journeys Tested: N
Screenshots Captured: N
Issues Found: N (N fixed, N remaining)

### Issues Fixed
- [Description] — [file:line]

### Remaining Issues
- [Description] — [severity] — [file:line]

Screenshots: e2e-screenshots/
```

### Emit Events

```
[cf_test_e2e] started
  run_id: <uuid>

[cf_test_e2e] checkpoint phase=app_started
  url: http://localhost:3000

[cf_test_e2e] checkpoint phase=journeys_tested
  tested: N
  passed: N
  failed: N

[cf_test_e2e] completed
  run_id: <uuid>
  journeys: N
  screenshots: N
  issues_found: N
  issues_fixed: N
```

## Outputs

| Output | Description |
|--------|-------------|
| `e2e-screenshots/` | Screenshots of every test step |
| Test report | Summary of journeys tested, issues found/fixed |
| `e2e-test-report.md` (optional) | Saved report if user requests |
| Feature impl_status updates | `impl_status: tested` in feature frontmatter |

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

> "E2E testing complete. N of N journeys passed. Screenshots saved to `e2e-screenshots/`.
> Next: run `cf_quality_verify` to verify full implementation against the concept."

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Testing without starting the app | The agent tries to test statically | Start the dev server first. E2E tests require a running app. |
| Using hardcoded test data | The agent invents form values | Use seed.json scenarios for all test data. |
| Not re-snapshotting after navigation | The agent reuses stale element refs | Always re-snapshot after page changes (see browser skill ref lifecycle). |
| Skipping responsive testing | The agent only tests desktop | Test at mobile, tablet, and desktop breakpoints. |
| Not cleaning up | The agent leaves the dev server running | Always stop the dev server and close browser sessions. |

## Integration

- **Called by:** orchestrator or standalone
- **Reads from:** `_concept/` (features, screens, data model, seed data), source code
- **Uses:** **browser** skill for all browser automation
- **Feeds into:** feature impl_status updates (`impl_status: tested`), bug fixes
- **Feedback loops:** Updates feature frontmatter `impl_status` to `tested` for passing journeys
