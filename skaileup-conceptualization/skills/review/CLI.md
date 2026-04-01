# review CLI

## Trigger

Invoke with: "review", "audit the concept", "check for issues", "concept health",
"garden", "cleanup", "tidy up the concept", "fix entropy", or "quality score".

## Modes

- **Audit** (default): scans `_concept/`, reports issues, writes `quality.json`
- **Garden**: auto-fixes safe issues (stale fields, broken refs, status removal), reports changes

## Output

- `_concept/quality.json` — quality score (0–100) with 6-category breakdown
- Audit report printed in the conversation
- Gardening report printed with before → after score

## When to Use

- After each skill completes — quick pass to catch drift
- Before `e2e` — ensure structure is clean before testing
- Before merging concept changes — gate on quality score ≥ 70
- After editing concept files manually — verify cross-references are intact
- Any time you want a birds-eye view of concept health

## Next Steps

After audit:
- Score ≥ 70: proceed to next pipeline step
- Score < 70: fix CRITICAL/HIGH issues first, then re-run `review`
- Run `sync` to repair broken cross-references automatically
- Run `garden` mode for safe mechanical fixes
