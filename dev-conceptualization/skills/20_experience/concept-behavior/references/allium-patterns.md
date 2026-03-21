# Allium Patterns Reference

This file contains the HOW and WHY for writing `.allium` behavioral specs.
The SKILL.md DSL says WHAT to produce; this file explains the constructs in detail.

## What Allium Is

Allium is a domain specification language that captures **observable behavior**:
what the system does, not how it's built. It describes entities with states,
rules with preconditions and postconditions, and surfaces that define what
actors can see and do.

Allium does NOT describe: database schemas, API designs, UI layouts, or
internal algorithms. Those are downstream concerns.

## Constructs You Use

| Construct | Purpose | Use When |
|-----------|---------|----------|
| `entity` | Domain object with states, fields, relationships | Every noun in the features |
| `external entity` | Object managed outside this spec | Auth providers, payment, email |
| `rule` | State transition with requires/ensures | Every "when X happens, Y changes" |
| `surface` | Boundary contract (who sees what, can do what) | Every role-based access pattern |
| `config` | Configurable values (timeouts, limits) | Magic numbers in features |
| `open question` | Unresolved decisions | Ambiguities discovered during formalization |

**You do NOT use:** contracts, sum types/variants, type parameters, module system
(`use` declarations), `given` blocks, `deferred` specs, or expression-bearing
invariants. Keep it simple — this is a concept pipeline, not a production spec.

## Entity Pattern

Extract entities from features. Map feature "Error States" and status descriptions
to entity state enums:

```
entity User {
    email: Email
    password_hash: String
    status: active | locked | suspended
    failed_login_count: Integer
    last_login_at: Timestamp
}
```

## Rule Pattern

Convert feature requirements and error states into formal rules:

```
rule UserLogin {
    when: LoginAttempt(user, credentials)
    requires: user.status = active
    requires: credentials.valid
    ensures: user.last_login_at = now
    ensures: user.failed_login_count = 0
}

rule LoginFailure {
    when: LoginAttempt(user, credentials)
    requires: user.status = active
    requires: not credentials.valid
    ensures: user.failed_login_count = user.failed_login_count + 1
}

rule AccountLockout {
    when: user: User.failed_login_count >= config.max_login_attempts
    requires: user.status = active
    ensures: user.status = locked
}
```

## Surface Pattern

Map feature roles to surfaces — what each actor can see and do:

```
surface LoginPage {
    facing visitor: User

    exposes:
        -- nothing (unauthenticated)

    provides:
        LoginAttempt(visitor, credentials)
            when visitor.status != locked
}
```

## Config Pattern

Extract magic numbers from feature requirements:

```
config {
    max_login_attempts: Integer = 5
    lockout_duration: Duration = 30.minutes
    session_timeout: Duration = 24.hours
}
```

## Open Questions

Flag ambiguities discovered while formalizing:

```
open question "Should locked accounts auto-unlock after lockout_duration, or require admin intervention?"
```

## Behavioral Analysis Questions

For each feature group, extract answers to:

1. What entities are implied? What states can they be in?
2. What causes state transitions? (user actions, time, other entities)
3. What preconditions must hold before a transition?
4. What are the postconditions after a transition?
5. Who can see what? Who can do what? (surfaces)
6. Are there any configurable values (timeouts, limits, defaults)?
7. Are there ambiguities the features don't resolve?
