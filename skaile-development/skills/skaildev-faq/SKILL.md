---
name: "skaildev-faq"
description: "Evaluates resolved agent-framework questions for FAQ-worthiness and curates entries in agent-framework/docs/faq.md. Called by the workspace advisor after resolving a framework question — proposes entries for user approval before writing."
metadata:
  version: "1.0.0"
  tags:
    - "faq"
    - "documentation"
    - "agent-framework"
    - "curation"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    inputs_required:
      - key: QUESTION
        prompt: "What was the user's question?"
        description: "The original question about the agent framework"
      - key: ANSWER
        prompt: "What was the resolved answer?"
        description: "The answer that was provided to the user"
    inputs_optional:
      - key: CONTEXT
        prompt: "Which packages or areas did this question touch?"
        description: "Package names or areas relevant to the question (e.g., bridge, runner, flow-engine)"
    reads:
      - agent-framework/docs/faq.md
      - "agent-framework/*/docs/**/*.md"
      - "agent-framework/*/README.md"
    produces:
      - path: "agent-framework/docs/faq.md"
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

# skaildev-faq — Agent Framework FAQ Curation

## Overview

Evaluates whether a resolved question about the Skaile Agent Framework belongs in the
FAQ documentation. Called by the `skaile-workspace-advisor` after resolving a framework
question during a normal conversation. Proposes the entry to the user before writing.

## When to Use

- When the workspace advisor has just resolved a question about the agent framework
- When a user explicitly asks to add a Q&A to the FAQ

## When NOT to Use

- For questions about forge apps, platform, or ai-resources — this is agent-framework only
- For documentation updates unrelated to FAQ — use `skaildev-doc`
- For devlog entries — use `skaildev-devlog`

---

ROLE  FAQ curator for the Skaile Agent Framework. You evaluate resolved Q&A pairs
      against existing documentation and decide whether they belong in the FAQ.

READS
  agent-framework/docs/faq.md                    — existing FAQ entries (check for duplicates)
  agent-framework/*/docs/**/*.md                 — existing package docs (check if already covered)
  agent-framework/*/README.md                    — package READMEs (check if already covered)

WRITES
  agent-framework/docs/faq.md                    — append new Q&A entry (only after user approval)

MUST  check existing faq.md for duplicate or near-duplicate questions before proposing
MUST  check existing package docs to verify the answer isn't already well-covered
MUST  present the formatted entry to the user and wait for explicit approval before writing
MUST  append new entries at the end of faq.md (preserve existing entries)
MUST  use the simple Q&A format (### heading = question, body = answer)
NEVER write to faq.md without user approval
NEVER add entries that duplicate existing documentation
NEVER modify or reorder existing FAQ entries when appending

EMIT [skaildev-faq] started

# ── Step 1: Check for Duplicates ────────────────────────────────

Read `agent-framework/docs/faq.md`.

Scan all existing entries. If the QUESTION is a duplicate or near-duplicate of an
existing entry (same topic, same answer), stop:

  EMIT [skaildev-faq] skipped reason="duplicate of existing entry"
  → Tell the user: "This is already covered in the FAQ: <existing question heading>"
  → Done.

# ── Step 2: Check Existing Documentation ────────────────────────

Check whether the ANSWER is already well-covered in existing package documentation.
Read the relevant docs based on CONTEXT (or scan broadly if no context provided):

  - `agent-framework/<package>/docs/concepts.md`
  - `agent-framework/<package>/docs/index.md`
  - `agent-framework/<package>/README.md`
  - `agent-framework/<package>/docs/developer-guide/index.md`

"Well-covered" means the information is clearly stated and easy to find — not buried
in a tangential paragraph.

# ── Step 3: Evaluate FAQ-Worthiness ─────────────────────────────

Apply these three criteria:

| # | Criterion | Question to ask yourself |
|---|-----------|------------------------|
| 1 | Not already documented | Is this answer clearly stated in existing docs, or only implicit/buried? |
| 2 | Likely recurrent | Would another developer working with the framework plausibly ask this? |
| 3 | Non-obvious | Is the answer something you wouldn't know just from reading the code or type signatures? |

**All three must be true** for the entry to be FAQ-worthy.

IF not FAQ-worthy:
  EMIT [skaildev-faq] skipped reason="<which criterion failed>"
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
  > Should I add this to the agent-framework FAQ?"

Wait for the user's response.

IF user declines:
  EMIT [skaildev-faq] skipped reason="user declined"
  → Done.

# ── Step 5: Append to FAQ ──────────────────────────────────────

IF user approves:
  - Read `agent-framework/docs/faq.md` again (ensure no concurrent edits)
  - Append the formatted entry at the end of the file
  - Ensure a blank line separates the new entry from the previous content

EMIT [skaildev-faq] entry_added question="<short question summary>"

# ── Step 6: Initialize FAQ Page (if needed) ─────────────────────

IF `agent-framework/docs/faq.md` does not exist, create it before appending:

```markdown
---
title: FAQ
description: Frequently asked questions about the Skaile Agent Framework.
---

Answers to common questions about the agent framework — how packages relate,
why certain design decisions were made, and how to work with the runtime stack.
```

Then proceed with Step 5.

---

CHECKLIST
  - [ ] Checked faq.md for duplicate entries
  - [ ] Checked package docs for existing coverage
  - [ ] All three FAQ-worthiness criteria evaluated
  - [ ] Entry formatted as simple ### Q / answer pair
  - [ ] User explicitly approved the entry before writing
  - [ ] Entry appended (not prepended) to faq.md
  - [ ] Existing entries not modified

## Integration

- **Called by:** `skaile-workspace-advisor` (after resolving agent-framework questions)
- **Calls:** none
- **Related:** `skaildev-doc` (general documentation), `skaildev-devlog` (change log)
