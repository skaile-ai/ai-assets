---
name: improving-codebase-architecture
description: Use when the user wants to improve architecture, find refactoring opportunities, consolidate tightly-coupled modules, or make a codebase more testable. Surfaces deepening opportunities informed by the project glossary and ADRs.
---

> Read `skaile-powers/references/config.md` before proceeding.

# Improve Codebase Architecture

Surface architectural friction and propose **deepening opportunities** — refactors that turn shallow modules into deep ones. The aim is testability and AI-navigability.

## Glossary

Use these terms exactly in every suggestion. Full definitions in [`skaile-powers/references/architecture-language.md`](../../../references/architecture-language.md).

Key principles:

- **Deletion test**: imagine deleting the module. If complexity vanishes, it was a pass-through. If complexity reappears across N callers, it was earning its keep.
- **The interface is the test surface.**
- **One adapter = hypothetical seam. Two adapters = real seam.**

This skill is _informed_ by the project's domain model. The domain language gives names to good seams; ADRs record decisions the skill should not re-litigate.

## Process

### 1. Explore

Read the project's domain glossary (`glossary/glossary.md` — see config.md) and any ADRs in `decisions/<category>/` (see config.md) in the area you're touching first.

Then use the Agent tool with `subagent_type=Explore` to walk the codebase. Don't follow rigid heuristics — explore organically and note where you experience friction:

- Where does understanding one concept require bouncing between many small modules?
- Where are modules **shallow** — interface nearly as complex as the implementation?
- Where have pure functions been extracted just for testability, but the real bugs hide in how they're called (no **locality**)?
- Where do tightly-coupled modules leak across their seams?
- Which parts of the codebase are untested, or hard to test through their current interface?

Apply the **deletion test** to anything you suspect is shallow: would deleting it concentrate complexity, or just move it? A "yes, concentrates" is the signal you want.

### 2. Present candidates

Present a numbered list of deepening opportunities. For each candidate:

- **Files** — which files/modules are involved
- **Problem** — why the current architecture is causing friction
- **Solution** — plain English description of what would change
- **Benefits** — explained in terms of locality and leverage, and also in how tests would improve

**Use the project glossary vocabulary for the domain, and [`skaile-powers/references/architecture-language.md`](../../../references/architecture-language.md) vocabulary for the architecture.** If the glossary defines "Order," talk about "the Order intake module" — not "the FooBarHandler," and not "the Order service."

**ADR conflicts**: if a candidate contradicts an existing ADR in `decisions/<category>/` (see config.md), only surface it when the friction is real enough to warrant revisiting the ADR. Mark it clearly (e.g. _"contradicts architecture/0007 — but worth reopening because…"_). Don't list every theoretical refactor an ADR forbids.

Do NOT propose interfaces yet. Ask the user: "Which of these would you like to explore?"

### 3. Grilling loop

Once the user picks a candidate, drop into the `grill-me` skill to walk the design tree with the user.

Side effects happen inline as decisions crystallize:

- **Naming a deepened module after a concept not in the glossary?** Add the term to the project glossary (`glossary/glossary.md` — see config.md) using the glossary format in `config.md`. Create the file lazily if it doesn't exist.
- **Sharpening a fuzzy term during the conversation?** Update the project glossary (`glossary/glossary.md` — see config.md) right there.
- **User rejects the candidate with a load-bearing reason?** Offer an ADR, framed as: _"Want me to record this as an ADR so future architecture reviews don't re-suggest it?"_ Only offer when the reason would actually be needed by a future explorer to avoid re-suggesting the same thing — skip ephemeral reasons ("not worth it right now") and self-evident ones. Write the ADR to `decisions/<category>/` using the decision (ADR) format in `config.md`.
- **ADR accepted?** Append a devlog entry — see config.md → Devlog.
- **Want to explore alternative interfaces for the deepened module?** See [INTERFACE-DESIGN.md](INTERFACE-DESIGN.md).

## Output

When the user picks a deepening candidate to act on:

- If the change is large enough to need a spec first, hand off to `brainstorming` — the grilling session will produce a numbered spec that feeds directly into `writing-plans`.
- If the shape is already clear and well-understood, hand off directly to `writing-plans` — so the implementation joins the skaile-powers numbered spec+bead thread rather than dead-ending in a loose suggestion list.

Do not leave the session with a suggestion list and no next step. Every accepted candidate must enter the workflow.
