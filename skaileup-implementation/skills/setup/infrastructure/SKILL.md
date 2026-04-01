---
name: "infrastructure"
description: "Sets up custom backend infrastructure from the architecture doc. Implements custom NestJS/backend modules, provider abstractions (real + in-memory), additional processes, and communication infrastructure (WebSocket, SSE). Only needed when architecture.md defines custom_modules or processes beyond the standard stack. Reads _concept/3_blueprint/2_architecture/architecture.md."
metadata:
  version: "1.0.0"
  tags:
    - "infrastructure"
    - "backend"
    - "modules"
    - "providers"
    - "websocket"
    - "sse"
    - "adapters"
    - "integration"
    - "nestjs"
  source: "SAXE"
  prerequisites:
    files:
      - path: "_concept/3_blueprint/2_architecture/architecture.md"
        gate: hard
        description: "Architecture doc defines custom_modules and processes that this skill implements"
      - path: "backend"
        gate: hard
        description: "Project must be scaffolded with a backend directory before infrastructure can be added"
    reads:
      - path: "_concept/3_blueprint/1_techstack/stack.md"
        description: "Tech stack for backend framework patterns and library choices"
      - path: "_concept/2_experience/2_features"
        description: "Features to understand which infrastructure modules are needed"
    produces:
      - path: "backend/libs"
        description: "Custom backend modules, providers, and service abstractions"
      - path: "docker-compose.yml"
        description: "Container orchestration for databases and external services"
      - path: ".env.example"
        description: "Environment variable template for infrastructure configuration"
---

# Infrastructure — Custom Backend Modules and Services

> **Stack note:** This skill implements custom backend infrastructure using a
> layered module pattern. The layer structure (shared contracts, providers,
> platform services, communication, additional processes) is broadly applicable,
> but the concrete implementation details (NestJS module/service patterns,
> `@fastify/websocket`, `docker-compose.yml`) assume a NestJS-based backend.
> If your stack uses a different backend framework, adapt the patterns accordingly
> from your tech stack profile.

## Overview

Implements custom backend infrastructure defined in `architecture.md`:
provider abstractions, platform services, communication infrastructure
(WebSocket/SSE), and additional processes. Builds bottom-up in 5 layers
to ensure clean dependency ordering.

**Only needed when** `architecture.md` defines `custom_modules` or `apps`
beyond the standard stack defaults.

## When to Use

- `architecture.md` exists and defines custom_modules, protocols, or external_integrations
- User says "set up infrastructure", "implement backend modules", "set up external integrations"
- The `implement` orchestrator dispatches this as Phase 3.5

## When NOT to Use

- No `architecture.md` or it has only standard modules — skip this skill
- For standard CRUD features — use `implement-feature`
- For initial project setup — use `scaffold`

## Prerequisites

**Hard gates:**
1. `_concept/3_blueprint/2_architecture/architecture.md` exists
2. Project has been scaffolded (backend/ directory exists)

---

ROLE  Infrastructure agent — implements custom backend modules, provider abstractions, processes, and communication from the architecture doc.

READS
  _concept/3_blueprint/2_architecture/architecture.md    — primary: custom_modules, protocols, integrations, data flows
  _concept/3_blueprint/1_techstack/stack.md               — additional tech requirements
  _concept/2_experience/2_features/**/*.md                — features referencing infrastructure

WRITES
  backend/libs/<module>/src/                  — custom backend module code
  backend/apps/<process>/src/                 — additional process entry points
  frontend/src/lib/<name>.types.ts            — frontend-only type exports (if needed)
  docker-compose.yml                          — additional services
  .env.example                                — new environment variables

REFERENCES
  skaileup-shared/contracts/concept_structure.md  — canonical _concept/ paths
  references/layer_patterns.md      — implementation patterns per layer, provider template
  references/dependency_mapping.md  — external integration → npm package mapping

MUST  read architecture.md before any work
MUST  follow module dependency graph (implement bottom-up by layer)
MUST  create both real AND in-memory implementations for every provider
MUST  ensure real implementations degrade gracefully when credentials missing
MUST  ensure in-memory implementations return deterministic, configurable responses
MUST  commit once per implementation layer
MUST  verify build after each layer
NEVER create mock implementations using setTimeout or fake delays
NEVER skip in-memory adapters (required for stateless dev and E2E testing)
NEVER hardcode credentials or API keys
NEVER modify _concept/ files

