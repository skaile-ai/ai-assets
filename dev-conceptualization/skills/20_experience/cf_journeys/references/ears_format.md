# EARS Format — Easy Approach to Requirements Syntax

EARS provides five sentence patterns for writing precise, unambiguous acceptance criteria.
One requirement per sentence. Use the simplest pattern that fully captures the behavior.

## Patterns

### 1. Ubiquitous
> THE SYSTEM SHALL <action>

Always-true behavior regardless of state or trigger.

```
THE SYSTEM SHALL encrypt all passwords using bcrypt with minimum cost factor 12.
THE SYSTEM SHALL display all dates in the user's local timezone.
```

### 2. Event-Driven
> WHEN <trigger>, THE SYSTEM SHALL <action>

A specific event causes a specific response.

```
WHEN a user submits a login form with valid credentials, THE SYSTEM SHALL create a session and redirect to the dashboard.
WHEN a payment fails, THE SYSTEM SHALL notify the user and retain their cart contents.
WHEN a session expires, THE SYSTEM SHALL redirect to the login screen and preserve the intended destination URL.
```

### 3. State-Driven
> IF <state>, THE SYSTEM SHALL <action>

Behavior conditional on system state (not a one-time trigger).

```
IF a user is not authenticated, THE SYSTEM SHALL redirect to the login screen.
IF the upload queue is full, THE SYSTEM SHALL reject new uploads with an informative error.
IF two-factor authentication is enabled, THE SYSTEM SHALL require a verification code on login.
```

### 4. Optional Feature
> WHERE <feature is enabled>, THE SYSTEM SHALL <action>

Behavior that only applies when a feature flag or configuration is active.

```
WHERE email notifications are enabled, THE SYSTEM SHALL send a summary digest every Monday at 08:00 local time.
WHERE the dark mode preference is set, THE SYSTEM SHALL apply the dark color theme to all screens.
```

### 5. Complex
> IF <state> AND WHEN <event>, THE SYSTEM SHALL <action>

Combines state + event for more precise conditional behavior.

```
IF the user is an admin AND WHEN they delete a user account, THE SYSTEM SHALL soft-delete the account and retain all associated data for 30 days.
IF rate limiting is active AND WHEN a client exceeds 100 requests per minute, THE SYSTEM SHALL return HTTP 429 with a Retry-After header.
```

## Guidelines

- Write one observable, testable behavior per criterion
- Reference concrete values from the project brief (e.g. "30 days", "100 requests per minute")
- Use uppercase for EARS keywords (WHEN, IF, WHERE, SHALL, AND)
- Avoid subjective language: "fast", "user-friendly", "appropriate"
- Avoid implementation details: say *what*, not *how*
- Each story needs at least 1 criterion; hero stories need at least 2
