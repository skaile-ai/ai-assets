# cf_quality_ready CLI

## Trigger

Invoke with: "Is the app ready for testing?" or "Check readiness" or `/ready`

## Output

Readiness table printed in conversation — no files written.

## Recommended Workflow Position

```
cf_orchestrator → cf_datamodel → cf_concept_mock → /ready → cf_quality_audit → cf_test_e2e
                                            ↑
                                      Run this here
```

## What It Checks

For each feature in `_concept/03_features/`:
1. Concept doc exists
2. Screen spec exists in `_concept/07_screens/`
3. Data model entry in `_concept/06_datamodel/model.json`
4. Brand tokens exist at `_concept/04_brand/tokens.json`
5. Tech stack exists at `_concept/05_techstack/stack.md`
6. Mockup HTML exists in `_concept/05_mockups/`
7. Status is `implemented` or `mockup_ready` in frontmatter
