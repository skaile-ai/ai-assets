# Journey Stages

Story maps in stories.json are organized into four stages that control scope, priority,
and development order. Every story map is assigned exactly one stage.

## The Four Stages

### 1. Hero

**Definition:** The single most important user journey. This is the end-to-end flow
that defines why the app exists. If the hero journey fails, the app has no value.

**Rules:**
- Exactly one story map has stage "hero" — never zero, never more than one.
- All stories in the hero journey have priority "must".
- The hero journey is implemented first and tested most thoroughly.
- It derives directly from the `hero_flow` field in brief.md.

**How to identify it:** Ask "If a user could only do ONE thing with this app, what
would it be?" The answer is the hero journey.

**Example:**
> **Hero: Complete the primary value action**
> A user opens the app, provides their input, the system processes it, and the
> user receives the core value outcome they came for — end-to-end, without
> needing any other flow to succeed first.
>
> Stories: open app → provide input → system processes → view result → take action on result

---

### 2. Vital

**Definition:** Other critical user journeys required for MVP scope. These flows
complement the hero journey and make the app viable for daily use. Without them,
the app works but is incomplete.

**Rules:**
- Multiple story maps can have stage "vital".
- Stories use priority "must" or "should".
- Vital journeys are implemented after the hero journey.
- They cover the other major use cases that users need regularly.

**How to identify them:** Ask "What else must users be able to do for this app to
be useful on day one?" Each answer is a vital journey.

**Examples:**
> **Vital: Manage existing records**
> A user returns to the app, browses previously created items, edits one,
> and saves the changes — the standard read/update loop.
>
> **Vital: Collaborate with others**
> A user invites a colleague, assigns them a role, and both work on shared
> content simultaneously.

---

### 3. Hygiene

**Definition:** Admin and operational flows that enable the app to function. Users
do not buy the app for these flows, but the app cannot operate without them. These
are the "plumbing" that supports the hero and vital journeys.

**Rules:**
- Multiple story maps can have stage "hygiene".
- Stories use priority "should" or "could".
- Hygiene flows are typically implemented alongside or after vital journeys.
- Many hygiene flows follow standard patterns (user management, settings, etc.).

**How to identify them:** Ask "What operational tasks must someone perform so the
app keeps running?" and "What setup is needed before the hero journey can happen?"

**Examples:**
> **Hygiene: User and team management**
> An admin invites team members, assigns roles, manages permissions,
> and deactivates accounts when people leave.
>
> **Hygiene: Application settings**
> A user configures their preferences, notification settings, connected
> integrations, and account details.

---

### 4. Backlog

**Definition:** Future user journeys that are out of MVP scope. These are valuable
flows identified during journey mapping that will be built in later releases. They
capture vision without committing to immediate implementation.

**Rules:**
- Multiple story maps can have stage "backlog".
- Stories use priority "could" or "wont" (for this release).
- Backlog journeys are NOT implemented in the initial build.
- They inform architecture decisions (design for extensibility where backlog
  journeys suggest future needs).

**How to identify them:** Ask "What would make this app great in version 2?" and
"What did competitors do that we are not building yet?"

**Examples:**
> **Backlog: Advanced analytics and reporting**
> A power user browses usage analytics, creates custom reports, exports
> data to a spreadsheet, and schedules automated report delivery.
>
> **Backlog: Public API and integrations**
> A developer authenticates with an API key, queries the app's data via
> REST endpoints, and builds an automated workflow using webhooks.

---

## Stage Distribution Guidelines

A well-balanced story map typically follows this distribution:

| Stage   | Story maps | Stories | Priority            |
|---------|------------|---------|---------------------|
| Hero    | 1          | 3-8     | all must            |
| Vital   | 2-5        | 5-20    | must + should       |
| Hygiene | 2-4        | 4-12    | should + could      |
| Backlog | 1-5        | 3-15    | could + wont        |

**Warning signs:**
- No hero journey: the app's core value is unclear.
- Too many hero stories (>10): the hero flow is too broad; split it.
- No hygiene flows: operational reality is being ignored.
- No backlog: the team has not thought about the future; the architecture may
  paint itself into a corner.
- Everything is "must": no prioritization has actually happened.

## Relationship to Features

Story maps feed directly into features (step 2 in the experience phase). Each story's
`downstream.candidate_features` field hints at which features will be derived from it.
The mapping is:

- Hero stories produce must-have features.
- Vital stories produce must-have and nice-to-have features.
- Hygiene stories produce standard operational features (often platform defaults).
- Backlog stories are recorded but do not produce features in the current cycle.

The features skill reads stories.json and uses these hints as a starting point,
but may split, merge, or reframe features based on technical considerations.
