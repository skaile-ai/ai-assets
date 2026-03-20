# Readiness Report Templates

## Readiness Table

```
## Readiness Report

| Feature | Group | Screen | Data Model | Mockup | Ready? |
|---------|-------|--------|------------|--------|--------|
| login | 01_user_auth | ✓ | ✓ | ✓ | ✅ Yes |
| dashboard | 02_dashboard | ✓ | ✗ | ✗ | ❌ No |
| profile | 03_settings | ✗ | ✗ | ✗ | ❌ No |

Global: Brand tokens ✓ | Tech stack ✓
```

## Fix Recommendations

For each feature that is NOT ready, list missing items with remediation:

```
## What to Do

### <feature> (<group>)
- ✗ Screen spec missing → run `concept-2-experience-3-screens`
- ✗ Data model missing → run `concept-3-blueprint-3-datamodel`
- ✗ Mockup missing → run `app-design`
- ✗ Status not ready → update frontmatter to `status: implemented` or `status: mockup_ready`
```

## Verdict

```
X of Y features are ready for E2E testing.
Ready: [list]
Not ready: [list]
```

Verdict messages by readiness level:

- **ALL ready:** "All features ready. Run `app-e2e` with confidence."
- **SOME ready:** "Partial readiness. Run `app-e2e` only for ready features, or fix gaps first."
- **NONE ready:** "No features ready for E2E testing. Fix gaps above first."

## Per-Feature Checks

| Check | How to verify |
|-------|---------------|
| Concept doc | `_concept/2_experience/2_features/<group>/<feature>.md` exists |
| Screen spec | At least one `.md` in `_concept/2_experience/3_screens/` with this feature in `implements:` |
| Data model | Feature listed in `_concept/3_blueprint/3_datamodel/feature_map.json` for at least one model in `_concept/3_blueprint/3_datamodel/postxl-schema.json` |
| Brand tokens | `_concept/1_discovery/3_brand/tokens.json` exists |
| Tech stack | `_concept/3_blueprint/1_techstack/stack.md` exists |
| Mockup | At least one `.html` in `_concept/05_mockups/` linked from the feature or screen |
| Status | `status: implemented` or `status: mockup_ready` in feature frontmatter |
