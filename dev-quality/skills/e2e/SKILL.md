---
name: e2e
description: "End-to-end browser testing. Reads screen specs, features, and data model from _concept/. Uses agent-browser to test every user journey, take screenshots, and validate database records."
keywords: [testing, e2e, browser, screenshots, validation, journey, playwright, agent-browser]
source: MERGED
version: 1.0.0
disable-model-invocation: true
user_inputs:
  dialog: []
  files: []
---

# E2E — End-to-End Browser Testing

## Overview

The **e2e** skill runs end-to-end browser tests against a running application.
It reads screen specs, features, and data model from `_concept/`, uses the
**browser** skill to test every user journey, takes screenshots, and validates
database records.

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

**Hard gates:**
1. Source code must exist (`package.json`, `pyproject.toml`, or equivalent)
2. App must be runnable (dev server can be started)

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | `_concept/1_discovery/1_overview/brief.md` | Yes |
| **Must read** | `_concept/2_experience/2_features/**/*.md` | Yes |
| **Must read** | `_concept/3_blueprint/3_datamodel/model.json` | Yes |
| **Must read** | `_concept/3_blueprint/3_datamodel/seed.json` | Yes |
| **Must read** | `_concept/2_experience/3_screens/**/*.md` | Yes |
| **Must read** | `package.json` (or equivalent) | Yes |
| **Optional** | `.env.example` | No (auth and DB connection) |
| **Never load** | `_concept/_grounding/`, `_concept/1_discovery/2_brand/` | — |

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Testing without starting the app | Start the dev server first. E2E tests require a running app. |
| Using hardcoded test data | Use seed.json scenarios for all test data. |
| Not re-snapshotting after navigation | Always re-snapshot after page changes (element refs become stale). |
| Skipping responsive testing | Test at mobile, tablet, and desktop breakpoints. |
| Not cleaning up | Always stop the dev server and close browser sessions. |

---

ROLE  E2E Testing agent — runs browser tests against every user journey, captures screenshots, validates database state.

READS
  _concept/1_discovery/1_overview/brief.md          — app name, purpose
  _concept/2_experience/2_features/**/*.md          — feature specs, requirements, success criteria
  _concept/3_blueprint/3_datamodel/model.json       — model definitions for DB validation
  _concept/2_experience/3_screens/**/*.md           — screen specs with routes, components, states
  ? _concept/3_blueprint/3_datamodel/seed.json      — scenario-based test data

WRITES
  e2e-screenshots/**/*.png              — per-journey step screenshots
  ? e2e-test-report.md                  — optional full markdown report

REFERENCES
  dev-shared/contracts/seed_data.md              — seed scenario format and data quality rules
  dev-shared/contracts/feedback_loop.md          — how to update feature last_updated after testing
  references/report_template.md                  — report format, seed usage, responsive breakpoints, DB validation

REQUIRES
  hard: agent-browser
  soft: docker (database validation deferred without it)
  state: _concept/2_experience/3_screens/**/*.md exist
  state: _concept/2_experience/2_features/**/*.md exist

MUST  run audit before this skill for static analysis
MUST  use seed.json scenario data for all form inputs (never invent test data)
MUST  screenshot every step to e2e-screenshots/<journey>/
MUST  validate database records after data-modifying interactions
MUST  update last_updated on feature files for every passing journey
NEVER  skip responsive testing on key pages
NEVER  leave dev server running after completion

EMIT  [e2e] started run_id=<uuid>

STEP 1: Pre-flight checks
  - $ uname -s
  IF output is not "Linux" or "Darwin"
    - Stop: "agent-browser only supports Linux, WSL, and macOS."
  - Verify app has a browser-accessible frontend (package.json with dev script, pages/, or index.html)
  IF no frontend found
    - Stop: "No browser-accessible frontend detected."
  - $ agent-browser --version || (npm install -g agent-browser && agent-browser install --with-deps)

STEP 2: Parallel research (two sub-agents)
  - Sub-agent 1 — Concept & User Journeys:
    - Read _concept/1_discovery/1_overview/brief.md for app name, purpose
    - Read _concept/2_experience/2_features/**/*.md for every feature, requirements, success criteria
    - Read _concept/2_experience/3_screens/**/*.md for every screen: route, components, template data, states
    - Read package.json for dev server command, port, URL
    - Read .env.example or feature docs for auth info
    - Synthesize: startup guide (exact commands) + user journey list (steps, interactions, expected outcomes)
  - Sub-agent 2 — Database Schema & Data Flows:
    - Read _concept/3_blueprint/3_datamodel/model.json for models, relationships, field types
    - Cross-reference .env.example for connection details
    - Return: DB type/connection, model-to-table mapping, data flows per user action, validation queries

STEP 3: Start application
  - Install dependencies
  - Start dev server in background
  - Wait for server ready
  - $ agent-browser open <url>
  - Confirm page loads successfully
  - $ agent-browser screenshot e2e-screenshots/00-initial-load.png

EMIT  [e2e] checkpoint phase=app_started url=<url>

STEP 4: Test user journeys
  - For each journey from Step 2 sub-agent 1:
    - Use agent-browser commands with seed data from _concept/3_blueprint/3_datamodel/seed.json
      (see references/report_template.md for scenario mapping)
    - Screenshot every interaction step to e2e-screenshots/<journey>/
    - Analyze screenshots with Read tool
    - $ agent-browser console
    - Check for JS errors in console output
    - After data-modifying interactions, query DB to verify records match model.json definitions
    IF issue found
      - Document the issue
      - Attempt fix in source code
      - Re-test and screenshot the fix
  UNTIL all journeys tested

EMIT  [e2e] checkpoint phase=journeys_tested tested=<N> passed=<N> failed=<N>

STEP 5: Responsive testing
  - For each key page, test at three viewports: 375x812, 768x1024, 1440x900
  - Screenshot each viewport to e2e-screenshots/responsive/

STEP 6: Cleanup
  - Stop dev server
  - Close browser session

STEP 7: Update feature tracking (feedback loop)
  - For every successfully tested journey:
    - Find corresponding feature in _concept/2_experience/2_features/
    - Update last_updated in frontmatter to today's date

EMIT  [e2e] feedback_loop updated 2_experience/2_features/<group>/<feature>.md updated last_updated

STEP 8: Report
  - Present summary (see references/report_template.md for format)
  - Optionally export to e2e-test-report.md

EMIT  [e2e] completed run_id=<uuid> journeys=<N> screenshots=<N> issues_found=<N> issues_fixed=<N>

CHECKLIST
  - [ ] Pre-flight checks passed (platform, frontend, agent-browser)
  - [ ] All user journeys tested with seed scenario data
  - [ ] Screenshots captured for every step
  - [ ] Database records validated after data-modifying actions
  - [ ] Responsive testing completed at all three breakpoints
  - [ ] Feature last_updated updated via feedback loop for passing journeys
  - [ ] Dev server stopped and browser session closed
  - [ ] Summary report presented to user
