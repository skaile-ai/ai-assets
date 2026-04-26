# ready CLI

## Trigger

Invoke with: `/ready`, "is it ready?", "check readiness", "pre-flight check", "is the app ready for testing?"

## Output

Readiness table printed in conversation — no files written.

## What It Checks

**Global (once):**
- Brand tokens exist at `_concept/discovery/brand/tokens.json`
- Tech stack exists at `_concept/blueprint/techstack.md`

**Per feature in `_concept/experience/features/`:**
1. Concept doc exists
2. Screen spec exists in `_concept/experience/screens/` (feature in `implements:`)
3. Data model entry in `feature_map.json` for at least one model
4. Storybook composition or mockup HTML (soft — warning only if absent)

## Recommended Workflow Position

```
datamodel → storybook → ready → audit → e2e
                           ↑
                      Run this here
```
