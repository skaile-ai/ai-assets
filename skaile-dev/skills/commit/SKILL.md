---
name: commit
description: "Use at the end of a skaile-dev work session to write a devlog entry summarising all changes made and commit everything to git. Triggered by /commit."
keywords: [commit, devlog, session, summary, git, changelog]
metadata:
  stage: alpha
  domain: skaile-dev
---

# Commit — Session Wrap-up

You are wrapping up a development session on the `skaile-dev` monorepo.
Your job is to: (1) understand what changed, (2) write a concise devlog entry, (3) stage and commit.

---

## Step 1 — Survey the changes

Run these commands and read their output carefully:

```bash
git status
git diff --stat HEAD
git diff HEAD
```

Also check what devlog entries already exist for today so you don't duplicate:

```bash
ls _devlog/ | grep $(date +%Y-%m-%d)
```

---

## Step 2 — Write the devlog entry

Choose a short slug that describes the main theme of this session (e.g. `tutorial-docs`, `flow-yaml-tests`, `bridge-refactor`).

Create `_devlog/<YYYY-MM-DD>_<slug>.md` with this structure:

```markdown
# <YYYY-MM-DD> — <human-readable title>

## Summary

One paragraph. What was the goal of this session and what was achieved?

## Changes

One H3 per package or area that was meaningfully changed. Under each heading, use a bullet list — be specific about files, functions, and why (not just what).

### <package-or-area>

- **`<file>`** — what changed and why
- ...

## Design Decisions

(Optional) Any non-obvious choices made. Why this approach over alternatives?
```

Rules:
- Keep each bullet to one or two sentences. Focus on the *why*, not just the *what*.
- Omit the `## Design Decisions` section if there were no notable decisions.
- If a devlog entry for today already covers a related topic, prefer adding an H3 section to that file rather than creating a new one. Only create a new file if the topics are clearly separate.
- Do not list trivial changes (whitespace, typos in comments) unless they fixed a bug.

---

## Step 3 — Stage and commit

Stage only the files that belong to this session's work. Use specific paths — do not `git add .` blindly if unrelated files are modified.

```bash
git add <file1> <file2> ...
git add _devlog/<date>_<slug>.md
```

Verify what is staged:

```bash
git diff --staged --stat
```

Write the commit message following the monorepo convention:

```
<type>(<scope>): <short description>

<optional body: one or two sentences if context is needed>

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

**Type:** `feat`, `fix`, `docs`, `test`, `refactor`, `chore`
**Scope:** the primary package or area changed (e.g. `skaile-agent-cli`, `arm`, `flow-engine`, `ai-resources`)
If multiple packages are touched equally, use the most prominent one or omit scope.

```bash
git commit -m "$(cat <<'EOF'
<type>(<scope>): <description>

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

After committing, confirm success:

```bash
git log --oneline -3
```

---

## Constraints

- Never amend a previous commit — always create a new one.
- Never use `--no-verify`.
- Do not stage files from unrelated in-progress work (e.g. half-finished features in other packages).
- If `git diff HEAD` is empty or only contains the devlog file itself, say so and ask the user whether to proceed with a docs-only commit.
