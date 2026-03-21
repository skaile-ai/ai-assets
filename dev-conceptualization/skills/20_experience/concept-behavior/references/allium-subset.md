# Allium Language Subset for Concept Forge

This is the subset of Allium v2 used by the `concept-behavior` skill.
For the full language, see the allium skill's `references/language-reference.md`.

## File Structure

Every `.allium` file starts with:

```
-- allium: 2
-- <description>
```

Comments use `--` (double dash). Section headers use divider lines:

```
------------------------------------------------------------
-- Section Name
------------------------------------------------------------
```

## Naming Conventions

- **PascalCase:** entities, rules, surfaces, triggers
- **snake_case:** fields, config parameters, enum literals

## Entities

Domain objects with identity. Fields, inline enums, relationships, projections,
and derived values.

```
entity Task {
    title: String
    description: String
    status: todo | in_progress | done          -- inline enum
    priority: low | medium | high | urgent

    -- Relationships
    assigned_to: User with tasks = this        -- many-to-one
    comments: Comment with task = this         -- one-to-many

    -- Projections (filtered subsets)
    open_comments: comments where resolved = false

    -- Derived values (computed from other fields)
    is_overdue: due_date < now and status != done
}
```

**`with` vs `where`:**
- `with` declares relationships: `slots: Slot with task = this`
- `where` filters projections: `active: slots where status = active`

## External Entities

Entities managed outside this specification:

```
external entity EmailService { send: (to: Email, subject: String, body: String) }
external entity PaymentProvider { charge: (amount: Money, token: String) }
```

## Rules

Behavioral transitions with preconditions and postconditions.

```
rule AssignTask {
    when: AssignTask(task, assignee)
    requires: task.status != done
    requires: assignee.status = active
    ensures: task.assigned_to = assignee
    ensures: task.status = todo
}
```

### Trigger Types

| Type | Syntax | When It Fires |
|------|--------|---------------|
| External stimulus | `when: ActionName(params)` | Action from outside the system |
| State transition | `when: e: Entity.field transitions_to value` | Field changes to value |
| State becomes | `when: e: Entity.field becomes value` | Field has value (creation or transition) |
| Temporal | `when: e: Entity.field <= now` | Time-based (always add `requires` guard) |
| Derived condition | `when: e: Entity.derived_field` | Derived value becomes true |
| Entity creation | `when: e: Entity.created` | New entity created |

### Ensures Patterns

| Pattern | Syntax |
|---------|--------|
| State change | `ensures: entity.field = value` |
| Entity creation | `ensures: Entity.created(field: value, ...)` |
| Trigger emission | `ensures: TriggerName(params)` |
| Entity removal | `ensures: not exists entity` |
| Iteration | `ensures: for x in collection: x.field = value` |
| Conditional | `ensures: if condition: entity.field = value` |

### Temporal Rules Need Guards

```
-- WRONG: can re-fire
rule TokenExpiry {
    when: t: Token.expires_at <= now
    ensures: t.status = expired
}

-- CORRECT: requires prevents re-firing
rule TokenExpiry {
    when: t: Token.expires_at <= now
    requires: t.status = active
    ensures: t.status = expired
}
```

## Surfaces

Boundary contracts — what actors can see and do.

```
surface TaskBoard {
    facing viewer: TeamMember

    context team: Team where members includes viewer

    exposes:
        for task in team.tasks:
            task.title
            task.status
            task.assigned_to.name

    provides:
        CreateTask(viewer, team)
            when viewer.role in {admin, member}
        AssignTask(task, assignee)
            when viewer.role = admin or task.assigned_to = viewer
}
```

- `facing` — who is looking at this surface (actor type)
- `context` — scoping entity (optional)
- `exposes` — visible data fields
- `provides` — available actions with optional `when` guards
- `related` — links to other surfaces (optional)

## Config

Configurable parameters with types and defaults:

```
config {
    max_login_attempts: Integer = 5
    session_timeout: Duration = 24.hours
    invitation_expiry: Duration = 7.days
}
```

Rules reference config as `config.max_login_attempts`.

## Open Questions

Unresolved decisions discovered during formalization:

```
open question "Should task deletion be soft-delete or hard-delete?"
open question "Can a task be reassigned while in_progress?"
```

## Expressions

| Kind | Examples |
|------|----------|
| Navigation | `task.assigned_to.email`, `reply?.author` |
| Null coalescing | `timezone ?? "UTC"` |
| Collection count | `slots.count` |
| Any/all | `members.any(m => m.role = admin)` |
| Set membership | `status in {active, pending}` |
| Boolean | `a and b`, `a or b`, `not a`, `a implies b` |
| Comparison | `count >= 2`, `status = active` |
| Arithmetic | `count + 1`, `balance - amount` |

## Anti-Patterns to Avoid

- **Implementation leakage:** No database types, API paths, or UI elements
- **Missing temporal guards:** Every temporal trigger needs a `requires` clause
- **Magic numbers:** Use `config` blocks, not hardcoded values
- **Overly broad enums:** Extract named enums when the same set appears on multiple fields