EMIT [infrastructure] started run_id=<uuid> modules=<N> processes=<M> integrations=<K>

# ── Workflow ──────────────────────────────────────────────────────

STEP 1: Parse architecture document
  - Read architecture.md frontmatter: apps[], custom_modules[], protocols[], external_integrations[]
  - Read architecture.md body: module dependency graph, data flows, protocol specs
  - Read feature specs to verify all infrastructure consumers are accounted for

STEP 2: Classify into layers (implement bottom-up)
  Layer 1: Shared contracts (type-only, no runtime deps)
  Layer 2: Provider abstractions (interface + real + in-memory adapter)
  Layer 3: Platform services (modules consuming providers)
  Layer 4: Communication infrastructure (WebSocket, SSE)
  Layer 5: Additional processes (separate entry points)

CHECKPOINT infrastructure_plan
  > "I need to set up [N] custom backend components to support features like [examples].
  >
  > Technical details: Layers: 5, Modules: [list]
  >
  > Approve to proceed."

STEP 3: Install dependencies
  - Install npm packages per architecture integrations
  - Add TypeScript path aliases for each custom module
  - Add dev scripts for additional processes
  $ git commit -m "chore: install infrastructure dependencies"

STEP 4: Layer 1 — Shared contracts
  - For each shared contract module:
    - Create module directory
    - Define message type schemas (from architecture protocol specs)
    - Create codec utilities (encode/decode with validation)
    - Create barrel index
  IF frontend needs these types
    - Create frontend type-only exports
  $ git commit -m "feat(infra): add shared contracts"
  EMIT [infrastructure] layer_complete layer=1

STEP 5: Layer 2 — Provider abstractions
  - For each provider:
    - Create interface with method signatures from architecture
    - Create real implementation wrapping external SDK
    - Create in-memory implementation with deterministic responses
    - Create module with forRoot(config) — real or in-memory based on config
  $ git commit -m "feat(infra): add provider abstractions"
  EMIT [infrastructure] layer_complete layer=2

STEP 6: Layer 3 — Platform services
  - For each platform service:
    - Create module + service
    - Inject provider tokens from Layer 2
    - Implement orchestration logic from architecture data flow sections
    - Register in main app module
  $ git commit -m "feat(infra): add platform services"
  EMIT [infrastructure] layer_complete layer=3

STEP 7: Layer 4 — Communication infrastructure
  IF protocols include websocket
    - Create WebSocket route handler with session management
    - Create frontend hook (useWebSocket) with auto-reconnect
  IF protocols include sse
    - Create SSE endpoint controllers
    - Create frontend hook (useSSE) with reconnect
  $ git commit -m "feat(infra): add communication infrastructure"
  EMIT [infrastructure] layer_complete layer=4

STEP 8: Layer 5 — Additional processes
  - For each additional process:
    - Create process entry point
    - Implement runtime from architecture description
    - Include stub mode when external credentials missing
    - Add dev script
    - Add Docker Compose service
  $ git commit -m "feat(infra): add additional processes"
  EMIT [infrastructure] layer_complete layer=5

STEP 9: Wire into main app
  - Import all custom modules into main app module
  - Update environment config to document new env vars
  - Update docker-compose.yml with new services
  - Update .env.example files
  $ git commit -m "feat(infra): wire modules into main app"

STEP 10: Verify
  - Build all (backend + frontend)
  - Start all processes — verify connectivity (WebSocket handshake, SSE stream)
  - Update _implementation/progress.json: infrastructure phase → approved
  EMIT [infrastructure] completed run_id=<uuid> layers=5 modules=<N> processes=<M>

CHECKPOINT infrastructure_complete
  > "Custom backend is ready. Your app can now [list capabilities in business terms].
  >
  > Technical details: Modules: N, Processes: M, Build: passed, Connectivity: verified
  >
  > Approve to continue."

CHECKLIST
  - [ ] Architecture parsed — all modules, processes, integrations identified
  - [ ] Layer plan presented and approved
  - [ ] Dependencies installed, path aliases configured
  - [ ] Layer 1: Shared contracts compile and export types
  - [ ] Layer 2: All providers have interface + real + in-memory implementations
  - [ ] Layer 3: Platform services registered and injecting providers
  - [ ] Layer 4: Communication protocols functional (verified handshake/stream)
  - [ ] Layer 5: Additional processes start without crashes
  - [ ] Full build passes
  - [ ] progress.json and PLANS.md updated
