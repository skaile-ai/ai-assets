# Flow Execution Verification

## What This Tests

Drives the `test-echo` deterministic test flow through the platform UI to verify
the entire flow execution stack end-to-end:

- `workspace.startFlow` tRPC route + ephemeral skill deployment
- `FlowOrchestrator` turn-based prompt assembly
- `FlowAdapter` connector registration inside the agent runtime
- `state_changed(store='flow:*')` events flowing back to the platform
- `FlowExecutionSyncService` write-through to the in-memory mirror
- `useFlowExecution` hook bootstrap + subscription
- `FlowExecutionPanel` rendering with input gate, approval gate, and skip path
- Chat breadcrumb cards rendering for flow lifecycle events

## When To Run

This is the ONE area where verify-ui breaks the "never send real messages to
the agent backend" rule. Running this test:

- Creates a real project + session container (Docker)
- Sends real prompts to the LLM backend (uses `ANTHROPIC_API_KEY` from the
  backend env)
- Burns Claude tokens (~3-5 turns at small node payloads)
- Takes 2-5 minutes wall clock

Only run when explicitly requested via `scope=flow-execution` or `scope=all`.
Never include in `smoke`.

## Prerequisites

1. Docker daemon running and `skaile-vm-agent:full` image built. If missing:
   - SKIP with "Docker image skaile-vm-agent:full not built — run platform/docker/agent/build.sh full"
2. `ANTHROPIC_API_KEY` set in `platform/backend/apps/api/.env`. If missing:
   - SKIP with "ANTHROPIC_API_KEY not set — flow execution would error before completing"
3. `testing` catalog package is auto-loaded by `CatalogAggregatorService` from
   `ai-assets/catalogs/dist/testing.catalog.json`. If the file is missing:
   - SKIP with "Testing catalog not built — run bun ai-assets/catalogs/build.ts"
4. Backend started with `bun run dev` (stateless mode) and reachable at
   `{base_url.replace(':3000', ':3001')}/health`.
5. Frontend reachable at `{base_url}`.

## Test Variants

- `full-run` — happy path; approve every node, verify final state
- `optional-skip` — same but use Request Changes on the optional `count-artifacts`
  node to exercise the optional-failure-completion path

Default to `full-run`. Run `optional-skip` only when explicitly requested.

## FULL Verification (full-run variant)

### Phase 1 — Create a fresh project with the flow pre-attached

A fresh project guarantees no prior flow state. Attaching the flow during
project setup means it lands in `skaile.yaml` from day one and the
deployFlowSkillsEphemeral path is exercised end-to-end.

1. Navigate to `{base_url}/projects/new`
2. `take_snapshot`
3. Click the "Empty Project" radio
4. Wait for the Project Name textbox to update its placeholder to "e.g. My
   Scratchpad"
5. Fill the Project Name textbox with `flow-verify-{timestamp}` (e.g.
   `flow-verify-abc123` — must be unique per run; use a short random suffix)
6. Verify the Slug appears below the textbox
7. Click the "Add from catalog" button under "Skills & Flows"
8. `take_snapshot`
9. Find the catalog entry "Test Echo Flow" (it should appear in the
   "Flow Execution Tests" category or in the flat list when filtered to
   Flows)
10. Click "Add" on its row
11. Cancel out of the catalog browser if needed (or it auto-closes)
12. Verify the project-setup form now lists "Test Echo Flow" as a selected
    asset
13. Click "Create Project"
14. `wait_for` text `["workspace", "Workspace", "Type a message"]` with 90s
    timeout (deployFlowSkillsEphemeral resolves and deploys all 3 test
    skills before the runner gets start_flow, so the project-setup pass
    can be a few seconds slower than a no-asset project)
15. Verify the URL contains `/projects/{slug}/main/workspace`
16. IF the URL does not navigate within 90s OR the request returns an error:
    - Capture network requests and console errors
    - FAIL: "setupProject did not complete cleanly — see logs"

**Alternate path (skip during smoke):** create the project with no assets,
then in the workspace open the right sidebar Assets tab, click "Browse
catalog", find Test Echo Flow, click Add. This exercises the mid-session
`aiResources` configure path instead of the project-setup path. Useful as
a separate check, not as the default.

### Phase 2 — Confirm `test-echo` is wired into the session

1. In the workspace, open the right sidebar (click the toggle if collapsed)
2. Click the "Assets" tab on the right
3. Verify "Test Echo Flow" appears with a "Run" button and a "Remove" button
4. IF the entry is missing:
   - FAIL: "Test Echo Flow was not deployed during project setup — check
     deployFlowSkillsEphemeral and the project-setup → skaile.yaml write"

### Phase 3 — Start the flow

Click the "Run" button next to "Test Echo Flow" in the right-sidebar Assets
tab.

`wait_for` text `["Flow started", "test-echo", "ask-name", "Running"]` with
30s timeout.

Then:

1. Verify the right sidebar tab auto-switches to a new "Flow" / "Flow
   Execution" tab (or an equivalent tab that renders the FlowExecutionPanel)
2. `take_snapshot`
3. Verify the panel header shows `test-echo` and `running` status
4. Verify the panel renders 3 node cards: ask-name, write-greeting,
   count-artifacts
5. Verify node `ask-name` shows status `Running` or `Needs input`
6. Verify the chat stream shows a `Flow started Test Echo Flow` breadcrumb
7. **Critical regression check:** verify the chat stream does NOT contain the
   text "flow connector isn't registered" — that's the smoke signal for the
   FlowAdapter-not-in-ConnectorManager bug fixed in the runner.

