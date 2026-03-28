# skaildev-update-docs — CLI Usage

## Basic Usage

```bash
skaile skill run skaildev-update-docs --project-dir .
```

Runs against the current HEAD. Detects changes in `HEAD~1..HEAD` and syncs all
five documentation tiers.

## Common Invocations

```bash
# Sync docs for all changes on the current feature branch
skaile skill run skaildev-update-docs \
  --input git_range="main...HEAD"

# Sync docs for a specific package only
skaile skill run skaildev-update-docs \
  --input scope="forge/project"

# Dry run: report what's stale but do not write anything
skaile skill run skaildev-update-docs \
  --input dry_run=true

# After a specific commit
skaile skill run skaildev-update-docs \
  --input git_range="abc1234..HEAD"
```

## Integration with skaildev-implement

`skaildev-implement` calls this skill automatically after tests pass. If you are
running implementation manually, invoke this skill before finishing the branch:

```bash
# 1. Implementation done, tests passing
# 2. Sync docs
skaile skill run skaildev-update-docs

# 3. Then write devlog
skaile skill run skaildev-devlog

# 4. Then finish the branch
skaile skill run skaildev-git-workflow --input mode=finish
```

## When the Starlight Site Needs Rebuilding

After this skill updates Starlight pages, rebuild the docs site to verify links:

```bash
cd docs && bun run build
```

If build fails due to broken links or missing sidebar entries, this skill will have
flagged them in the completion report.
