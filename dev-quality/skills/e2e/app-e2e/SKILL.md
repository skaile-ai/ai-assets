---
name: app-e2e
description: "End-to-end browser testing. Reads screen specs, features, and data model from _concept/. Uses agent-browser to test every user journey, take screenshots, and validate database records."
disable-model-invocation: true
keywords: testing, e2e, browser, screenshots, validation
metadata:
  stage: alpha
  requires:
  - quality-contract
---

ROLE  E2E Testing agent — runs browser tests against every user journey, captures screenshots, validates database state.

READS
  _concept/1_discovery/1_overview/brief.md          — app name, purpose
  _concept/2_experience/2_features/**/*.md          — feature specs, requirements, success criteria
  _concept/3_blueprint/3_datamodel/postxl-schema.json — model definitions for DB validation
  _concept/2_experience/3_screens/**/*.md           — screen specs with routes, components, states
  ? _concept/3_blueprint/3_datamodel/seed.json     — scenario-based test data (see shared/contracts/seed_data.md)

WRITES
  e2e-screenshots/**/*.png              — per-journey step screenshots
  ? e2e-test-report.md                  — optional full markdown report

REFERENCES
  shared/contracts/seed_data.md                  — seed scenario format and data quality rules
  shared/contracts/feedback_loop.md              — how to update feature status after testing
  references/report_template.md         — report format, seed usage, responsive breakpoints, DB validation

REQUIRES
  hard: agent-browser
  soft: docker (database validation deferred without it)
  state: _concept/2_experience/3_screens/**/*.md exist
  state: _concept/2_experience/2_features/**/*.md exist

MUST  run app-audit before this skill for static analysis
MUST  use seed.json scenario data for all form inputs (never invent test data)
MUST  screenshot every step to e2e-screenshots/<journey>/
MUST  validate database records after data-modifying interactions
MUST  update feature status to "tested" for every passing journey
NEVER  skip responsive testing on key pages
NEVER  leave dev server running after completion

EMIT  [app-e2e] started run_id=<uuid>

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
    - Read _concept/3_blueprint/3_datamodel/postxl-schema.json for models, relationships, field types
    - Cross-reference .env.example for connection details
    - Return: DB type/connection, model-to-table mapping, data flows per user action, validation queries

STEP 3: Start application
  - Install dependencies (pnpm install or equivalent)
  - Start dev server in background
  - Wait for server ready
  - $ agent-browser open <url>
  - Confirm page loads successfully
  - $ agent-browser screenshot e2e-screenshots/00-initial-load.png

STEP 4: Test user journeys
  - For each journey from Step 2 sub-agent 1:
    - Use agent-browser commands with seed data from _concept/3_blueprint/3_datamodel/seed.json
      (see references/report_template.md for scenario mapping)
    - Screenshot every interaction step to e2e-screenshots/<journey>/
    - Analyze screenshots with Read tool
    - $ agent-browser console
    - Check for JS errors in console output
    - After data-modifying interactions, query DB to verify records match postxl-schema.json
    IF issue found
      - Document the issue
      - Attempt fix in source code
      - Re-test and screenshot the fix
  UNTIL all journeys tested

STEP 5: Responsive testing
  - For each key page, test at three viewports: 375x812, 768x1024, 1440x900
  - Screenshot each viewport to e2e-screenshots/responsive/

STEP 6: Cleanup
  - Stop dev server
  - Close browser session

STEP 7: Update feature status (feedback loop)
  - For every successfully tested journey:
    - Find corresponding feature in _concept/2_experience/2_features/
    - Set status: tested in frontmatter

EMIT  [app-e2e] feedback_loop updated 2_experience/2_features/<group>/<feature>.md set status: tested

STEP 8: Report
  - Present summary (see references/report_template.md for format)
  - Optionally export to e2e-test-report.md

EMIT  [app-e2e] completed run_id=<uuid> journeys=N screenshots=N issues_found=N issues_fixed=N

CHECKLIST
  - [ ] Pre-flight checks passed (platform, frontend, agent-browser)
  - [ ] All user journeys tested with seed scenario data
  - [ ] Screenshots captured for every step
  - [ ] Database records validated after data-modifying actions
  - [ ] Responsive testing completed at all three breakpoints
  - [ ] Feature statuses updated to "tested" via feedback loop
  - [ ] Dev server stopped and browser session closed
  - [ ] Summary report presented to user
