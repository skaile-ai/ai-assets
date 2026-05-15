---
name: brainstorming
description: "You MUST use this before any creative work - creating features, building components, adding functionality, or modifying behavior. A grilling session that interrogates the idea against the project's domain model, sharpens terminology, captures decisions, and produces a numbered design spec."
---

> Read `skaile-powers/references/config.md` before proceeding.

# Brainstorming Ideas Into Designs

Turn an idea into a fully formed design through a **grilling session** — relentless, one-question-at-a-time interrogation that resolves every branch of the design tree. Along the way, sharpen the project's shared language and capture hard-to-reverse decisions. The session ends with an approved, numbered spec.

<HARD-GATE>
Do NOT invoke any implementation skill, write any code, or scaffold any project until you have presented a design and the user has approved it. This applies to EVERY project regardless of perceived simplicity.
</HARD-GATE>

## Anti-Pattern: "This Is Too Simple To Need A Design"

Every project goes through this process. A todo list, a single-function utility, a config change — all of them. "Simple" projects are where unexamined assumptions cause the most wasted work. The design can be short, but you MUST present it and get approval.

## Checklist

Create a task for each item and complete them in order:

1. **Explore project context** — files, docs, recent commits, the glossary, relevant ADRs
2. **Offer visual companion** (if visual questions are ahead) — its own message, see below
3. **Grill the idea** — interrogate one question at a time; maintain the glossary and ADRs inline
4. **Present the design** — in sections scaled to complexity, approval after each
5. **Write the numbered spec** — save to the specs path in `config.md`, commit it
6. **Spec self-review** — inline check for placeholders, contradictions, ambiguity, scope
7. **User reviews the written spec**
8. **Transition to implementation** — invoke `writing-plans`

## Process Flow

```
  Explore project context (incl. glossary + ADRs)
            │
   Visual questions ahead? ──yes──► Offer visual companion (own message)
            │ no                              │
            ▼                                 ▼
   ┌──────────────────────────────────────────────┐
   │  GRILL: one question at a time               │
   │   • recommend an answer for every question   │
   │   • explore the codebase instead of asking   │
   │     when the answer is already there         │
   │   • challenge terms against the glossary     │
   │   • update glossary + ADRs inline            │
   └────────────────────┬─────────────────────────┘
                         ▼
              Present design sections
                         │
           User approves? ──no──► revise, re-present
                         │ yes
                         ▼
            Write numbered spec ──► self-review (fix inline)
                         │
           User reviews spec ──changes──► revise
                         │ approved
                         ▼
              Invoke writing-plans
```

**The terminal state is invoking `writing-plans`.** It is the ONLY skill you invoke after brainstorming.

## The Grilling Session

This is the core of the skill. **Interview the user relentlessly about every aspect of the idea until you reach a genuine shared understanding.** Walk down each branch of the design tree, resolving dependencies between decisions one by one.

**Rules of the grill:**

- **One question per message.** Never batch. If a topic needs more exploration, split it across several turns.
- **Recommend an answer to every question.** Don't ask open-endedly — state your recommended answer and the reasoning, then let the user confirm or redirect. "I'd default to X because Y — agree?"
- **Explore instead of asking.** If a question can be answered by reading the codebase, read the codebase. Don't make the user tell you what the code already says.
- **Prefer multiple choice** when the options are discrete.
- **Stress-test with concrete scenarios.** When domain relationships are in play, invent specific edge-case scenarios that force precision about the boundaries between concepts.
- **Cross-reference with code.** When the user states how something works, check whether the code agrees. Surface contradictions: "Your code cancels whole Orders, but you just said partial cancellation is possible — which is right?"

**Scope check first.** Before detailed questions, assess scope. If the idea spans multiple independent subsystems, flag it immediately and help the user decompose into sub-projects — each gets its own spec → plan → implementation cycle. Don't grill the details of a project that needs decomposition first.

**Explore approaches.** Once the shape is clear, propose 2-3 approaches with trade-offs. Lead with your recommendation and the reasoning.

## Maintaining the Shared Language (inline, during the grill)

The project's glossary (location in `config.md`) is its **ubiquitous language**. Keep it sharp as you grill — don't batch these updates, capture them as decisions crystallize.

