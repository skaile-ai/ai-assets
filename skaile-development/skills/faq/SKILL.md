---
name: "faq"
description: "[skaile-development] Evaluates resolved questions about the skaile-dev
  monorepo for FAQ-worthiness and curates entries. Broadened to all monorepo packages."
metadata:
  tags:
  - "faq"
  - "documentation"
  - "curation"
  - "skaile-development"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    inputs_required:
    - key: QUESTION
      prompt: "What was the user's question?"
      description: "The original question"
    - key: ANSWER
      prompt: "What was the resolved answer?"
      description: "The answer that was provided to the user"
    inputs_optional:
    - key: CONTEXT
      prompt: "Which packages or areas did this question touch?"
      description: "Package names or areas relevant to the question (e.g., bridge,
        runner, flow-engine, forge, platform)"
    reads:
    - "*/docs/faq.md"
    - "**/docs/**/*.md"
    - "*/README.md"
    produces:
    - path: "<area>/docs/faq.md"
      description: "FAQ page with curated Q&A entries appended"
  user_inputs:
    dialog:
    - id: "question"
      label: "The user's original question"
      type: "text"
      required: true
    - id: "answer"
      label: "The resolved answer"
      type: "text"
      required: true
    - id: "context"
      label: "Packages or areas the question touched (comma-separated)"
      type: "text"
      required: false
    files: []
---

# FAQ Curation

## Overview

Evaluates whether a resolved question belongs in the FAQ documentation for the relevant
monorepo area. Called by the `skaile-development` after resolving a question during
a normal conversation. Proposes the entry to the user before writing.

## When to Use

- When the workspace advisor has just resolved a question about any monorepo package
- When a user explicitly asks to add a Q&A to the FAQ

## When NOT to Use

- For documentation updates unrelated to FAQ — use `doc`
- For devlog entries — use `devlog`

---

ROLE  FAQ curator for the skaile-dev monorepo. You evaluate resolved Q&A pairs
      against existing documentation and decide whether they belong in the FAQ
      for the relevant area.

## FAQ File Resolution

Determine the target FAQ file from the context input:

| Area | FAQ Path |
|---|---|
| agent-framework | `agent-framework/docs/faq.md` |
| forge | `forge/docs/faq.md` |
| platform | `platform/docs/faq.md` |
| ai-assets | `ai-assets/docs/faq.md` |

If context maps to multiple areas or is ambiguous, ask the user which FAQ file to target.

READS
  <area>/docs/faq.md                           — existing FAQ entries (check for duplicates)
  <area>/**/docs/**/*.md                       — existing package docs (check if already covered)
  <area>/*/README.md                           — package READMEs (check if already covered)

WRITES
  <area>/docs/faq.md                           — append new Q&A entry (only after user approval)

MUST  resolve the target FAQ file from context before checking for duplicates
MUST  check existing faq.md for duplicate or near-duplicate questions before proposing
MUST  check existing package docs to verify the answer isn't already well-covered
MUST  present the formatted entry to the user and wait for explicit approval before writing
MUST  append new entries at the end of faq.md (preserve existing entries)
MUST  use the simple Q&A format (### heading = question, body = answer)
NEVER write to faq.md without user approval
NEVER add entries that duplicate existing documentation
NEVER modify or reorder existing FAQ entries when appending

EMIT [faq] started

# ── Step 1: Check for Duplicates ────────────────────────────────

Resolve the target FAQ file from the context input using the FAQ File Resolution table above.

Read the target FAQ file (e.g., `agent-framework/docs/faq.md`).

Scan all existing entries. If the QUESTION is a duplicate or near-duplicate of an
existing entry (same topic, same answer), stop:

  EMIT [faq] skipped reason="duplicate of existing entry"
  → Tell the user: "This is already covered in the FAQ: <existing question heading>"
  → Done.

# ── Step 2: Check Existing Documentation ────────────────────────

Check whether the ANSWER is already well-covered in existing package documentation.
Read the relevant docs based on CONTEXT (or scan broadly if no context provided):

  - `<area>/<package>/docs/concepts.md`
  - `<area>/<package>/docs/index.md`
  - `<area>/<package>/README.md`
  - `<area>/<package>/docs/developer-guide/index.md`

"Well-covered" means the information is clearly stated and easy to find — not buried
in a tangential paragraph.

# ── Step 3: Evaluate FAQ-Worthiness ─────────────────────────────

Apply these three criteria:

| # | Criterion | Question to ask yourself |
|---|-----------|------------------------|
| 1 | Not already documented | Is this answer clearly stated in existing docs, or only implicit/buried? |
| 2 | Likely recurrent | Would another developer working with the codebase plausibly ask this? |
| 3 | Non-obvious | Is the answer something you wouldn't know just from reading the code or type signatures? |

**All three must be true** for the entry to be FAQ-worthy.

IF not FAQ-worthy:
  EMIT [faq] skipped reason="<which criterion failed>"
  → Tell the user which criterion failed and why. Be brief.
  → Done.

# ── Step 4: Propose Entry ──────────────────────────────────────

Format the entry as a simple Q&A pair:

```markdown
### <Question — phrased as a clear, direct question>

<Answer — concise, accurate, in plain language. Include package names or code
references where helpful. Keep to 1–3 paragraphs.>
```

Present the formatted entry to the user:

  > "This looks like a good FAQ candidate. Here's the proposed entry:
  >
  > <formatted entry>
  >
  > Should I add this to the FAQ?"

Wait for the user's response.

IF user declines:
  EMIT [faq] skipped reason="user declined"
  → Done.

# ── Step 5: Append to FAQ ──────────────────────────────────────

IF user approves:
  - Read the target FAQ file again (ensure no concurrent edits)
  - Append the formatted entry at the end of the file
  - Ensure a blank line separates the new entry from the previous content

EMIT [faq] entry_added question="<short question summary>"

# ── Step 6: Initialize FAQ Page (if needed) ─────────────────────

IF the target FAQ file does not exist, create it before appending:

```markdown
---
title: <Area> FAQ
description: Frequently asked questions about <area>.
---

Answers to common questions about <area> — how packages relate,
why certain design decisions were made, and how to work with the codebase.
```

Then proceed with Step 5.

---

CHECKLIST
  - [ ] Target FAQ file resolved from context
  - [ ] Checked faq.md for duplicate entries
  - [ ] Checked package docs for existing coverage
  - [ ] All three FAQ-worthiness criteria evaluated
  - [ ] Entry formatted as simple ### Q / answer pair
  - [ ] User explicitly approved the entry before writing
  - [ ] Entry appended (not prepended) to faq.md
  - [ ] Existing entries not modified

## Integration

- **Called by:** `skaile-development` (after resolving questions about any monorepo area)
- **Calls:** none
- **Related:** `doc` (general documentation), `devlog` (change log)
