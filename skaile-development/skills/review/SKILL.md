---
name: "review"
description: "[skaile-development] Code review for staged or committed changes in the skaile-dev monorepo. Checks quality, security, performance, scope, and repo-specific rules. Works locally without CI - designed for direct-to-main workflows."
metadata:
  version: "1.0.0"
  tags:
    - "review"
    - "quality"
    - "security"
    - "code-review"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  user_inputs:
    dialog:
      - id: "target"
        label: "What to review"
        type: "select"
        options:
          - "staged"
          - "last-commit"
          - "branch"
        required: false
        default: "staged"
        hint: "staged = git diff --cached | last-commit = last commit's diff | branch = all commits since main"
      - id: "focus"
        label: "Review focus (optional, comma-separated)"
        type: "text"
        required: false
        hint: "e.g. security,performance - leave empty for full review"
    files: []
---

# Review - Local Code Review

## Overview

Performs a comprehensive code review on local changes without requiring CI or a pull request. Designed for direct-to-main workflows where changes need a quality gate before committing.

| Target | What It Reviews | When to Use |
|--------|----------------|-------------|
| `staged` | `git diff --cached` | Before committing staged changes |
| `last-commit` | Diff of the most recent commit | After committing, before pushing |
| `branch` | `git diff main...HEAD` | Before merging a feature branch |

## When to Use

- Working directly on main and want a quality check before committing
- As a pre-push sanity check
- When the `/git commit` skill offers an optional review step
- Quick local review without opening a PR

## When NOT to Use

- PR is already open with CI review enabled - let CI handle it
- Reviewing someone else's code - use the GitHub PR review workflow instead
- Diff is very large, high-risk, or cross-package — escalate to `audit scope=package` instead of running review

## When to Escalate

Automatic escalation to `audit` (the deeper skill) is recommended when:

- The diff is larger than 300 LOC or touches more than 10 files
- The diff touches any of these high-risk areas:
  - `platform/backend/libs/session-manager/**`
  - `platform/backend/libs/agent-gateway/**`
  - `platform/backend/libs/vm-agent-*/**` (auth/session boundary)
  - `agent-framework/bridge/**` (adapter boundary)
  - `agent-framework/runner/**` (lifecycle + data isolation)
  - `agent-framework/workspace/**` (multi-tenant state)
  - `.github/**` (CI pipeline)
  - `**/prisma/schema.prisma` or `**/drizzle/**` (schema drift potential)

When these conditions are met, `review` should print a one-line notice and suggest the exact
`audit` invocation rather than producing a shallow diff-only review.

---

ROLE  Code reviewer for local changes in the skaile-dev monorepo. Reviews diffs for quality, security, performance, scope, and adherence to repo-specific conventions.

READS
  git diff (staged, last-commit, or branch depending on target)
  REVIEW.md in affected package roots (if present)
  CLAUDE.md in affected package roots (for conventions)

WRITES
  nothing - review is read-only, outputs findings to the user

MUST  read the full diff before making any findings
MUST  load REVIEW.md from each affected package root to apply repo-specific rules
MUST  categorize findings by severity (Important, Nit, Pre-existing)
MUST  cite specific file:line for each finding
MUST  keep the review concise - max 5 Nits, summarize if more
NEVER  make changes to files - this is a read-only review
NEVER  report issues that linters/formatters/type-checkers already catch
NEVER  report issues in files outside the diff
NEVER  flag test-only code for violating production rules

EMIT [review] started target=<target>

# ── Review Process ───────────────────────────────────────────────

STEP 1: Gather the Diff

  IF target = staged
    $ git diff --cached
    IF empty -> STOP: "No staged changes to review. Stage files with git add first."

  IF target = last-commit
    $ git diff HEAD~1..HEAD

  IF target = branch
    $ git diff main...HEAD
    IF empty -> STOP: "No changes relative to main."

STEP 2: Identify Affected Packages and Assess Risk

  Map changed file paths to their package roots:
  - platform/backend/libs/foo/bar.ts -> platform/
  - agent-framework/runner/src/baz.ts -> agent-framework/
  - ai-assets/skaileup-shared/contracts/x.md -> ai-assets/

  For each affected package root:
  - Read REVIEW.md if present (repo-specific review rules)
  - Read CLAUDE.md if present (conventions, architecture)

  Assess size and risk:
  - Count changed lines across the diff
  - Count changed files
  - Check if any path matches the high-risk list in "When to Escalate" above

  IF lines > 300 OR files > 10 OR any high-risk path touched:
    - Print:
      > "This diff crosses the threshold for a full audit.
      >  Recommend: audit scope=package target=<first-affected-package>
      >  Continue with shallow review anyway? (yes/no)"
    IF user says no → STOP, user will run audit
    IF user says yes → continue, but also print "⚠ diff exceeds review scope — audit recommended before merge"

STEP 3: Analyze Changes

  For each changed file, check:

  **Quality**
  - Clean code: readability, naming, single responsibility
  - Error handling: are failure modes covered?
  - Edge cases: null/undefined, empty arrays, boundary conditions
  - Dead code: unused imports, unreachable branches

  **Security**
  - Input validation at system boundaries (API routes, WebSocket handlers)
  - No secrets, credentials, or PII in code or logs
  - SQL/command injection (Prisma raw queries, child_process, exec)
  - Auth checks: are new endpoints properly guarded?
  - XSS: user content rendered without sanitization

  **Performance**
  - N+1 queries (Prisma includes, loops with DB calls)
  - Unbounded collections (missing pagination, no size limits)
  - Memory leaks (event listeners not cleaned up, unclosed streams)
  - Unnecessary re-renders (React deps arrays, memo boundaries)

  **Scope**
  - Changes are focused on a single concern
  - No unrelated refactoring mixed with feature changes
  - No accidental file inclusions (build artifacts, env files)

  **Repo-specific** (from REVIEW.md)
  - Apply all "Always check" rules from the package's REVIEW.md
  - Apply all "Do not report" exclusions

  IF focus was specified:
    Only report findings in the requested categories.
    Still scan everything, but only surface findings matching the focus.

STEP 4: Classify Findings

  Each finding gets a severity:

  | Severity | Icon | Meaning |
  |----------|------|---------|
  | Important | [!] | Would break behavior, leak data, or block rollback. Must fix. |
  | Nit | [~] | Minor improvement. Optional. Cap at 5 per review. |
  | Pre-existing | [*] | Bug in code the diff touches but did not introduce. Informational. |

STEP 5: Output Review

  Print the review in this format:

  ── Review (<target>) ───────────────────────────────────────────

  Packages: <list of affected package roots>
  Files changed: <count>
  Review focus: <focus or "full">

  [if findings exist]

  <severity-icon> <file>:<line> - <one-line summary>
    <brief explanation, 1-2 sentences max>

  [repeat for each finding, grouped by severity: Important first, then Nit, then Pre-existing]

  [if more than 5 Nits]
  Plus <N> similar nits not shown.

  [if no findings]
  No issues found. Changes look good.

  ── Summary ─────────────────────────────────────────────────────

  Important: <count>  Nit: <count>  Pre-existing: <count>

  [if Important > 0]
  Recommendation: Fix important issues before committing.

  [if Important = 0]
  Recommendation: Good to commit.

  ────────────────────────────────────────────────────────────────

EMIT [review] complete findings=<total_count> important=<important_count>

# ── End ──────────────────────────────────────────────────────────

## Integration

- **Called by:** `git` skill (optional review step in commit mode)
- **Reads:** REVIEW.md, CLAUDE.md from affected packages
- **Writes:** nothing (read-only)
