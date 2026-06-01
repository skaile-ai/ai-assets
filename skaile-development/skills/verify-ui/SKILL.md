---
name: 'verify-ui'
description: 'Visual verification of Skaile platform UI changes using browser automation.
  After implementing a UI feature, run this skill to systematically check that pages
  render correctly, navigation works, and key interactive elements are present. Uses
  agent-browser CLI and chrome-devtools MCP tools.'
metadata:
  tags:
  - 'verification'
  - 'ui'
  - 'browser'
  - 'visual'
  - 'skaile-development'
  source: 'MERGED'
  stage: 'beta'
  prerequisites:
    files:
    - path: 'platform/frontend/package.json'
      gate: hard
      description: 'Platform frontend must exist'
  user_inputs:
    dialog:
    - id: 'scope'
      label: 'What to verify'
      type: 'select'
      options:
      - 'smoke'
      - 'all'
      - 'dashboard'
      - 'project-creation'
      - 'workspace'
      - 'settings'
      - 'admin'
      - 'flow-execution'
      required: true
      default: 'smoke'
      hint: "'smoke' = quick pass through main pages; 'all' = comprehensive check
        of every area; 'flow-execution' = end-to-end test-echo flow run (creates a
        real session, sends real LLM calls — opt-in only)"
    - id: 'base_url'
      label: 'Frontend URL'
      type: 'text'
      required: false
      default: 'http://localhost:3000'
    - id: 'tool_preference'
      label: 'Browser tool'
      type: 'select'
      options:
      - 'auto'
      - 'agent-browser'
      - 'chrome-devtools'
      required: false
      default: 'auto'
      hint: "'auto' tries chrome-devtools MCP first, falls back to agent-browser CLI"
    files: []
---

# Verify UI - Platform Visual Verification

## Overview

Systematically verify that Skaile platform pages render correctly after code changes. Uses browser automation to navigate pages, check that key elements exist, and report results.

Run this after implementing UI changes to confirm your work before committing.

## Tool Selection

IF tool_preference = "auto"
TRY chrome-devtools MCP tools first (they connect to an already-running Chrome - lower overhead)
IF chrome-devtools unavailable: fall back to agent-browser CLI
IF tool_preference = "chrome-devtools"
USE chrome-devtools MCP tools exclusively
IF tool_preference = "agent-browser"
USE agent-browser CLI exclusively

See `references/common-patterns.md` for the command mapping between both tools.

## Auth Detection

STEP 1: Navigate to {base_url}/dashboard
STEP 2: Check the current URL

- IF URL still contains /dashboard -> noAuth mode, proceed normally
- IF URL redirected to /login or external Keycloak domain -> auth mode active
  STEP 3: IF auth mode active
- Check if agent-browser auth vault has a "skaile" profile: `agent-browser auth list`
- IF profile exists: `agent-browser auth login skaile`
- IF no profile: WARN user "Auth is enabled. Either restart frontend with VITE_AUTH=false or configure auth: agent-browser auth save skaile --url {base_url}/login --username <user> --password-stdin"
- STOP verification if unable to authenticate

## Verification Workflow

EMIT [verify-ui] started scope={scope} url={base_url}

STEP 1: Confirm frontend is reachable

- Navigate to {base_url}
- IF page fails to load: STOP with error "Frontend not reachable at {base_url}. Is the dev server running?"
- Wait for networkidle

STEP 2: Detect auth mode (see above)

STEP 3: Execute verification based on scope
IF scope = "smoke" - Run abbreviated version of each area (load page, check it renders, move on) - Read references/dashboard-and-navigation.md -> run SMOKE section only - Read references/project-creation.md -> run SMOKE section only - Read references/settings.md -> run SMOKE section only. Do NOT include flow-execution in smoke.
IF scope = "all" - Run every reference file in order: dashboard, project-creation, workspace, settings, admin. Do NOT include flow-execution unless explicitly requested.
IF scope = "flow-execution" - Read references/flow-execution.md and run the FULL Verification (full-run variant) end-to-end. This is the only scope that breaks the read-only rule and sends real prompts to the agent backend.
IF scope = specific area - Read references/{scope}.md and execute all checks

STEP 4: Report results

```
## UI Verification Report - {date}

| Area | Status | Checks | Notes |
|------|--------|--------|-------|
| Dashboard | PASS | 5/5 | |
| Navigation | PASS | 12/12 | |
| Project Creation | WARN | 3/4 | SharePoint form not tested (no provider configured) |
| Settings | PASS | 7/7 | |

### Issues Found
- [FAIL] /settings/ai-providers: "Add Provider" button not found in snapshot
- [WARN] /projects/{id}/main: No projects exist to test workspace view
```

EMIT [verify-ui] complete status={overall_status} checks={passed}/{total}

STEP 5: Close browser session

- `agent-browser close` or equivalent cleanup

## Rules

MUST re-snapshot after every navigation (refs are invalidated on page change)
MUST use semantic discovery via `snapshot -i` -- never assume specific refs like @e1
MUST close browser session when done
MUST continue verification if one page errors -- report it and move to next area
MUST wait for networkidle after navigation before snapshotting
NEVER hardcode CSS selectors -- discover elements via snapshot text/labels
NEVER fail the entire verification because one check failed
NEVER interact with destructive actions (delete buttons, drop operations) during verification
NEVER send real messages to the agent backend -- only verify UI structure exists. EXCEPTION: scope='flow-execution' is allowed to drive a real test flow end-to-end, including creating a project and sending input/approval to the runner. This is the only scope that breaks this rule.

## Scope Reference

| Scope            | What It Checks                                                 | Approx Time |
| ---------------- | -------------------------------------------------------------- | ----------- |
| smoke            | Dashboard loads, nav works, /projects loads, one settings page | ~30s        |
| dashboard        | Dashboard content, sidebar nav, all nav items route correctly  | ~1min       |
| project-creation | /projects/new wizard, all 4 source type forms, validation      | ~1min       |
| workspace        | Session view, chat panel, pipeline nav, resource browser       | ~1min       |
| settings         | All 7 settings pages load with correct forms/tables            | ~1min       |
| admin            | Session manager, data management, admin-only visibility        | ~30s        |
| all              | Everything above (excludes flow-execution)                     | ~4min       |
| flow-execution   | End-to-end test-echo flow run via the platform UI — creates a real session, drives input + approvals, verifies completion + chat breadcrumbs. **Sends real LLM prompts. Burns tokens. Opt-in only.** | ~3-5min     |
