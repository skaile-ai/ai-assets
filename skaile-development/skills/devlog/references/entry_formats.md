# Devlog Entry Formats

Quick reference for writing devlog entries and reports. See `SKILL.md` for the full workflow.

---

## Short Entry (DEVLOG.md)

**Use for:** every meaningful change. Prepend to `_devlog/DEVLOG.md`.

```markdown
## YYYY-MM-DD — <Title>

**What changed:** <what is different now>

**Why:** <motivation or problem solved>

**Affected:** `<package>`, `<package>`

**Implications:** <downstream effects, breaking changes, or "None">

---
```

**Good titles:** Action + subject, plain language.
- "Add workspace rename to forge/project"
- "Fix session token expiry race condition in platform backend"
- "Move shared auth logic into forge/common-backend"
- "Add agent-framework/connectors package"

**Bad titles:** technical jargon, vague, or commit-message style.
- "feat: add rename command" ← commit style
- "Update stuff" ← too vague
- "Refactor UUID handling in drizzle schema" ← jargon-heavy

---

## Detailed Report Template (`_devlog/reports/<date>-<topic>.md`)

**Use for:** shared contract changes, architecture shifts, breaking changes affecting 3+ packages.

```markdown
# <Title> — Detailed Change Report

**Date:** YYYY-MM-DD
**Change type:** contract | architecture | breaking-api | paradigm | security
**Affected packages:** `pkg-a`, `pkg-b`, `pkg-c`
**Status:** complete | in-progress | proposed

---

## Summary

2–3 sentences. What changed and why. Plain language.

## What Was There Before

Describe the old state. Code snippets welcome.

## What Is There Now

Describe the new state. Code snippets welcome.

## Why This Changed

Full rationale. What problem it solves. What alternatives were rejected.

## Implications

### Breaking Changes

- **`<package>`**: <what breaks and what to do>

### Migration Guide

Step-by-step for adapting to the change. Or: "No migration required."

### Downstream Effects

What else needs updating as a consequence.

## What to Check After This Change

- [ ] Run full test suite (`bun x --bun vitest run`)
- [ ] Verify <specific behavior> still works
- [ ] Review <related file> for stale references

## Related

- Commit: `<hash>`
- Contract: `skaileup-shared/contracts/<file>.md`
- CLAUDE.md: `<package>/CLAUDE.md#<section>`
```

---

## Linking Report from DEVLOG.md Entry

When a report exists, add a link line to the short entry:

```markdown
## 2026-03-25 — Redesign agent dispatch protocol

**What changed:** ...

**Why:** ...

**Affected:** `agent-framework/bridge`, `agent-framework/runner`, `skaileup-shared`

**Implications:** Breaking change for all custom agent drivers. See migration guide.

**Report:** [Full analysis and migration guide](_devlog/reports/2026-03-25-agent-dispatch-redesign.md)

---
```

---

## Implications Writing Guide

The implications line is the most important part for future readers. Write it as:

**No implications:** "None. Fully backward-compatible."

**Soft implications:** "Developers adding new features should now use X instead of Y. Existing code continues to work."

**Hard implications (migration required):** "Breaking change. Existing sessions are invalidated. Run `bun run migrate` before deploying. See the detailed report for step-by-step migration."

**Cascading implications:** "This changes how all skills in skaileup-quality domain interpret acceptance criteria. Review the updated contract before writing new skills."
