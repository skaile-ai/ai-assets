# Acceptance Criteria Format

How to derive testable acceptance criteria from concept artifacts and structure them
for TDD-style implementation.

## Source Artifacts

Each feature's ACs are derived from multiple concept sources:

| Source | What to extract |
|---|---|
| `experience/features/<group>/<feature>.md` | Requirements checklist, success criteria, error states |
| `experience/screens/<group>/<screen>.md` | Component inventory, states (default/loading/error/empty/success), user actions |
| `experience/journeys/stories.json` | EARS acceptance criteria, state transitions, guard conditions |
| `blueprint/datamodel/seed.json` | Test data for realistic assertions |
| `discovery/brand/tokens.json` | Visual expectations (colors, spacing — for visual checks only) |

---

## AC File Format

Save to `_implementation/acceptance_criteria/<group>/<feature>.ac.md`:

```markdown
---
feature_ref: _concept/experience/features/<group>/<feature>.md
screen_refs:
  - _concept/experience/screens/<group>/<screen>.md
derived_from:
  - requirements: 5     # count from feature spec
  - screen_states: 4    # count from screen spec
  - behavior_rules: 3   # count from allium spec
last_updated: YYYY-MM-DD
---

# Acceptance Criteria: <Feature Name>

## AC-1: <Short descriptive title>

**Given** <precondition>
**When** <action>
**Then** <observable outcome>

- Assert: <specific, testable assertion>
- Assert: <another assertion>

**Test type:** assertion
**Seed scenario:** populated

## AC-2: ...

## AC-N: User Flow (snapshot)

**Given** <starting state>
**When** <user completes the full happy path>
**Then** <final state>

**Test type:** snapshot
**Seed scenario:** populated
```

---

## Rules for Writing ACs

1. **One AC per observable behavior.** Do not combine multiple behaviors.
2. **Specific assertions over vague checks.** "User sees exactly 'Invalid email or password'" not "User sees an error."
3. **Reference concrete values.** Use seed data names, exact routes, specific status codes.
4. **Test type is either `assertion` or `snapshot`.** Only the final high-level user flow AC uses `snapshot`. All others use `assertion`.
5. **Every AC must be independently verifiable.** No AC depends on another AC's side effects.
6. **Cover the error states.** The concept screen specs list error states explicitly — each becomes an AC.
7. **Include edge cases from behavioral specs.** Allium guard conditions and `requires` clauses map directly to ACs.

---

## Deriving ACs: Step by Step

### Step 1: Extract from feature requirements

Each checkbox in the feature's `## Requirements` section becomes one or more ACs.
The feature's `## Success Criteria` section provides the happy-path ACs.
The feature's `## Error States` section provides the error-path ACs.

### Step 2: Extract from screen states

Each state in the screen spec's `## States` section (default, loading, error, empty, success)
becomes at least one AC. The `## User Actions` section maps to ACs that verify
what happens when the user performs each action.

### Step 3: Extract from behavioral specs

Each `rule` block in the Allium spec has `requires` (preconditions) and `ensures`
(postconditions). Each rule maps to one or more ACs. Guard conditions (`requires`)
become the Given clause. Effects (`ensures`) become the Then clause.

### Step 4: Assign seed scenarios

Match each AC to the most appropriate seed scenario:

- `empty` — for empty state / first-use ACs
- `single_user` — for minimal setup ACs
- `populated` — for most happy-path ACs
- `edge_cases` — for error handling / boundary ACs

### Step 5: Mark the snapshot AC

The final AC in each feature should be a high-level user flow snapshot test
that exercises the happy path end-to-end. This is the only AC with `test type: snapshot`.

---

## AC Count Guidelines

| Feature complexity | Expected ACs |
|---|---|
| Simple (1-2 screens, few states) | 4-6 |
| Medium (2-3 screens, several states) | 6-10 |
| Complex (3+ screens, many states, transitions) | 10-15 |

Too few ACs = undertested. Too many ACs = test maintenance burden.
Aim for one AC per distinct observable behavior.

---

## Backend Acceptance Criteria

Not every feature needs backend ACs. If the feature only uses generated CRUD
(list, create, update, delete via standard generated API endpoints), the generated
code is sufficient and no custom backend testing is needed.

Backend ACs are required when the feature has:

- Custom business logic (validation beyond schema types, state machines, workflows)
- External service integration (cloud APIs, git platforms, AI models)
- Custom authorization rules (row-level access, workspace scoping)
- Custom data transformations (computed fields, aggregations)

### Source Artifacts (backend)

| Source | What to extract for backend ACs |
|---|---|
| `experience/features/<group>/<feature>.md` | Business rules, validation requirements, integration requirements |
| `experience/journeys/stories.json` | EARS acceptance criteria, state transitions, guard conditions |
| `blueprint/datamodel/model.json` | Model relationships, enum constraints, auth rules |

### Backend AC Format

Append backend ACs to the same AC file, after the frontend ACs:

```markdown
## Backend Acceptance Criteria

### AC-B1: <Short descriptive title>

**Given** <precondition — system state, existing data>
**When** <API action — service method call, event dispatch, or endpoint invocation>
**Then** <observable outcome — response, state change, side effect>

- Assert: <specific assertion — return value, status change, error code>

**Test type:** unit | e2e
**Test target:** <ServiceClass | Adapter | Handler>
```

### Rules for Backend ACs

1. **One AC per business rule.** Each custom validation, state transition, or
   integration behavior gets its own AC.
2. **Specify the test type.** Use `unit` for service-level logic. Use `e2e` for
   behavior observable through the UI.
3. **Cover error paths.** For every external service call, write an AC for the
   failure case (service down, invalid credentials, timeout).
4. **Reference the adapter pattern.** ACs that involve external services should
   describe the expected behavior, not the implementation. The in-memory adapter
   provides predictable responses for testing.
5. **Keep AC-B numbering separate.** Backend ACs use `AC-B1`, `AC-B2`, etc.
   to distinguish from frontend `AC-1`, `AC-2`.

### Backend AC Count Guidelines

| Backend complexity | Expected backend ACs |
|---|---|
| No custom logic (pure CRUD) | 0 — skip backend ACs entirely |
| Light customization (1-2 custom actions) | 2-4 |
| Moderate (validation + external service) | 4-8 |
| Heavy (state machine + multi-service) | 8-12 |

### Deriving Backend ACs

**From feature requirements:**

- "Validate cloud credentials" → AC-B for successful validation, AC-B for
  invalid credentials, AC-B for provider unreachable

**From behavioral specs:**

- Each `rule` block with `ensures` clauses that modify backend state →
  one AC-B per postcondition
- Guard conditions with `requires` → AC-B that verifies rejection when
  precondition is not met

**From data model:**

- Custom enum transitions → AC-B per valid transition, AC-B for invalid
  transition attempt
- Unique constraints with business meaning → AC-B for duplicate rejection
