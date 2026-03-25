# update-starlight-docs CLI

## Trigger

Invoke with: "update docs", "sync documentation", "check if docs are current",
"are the docs still correct", "documentation sync", or automatically after any
implementation step completes.

## Modes

- **Post-step (automatic)**: runs after scaffold, foundation, infrastructure, implement-feature, etc. Scopes to files changed in the current step.
- **Branch review**: scans all changes on the current feature branch vs main. Use before merging.
- **Full scan**: checks all doc pages with `_sources` for staleness. Use for periodic maintenance.

## Flags

| Flag | Default | Description |
|------|---------|-------------|
| `--scope` | `step` | `step` (last commit), `branch` (vs main), `full` (all tracked pages) |
| `--dry-run` | `false` | Report what would change without writing files |
| `--scaffold` | `true` | Create new doc pages for undocumented changes |

## Output

- Updated doc pages with corrected content and refreshed `_source_hash` / `_last_synced`
- New doc pages for significant uncovered source files (when `--scaffold` is true)
- Console summary: pages checked, updated, created, issues found

## Next Steps (after completion)

- Review updated doc pages for tone and accuracy
- Commit documentation changes alongside source code
- Run `verify` to confirm full project health
