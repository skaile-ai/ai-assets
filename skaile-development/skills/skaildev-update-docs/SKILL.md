---
name: skaildev-update-docs
description: >
  DEPRECATED: Use skaildev-doc with --mode update instead.
  Post-implementation documentation sync for skaile-dev.
metadata:
  version: "1.1.0"
  tags:
    - documentation
    - deprecated
  stage: deprecated
  source: MERGED
  deprecated: true
  superseded_by: skaildev-doc
---

## DEPRECATED

This skill has been superseded by `skaildev-doc` (mode: update).

**To update documentation after implementation:**

```bash
skaile skill run skaildev-doc --input MODE=update
```

**What changed:**
- `skaildev-doc` is a superset covering write, update, audit, and status modes
- Uses git-commit-based staleness tracking (`_based_on_commit`) instead of `_source_hash`
- Adds `@doc:` annotation support for inline documentation guidance
- Helper scripts provide deterministic data collection before agent acts

See `ai-resources/skaile-development/skills/skaildev-doc/SKILL.md` for the full skill.
