# Architecture Document Template

The output file `_concept/blueprint/architecture.md` has six sections.
Every section starts with what the chosen stack provides by default, then adds
project-specific extensions where needed.

## Frontmatter

```yaml
---
apps: [frontend, api]
custom_modules: []
protocols: [http, rest]
external_integrations: []
last_updated: YYYY-MM-DD
---
```

**Field definitions:**
- `apps` — all running processes/services (frontend, api, workers, etc.)
- `custom_modules` — backend modules/extensions beyond what the stack provides
- `protocols` — all communication protocols in use (http, websocket, sse, grpc, etc.)
- `external_integrations` — third-party services the app depends on

---

## Section 1: System Overview

High-level map of all apps/services and how they connect.
Start with what the stack provides (from stack.md), then add project-specific services.

```markdown
# System Overview

## Apps & Services

| App/Service | Type | Purpose | Port |
|-------------|------|---------|------|
| frontend    | [framework] | UI application | [port] |
| api/backend | [framework] | Backend API    | [port] |
| database    | [engine]    | Primary storage | [port] |
<!-- Add project-specific services here -->

## System Diagram

[ASCII diagram showing all apps and their connections]

Example:
  browser → frontend (3000)
             ↓ [REST/GraphQL/tRPC]
           backend (8000)
             ↓ [SQL]
           database (5432)
```

---

## Section 2: Backend Structure

Document the backend module layout. Adapt the structure to the chosen stack:

- **BaaS (Directus, Supabase):** document custom extensions, functions, hooks, policies
- **Framework (NestJS, Rails, Django):** document module/service composition
- **Minimal (Nuxt Nitro, Express):** document API route organization
- **Serverless:** document function groupings and triggers

```markdown
## Backend Structure

### Stack Defaults

[Document what the chosen stack generates or provides out of the box:
 auto-generated CRUD endpoints, built-in auth, file uploads, etc.]

### Custom Modules / Extensions

| Module / Extension | Purpose | Depends On |
|--------------------|---------|-----------|
<!-- Document project-specific backend additions -->
```

---

## Section 3: Data Flow

Document all data flow paths. Always include the stack's standard CRUD flow first,
then add custom flows for non-standard patterns.

```markdown
## Data Flow

### Standard CRUD Flow

[Diagram or description of how standard read/write operations flow through the stack]

Example (REST stack):
  browser → API route → service layer → ORM → database
  browser ← JSON response ← service ← ORM ← database

### Custom Data Flows

<!-- Document any non-CRUD flows. For each custom flow, describe:
     - Trigger (user action, schedule, event)
     - Processing steps
     - Output / side effects -->
```

---

## Section 4: Communication Protocols

Document all protocols in use. Start with the stack's default, then add extras.

```markdown
## Communication Protocols

### Default Protocol

[The protocol the stack uses for standard operations — REST, GraphQL, tRPC, etc.]

### Additional Protocols

| Protocol  | Between         | Purpose                  | Message Format |
|-----------|-----------------|--------------------------|----------------|
<!-- WebSocket, SSE, gRPC, message queues, etc. -->
```

For each non-standard protocol, document:
- **Endpoints/channels** — what connections exist (URLs, topics, queues)
- **Message types** — payloads exchanged (list fields and types)
- **Lifecycle** — connection setup, teardown, reconnection strategy
- **Error handling** — timeouts, retries, fallbacks

---

## Section 5: External Integrations

Document all third-party services the app depends on.

```markdown
## External Integrations

| Integration | Purpose | Module/Function | Auth Method |
|-------------|---------|-----------------|------------|
<!-- e.g., Stripe (payments), SendGrid (email), Claude API (AI), S3 (files) -->
```

For each integration, document:
- **API/SDK** — specific library or HTTP API used
- **Data exchanged** — what goes in and what comes back
- **Error handling** — retry strategy, fallbacks, circuit breaker
- **Credentials** — how secrets are stored and accessed (env vars, secrets manager, etc.)

---

## Section 6: Infrastructure

Document the deployment topology — containers, managed services, environment config.

```markdown
## Infrastructure

### Services

| Service | Image / Platform | Ports | Volumes | Depends On |
|---------|-----------------|-------|---------|-----------|
<!-- Docker Compose services, managed cloud services, serverless functions, etc. -->

### Environment Variables

| Variable | Service | Purpose |
|----------|---------|---------|
<!-- All required configuration — no secret values, just variable names and purpose -->
```

---

## Baseline-First Rule

Every section must show what the stack already provides before adding extensions.
This prevents over-engineering and makes the "what we added" clearly visible.

If the stack covers everything needed for a section, write "Stack defaults cover
all requirements — no custom additions needed." Do not skip the section.
