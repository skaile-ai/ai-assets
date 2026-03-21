# EARS — Easy Approach to Requirements Syntax

EARS provides five sentence patterns for writing unambiguous, testable requirements.
Every acceptance criterion in stories.json uses one of these patterns.

## Why EARS

Traditional "the system shall" requirements are often vague, ambiguous, or untestable.
EARS fixes this by providing a small set of structured templates that force the author
to specify the trigger condition, the system scope, and the observable action.

## The Five Patterns

### 1. Ubiquitous

Requirements that apply at all times, without any trigger or condition.

**Template:**
```
THE SYSTEM SHALL <action>.
```

**When to use:** Always-true behavior. System-wide invariants. Non-functional requirements.

**Examples:**
- THE SYSTEM SHALL encrypt all data at rest using AES-256.
- THE SYSTEM SHALL display the app logo in the navigation header.
- THE SYSTEM SHALL respond to API requests within 500ms.

---

### 2. Event-Driven

Requirements triggered by a specific event or user action.

**Template:**
```
WHEN <event>, THE SYSTEM SHALL <action>.
```

**When to use:** User interactions, incoming messages, timer expirations, state transitions triggered by external events.

**Examples:**
- WHEN the user submits the login form, THE SYSTEM SHALL validate credentials against the identity provider.
- WHEN a new project is created, THE SYSTEM SHALL generate a unique project slug from the project name.
- WHEN the session token expires, THE SYSTEM SHALL redirect the user to the login page.
- WHEN a file upload completes, THE SYSTEM SHALL display a success notification for 5 seconds.

---

### 3. State-Driven

Requirements that apply only when the system is in a specific state.

**Template:**
```
IF <state>, THE SYSTEM SHALL <action>.
```

**When to use:** Behavior that depends on current system state, configuration, or data conditions. The requirement is continuously active while the state holds.

**Examples:**
- IF the user has the admin role, THE SYSTEM SHALL display the user management menu item.
- IF the project status is "archived", THE SYSTEM SHALL disable all edit controls.
- IF no records match the current filter, THE SYSTEM SHALL display the empty state illustration with a "Clear filters" action.
- IF the database connection is unavailable, THE SYSTEM SHALL display a maintenance page.

---

### 4. Optional Feature

Requirements that apply only when a specific feature or capability is enabled.

**Template:**
```
WHERE <feature is enabled>, THE SYSTEM SHALL <action>.
```

**When to use:** Configurable features, tenant-specific settings, feature flags, optional modules, licensed capabilities.

**Examples:**
- WHERE two-factor authentication is enabled, THE SYSTEM SHALL require a TOTP code after password entry.
- WHERE the export module is licensed, THE SYSTEM SHALL display an "Export to CSV" button on list views.
- WHERE email notifications are enabled, THE SYSTEM SHALL send a summary digest at the configured interval.
- WHERE the dark mode preference is set, THE SYSTEM SHALL apply the dark color token palette.

---

### 5. Complex (Combined)

Requirements involving both a state precondition and an event trigger.

**Template:**
```
IF <state> AND WHEN <event>, THE SYSTEM SHALL <action>.
```

**When to use:** Behavior that requires both a precondition to be true and a trigger to occur. Use sparingly — most requirements fit one of the simpler patterns.

**Examples:**
- IF the user has unsaved changes AND WHEN the user navigates away, THE SYSTEM SHALL display a confirmation dialog.
- IF the project is in "review" status AND WHEN the reviewer clicks "Approve", THE SYSTEM SHALL transition the project to "approved" and notify the owner.
- IF the upload queue contains more than 10 items AND WHEN a new file is added, THE SYSTEM SHALL display a "Queue full — please wait" message.
- IF the user's subscription has expired AND WHEN the user attempts to create a new project, THE SYSTEM SHALL redirect to the billing page.

---

## Writing Guidelines

1. **One requirement per statement.** Do not combine multiple actions with "and". Split them into separate criteria.
2. **Observable actions only.** The action must be something a tester can verify — a visible UI change, a measurable system response, or a data state change.
3. **Use the simplest pattern that fits.** Start with ubiquitous. Add WHEN or IF only when the requirement genuinely has a trigger or precondition. Use complex only when both state and event matter.
4. **Avoid ambiguity.** Replace vague words ("quickly", "appropriate", "user-friendly") with specific, measurable terms ("within 2 seconds", "the error message text", "a 16px red border").
5. **Reference concrete values.** Use entity names, field names, status values, and role names from the concept artifacts rather than abstract placeholders.

## Pattern Selection Quick Reference

| Situation | Pattern |
|-----------|---------|
| Always true, no trigger needed | Ubiquitous |
| Something happens, system responds | Event-driven |
| System is in a state, behavior changes | State-driven |
| Feature/config is on, behavior activates | Optional feature |
| State must hold AND event must occur | Complex |
