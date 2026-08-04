---
name: "meat"
description: "[skaile-development] Distill a git diff into a 'reading diff' for a
  human: a judgment-call header plus an abridged diff whose every line is verbatim
  source - mechanical noise (renames, lockfiles, generated files, formatting,
  import churn) collapsed or dropped. Comprehension, not critique - it does not
  hunt defects. Use when the user wants to understand a diff rather than review
  it: before approving a merge, catching up on a branch, reading agent-written
  code, or when the ship skill offers a reading diff at its final merge question.
  Concept and rubric from boldsoftware/meat, reimplemented as a prompt - no
  binary, no extra API key."
version: 1.0.0
metadata:
  tags:
  - "meat"
  - "reading-diff"
  - "diff"
  - "comprehension"
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

> Abridge a code diff into a **reading diff** (concept + abridging rubric distilled
> from [boldsoftware/meat](https://github.com/boldsoftware/meat)). The reader is a
> senior engineer reading a diff of GOOD code - it compiles and its tests pass; they
> are NOT hunting for nil panics or sweating details. They want to understand the
> change at a high level: what changed, where data comes from and goes, what new
> control flow or behavior appeared. Comprehension, not critique - for defect
> hunting use the `review` skill.

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
  git diff / diff --stat / log for the resolved range (always with `-M` for rename/move detection)
  surrounding source files only when it would change your judgment about whether
  a line is load-bearing (or whether a file is generated) - do NOT over-investigate

WRITES  nothing

MUST  resolve the range first: input `range` ("staged" -> `--cached`) else auto
      (branch != default branch -> `origin/<default_branch>...HEAD`, else `HEAD~1..HEAD`)
MUST  classify mechanical noise from `git diff --stat -M <range>` BEFORE reading hunks:
      lockfiles, generated files (postxl-lock-listed, `*.generated.*`, snapshots,
      "Code generated" headers), pure renames, formatting-only files - collapse
      these unread to one line each; keep the hand-written change that drove a
      regeneration
MUST  render the reading-diff body from verbatim source only: every retained line
      is copied exactly from the original diff; a folded block becomes exactly one
      correctly marked and indented `...` row; an in-line elision may only delete
      characters, with every omitted span shown as `...`
MUST  when unsure whether a line matters, KEEP it
MUST  self-check once before emitting: reread the result; if it is still
      mechanically verbose, compress one more pass - but keep uncertain or
      semantically distinct code
MUST  for large diffs (> ~1500 substantive changed lines): chunk at file boundaries
      into groups of <= ~1500 lines, dispatch a fresh subagent per chunk with the
      abridging rules + output format below, then merge into ONE reading diff
NEVER invent or alter program logic in the reading diff - no authored lines,
      identifiers, comments, or reordering; removal and compression are allowed,
      lying is not
NEVER report style nits, lint findings, or defect verdicts - that is `review`'s job
NEVER modify files, stage, commit, or touch git state

STEP 1: Resolve range + stat
  $ git diff --stat -M <range>   (and `git log --oneline <range>` for multi-commit ranges)
  Bucket every file: SUBSTANTIVE | MECHANICAL (generated / lockfile / rename / format-only).

STEP 2: Abridge the substantive files ($ git diff -M <range>)
  Small (<= ~1500 lines): read the diff directly. Larger: chunk-and-merge per MUST above.
  IF `focus` given: expand that area, compress the rest harder.

  Abridging rules (keep / collapse / drop):
  1. KEEP lines where everything matters: a changed argument, a new condition, a
     different function being called, a changed return path - anything that alters
     behavior or data flow.
  2. COLLAPSE mechanical repetition: keep the semantic anchor that names the
     operation, fold the repeated members/calls/cases into one `...` row. For a
     rename or call-site migration repeated across hunks, keep ONE representative
     old/new pair and note the multiplier ("+11 more call sites"); retain another
     only when it exposes a distinct condition, transformation, or effect.
  3. ELIDE error-message construction: keep the control flow and the erroring call,
     replace the message arguments with `...` (reads as `t.Errorf(...)`) - unless
     error identity, type, wrapping, or status is itself what changed.
  4. DROP by default: import/require/use churn (never even mention it), the standard
     context rows, blank lines, formatter realignment, forced zero-value plumbing,
     changelog-style comments and line-by-line narration. KEEP comments that carry
     contracts, security/compatibility caveats, or non-obvious rationale. Keep a
     context row only when it identifies the owning definition, closes a retained
     construct, or shows control flow needed to read the change.
  5. MOVES read as moves: with `-M` detection, present a relocated block as
     relocation ("moved, unchanged" or both sides folded identically) - never as a
     one-sided deletion plus an unexplained addition.
  6. Preserve orientation: keep `diff --git` / `---` / `+++` / `@@` lines for
     partially retained files; when an entire file is noise, drop its whole section
     rather than leaving orphan headers. A diff may legitimately abridge to
     NOTHING - say so instead of padding.

STEP 3: Emit the reading diff
  ````
  ── Reading diff: <range> (<N> files, +<A>/-<D>) ─────────────────
  TL;DR: <2-4 sentences: what this change does, conceptually>

  Decisions that need human judgment:
  - <decision> - <why it is a judgment call> (<file refs>)

  Mechanical (collapsed, not shown below):
  - <group>: <one line, e.g. "bun.lock regenerated", "12 files: rename only">

  Surprises / risks:
  - <anything unexpected - or "none">

  ```diff
  <the abridged diff: verbatim retained lines, `...` fold rows, headers per rule 6>
  ```
  ─────────────────────────────────────────────────────────────────
  ````
  Omit an empty prose section rather than writing "none" (except Surprises - state
  "none" explicitly there; its absence is information). The diff body carries the
  behavior changes; do not restate them as prose.

## Common Mistakes

| Mistake | Instead |
|---------|---------|
| Drifting into defect review ("this could NPE") | Comprehension only; point the user at `review` |
| Authoring or paraphrasing code lines in the body | Only delete, fold (`...`), or elide within a line - every kept character is verbatim source |
| Reading generated/lockfile hunks | Classify from `--stat` and collapse unread |
| Showing all 12 hunks of a mechanical rename | One representative pair + a multiplier note |
| A moved block shown as delete + add | Present it as relocation (rule 5) |
| Output as long as the diff | Abridge harder - then self-check once more |
| Per-file narration | The prose header is conceptual; the diff body is the detail |

## Integration

- **Called by:** the `ship` skill (optional "Reading diff first" option at its final
  merge question, when this skill is installed); usable standalone via `/meat`
- **Reads:** git state only · **Writes:** nothing
- **Provenance:** abridging rubric distilled from boldsoftware/meat's system prompt
  (its coordinate-based edit-plan compiler and mechanical validation are not
  reproducible in a prompt - approximated by the verbatim-lines rules above)
