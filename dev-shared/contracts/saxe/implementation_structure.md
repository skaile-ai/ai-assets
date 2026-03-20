# Implementation Structure

Canonical paths, naming rules, and progress tracking for the `_implementation/` folder.

## Folder Layout

```
_implementation/
├── PLANS.md                        — Durable implementation plan (see plans.md)
├── progress.json                   — Machine-readable feature status
├── decisions.md                    — Dated implementation decisions
├── features/                       — Per-feature tracking (mirrors _concept/2_experience/2_features/)
│   ├── <NN_group>/
│   │   └── <feature>.md            — Status, ACs, commits, blockers
│   └── ...
├── acceptance_criteria/            — AC specs derived from concept
│   ├── <NN_group>/
│   │   └── <feature>.ac.md         — Structured ACs (see acceptance_criteria.md)
│   └── ...
├── logs/                          — Structured event logs per feature (JSONL)
│   └── <group>__<feature>.jsonl   — Append-only event stream
└── verification/                   — Test results and evidence
    ├── reports/                    — Per-feature and full-suite reports
    │   ├── <feature>-report.json
    │   ├── uat-report.json         — UAT results per user journey
    │   └── full-verification.json
    └── screenshots/                — Browser screenshots from agent-browser
        ├── <feature>/
        ├── <group>_preview/        — Feature group preview screenshots
        └── full-walkthrough/
```

## Naming Rules

- All folder and file names use `lowercase_snake_case`
- Feature group folders use the same `NN_` prefix as `_concept/2_experience/2_features/`
- Feature file names match their `_concept/2_experience/2_features/` counterparts exactly

## progress.json Schema

```json
{
  "app_name": "string",
  "complexity_tier": "small | standard | complex",
  "started_at": "ISO date",
  "last_updated": "ISO date",
  "current_phase": "scaffold | generate | foundation | infrastructure | features | uat | verify | complete",
  "git_branch": "string",
  "phases": {
    "scaffold": {
      "status": "pending | in_progress | approved",
      "completed_at": null
    },
    "generate": { "status": "pending", "completed_at": null },
    "foundation": { "status": "pending", "completed_at": null },
    "infrastructure": {
      "status": "pending | skipped | in_progress | approved",
      "completed_at": null
    },
    "features": { "status": "pending", "completed_at": null },
    "uat": {
      "status": "pending | in_progress | approved",
      "completed_at": null,
      "iterations": 0
    },
    "verify": { "status": "pending", "completed_at": null }
  },
  "infrastructure": {
    "custom_modules": [
      {
        "name": "string",
        "layer": "shared_contract | provider_abstraction | platform_service | communication | process",
        "status": "pending | in_progress | completed",
        "has_real_impl": true,
        "has_inmemory_impl": true,
        "completed_at": null
      }
    ],
    "additional_processes": [
      {
        "name": "string",
        "port": 0,
        "status": "pending | in_progress | completed",
        "completed_at": null
      }
    ],
    "communication": [
      {
        "protocol": "websocket | sse",
        "endpoint": "string",
        "status": "pending | in_progress | completed"
      }
    ]
  },
  "feature_groups": [
    {
      "group": "01_auth_workspace",
      "status": "pending | in_progress | approved",
      "features": [
        {
          "name": "sso_login",
          "status": "pending | ac_written | tests_written | implementing | testing | approved",
          "branch": "feat/01-auth-workspace/sso-login",
          "commits": [],
          "started_at": null,
          "completed_at": null,
          "approval_method": null
        }
      ]
    }
  ]
}
```

The `approval_method` field is set to `"human"` or `"auto"` when a feature is
approved, indicating which approval path was taken.

## Feature Tracking File Format

Each `_implementation/features/<group>/<feature>.md` file:

```yaml
---
status: pending | ac_written | tests_written | implementing | testing | approved
feature_ref: _concept/2_experience/2_features/<group>/<feature>.md
screen_refs:
  - _concept/2_experience/3_screens/<group>/<screen>.md
journey_refs: []  # story IDs from _concept/2_experience/1_journeys/stories.json
branch: feat/<group-slug>/<feature-slug>
commits: []
started_at: null
completed_at: null
---

## Acceptance Criteria

→ See `_implementation/acceptance_criteria/<group>/<feature>.ac.md`

## Implementation Notes

(Added during implementation — blockers, decisions, deviations from spec)

## Verification

- [ ] All E2E tests passing
- [ ] Storybook stories created
- [ ] agent-browser visual check passed
- [ ] No lint/type errors introduced
```

## Implementation Logs

Each feature emits structured events to `_implementation/logs/<group>__<feature>.jsonl`
(one JSON object per line, append-only). Double-underscore encodes the group/feature
path in a flat filename (e.g., `01_auth_workspace__sso_login.jsonl`).

### Event Schema

```json
{
  "ts": "2026-03-12T14:30:00Z",
  "event": "<event_type>",
  "feature": "01_auth_workspace/sso_login",
  "phase": "ac | test | implement | verify | merge",
  "detail": {},
  "run_id": "uuid"
}
```

### Event Types

| Event           | Phase            | Detail fields                                                   |
| --------------- | ---------------- | --------------------------------------------------------------- |
| `env_up`        | test             | `ports`: `{ fe, api, db }`                                      |
| `ac_written`    | ac               | `count`: number of ACs                                          |
| `tests_written` | test             | `count`: number of E2E tests                                    |
| `test_run`      | implement/verify | `total`, `passed`, `failed`, `skipped`, `duration_ms`           |
| `build`         | implement/verify | `passed`: bool, `errors`: number                                |
| `storybook`     | implement/verify | `passed`: bool, `stories`: number                               |
| `browser_check` | verify           | `passed`: bool, `screenshots`: string[], `deviations`: string[] |
| `error`         | any              | `message`: string, `category`: string                           |
| `fix`           | implement        | `message`: string, `related_error_ts`: string (optional)        |
| `approval`      | merge            | `method`: `"human"` or `"auto"`                                 |
| `merge`         | merge            | `branch`: string, `commit`: string                              |
| `env_down`      | merge            | `cleanup`: bool                                                 |

### Querying Logs

```bash
# All errors across all features
cat _implementation/logs/*.jsonl | jq 'select(.event == "error")'

# Test run history for a specific feature
cat _implementation/logs/01_auth_workspace__sso_login.jsonl | jq 'select(.event == "test_run")'

# Features that had build failures
cat _implementation/logs/*.jsonl | jq 'select(.event == "build" and .detail.passed == false) | .feature' | sort -u
```

### Retention

Log files are append-only and committed to git with the feature branch.
They are squash-merged along with the feature code. The full log history
is preserved on the implementation branch.

## Read Direction

Implementation skills read from `_concept/` (all numbered folders) and write to
`_implementation/`. The project source code (backend/, frontend/, e2e/) lives in
the project root alongside `_concept/` and `_implementation/`.

## Status Lifecycle

**Phase status:** `pending` → `in_progress` → `approved`

**Infrastructure phase status:** `pending` → `in_progress` → `approved` (or `skipped` if no architecture doc)

**Infrastructure module status:** `pending` → `in_progress` → `completed`

**Feature status:** `pending` → `ac_written` → `tests_written` → `implementing` → `testing` → `approved`

Transitions happen at checkpoints. Once `approved`, status is final.
