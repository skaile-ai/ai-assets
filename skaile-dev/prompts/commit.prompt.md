---
name: commit
description: "Wrap up the current skaile-dev work session: survey git changes, write a _devlog entry, and commit."
version: "1.0"
---

Survey the current git state, write a concise devlog entry, then stage and commit everything from this session.

## Step 1 — Survey the changes

```bash
git status
git diff --stat HEAD
```

Read the diff carefully enough to summarise what changed and why.

Check whether a devlog entry for today already exists on the same topic:

```bash
ls _devlog/ | grep $(date +%Y-%m-%d)
```

## Step 2 — Write the devlog entry

Choose a short slug for the session theme (e.g. `tutorial-docs`, `flow-yaml-tests`).

Create `_devlog/<YYYY-MM-DD>_<slug>.md`:

```markdown
# <YYYY-MM-DD> — <human-readable title>

## Summary

One paragraph: goal of the session and what was achieved.

## Changes

One H3 per package or area that changed, with bullet points focused on the *why*:

### <package-or-area>

- **`<file>`** — what changed and why

## Design Decisions

(Only if non-obvious choices were made — otherwise omit this section.)
```

If today's devlog already covers a related topic, add an H3 section there instead of creating a new file.

## Step 3 — Stage and commit

Stage only the files belonging to this session — no blanket `git add .` if unrelated work is present:

```bash
git add <specific files...>
git add _devlog/<date>_<slug>.md
git diff --staged --stat
```

Commit using the monorepo convention:

```bash
git commit -m "$(cat <<'EOF'
<type>(<scope>): <short description>

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

**type:** `feat` · `fix` · `docs` · `test` · `refactor` · `chore`
**scope:** primary package or area (e.g. `skaile-agent-cli`, `arm`, `ai-resources`)

Confirm with `git log --oneline -3`.

---

**Constraints:** never `--amend` a previous commit · never `--no-verify` · if there is nothing to commit beyond the devlog itself, say so and ask before proceeding.
