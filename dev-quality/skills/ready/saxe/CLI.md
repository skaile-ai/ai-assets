# app-ready CLI

## Trigger

Invoke with: "Is the app ready for testing?" or "Check readiness" or `/ready`

## Output

Readiness table printed in conversation — no files written.

## Recommended Workflow Position

```
concept → concept-3-blueprint-3-datamodel → app-design → /ready → app-audit → app-e2e
                                            ↑
                                      Run this here
```

## What It Checks

For each feature in `_concept/2_experience/2_features/`:
1. Concept doc exists
2. Screen spec exists in `_concept/2_experience/3_screens/`
3. Data model entry in `_concept/3_blueprint/3_datamodel/postxl-schema.json`
4. Brand tokens exist at `_concept/1_discovery/3_brand/tokens.json`
5. Tech stack exists at `_concept/3_blueprint/1_techstack/stack.md`
6. Mockup HTML exists in `_concept/05_mockups/`
7. Status is `implemented` or `mockup_ready` in frontmatter
