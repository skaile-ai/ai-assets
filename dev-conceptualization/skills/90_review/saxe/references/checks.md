# Audit Checks Reference

Detailed check tables and severity classifications for the review skill.

## Pipeline Structure Checks (Step 1)

| Check | How |
|-------|-----|
| Folder exists | `_concept/<folder>/` present |
| Has content | At least one expected output file exists |
| Frontmatter valid | All `.md` files have required fields per `shared/contracts/frontmatter.md` |
| Status | Read `status` from frontmatter |

## Frontmatter Compliance (Step 2)

For every `.md` file in `_concept/`, verify:

- Has YAML frontmatter delimiters (`---`)
- `status` field exists and is a valid lifecycle value (draft, approved, implemented, tested)
- `last_updated` field exists and is a valid ISO date (YYYY-MM-DD)
- **Feature files** additionally require: `priority`, `roles`, `screens`, `data_entities`
- **Screen files** additionally require: `implements`, `data_entities`, `layout`

## Golden Principles (Step 3)

For every applicable rule in `shared/contracts/golden_principles.md`:

- Entities have `id` (uuid, primary) + `created_at` + `updated_at`
- Enum values are PascalCase
- Feature groups are sequential (no gaps in numbering)
- Screen groups mirror feature group numbers
- Every feature has at least one requirement checkbox
- Every screen has component inventory + states section
- All paths in frontmatter resolve to existing files

## Cross-Reference Integrity (Step 4)

For every feature with `screens:` entries:
- Does each referenced screen file exist?
- Does that screen's `implements:` list this feature?

For every screen with `implements:` entries:
- Does each referenced feature file exist?
- Does that feature's `screens:` list this screen?

For every model in `postxl-schema.json` with entries in `feature_map.json`:
- Does each referenced feature file exist?

## Cascade Warning Severity (Step 5)

| Change type | Severity | Cascade? |
|-------------|----------|----------|
| Frontmatter fields added/removed | HIGH | Yes — downstream may depend on those fields |
| Body content edited | MEDIUM | Yes — downstream may reference that content |
| Only `last_updated` or `status` changed | LOW | No cascade needed |
| File deleted | HIGH | Cross-references will break |
| New file added | MEDIUM | Downstream may need to incorporate it |

## Entropy Indicators (Step 6)

| Indicator | Condition | Label |
|-----------|-----------|-------|
| Stale file | `last_updated` > 30 days old | STALE |
| Stagnant feature | `status: draft` untouched 14+ days | STAGNANT |
| Missing link | Feature with empty `screens: []` after screen step exists | MISSING LINK |
| Orphaned model | Model in postxl-schema.json with no feature_map.json entry | ORPHANED MODEL |
| Unexpected file | File outside expected `_concept/` structure | UNEXPECTED FILE |
| Group mismatch | Feature group without matching screen group | GROUP MISMATCH |
| Plan drift | PLANS.md progress out of sync with actual state | PLAN DRIFT |
