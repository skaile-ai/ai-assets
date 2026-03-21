# E2E Report Template

## Summary Output

Present this summary when all journeys are complete:

```
## E2E Testing Complete

Journeys Tested: N
Screenshots Captured: N
Issues Found: N (N fixed, N remaining)

### Issues Fixed
- [Description] — [file:line]

### Remaining Issues
- [Description] — [severity] — [file:line]

Screenshots: e2e-screenshots/
```

Optional: export full report to `e2e-test-report.md`.

## Feedback Loop Event Format

For every successfully tested journey, update the corresponding feature file
in `_concept/2_experience/2_features/` (update `last_updated` in frontmatter) and emit:

```
[e2e] feedback_loop updated 2_experience/2_features/<group>/<feature>.md
  updated last_updated: <today>
```

## Seed Data Scenario Usage

Use scenario-based data from `_concept/3_blueprint/3_datamodel/seed.json`:

| Scenario | Use for |
|----------|---------|
| `populated` | Core journey form inputs, main happy-path flows |
| `empty` | First-use / onboarding flow tests |
| `edge_cases` | Validation, layout stress tests, boundary values |
| Custom scenarios | Any additional scenarios defined in seed.json |

## Responsive Breakpoints

Test key pages at these viewports:

| Device | Width x Height |
|--------|---------------|
| Mobile | 375 x 812 |
| Tablet | 768 x 1024 |
| Desktop | 1440 x 900 |

## Database Validation

After data-modifying interactions, query the database to verify:
- Records were created/updated/deleted as expected
- Field values match `model.json` entity definitions
- Relations (foreign keys) are correctly populated
- Enum fields contain valid values from model.json enum definitions
