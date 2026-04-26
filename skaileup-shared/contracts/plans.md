# PLANS.md Convention

Plans are first-class artifacts that track multi-step work across sessions.
They are checked into the repo alongside `_concept/` and survive interruptions,
handoffs, and agent restarts.

## Plan Types

### Concept Plan

Tracks progress through the concept pipeline.
Written by the `concept-orchestrator` skill at session start.
Updated at each checkpoint.

**Location:** `PLANS.md` in project root (or `_concept/PLANS.md`)

### Implementation Plan

Tracks progress through coding, testing, and deployment.
Written by coding agents after the concept pipeline is complete.
References concept artifacts as source of truth.

**Location:** `PLANS.md` in project root (appended below concept plan,
or as a separate section)

---

## Format

```markdown
# Plans

## Concept Plan: <App Name>

### Scope

<One-paragraph description of what this concept session covers>

### Progress

- [x] discovery — approved YYYY-MM-DD
- [x] discovery/2_research — skipped (reason)
- [ ] experience/features — in progress (3 of 5 groups done)
- [ ] discovery/brand — not started
- [ ] blueprint — not started
- [ ] blueprint/architecture.md — not started
- [ ] blueprint/datamodel — blocked (requires features, techstack)
- [ ] experience/screens — blocked (requires features, brand, datamodel)

### Decisions

- YYYY-MM-DD: <decision and reasoning>

### Open Questions

- <question that needs user input>

### Blockers

- <what's blocking and what's needed to unblock>

---

## Implementation Plan: <App Name>

### Scope

<What will be built, what's out of scope for this phase>

### Source Artifacts

- Brief: _concept/discovery/brief.md
- Features: _concept/experience/features/ (N features, M must-have)
- Tech stack: _concept/blueprint/techstack.md
- Data model: _concept/blueprint/datamodel/model.json (N entities)
- Screens: _concept/experience/screens/ (N screens)
- Brand: _concept/discovery/brand/tokens.json

### Progress

- [ ] Project scaffold (scaffold)
- [ ] Foundation (brand tokens, auth config, app shell) (foundation)
- [ ] Infrastructure (if architecture specifies custom modules) (infrastructure)
  - [ ] Shared contracts: <list from architecture>
  - [ ] Providers: <list from architecture> (real + in-memory)
  - [ ] Platform services: <list from architecture>
  - [ ] Communication: <protocols from architecture>
- [ ] Database migrations (migrate)
- [ ] Seed data (seed)
- [ ] Feature: 01_user_auth (implement-feature)
  - [ ] Login screen
  - [ ] Registration screen
  - [ ] Password reset
- [ ] Feature: 02_dashboard (implement-feature)
  - [ ] Overview screen
- [ ] E2E tests (e2e)
- [ ] Deploy

### Implementation Decisions

- YYYY-MM-DD: <technical decision and reasoning>

### Known Technical Debt

- <item> — <why it was deferred> — <when to address>

### Verification

- [ ] All must-have features implemented
- [ ] audit passes (0 critical, 0 high)
- [ ] e2e passes for all ready features
- [ ] Responsive on mobile, tablet, desktop
```

---

## Rules

- Create PLANS.md at session start, before writing any concept files
- Update progress checkboxes at each approval checkpoint
- Log every significant decision with date and reasoning
- Never delete completed items — check them off
- If a step is skipped, note the reason in the checkbox
- Implementation plan references concept artifacts by path
- Implementation tasks map 1:1 to features and screens where possible
