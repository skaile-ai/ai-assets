---
name: "skaildev-devlog"
description: "Maintains the human-readable _devlog for the skaile-dev monorepo. Writes short plain-language entries to _devlog/DEVLOG.md after every meaningful change. Generates detailed reports in _devlog/reports/ when architectural concepts, shared contracts, or core patterns change. Run after completing any implementation work."
metadata:
  version: "1.0.0"
  tags:
    - "devlog"
    - "changelog"
    - "documentation"
    - "institutional-memory"
    - "plain-language"
    - "reports"
    - "skaile-dev"
  source: "MERGED"
  stage: "beta"
  produces:
    - path: "_devlog/DEVLOG.md"
      description: "Running development log with short plain-language entries"
    - path: "_devlog/reports/<date>-<topic>.md"
      description: "Detailed report for conceptual/architectural changes (only when needed)"
  user_inputs:
    dialog:
      - id: "what_changed"
        label: "What changed? (plain language, 1–2 sentences)"
        type: "text"
        required: true
      - id: "why"
        label: "Why was this change made?"
        type: "text"
        required: true
      - id: "packages"
        label: "Affected packages (comma-separated)"
        type: "text"
        required: false
      - id: "implications"
        label: "Any implications, breaking changes, or follow-up needed?"
        type: "text"
        required: false
      - id: "report_needed"
        label: "Does this change warrant a detailed report? (yes/no)"
        type: "select"
        options: ["no", "yes"]
        required: false
        default: "no"
        hint: "yes = architectural or conceptual change that affects how the system works broadly"
    files: []
---

# Devlog — Development Log Maintenance

## Overview

Maintains the `_devlog/` directory — the human-readable institutional memory of the
skaile-dev monorepo. Answers "what changed, why, and what are the implications" in plain
language that any developer (or AI agent in a future session) can quickly scan.

Two output types:

| Type | When | Location |
|------|------|---------|
| Short entry | Every meaningful change | `_devlog/DEVLOG.md` (append) |
| Detailed report | Conceptual/architectural changes | `_devlog/reports/<date>-<topic>.md` |

**Every meaningful change gets a short entry.** Detailed reports are reserved for changes
that alter how the system fundamentally works — shared contracts, architectural patterns,
breaking API changes, new development paradigms.

## When to Use

- After completing an implementation task (called by `implement-skaile` automatically)
- When user asks to "log this change" or "add a devlog entry"
- Any time work is done that future developers or agents should know about

## When NOT to Use

- For documenting current system state (use README.md, CLAUDE.md, or Starlight docs)
- For writing a git commit message (use `git-workflow`)
- Standalone doc updates without code changes (just add docs, no devlog needed)

## The _devlog is NOT Documentation

The devlog records **what changed and why** — it's historical. It does NOT:
- Replace README.md (which must always reflect the current state)
- Replace CLAUDE.md (which must always reflect current architecture)
- Replace Starlight docs (which must always be current reference)

See `references/doc_tiers.md` for the full decision table.

---

ROLE  Development log writer — records changes in plain language with implications and context for future developers and AI agents.

READS
  git log / git diff                         — recent commits and changes for context
  _devlog/DEVLOG.md                          — existing log (append to it, never overwrite)
  _implementation/decisions.md              — decisions and concerns to incorporate
  ? <package>/CLAUDE.md                     — context for architectural change reports

WRITES
  _devlog/DEVLOG.md                         — new entry appended at top of file
  _devlog/reports/<date>-<topic>.md         — detailed report (when report_needed=yes)

MUST  append to DEVLOG.md — never overwrite it
MUST  write at the top of the file (newest entries first)
MUST  use plain language — no jargon, no implementation detail dumps
MUST  include: what changed, why, affected packages, implications
MUST  write reports for architectural/conceptual changes
NEVER duplicate an entry for the same change
NEVER write entries that are just git commit messages
NEVER write entries without a "why" — the reason matters most

EMIT [skaildev-devlog] started

# ── DEVLOG.md Initialization ─────────────────────────────────────

IF _devlog/DEVLOG.md does not exist:
  - Create _devlog/ directory
  - Create _devlog/DEVLOG.md with header:

    ```
    # skaile-dev Development Log

    Human-readable record of what changed in this codebase, why it changed,
    and what the implications are. Newest entries first.

    For architecture, see CLAUDE.md. For usage, see README.md.
    For in-depth reference, see the Starlight docs.
    This log is the *why* — not a replacement for any of the above.

    ---
    ```

# ── Short Entry ──────────────────────────────────────────────────

STEP 1: Collect entry content
  - Date: today (YYYY-MM-DD)
  - What changed: from what_changed input (or derive from recent git log)
  - Why: from why input
  - Packages: from packages input (or derive from git diff)
  - Implications: from implications input (or assess from the change)

STEP 2: Assess implications
  IF implications not provided:
    - Read the changed files
    - Assess:
      - Does this change any public API? → flag for README/docs update
      - Does this add/remove a package? → flag for workspace changes
      - Does this change a shared contract? → flag as broad impact
      - Does this introduce a breaking change? → must be explicit
      - Does this require migration steps for existing users? → document them
    - Write a concise implications sentence

