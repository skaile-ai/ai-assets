# Gardening Mode — Safe vs Unsafe Fixes

Gardening mode auto-fixes **safe** issues without asking. Unsafe issues are
reported for human attention. This file defines the boundary.

## Safe Auto-Fixes (applied immediately)

| Issue | Fix applied |
|-------|-------------|
| Missing `last_updated` in frontmatter | Set to today's date |
| `last_updated` format invalid | Rewrite as YYYY-MM-DD |
| Missing `status` field | Set to `draft` |
| Missing `screens: []` in feature | Add empty array |
| Missing `data_entities: []` in feature | Add empty array |
| Broken screen reference in feature | Remove the broken entry from `screens:[]` |
| Broken feature reference in screen | Remove from `implements:[]` |
| Broken entry in `feature_map.json` | Remove the broken path |
| PLANS.md progress doesn't match reality | Update checkboxes to match actual state |

### Why these are safe

These fixes are mechanical and reversible:
- Adding missing metadata fields uses safe defaults (`draft`, today's date, empty array)
- Removing broken references only cleans up pointers to files that no longer exist
- PLANS.md checkbox updates reflect observed ground truth

## Unsafe Issues (reported, NOT auto-fixed)

| Issue | Why not auto-fix |
|-------|-----------------|
| Missing pipeline steps (empty folders) | Requires running a skill to generate content |
| Orphaned entities in postxl-schema.json | User may want them for future features |
| Stale files (30+ days old) | User may still need them; staleness is contextual |
| Golden principle violations (missing fields in model) | Changes data model semantics |
| Feature group number gaps | Renumbering cascades to screens, references, feature_map |

### Why these are unsafe

These fixes require judgment or have cascading side effects:
- Missing steps need creative/analytical work, not mechanical patching
- Orphaned entities may be intentionally kept for planned features
- Stale files may be stable references that don't need refreshing
- Model field changes alter the data contract downstream tools consume
- Renumbering groups would break every cross-reference in the pipeline