- **Challenge against the glossary.** When a term conflicts with an existing definition, call it out: "Your glossary defines 'cancellation' as X, but you seem to mean Y — which is it?"
- **Sharpen fuzzy language.** When the user uses a vague or overloaded term, propose a precise canonical one: "You're saying 'account' — do you mean the Customer or the User? Those are different things."
- **Update the glossary inline.** When a term is resolved, write it to the glossary right there, in the format defined in `config.md`. Create the glossary file lazily if it doesn't exist yet.

The glossary is a glossary — keep implementation details, specs, and decisions out of it.

## Capturing Decisions (ADRs, offered sparingly)

When the grill produces a decision that is **hard to reverse, surprising without context, and the result of a real trade-off** (all three — see `config.md`), offer to record an ADR. Write it to the right category folder (`architecture`, `design`, or `code-style`) using the format and numbering in `config.md`. If any of the three criteria is missing, skip it — most decisions don't need an ADR.

When an ADR is accepted, note that it must be mirrored into the decision log when the work lands (see `config.md` → decision-log integration).

## Presenting the Design

Once you understand what you're building, present the design:

- Scale each section to its complexity — a few sentences if straightforward, up to 200-300 words if nuanced
- Ask after each section whether it looks right
- Cover: architecture, components, data flow, error handling, testing
- Use the glossary's vocabulary throughout

**Design for isolation and clarity.** Break the system into units with one clear purpose, a well-defined interface, and the ability to be understood and tested independently. Favor **deep modules** — a lot of behavior behind a small, stable interface. For each unit, you should be able to answer: what does it do, how do you use it, what does it depend on? When a file would grow large, that's a signal it's doing too much.

**In existing codebases:** explore the structure first and follow established patterns. Include targeted improvements where existing problems affect the work; don't propose unrelated refactoring.

## Writing the Spec

After the user approves the design, write the spec to the specs path in `config.md`, numbered: `NNNN-<topic>.md`. Scan the specs directory for the highest existing number and increment.

Commit the spec to git.

**Spec self-review** — read it with fresh eyes:

1. **Placeholder scan** — any "TBD", "TODO", vague requirements? Fix them.
2. **Internal consistency** — do sections contradict each other?
3. **Scope check** — focused enough for a single plan, or does it need decomposition?
4. **Ambiguity check** — could any requirement be read two ways? Pick one, make it explicit.
5. **Language check** — does the spec use the glossary's canonical terms?

Fix issues inline.

**User review gate:**

> "Spec written and committed to `<path>`. Please review it and let me know if you want changes before we move to the implementation plan."

Wait for the response. If changes are requested, make them and re-run the self-review. Only proceed once the user approves.

## Transition to Implementation

Invoke `writing-plans` to create the implementation plan. Do NOT invoke any other skill.

## Visual Companion

A browser-based companion for showing mockups, diagrams, and visual options. Available as a tool, not a mode — accepting it means it's available for questions that benefit from visual treatment, not that every question goes through the browser.

**Offering it:** when you anticipate visual content ahead (mockups, layouts, diagrams), offer it once, as its own message with no other content:

> "Some of what we're working on might be easier to explain if I can show it to you in a web browser. I can put together mockups, diagrams, comparisons, and other visuals as we go. This feature is still new and can be token-intensive. Want to try it? (Requires opening a local URL)"

Wait for the response. If they decline, proceed text-only.

**Per-question decision:** even after the user accepts, decide per question. The test: would the user understand this better by seeing it than reading it? Use the browser for mockups, wireframes, layout comparisons, architecture diagrams. Use the terminal for requirements questions, conceptual choices, trade-off lists, scope decisions.

If they accept, read `skaile-powers/skills/planning/brainstorming/visual-companion.md` before proceeding.

## Key Principles

- **One question at a time** — never overwhelm
- **Always recommend an answer** — never ask open-endedly
- **Explore before asking** — the codebase answers many questions
- **Sharpen the language** — a precise shared vocabulary pays off every session
- **YAGNI ruthlessly** — cut unnecessary features from every design
- **Be flexible** — go back and clarify when something doesn't fit
