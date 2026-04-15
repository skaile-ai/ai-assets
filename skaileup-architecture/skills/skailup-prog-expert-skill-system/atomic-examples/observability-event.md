# Observability Events

Every skill emits structured events at major transitions:

```
[skill_name] event_type details
  run_id: <uuid>
  timestamp: <ISO 8601>
```

## Required Events

```
[cf_datamodel] started
  run_id: a1b2c3d4-...
  inputs: [03_features/, 05_techstack/stack.md]

[cf_datamodel] checkpoint phase=entities_defined
  entities: [user, project, task]

[cf_datamodel] feedback_loop updated 03_features/01_auth/login.md
  set data_entities: [user, session]

[cf_datamodel] completed
  run_id: a1b2c3d4-...
  files: [model.dbml, model.json, seed.json]
  entities: 5
  relationships: 8

[cf_datamodel] failed
  run_id: a1b2c3d4-...
  error: "No feature files found in 03_features/"
  attempted: "read feature specs"

[cf_datamodel] blocked waiting=user_input
  need: "Database type preference (PostgreSQL, SQLite, etc.)"
```

## Validation Events (Quality Skills)

```
[cf_quality_review] audit_pass check=cross_references
  features_with_screens: 12/12

[cf_quality_review] audit_fail check=frontmatter
  file: 03_features/01_auth/login.md
  expected: last_updated field
  actual: missing

[cf_quality_review] audit_warn check=freshness
  file: 04_brand/identity.md
  last_updated: 2026-01-15
  days_stale: 59
```
