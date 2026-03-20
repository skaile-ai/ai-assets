# Journey Stages

Story maps are organized into four stages. The stage reflects scope, priority, and
implementation order — not the type of user or feature.

## Stages

### hero — The Core Value Loop
**Definition:** The one journey that, if broken, the app has no value.
Represents the minimum end-to-end flow that delivers the core promise.

- **Exactly one** hero journey per concept
- All stories in the hero journey have `priority: "must"`
- Requires explicit user approval (CHECKPOINT after mapping it)
- 3–8 stories covering the full value delivery cycle
- Example: *idea → concept design → review → approval*

### vital — MVP-Critical Flows
**Definition:** Journeys the app needs to be truly useful, even if the app
can technically exist without them for a short time.

- 2–5 vital journeys per concept
- Stories have `priority: "must"` or `"should"`
- These form the MVP scope alongside the hero journey
- Examples: *iterate on a concept, monitor a deployed app, collaborate with teammates*

### hygiene — Operational Plumbing
**Definition:** Admin, settings, and management flows that keep the app running.
Users need them eventually but they're not why users chose the app.

- 2–4 hygiene journeys
- Stories typically have `priority: "should"` or `"could"`
- Examples: *manage team members, configure notifications, update billing, view audit logs*

### backlog — Future Scope
**Definition:** Valuable but out of MVP scope. Documenting them sets direction
without committing to delivery.

- 1–5 backlog journeys
- Stories have `priority: "could"` or `"wont"`
- Examples: *marketplace integrations, white-label deployment, advanced analytics*

## Distribution Guidelines

| Stage | Count | Story Count | Priority |
|-------|-------|-------------|----------|
| hero | exactly 1 | 3–8 | must |
| vital | 2–5 | 3–8 each | must/should |
| hygiene | 2–4 | 2–5 each | should/could |
| backlog | 1–5 | 1–4 each | could/wont |

## Relationship to Features

The `downstream.candidate_features` field in each story hints at what feature
groups the features skill should create. The features skill uses these hints to:

1. Group related stories into feature groups
2. Name the groups (`01_user_auth`, `02_dashboard`, etc.)
3. Prioritize features by story priority distribution
4. Write feature requirements that trace back to story acceptance criteria

A single feature group may serve stories across multiple journey stages.
The feature priority is derived from the highest-priority story that requires it.