STEP 3: Write entry

  Entry format (prepend to DEVLOG.md after the header):

  ```markdown
  ## YYYY-MM-DD — <Title> (3–6 words, plain language)

  **What changed:** <1–2 sentences. What is different now that wasn't before. No jargon.>

  **Why:** <1–2 sentences. What problem this solves or feature this enables.>

  **Affected:** `<package>`, `<package>` (comma-separated, backtick-wrapped)

  **Implications:** <1–2 sentences. What downstream effects, breaking changes, or follow-up
  actions this creates. Use "None" if truly no implications.>

  ---
  ```

  Examples of good entries:

  ```markdown
  ## 2026-03-25 — Add devlog skill to skaile-dev-ops domain

  **What changed:** A new `devlog` skill is available in the `skaile-dev-ops` domain. It
  maintains `_devlog/DEVLOG.md` with short entries and generates detailed reports for
  architectural changes.

  **Why:** Developers and AI agents starting a new session had no quick way to understand
  recent changes and their implications without digging through git log.

  **Affected:** `ai-resources/skaile-dev-ops`

  **Implications:** The `implement-skaile` orchestrator now triggers `devlog` automatically
  after completing work. Existing workflows are unaffected.

  ---
  ```

  ```markdown
  ## 2026-03-22 — Rename users table primary key from id to user_id

  **What changed:** The `users` table in forge/project and forge/concept now uses `user_id`
  as the primary key column name instead of `id`.

  **Why:** Consistent with the platform's naming convention; reduces confusion when
  joining tables in complex queries.

  **Affected:** `forge/project`, `forge/concept`, `forge/common-backend`

  **Implications:** Breaking change. All existing sessions are invalidated (the FK column
  in `sessions` also changed). Run the provided migration script before deploying.
  Auth cookie `pichi_auth` must be re-issued to existing users.

  ---
  ```

EMIT [skaildev-devlog] entry_written date=<date> title=<title>

# ── Detailed Report (when report_needed = yes) ────────────────────

STEP 4: Assess if report is needed
  IF report_needed = yes OR any of these conditions:
    - A shared contract in `dev-shared/contracts/` changed
    - A core architectural pattern changed (e.g., how agents dispatch, how sessions work)
    - A breaking API change affects 3+ packages
    - A new development paradigm is introduced to the monorepo
    - A security-related change is made to auth or session handling
  THEN generate a detailed report

STEP 5: Write detailed report
  - Create: `_devlog/reports/<YYYY-MM-DD>-<topic-slug>.md`
  - Link from the short DEVLOG.md entry: `See report: [_devlog/reports/<filename>]`

  Report structure:

  ```markdown
  # <Title> — Detailed Change Report

  **Date:** YYYY-MM-DD
  **Change type:** <contract | architecture | breaking-api | paradigm | security>
  **Affected packages:** <list>
  **Status:** <complete | in-progress | proposed>

  ---

  ## Summary

  <2–3 sentences: what changed and why in plain language>

  ## What Was There Before

  <Describe the old behavior, pattern, or contract. Include code snippets if helpful.>

  ## What Is There Now

  <Describe the new behavior, pattern, or contract. Include code snippets if helpful.>

  ## Why This Changed

  <Full rationale: what problem was it solving, what triggered the change, what alternatives
  were considered and rejected.>

  ## Implications

  ### Breaking Changes

  <List every breaking change with: what breaks, what packages are affected, and what to do.>

  ### Migration Guide

  <Step-by-step instructions for adapting existing code, data, or workflows to the change.
  If no migration is needed, say "No migration required.">

  ### Downstream Effects

  <What other systems, skills, or workflows are affected. What needs to be updated as a result.>

  ## What to Check After This Change

  <Checklist of things to verify: tests to run, configs to review, behaviors to spot-check.>

  ## Related

  <Links to relevant commits, PRs, contract files, CLAUDE.md sections, or skill files.>
  ```

EMIT [skaildev-devlog] report_written path=_devlog/reports/<filename>

# ── Commit the devlog ─────────────────────────────────────────────

STEP 6: Commit devlog updates
  $ git add _devlog/
  $ git commit -m "docs(devlog): record <title>"

EMIT [skaildev-devlog] completed entry=<title> report=<yes|no>

CHECKLIST
  - [ ] DEVLOG.md exists (created if needed)
  - [ ] New entry prepended at top (newest first)
  - [ ] Entry has: what changed, why, affected, implications
  - [ ] Plain language — no internal jargon
  - [ ] Detailed report generated when report_needed=yes
  - [ ] Report linked from DEVLOG.md entry
  - [ ] Changes committed with "docs(devlog): ..." message

---

## When Is a Report "Report-Worthy"?

| Change | Short entry only | Report needed |
|--------|-----------------|---------------|
| New skill or domain added | Yes | No (unless changes how system works) |
| Bug fix | Yes | No |
| New feature in one package | Yes | No |
| Breaking API change (1–2 packages) | Yes | No |
| Breaking API change (3+ packages) | Yes | Yes |
| Shared contract changed | Yes | Yes |
| Core architectural pattern changed | Yes | Yes |
| New development paradigm introduced | Yes | Yes |
| Security/auth change | Yes | Yes |
| Large refactor across many packages | Yes | Judgment call |

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Writing git-commit-style entries ("feat: add X") | Write in plain language: "Added X to forge/project, which allows users to..." |
| Skipping the "why" | The why is the most valuable part — always include it |
| Overwriting DEVLOG.md | Always prepend — never overwrite or truncate |
| Writing reports for every change | Reports are for conceptual/architectural changes only |
| Treating devlog as a substitute for docs | The devlog is history. README/CLAUDE.md/Starlight are current truth. |

## Integration

- **Called by:** `implement-skaile` (step 10: write devlog)
- **Calls:** none
- **Preceded by:** `run-tests`, `update-skaile-docs`
- **References:** `references/doc_tiers.md` for the report decision criteria
