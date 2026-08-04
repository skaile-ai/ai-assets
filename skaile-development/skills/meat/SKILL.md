---
name: "meat"
description: "[skaile-development] Distill a git diff into a 'reading diff' for a
  human: the concepts, algorithm choices, architecture decisions, and behavior
  changes that need human judgment, with mechanical noise (renames, lockfiles,
  generated files, formatting, import shuffles) collapsed to one-liners. Comprehension,
  not critique - it does not hunt defects. Use when the user wants to understand a
  diff rather than review it: before approving a merge, catching up on a branch,
  reading agent-written code, or when the ship skill offers a reading diff at its
  final merge question. Concept from boldsoftware/meat, reimplemented as a prompt -
  no binary, no extra API key."
version: 1.0.0
metadata:
  tags:
  - "meat"
  - "reading-diff"
  - "diff"
  - "comprehension"
  - "code-review"
  - "skaile-development"
  source: "NEW"
  stage: "alpha"
  user_inputs:
    dialog:
    - id: "range"
      label: "Diff range (git range, or 'staged'). Empty = auto"
      type: "text"
      required: false
      hint: "e.g. origin/main...HEAD, HEAD~3..HEAD, abc123..def456, staged. Auto:
        on a feature branch -> origin/<default_branch>...HEAD; on the default
        branch -> last commit."
    - id: "focus"
      label: "Optional focus area"
      type: "text"
      required: false
      hint: "e.g. 'the auth changes' - weights the reading diff toward one area"
    files: []
---

# Meat - Reading Diff

> Abridge a code diff into a **reading diff** (concept: [boldsoftware/meat](https://github.com/boldsoftware/meat)):
> surface what needs human judgment, collapse what doesn't. This is comprehension,
> not critique - for defect hunting use the `review` skill.

## When to Use

- Before approving a merge (the `ship` skill offers this at its final question)
- Catching up on a branch or a batch of agent-written commits
- Any "what does this diff actually do?" question

## When NOT to Use

- You want defects found - use `review`
- The diff is a handful of lines - just read it

---

ROLE  Diff abridger. Read-only: produces a report, changes nothing.

READS
  git diff / diff --stat / log for the resolved range
  surrounding source files only when a hunk is unintelligible without context

WRITES  nothing

MUST  resolve the range first: input `range` ("staged" -> `--cached`) else auto
      (branch != default branch -> `origin/<default_branch>...HEAD`, else `HEAD~1..HEAD`)
MUST  classify mechanical noise from `git diff --stat -M <range>` BEFORE reading hunks:
      lockfiles, generated files (postxl-lock-listed, `*.generated.*`, snapshots),
      pure renames, formatting-only files - collapse these unread to one line each
MUST  keep the output substantially shorter than the diff - if it isn't, abridge harder
MUST  ground every judgment bullet in file references (`path:line` where useful)
MUST  for large diffs (> ~1500 substantive changed lines): chunk at file boundaries
      into groups of <= ~1500 lines, dispatch a fresh subagent per chunk with the
      abridging prompt + output format below, then merge into ONE reading diff
NEVER report style nits, lint findings, or defect verdicts - that is `review`'s job
NEVER modify files, stage, commit, or touch git state

STEP 1: Resolve range + stat
  $ git diff --stat -M <range>   (and `git log --oneline <range>` for multi-commit ranges)
  Bucket every file: SUBSTANTIVE | MECHANICAL (generated / lockfile / rename / format-only).

STEP 2: Read + abridge the substantive files
  Small (<= ~1500 lines): read the diff directly. Larger: chunk-and-merge per MUST above.
  While reading, collect: conceptual intent, decisions a human should weigh
  (architecture, algorithm choice, API shape, data model, security posture),
  observable behavior changes (incl. migrations, flags, config), and surprises
  (scope creep, TODOs, commented-out code, silent behavior shifts).
  IF `focus` given: expand that area, compress the rest harder.

STEP 3: Emit the reading diff
  ```
  ── Reading diff: <range> (<N> files, +<A>/-<D>) ─────────────────
  TL;DR: <2-4 sentences: what this change does, conceptually>

  Decisions that need human judgment:
  - <decision> - <why it is a judgment call> (<file refs>)

  Behavior changes:
  - <observable change, migration, flag, config> (<file refs>)

  Mechanical (collapsed):
  - <group>: <one line, e.g. "bun.lock regenerated", "12 files: import shuffle">

  Surprises / risks:
  - <anything unexpected - or "none">
  ─────────────────────────────────────────────────────────────────
  ```
  Omit an empty section rather than writing "none" (except Surprises - state "none"
  explicitly there; its absence is information).

## Common Mistakes

| Mistake | Instead |
|---------|---------|
| Drifting into defect review ("this could NPE") | Comprehension only; point the user at `review` |
| Reading generated/lockfile hunks | Classify from `--stat` and collapse unread |
| Output as long as the diff | Abridge harder - the reading diff must compress |
| Per-file narration | Organize by concept, not by file |

## Integration

- **Called by:** the `ship` skill (optional "Reading diff first" option at its final
  merge question, when this skill is installed); usable standalone via `/meat`
- **Reads:** git state only · **Writes:** nothing
