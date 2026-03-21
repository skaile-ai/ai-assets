# Feedback Loops: Cross-Reference Integrity

## The Problem

When Skill A creates artifacts that reference Skill B's artifacts, the references must be bidirectional. Without enforcement, links break silently and compound exponentially.

## The Protocol

### Rule: Both directions, both files, same transaction

When a downstream skill creates a reference to an upstream artifact:

1. **Downstream file** gets a forward reference (e.g., `implements: [path/to/upstream]`)
2. **Upstream file** gets a backlink appended (e.g., `screens: [{ path: path/to/downstream }]`)
3. Both files get `last_updated` timestamp
4. Observability event emitted: `[skill] feedback_loop updated path/to/upstream`

### Example: Screens referencing Features

When `skill_screens` creates `07_screens/01_auth/login.md`:

```yaml
# In 07_screens/01_auth/login.md (downstream)
---
implements:
  - 03_features/01_auth/login.md
  - 03_features/01_auth/password_reset.md
data_entities: [user, session]
last_updated: 2026-03-15
---
```

```yaml
# In 03_features/01_auth/login.md (upstream) — APPEND to screens array
---
screens:
  - path: 07_screens/01_auth/login.md
last_updated: 2026-03-15
---
```

### Example: Data Model referencing Features

When `skill_datamodel` creates entity definitions:

```yaml
# In 06_datamodel/model.json — entity has from_features
{
  "name": "user",
  "from_features": ["03_features/01_auth/login.md"]
}
```

```yaml
# In 03_features/01_auth/login.md — APPEND to data_entities
---
data_entities: [user, session]
last_updated: 2026-03-15
---
```

## Repair Protocol

When cross-references break (detected by quality audit):

1. Scan all artifact folders for references
2. Build reference graph (forward + backward)
3. Identify orphaned links (reference exists but target doesn't)
4. Identify missing backlinks (forward ref exists but no backlink)
5. Show diff to user before applying fixes
6. Update both files atomically

## Quality Metrics

- **Coverage:** % of features with at least one screen reference
- **Integrity:** % of forward references that have matching backlinks
- **Freshness:** days since `last_updated` per file (> 30 days = stale)
- **Orphans:** files that exist but are unreferenced by any other file

## Iron Law

> Cross-reference integrity is non-negotiable. Broken links compound exponentially.
> Fix immediately. Audit frequently. Auto-garden safe fixes.
