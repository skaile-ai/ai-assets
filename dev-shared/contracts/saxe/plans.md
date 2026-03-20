# PLANS.md Convention

Plans are first-class artifacts that track multi-step work across sessions.
They are checked into the repo alongside `_concept/` and survive interruptions,
handoffs, and agent restarts.

## Plan Types

### Concept Plan

Tracks progress through the concept pipeline (steps 01–07).
Written by `concept` orchestrator at session start.
Updated at each checkpoint.

**Location:** `PLANS.md` in project root (or `_concept/PLANS.md`)

### Implementation Plan

Tracks progress through coding, testing, and deployment.
Written by coding agents after the concept pipeline is complete.
References concept artifacts as source of truth.

**Location:** `PLANS.md` in project root (appended below concept plan,
or as a separate section)

## Format

```markdown
# Plans

## Concept Plan: <App Name>

### Scope

<One-paragraph description of what this concept session covers>

### Progress

- [x] 1_discovery/1_overview — approved YYYY-MM-DD
- [x] 1_discovery/2_research — skipped (reason)
- [ ] 2_experience/2_features — in progress (3 of 5 groups done)
- [ ] 1_discovery/3_brand — not started
- [ ] 3_blueprint/1_techstack — not started
- [ ] 3_blueprint/3_datamodel — blocked by 03, 05
- [ ] 2_experience/3_screens — blocked by 03, 04, 05, 06

### Decisions

- YYYY-MM-DD: <decision and reasoning>
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

- Brief: \_concept/1_discovery/1_overview/brief.md
- Features: \_concept/2_experience/2_features/ (N features, M must-have)
- Tech stack: \_concept/3_blueprint/1_techstack/stack.md
- Data model: \_concept/3_blueprint/3_datamodel/postxl-schema.json (N models)
- Screens: \_concept/2_experience/3_screens/ (N screens)
- Brand: \_concept/1_discovery/3_brand/tokens.json

### Progress

- [ ] Project scaffold (implement-1-setup-1-scaffold)
- [ ] Startup verification
- [ ] Foundation (brand, auth, shell)
- [ ] Infrastructure (if architecture specifies custom modules)
  - [ ] Shared contracts: <list from architecture>
  - [ ] Providers: <list from architecture> (real + in-memory)
  - [ ] Platform services: <list from architecture>
  - [ ] Communication: <protocols from architecture>
  - [ ] Additional processes: <list from architecture>
- [ ] Feature: 01_user_auth
  - [ ] Login screen
  - [ ] Registration screen
  - [ ] Password reset
- [ ] Feature: 02_dashboard
  - [ ] Overview screen
- [ ] E2E tests (app-e2e)
- [ ] Deploy

### Implementation Decisions

- YYYY-MM-DD: <technical decision and reasoning>

### Known Technical Debt

- <item> — <why it was deferred> — <when to address>

### Verification

- [ ] All must-have features implemented
- [ ] app-audit passes (0 critical, 0 high)
- [ ] app-e2e passes for all ready features
- [ ] Responsive on mobile, tablet, desktop
```

## Rules

- Create PLANS.md at session start, before writing any concept files
- Update progress checkboxes at each approval checkpoint
- Log every significant decision with date and reasoning
- Never delete completed items — check them off
- If a step is skipped, note the reason
- Implementation plan references concept artifacts by path
- Implementation tasks map 1:1 to features and screens where possible
