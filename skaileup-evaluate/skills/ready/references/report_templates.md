# Readiness Report Templates

## Readiness Table

```
## Readiness Report

| Feature | Group | Screen | Data Model | Mockup | Ready? |
|---------|-------|--------|------------|--------|--------|
| login | 01_user_auth | ✓ | ✓ | ✓ | ✅ Yes |
| dashboard | 02_dashboard | ✓ | ✗ | ✗ | ❌ No |
| profile | 03_settings | ✗ | ✗ | ~ | ❌ No |

Global: Brand tokens ✓ | Tech stack ✓
```

Legend: ✓ = present, ✗ = missing (required), ~ = absent (soft warning)

## Fix Recommendations

For each feature that is NOT ready, list missing required items with remediation:

```
## What to Do

### <feature> (<group>)
- ✗ Screen spec missing → run `screens`
- ✗ Data model missing → run `datamodel`
- ~ Mockup/composition absent → run `storybook` (recommended but not blocking)
```

## Verdict

```
X of Y features are ready for E2E testing.
Ready: [list]
Not ready: [list]
```

Verdict messages by readiness level:

- **ALL ready:** "All features ready. Run `e2e` with confidence."
- **SOME ready:** "Partial readiness. Run `e2e` only for ready features, or fix gaps first."
- **NONE ready:** "No features ready for E2E testing. Fix gaps above first."

## Per-Feature Checks

| Check | Required | How to verify |
|-------|----------|---------------|
| Concept doc | Yes | `_concept/2_experience/2_features/<group>/<feature>.md` exists |
| Screen spec | Yes | At least one `.md` in `_concept/2_experience/3_screens/` with this feature in `implements:` |
| Data model | Yes | Feature listed in `_concept/3_blueprint/3_datamodel/feature_map.json` for at least one model |
| Brand tokens | Yes (global) | `_concept/1_discovery/2_brand/tokens.json` exists |
| Tech stack | Yes (global) | `_concept/3_blueprint/1_techstack/stack.md` exists |
| Mockup/composition | Soft | Storybook page at `_concept/2_experience/4_storybook/src/pages/` OR html in `_concept/05_mockups/` |

**Note:** `status` field is not checked — status has been globally removed from all frontmatter.
Implementation progress is tracked in `_implementation/PLANS.md` (if present).
