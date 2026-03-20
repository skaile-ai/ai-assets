# Architecture Document Template

The output file `_concept/3_blueprint/2_architecture/architecture.md` has six sections.
Include PostXL defaults in every section, then extend with project-specific additions.

## Frontmatter

```yaml
---
status: draft
apps: [api, web]
custom_modules: []
protocols: [http, trpc]
external_integrations: []
last_updated: YYYY-MM-DD
---
```

## Section 1: System Overview

High-level diagram of all apps/services and how they connect.
Start with the PostXL default (api + web + postgres + keycloak)
and add any additional apps or services.

```
# System Overview

## Apps & Services

| App/Service | Type | Purpose | Port |
|-------------|------|---------|------|
| api | NestJS + Fastify | Backend API, tRPC server | 3000 |
| web | React 19 + Vite | Frontend SPA | 5173 |
| postgres | PostgreSQL | Primary database | 5432 |
| keycloak | Keycloak | Identity & access management | 8080 |
<!-- Add project-specific apps here -->

## System Diagram

[ASCII diagram showing all apps and their connections]
```

## Section 2: Backend Module Structure

Document the NestJS module layout. Start with PostXL defaults,
then add project-specific custom modules.

```
## Backend Module Structure

### Standard PostXL Modules (generated)

These are provided by PostXL and should not be modified:

| Module | Purpose |
|--------|---------|
| database | Prisma client, DatabaseService |
| repositories | Dual repo pattern (DB + in-memory) |
| authentication | Keycloak/OIDC, JWT validation, session |
| actions | Dispatcher, audit trail, authorization |
| router-trpc | tRPC server, plugin, context creation |
| update | Write layer (per-entity UpdateService) |
| view | Read layer (per-entity ViewService) |
| import | Excel/data import with preview |
| upload | File upload (multipart → S3) |
| s3 | AWS S3 client |
| excel-io | Excel preview/export |
| ai | Claude AI integration |
| seed | Database seeding |
| e2e | E2E test utilities |
| restApi | REST/Swagger fallback |

### Custom Modules (project-specific)

<!-- Document any additional NestJS modules needed -->

| Module | Purpose | Depends On |
|--------|---------|-----------|
```

## Section 3: Data Flow

Document all data flow paths. Always include the standard CRUD flow,
then add any custom flows.

```
## Data Flow

### Standard CRUD Flow (PostXL default)

[the default flow diagram — see references/postxl_defaults.md]

### Custom Data Flows

<!-- Document any non-CRUD data flows -->
```

## Section 4: Communication Protocols

Document all protocols used. Start with HTTP/tRPC (the default),
then add WebSocket, SSE, or other protocols as needed.

```
## Communication Protocols

### HTTP + tRPC (default)

All standard CRUD operations use tRPC over HTTP with superjson serialization
and request batching.

### Additional Protocols

<!-- Document any additional protocols (WebSocket, SSE, gRPC, etc.) -->

| Protocol | Between | Purpose | Message Format |
|----------|---------|---------|---------------|
```

For each non-standard protocol, document:
- **Endpoints/channels** — what connections exist
- **Message types** — what payloads are exchanged (with field descriptions)
- **Lifecycle** — connection setup, teardown, reconnection
- **Error handling** — timeouts, retries, fallbacks

## Section 5: External Integrations

Document all external system integrations.

```
## External Integrations

| Integration | Purpose | Module | Auth Method |
|-------------|---------|--------|------------|
```

For each integration, document:
- **API/SDK used** — specific library or API
- **Data exchanged** — what goes in and out
- **Error handling** — retry strategy, fallbacks, circuit breakers
- **Credentials** — how secrets are managed (env vars, Keycloak, vault)

## Section 6: Infrastructure

Document the Docker Compose topology and any cloud infrastructure.

```
## Infrastructure

### Docker Compose Services

| Service | Image | Ports | Volumes | Depends On |
|---------|-------|-------|---------|-----------|
| api | custom | 3000 | — | postgres, keycloak |
| web | custom | 5173 | — | api |
| postgres | postgres:16 | 5432 | pgdata | — |
| keycloak | keycloak/keycloak | 8080 | — | postgres |
<!-- Add project-specific services -->

### Environment Variables

| Variable | Service | Purpose |
|----------|---------|---------|
| DATABASE_URL | api | Prisma connection string |
| KEYCLOAK_URL | api | Identity provider URL |
| KEYCLOAK_REALM | api | Keycloak realm name |
| KEYCLOAK_CLIENT_ID | api | OIDC client identifier |
| KEYCLOAK_CLIENT_SECRET | api | OIDC client secret |
| AWS_S3_BUCKET | api | File storage bucket |
| AWS_S3_REGION | api | S3 region |
<!-- Add project-specific env vars -->
```
