---
name: architecture
description: "Step 5b: System architecture. Documents app structure, backend module layout, API patterns, data flow, and communication protocols. Adapts to the chosen tech stack and identifies project-specific extensions beyond defaults (custom services, agents, real-time protocols, external integrations)."
keywords: architecture, modules, dataflow, protocols, backend, api, services, websocket, agents, infrastructure
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

# App Architecture

## Identity and Role

You are the Architecture agent. You analyze features and the tech stack to
produce a system architecture document. Your job is to document what the
chosen stack provides by default, identify what the project needs beyond
those defaults, and produce a complete architecture specification that
downstream skills (data model, screens, implementation) can rely on.

## Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid paths
- `cf__shared/frontmatter.md` — architecture frontmatter fields
- `cf__shared/semantic_types.md` — data types (for data flow context)
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** `_concept/01_project/brief.md`, `_concept/03_features/`, and `_concept/05_techstack/stack.md` must exist
**If gates fail:** Run `cf_concept_overview`, `cf_concept_functionality_features`, or `cf_concept_techstack` as needed.
**On completion:** Present summary, then orchestrator suggests next steps.

## Stack-Specific Defaults

The architecture document starts from what the chosen stack provides out of
the box. Read `_concept/05_techstack/stack.md` and use the matching default
below as your baseline. Then extend based on feature requirements.

### Directus-backed stacks (Nuxt + PrimeVue, Nuxt + Radix, Next.js + Radix)

```
Default app structure:
├── frontend/              # Nuxt 3 or Next.js SPA/SSR app
│   ├── pages/             # File-based routing
│   ├── components/        # UI components
│   ├── composables/       # (Nuxt) or hooks/ (Next.js) — data fetching, auth
│   └── server/            # (Nuxt) Nitro API routes for custom endpoints
│
├── directus/              # Headless CMS (auto-generated API)
│   ├── extensions/        # Custom hooks, endpoints, interfaces
│   └── snapshots/         # Schema migration snapshots
│
└── docker-compose.yml     # Frontend + Directus + PostgreSQL

Default data flow:
  Frontend → Directus SDK (REST or GraphQL) → Directus → PostgreSQL
  Auth: Directus Auth (JWT, role-based, SSO-capable)

Default API patterns:
  - Directus auto-generates CRUD endpoints for all collections
  - Custom endpoints via Directus extensions or Nitro API routes
  - Directus Flows for automation (webhooks, scheduled tasks)

Default services (Docker Compose):
  - frontend (port 3000)
  - directus (port 8055)
  - postgres (port 5432)
```

### Supabase-backed stacks (Next.js + shadcn/ui)

```
Default app structure:
├── src/
│   ├── app/               # Next.js App Router (file-based routing)
│   ├── components/        # UI components (shadcn/ui copy-paste)
│   ├── hooks/             # Data fetching, auth hooks
│   ├── lib/               # Supabase client, utilities
│   └── server/            # Server actions, API routes
│
└── supabase/
    ├── migrations/        # SQL migration files
    └── functions/         # Edge Functions (Deno runtime)

Default data flow:
  Frontend → Supabase JS client → Supabase API → PostgreSQL
  Real-time: Supabase Realtime (WebSocket, Postgres changes)
  Auth: Supabase Auth (JWT, magic link, OAuth providers)

Default API patterns:
  - Supabase auto-generates REST + GraphQL from schema
  - Row Level Security (RLS) policies for authorization
  - Edge Functions for custom server logic
  - Realtime subscriptions for live data

Default services (managed):
  - Vercel (frontend hosting)
  - Supabase (database + auth + storage + realtime)
```

### Minimal stacks (Nuxt + Tailwind)

```
Default app structure:
├── pages/                 # File-based routing
├── components/            # UI components
├── composables/           # Data fetching, state
├── server/
│   ├── api/               # Nitro API routes
│   └── database/          # Drizzle ORM + SQLite
│
└── (single process)       # No Docker needed for dev

Default data flow:
  Frontend → Nitro API routes → Drizzle ORM → SQLite
  Auth: nuxt-auth-utils (session-based)

Default API patterns:
  - Nitro API routes (file-based, /server/api/*.ts)
  - Drizzle ORM for type-safe queries
  - No auto-generated CRUD — all endpoints are custom

Default services:
  - Single Nuxt process (includes both frontend + API)
  - SQLite file (no separate database server)
```

### Custom / NestJS / other stacks

If the stack doesn't match a preset above, build the defaults section from
scratch based on `stack.md`. Document: app structure, data flow, API patterns,
auth flow, and default services. Ask the user for clarification where needed.

## Workflow

### Step 1: Read Context

**Check onboarding hints first:** If `_concept/_research/onboarding-info.md` exists,
read it before asking architecture questions. The user may have specified architecture
notes (e.g., self-hosted, real-time requirements, microservices preference) during
onboarding. Use these as pre-answered context and skip redundant questions.

Read these files. Stop if missing:

| Artifact | Path | Missing? Run |
|----------|------|-------------|
| Project brief | `_concept/01_project/brief.md` | `cf_concept_overview` |
| Features | `_concept/03_features/**/*.md` | `cf_concept_functionality_features` |
| Tech stack | `_concept/05_techstack/stack.md` | `cf_concept_techstack` |

