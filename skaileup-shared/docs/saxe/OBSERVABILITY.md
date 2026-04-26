# Observability

Every skill must emit structured events at key transitions. These events
enable debugging, progress tracking, and pipeline health monitoring.

## Event Format

All events are printed to stdout as single-line structured messages:

```
[<skill_name>] <event_type> <details>
  run_id: <uuid>
  timestamp: <ISO 8601>
```

The `run_id` is a UUID generated at the start of each skill invocation.
It must be included in every event so all actions within a single run
can be correlated.

## Required Events

### Every skill must emit:

| Event        | When                        | Details                               |
| ------------ | --------------------------- | ------------------------------------- |
| `started`    | Skill begins execution      | `run_id`, inputs read                 |
| `checkpoint` | Major phase boundary        | Phase name, what was produced         |
| `completed`  | Skill finishes successfully | Summary of outputs                    |
| `failed`     | Skill cannot proceed        | Error description, what was attempted |
| `blocked`    | Waiting for human input     | What's needed, checkpoint prompt      |

### Skills that modify upstream files must also emit:

| Event           | When                                   | Details                                 |
| --------------- | -------------------------------------- | --------------------------------------- |
| `feedback_loop` | Updating a file owned by another skill | Target file, what changed, source skill |

### Skills that validate structure must also emit:

| Event        | When                       | Details                        |
| ------------ | -------------------------- | ------------------------------ |
| `audit_pass` | A check passed             | Check name                     |
| `audit_fail` | A check failed             | Check name, expected vs actual |
| `audit_warn` | A non-blocking issue found | Check name, description        |

## Example Event Trace

```
[concept-2-experience-3-screens] started
  run_id: a1b2c3d4-e5f6-7890-abcd-ef1234567890
  reads: discovery/brief.md, experience/features/*, discovery/3_brand/tokens.json, blueprint/techstack.md, blueprint/datamodel/postxl-schema.json

[concept-2-experience-3-screens] checkpoint phase=screen_list_confirmed
  screens: experience/screens/01_user_auth/login.md, experience/screens/01_user_auth/registration.md, experience/screens/02_dashboard/overview.md

[concept-2-experience-3-screens] feedback_loop updated experience/features/01_user_auth/login.md
  added screen: experience/screens/01_user_auth/login.md
  source_skill: concept-2-experience-3-screens

[concept-2-experience-3-screens] completed
  run_id: a1b2c3d4-e5f6-7890-abcd-ef1234567890
  screens_written: 3
  features_updated: 2
```

## Health Metrics

The `concept-review` skill collects these metrics across the pipeline:

| Metric       | How it's measured                                              |
| ------------ | -------------------------------------------------------------- |
| Coverage     | % of features with screens, data_entities, and approved status |
| Freshness    | Days since last_updated on each file                           |
| Consistency  | Cross-reference link integrity (broken links = 0 is healthy)   |
| Completeness | % of pipeline steps with at least one approved artifact        |

## Entropy Indicators

Events that signal entropy accumulation:

- `audit_warn` count increasing over time
- Files with `last_updated` older than 30 days
- Features with empty `screens:[]` after screens skill has run
- postxl-schema.json models with no entries in `feature_map.json`
- Orphaned files (exist in folder but not referenced by any cross-link)
