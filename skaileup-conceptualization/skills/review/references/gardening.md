# Gardening Mode — Safe vs Unsafe Fixes

Gardening mode auto-fixes **safe** issues without asking. Unsafe issues are
reported for human attention. This file defines the boundary.

## Safe Auto-Fixes (applied immediately)

| Issue | Fix applied |
|-------|-------------|
| Missing `last_updated` in frontmatter | Set to today's date |
| `last_updated` format invalid | Rewrite as `YYYY-MM-DD` |
| `status:` field present in frontmatter | Remove the field entirely (globally removed) |
| Missing `screens: []` in feature | Add empty array |
| Missing `data_entities: []` in feature | Add empty array |
| Broken screen reference in feature `screens:[]` | Remove the broken entry |
| Broken feature reference in screen `implements:[]` | Remove the broken entry |
| Broken feature path in `feature_map.json` | Remove the broken path entry |
| `PLANS.md` checkboxes don't match actual `_concept/` state | Update checkboxes to match observed state |

### Why these are safe

These fixes are mechanical and reversible:
- Removing `status:` fields enforces a globally-agreed policy — no judgment needed
- Adding missing metadata uses safe defaults (today's date, empty array)
- Removing broken references only cleans up pointers to files that no longer exist
- `PLANS.md` checkbox updates reflect observable ground truth

---

## Unsafe Issues (reported, NOT auto-fixed)

| Issue | Why not auto-fix |
|-------|-----------------|
| Missing pipeline steps (empty folders) | Requires running a skill to generate content |
| Orphaned entities in `model.json` | User may want them for future features |
| Stale files (30+ days old) | User may still need them; staleness is contextual |
| Golden principle violations in `model.json`/`model.dbml` | Changes data model semantics |
| Feature group number gaps | Renumbering cascades to screens, references, `feature_map.json` |
| Missing required frontmatter fields (non-structural) | Content must come from the user or a skill run |
| Cross-references missing (feature never added to screen) | Requires understanding intent, not just removing a pointer |

### Why these are unsafe

These fixes require judgment or have cascading side effects:
- Missing steps need creative/analytical work, not mechanical patching
- Orphaned entities may be intentionally kept for planned features
- Stale files may be stable references that don't need refreshing
- Model field changes alter the data contract downstream tools consume
- Renumbering groups breaks every cross-reference in the pipeline

---

## Gardening Boundary Rules

1. **NEVER delete files** — only remove broken references from frontmatter arrays
2. **NEVER modify `model.json` or `model.dbml`** directly — even to fix golden principle violations
3. **NEVER add content** to body sections — only add/remove/fix frontmatter fields
4. **ALWAYS report** every change made, even trivial ones
5. **Recalculate score** after all fixes to show improvement