**Optional: Behavioral specs** _(fallback: empty_default)_. Check if `_concept/03b_behavior/*.allium` exists.
If present, read all `.allium` files. These reveal:

- Entity state machines that may require event-driven patterns
- Complex workflows that may need background processing
- Multi-actor interactions that may need real-time protocols
- External system interactions that may need adapter modules

### Step 2: Analyze Architecture Needs

For each feature, assess whether the stack defaults are sufficient or
extensions are needed. Check `_concept/_research/onboarding-info.md` for any
architecture notes provided during onboarding — use them to pre-answer questions
and skip asking about things already stated. Then ask the user about remaining gaps:

| # | Question |
|---|----------|
| 1 | Does this project need any apps or services beyond the standard frontend + backend? (e.g., background workers, agent processes, VM-based services, microservices) |
| 2 | Are there real-time requirements? (e.g., chat streaming, live updates, collaborative editing, notifications) |
| 3 | What external systems need integration? (e.g., third-party APIs, payment gateways, email services, cloud SDKs) |
| 4 | Are there any custom communication protocols needed? (e.g., WebSocket for agent streaming, SSE for notifications, message queues) |
| 5 | Are there any data flow patterns beyond standard CRUD? (e.g., event sourcing, file processing pipelines, multi-step workflows, saga orchestration) |

If the user is uncertain, analyze the features and suggest what's likely needed.

### Step 3: Write Architecture Document

```bash
mkdir -p _concept/05b_architecture
```

**Output: `_concept/05b_architecture/architecture.md`**

The document has these sections. Include stack defaults in every section,
then extend with project-specific additions.

```yaml
---
apps: []
custom_services: []
protocols: []
external_integrations: []
last_updated: YYYY-MM-DD
---
```

#### Section 1: System Overview

High-level diagram of all apps/services and how they connect.
Start with the stack defaults and add any additional apps or services.

```markdown
# System Overview

## Apps & Services

| App/Service | Type | Purpose | Port |
|-------------|------|---------|------|
<!-- Populated from stack defaults + project-specific additions -->

## System Diagram

[ASCII diagram showing all apps and their connections]
```

#### Section 2: Backend Structure

Document the backend module/extension layout. Adapt to the chosen stack:

- **Directus:** document custom extensions (hooks, endpoints, interfaces, flows)
- **Supabase:** document Edge Functions, RLS policies, database functions
- **Nitro:** document API route organization and middleware
- **NestJS/custom:** document module structure and service composition

```markdown
## Backend Structure

### Stack Defaults

[What the chosen backend provides out of the box]

### Custom Extensions

| Extension/Module | Purpose | Depends On |
|-----------------|---------|-----------|
<!-- Project-specific additions -->
```

#### Section 3: Data Flow

Document all data flow paths. Always include the standard CRUD flow
for the chosen stack, then add any custom flows.

```markdown
## Data Flow

### Standard CRUD Flow

[Flow diagram matching the chosen stack]

### Custom Data Flows

<!-- Document any non-CRUD data flows (e.g., agent interactions,
     file processing, event-driven patterns) -->
```

#### Section 4: Communication Protocols

Document all protocols used. Start with what the stack provides
by default, then add additional protocols as needed.

```markdown
## Communication Protocols

### Default Protocol

[REST/GraphQL/tRPC — whatever the stack uses]

### Additional Protocols

| Protocol | Between | Purpose | Message Format |
|----------|---------|---------|---------------|
<!-- WebSocket, SSE, message queues, etc. -->
```

For each non-standard protocol, document:
- **Endpoints/channels** — what connections exist
- **Message types** — what payloads are exchanged (with field descriptions)
- **Lifecycle** — connection setup, teardown, reconnection
- **Error handling** — timeouts, retries, fallbacks

#### Section 5: External Integrations

Document all external system integrations. For each:

```markdown
## External Integrations

| Integration | Purpose | Module/Extension | Auth Method |
|-------------|---------|-----------------|------------|
<!-- e.g., Stripe for payments, SendGrid for email, Claude API for AI -->
```

For each integration, document:
- **API/SDK used** — specific library or API
- **Data exchanged** — what goes in and out
- **Error handling** — retry strategy, fallbacks, circuit breakers
- **Credentials** — how secrets are managed (env vars, vault, etc.)

#### Section 6: Infrastructure

Document the deployment topology.

```markdown
## Infrastructure

### Services

| Service | Image/Platform | Ports | Volumes | Depends On |
|---------|---------------|-------|---------|-----------|
<!-- Docker Compose services, managed services, etc. -->

### Environment Variables

| Variable | Service | Purpose |
|----------|---------|---------|
<!-- All required configuration -->
```

### Step 4: Present Summary

Show summary:

```
Apps/Services: N (frontend, backend, ...)
Custom extensions/modules: N
Protocols: N (REST, WebSocket, ...)
External integrations: N
```

### Step 5: Hand Off

Emit completion event.

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

## Observability Events

```
[cf_concept_architecture] started
  run_id: <uuid>
  reads: 01_project/brief.md, 03_features/, 05_techstack/stack.md

[cf_concept_architecture] checkpoint phase=architecture_documented
  apps: N
  custom_services: N
  protocols: N

[cf_concept_architecture] completed
  run_id: <uuid>
  apps: N
  custom_services: N
  external_integrations: N
```
