---
name: grill-me
description: A grilling discussion that stress-tests a plan or design and hardens the project's shared language. Interviews you relentlessly one question at a time, resolves every branch of the decision tree, and updates the glossary and ADRs inline. Use when you want to think something through or sharpen terminology WITHOUT committing to a full spec. Triggers on "grill me", "stress-test this", "let's talk this through".
---

> Read `skaile-powers/references/config.md` before proceeding.

# Grill Me

A grilling **discussion** — not a full design cycle. Interrogate the user about a plan, idea, or fuzzy concept until you reach genuine shared understanding, and capture what crystallizes into the project's domain docs (glossary + ADRs).

## When to Use This vs. Brainstorming

| Use `grill-me` | Use `brainstorming` |
|---|---|
| Think something through, no commitment | Design a feature you intend to build |
| Sharpen terminology, resolve a fuzzy concept | Produce an approved, numbered spec |
| Stress-test an existing plan or assumption | Drive toward implementation |

`grill-me` produces **no spec** and does **not** hand off to `writing-plans`. Its only durable outputs are updates to the glossary and, when warranted, ADRs. If the discussion reveals you actually want to build something, switch to `brainstorming`.

## The Grill

Interview the user relentlessly about every aspect of the topic until you reach a shared understanding. Walk down each branch of the decision tree, resolving dependencies between decisions one by one.

- **One question per message.** Never batch.
- **Recommend an answer to every question.** State your recommended answer and reasoning, then let the user confirm or redirect.
- **Explore instead of asking.** If a question can be answered by reading the codebase, read the codebase.
- **Prefer multiple choice** when options are discrete.
- **Stress-test with concrete scenarios.** Invent specific edge cases that force precision about the boundaries between concepts.
- **Cross-reference with code.** When the user states how something works, check the code agrees and surface contradictions.

## Maintaining the Domain Docs (inline)

This is what makes `grill-me` more than a conversation — it leaves the project's shared language sharper than it found it. Capture these as decisions crystallize; don't batch them.

**Glossary** (location and format in `config.md`):

- **Challenge against the glossary.** When a term conflicts with an existing definition, call it out.
- **Sharpen fuzzy language.** When a term is vague or overloaded, propose a precise canonical one and explain the distinction.
- **Update the glossary inline.** When a term is resolved, write it right there. Create the glossary file lazily if it doesn't exist.

**ADRs** (categories, format, and the 3-criteria test in `config.md`):

- When the grill produces a decision that is **hard to reverse, surprising without context, and a real trade-off**, offer to record an ADR in the right category (`architecture`, `design`, `code-style`).
- If any of the three criteria is missing, skip it.
- When an ADR is accepted, note it must be mirrored into the decision log when related work lands (see `config.md`). Also append an entry to `devlog/DEVLOG.md` (see `config.md` → Devlog) linking the ADR.

## Ending the Session

When the decision tree is resolved, summarize what was settled and which domain docs were updated. Do not invoke another skill — the user decides what happens next.