**Slash command path (alternative entry point — opt-in only):** typing
`/test-echo` in the chat input does NOT currently route to startFlow on the
frontend; it sends the literal text to the agent which replies "Unknown
skill: test-echo". The slash-command interception for flow IDs is a
documented frontend gap (filed as an issue) — do not test this path until
that gap is closed.

### Phase 4 — Provide input for `ask-name`

1. `wait_for` text `["What name should we greet?"]` with 60s timeout
   - This is the input prompt from the test-ask-name skill
2. IF the wait times out, FAIL: "ask-name node did not request input within
   60s — likely indicates the agent is not seeing the flow connector tools"
3. `take_snapshot`
4. Find the text input widget in the ask-name node card
5. Fill it with `verify-test`
6. Click the Submit button on the input widget
7. `wait_for` text `["awaiting approval", "Captured name", "Approve"]` with 60s timeout
8. `take_snapshot`
9. Verify the ask-name node card shows status `awaiting_approval`
10. Verify the approval summary contains `verify-test` (the captured name)
11. Verify an artifact link `workspace://test/name.txt` is visible

### Phase 5 — Approve `ask-name`

1. Click the "Approve" button on the ask-name node card
2. `wait_for` text `["complete", "write-greeting"]` with 30s timeout
3. `take_snapshot`
4. Verify ask-name shows status `complete`
5. Verify write-greeting shows status `running` or `awaiting_approval`

### Phase 6 — Approve `write-greeting`

1. `wait_for` text `["greeting.txt", "Hello, verify-test"]` with 60s timeout
2. `take_snapshot`
3. Verify write-greeting shows status `awaiting_approval`
4. Verify approval summary mentions `greeting`
5. Verify artifact link `workspace://test/greeting.txt` is visible
6. Click "Approve" on write-greeting
7. `wait_for` text `["count-artifacts"]` with 30s timeout

### Phase 7 — Approve `count-artifacts`

1. `wait_for` text `["count.txt", "Counted"]` with 60s timeout
2. `take_snapshot`
3. Verify count-artifacts shows status `awaiting_approval`
4. Verify approval summary mentions a count
5. Click "Approve"
6. `wait_for` text `["complete", "Flow finished"]` with 30s timeout

### Phase 8 — Verify completion and breadcrumbs

1. Verify all three nodes show status `complete`
2. Verify the flow panel shows a "complete" status header (or auto-closes
   shortly after)
3. Switch to the chat panel
4. `take_snapshot`
5. Verify chat breadcrumbs appear in order:
   - `flow_started`
   - `input_pending` (or `approval_pending` for ask-name)
   - `approval_pending` for write-greeting
   - `approval_pending` for count-artifacts
   - `flow_finished`
6. Check the workspace file tree (if visible) for:
   - `workspace/test/name.txt` containing exactly `verify-test`
   - `workspace/test/greeting.txt` containing exactly `Hello, verify-test!`
   - `workspace/test/count.txt` containing a single integer

### Phase 9 — Cleanup

1. Hibernate or close the test session if the test harness has a way to do so
   (the chrome-devtools test harness does not — leave the session running and
   note the project name in the test report)
2. Report which project was created so the user can clean it up manually

## Optional-skip variant (delta from full-run)

In Phase 7, instead of clicking Approve on count-artifacts:

1. Click "Request Changes"
2. Type "skip this" in the feedback textarea
3. Click Submit
4. The agent will receive the rejection on an optional node and is expected
   to call `fail_node` or `skip_node`
5. `wait_for` text `["skipped", "complete", "failed"]` with 60s timeout
6. Verify count-artifacts ends in status `skipped` OR `failed` (both are
   acceptable for an optional node — the flow should still complete)
7. Verify the flow as a whole reaches status `complete`
8. Verify `workspace/test/count.txt` does NOT exist

## Common failure modes

| Symptom | Likely cause |
|---------|--------------|
| Phase 1 hangs >60s on Create Project | tRPC mutation pending — check backend logs for Prisma timeouts or missing env vars |
| Phase 2 cannot find test-echo | Testing catalog not built or not loaded by CatalogAggregatorService |
| Phase 4 times out waiting for input prompt | Agent is not seeing the flow connector tools — `start_flow` handler is not registering the FlowAdapter with the driver |
| Phase 4 times out and the agent emits text without calling tools | Same as above — the orchestrator prompt tells the agent to call tools but the tools aren't in the SDK tool list |
| Approval click does not advance the flow | tRPC route hit but runner side-effect missing — check that `approve_flow_node` is in the runner serve.ts handler list |
| Flow panel never opens | useFlowExecution hook is not subscribing or the panel mount condition is wrong — check the workspace.page.tsx tab gate |
| Chat shows no breadcrumbs | FlowExecutionSyncService not attached to the dispatcher or breadcrumb diff is empty |

## What to report

- Which project name was created (so the user can clean it up)
- Which phase failed and the exact `wait_for` text that timed out
- A snapshot of the flow execution panel at the point of failure
- Network requests with non-200 status from the test window
- Any console errors

## Token budget

Each test-echo run uses approximately:
- 3 turns to the LLM (one per node + one wrap-up)
- ~2-5K tokens per turn (the orchestrator prompt + node skill body are small)
- Total: ~10-20K tokens per run

Don't run the full-run variant in CI loops — schedule it before merges or on
demand.
