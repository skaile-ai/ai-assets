---
name: implement-1-setup-3-infrastructure
description: "Set up custom backend infrastructure from the architecture doc. This skill should be used when the user asks to 'set up infrastructure', 'implement backend modules', 'set up external integrations', 'configure custom services', or when the orchestrator runs Phase 1 setup. Reads _concept/3_blueprint/2_architecture/architecture.md and implements custom NestJS modules, provider abstractions, additional processes, and communication infrastructure."
keywords: infrastructure, backend, modules, providers, websocket, sse, adapters, integration
---

ROLE  Infrastructure agent — implements custom backend modules, provider abstractions, processes, and communication from the architecture doc.

READS
  _concept/3_blueprint/2_architecture/architecture.md     — primary input: custom modules, protocols, integrations, data flows
  _concept/3_blueprint/1_techstack/stack.md                 — additional tech requirements
  _concept/2_experience/2_features/**/*.md                   — features referencing infrastructure (completeness check)

WRITES
  backend/libs/<module>/src/                     — custom NestJS module code
  backend/apps/<process>/src/                    — additional process entry points
  frontend/src/lib/<name>.types.ts               — frontend-only type exports (if needed)
  docker-compose.yml                             — additional services
  .env.example                                   — new environment variables

REFERENCES
  shared/contracts/prerequisites.md                       — tool prerequisite checks
  shared/contracts/implementation_structure.md            — tracking files
  shared/contracts/git_workflow.md                        — branch and commit conventions
  shared/contracts/verification.md                        — Level 1 build verification
  references/layer_patterns.md                   — implementation patterns per layer, provider template, dynamic module pattern
  references/dependency_mapping.md               — external integration → npm package mapping, protocol → package mapping

REQUIRES
  hard: pnpm, git
  soft: docker (for additional process services)
  state: _concept/3_blueprint/2_architecture/architecture.md exists
  state: project has been scaffolded (backend/ directory exists)

# --- Workflow ---

STEP 1: Parse architecture document
  - Read architecture.md frontmatter: apps[], custom_modules[], protocols[], external_integrations[]
  - Read architecture.md body: module dependency graph, data flows, protocol specs, provider interfaces
  - Read feature specs to verify all infrastructure consumers are accounted for
  EMIT [implement-1-setup-3-infrastructure] started run_id=<uuid> modules=<N> processes=<M> integrations=<K>

STEP 2: Classify into layers
  - Sort components bottom-up by dependency:
    Layer 1: Shared contracts (type-only, no runtime deps)
    Layer 2: Provider abstractions (interface + real + in-memory adapter)
    Layer 3: Platform services (NestJS modules consuming providers)
    Layer 4: Communication infra (WebSocket, SSE)
    Layer 5: Additional processes (separate entry points)
  - Present layer plan to user with component list per layer
  - See references/layer_patterns.md for implementation pattern per layer

CHECKPOINT infrastructure_plan
  > "I need to set up [N] custom backend components to support features like [business examples from features].
  >
  > Technical details (if interested):
  >   Layers: 5, Modules: [list]
  >
  > Approve to proceed."

STEP 3: Install dependencies and configure paths
  - Install npm packages per references/dependency_mapping.md
  - Add TypeScript path aliases to backend/tsconfig.json for each custom module
  - Add dev scripts to backend/package.json for additional processes
  $ cd backend && pnpm add <packages from architecture>
  $ cd backend && pnpm add -D <dev packages>
  $ git commit -m "chore: install infrastructure dependencies"

STEP 4: Implement Layer 1 — Shared contracts
  - For each shared contract module:
    - Create backend/libs/<name>/src/ directory
    - Define Zod schemas for message types (from architecture protocol specs)
    - Create codec utilities (encode/decode with validation)
    - Create barrel index.ts
  IF frontend needs these types
    - Create frontend/src/lib/<name>.types.ts (TypeScript types only, no Zod)
  $ cd backend && pnpm run build
  $ git commit -m "feat(infra): add shared contracts (<list>)"
  EMIT [implement-1-setup-3-infrastructure] layer_complete layer=1 modules=<list>

STEP 5: Implement Layer 2 — Provider abstractions
  - For each provider (e.g., ai-provider, git-provider, cloud-provider):
    - Create interface file with method signatures from architecture doc
    - Create real implementation(s) wrapping external SDK
    - Create in-memory implementation with deterministic, configurable responses
    - Create NestJS dynamic module with forRoot(config) — real or in-memory based on config
    - Create typed config with env var mapping
    - See references/layer_patterns.md for the provider template
  $ cd backend && pnpm run build
  $ cd backend && pnpm run test:jest --passWithNoTests
  $ git commit -m "feat(infra): add provider abstractions (<list>)"
  EMIT [implement-1-setup-3-infrastructure] layer_complete layer=2 modules=<list>

STEP 6: Implement Layer 3 — Platform services
  - For each platform service:
    - Create NestJS module + service in backend/libs/<name>/src/
    - Inject provider tokens from Layer 2
    - Implement orchestration logic from architecture data flow sections
    - Register in api.module.ts imports
  $ cd backend && pnpm run build
  $ git commit -m "feat(infra): add platform services (<list>)"
  EMIT [implement-1-setup-3-infrastructure] layer_complete layer=3 modules=<list>

STEP 7: Implement Layer 4 — Communication infrastructure
  IF protocols include websocket
    - Register @fastify/websocket in main.ts via raw Fastify instance
    - Create Fastify plugin for WebSocket routes
    - Create session management (per-connection state, heartbeat)
    - Create frontend React hook (useAgentWebSocket) with auto-reconnect
  IF protocols include sse
    - Create NestJS controller with SSE endpoints
    - Wire to platform services that emit progress events
    - Create frontend React hook (useProgressSSE) with reconnect
  $ cd backend && pnpm run build
  $ cd frontend && pnpm run test:types
  $ git commit -m "feat(infra): add communication infrastructure (<protocols>)"
  EMIT [implement-1-setup-3-infrastructure] layer_complete layer=4 protocols=<list>

STEP 8: Implement Layer 5 — Additional processes
  - For each additional process beyond api and web:
    - Create backend/apps/<name>/src/main.ts entry point
    - Create backend/apps/<name>/tsconfig.json extending backend tsconfig
    - Implement runtime from architecture description
    - Include stub mode when external credentials not configured
    - Add dev script to backend/package.json
    - Add Docker Compose service
    - Create docker/<name>/Dockerfile
  $ cd backend && pnpm run build
  $ git commit -m "feat(infra): add additional processes (<list>)"
  EMIT [implement-1-setup-3-infrastructure] layer_complete layer=5 processes=<list>

STEP 9: Wire into API
  - Update backend/apps/api/src/api.config.ts with new env vars (Zod schema validation)
  - Update backend/apps/api/src/api.module.ts — import modules with forRoot(config)
  - Update backend/apps/api/src/main.ts — register Fastify plugins
  - Update docker-compose.yml with new services
  - Update all .env.example files with new env vars documented
  - All custom code in generated files uses @custom-start/@custom-end markers
  $ cd backend && pnpm run build
  $ git commit -m "feat(infra): wire modules into API"

STEP 10: Verify
  $ cd backend && pnpm run build
  $ cd backend && pnpm run test:types
  $ cd frontend && pnpm run test:types
  $ pnpm run lint
  - Start all processes — API + additional processes
  - Verify connectivity: WebSocket handshake, SSE event stream (if applicable)
  - Update _implementation/progress.json: set infrastructure phase to approved
  - Update _implementation/PLANS.md: check off infrastructure items
  EMIT [implement-1-setup-3-infrastructure] completed run_id=<uuid> layers=5 modules=<N> processes=<M>

CHECKPOINT infrastructure_complete
  > "Custom backend is ready. Your app can now [list capabilities in business terms — e.g., 'process AI requests', 'send real-time updates', 'connect to payment processing'].
  >
  > Technical details (if interested):
  >   Modules: N, Processes: M, Build: passed, Connectivity: verified
  >
  > Approve to continue."

# --- Constraints ---

MUST  read architecture.md before any work
MUST  follow module dependency graph (implement bottom-up by layer)
MUST  create both real AND in-memory implementations for every provider
MUST  ensure real implementations gracefully degrade when credentials not configured (warn, don't crash)
MUST  ensure in-memory implementations return deterministic, configurable responses
MUST  use @custom-start/@custom-end markers in generated files
MUST  commit once per implementation layer
MUST  verify build after each layer
NEVER  create mock/simulation implementations using setTimeout or fake delays
NEVER  skip in-memory adapters (required for stateless dev and E2E testing)
NEVER  hardcode credentials or API keys
NEVER  modify standard PostXL generated modules (only extend via custom blocks)
NEVER  modify _concept/ files
NEVER  use `any` to work around path resolution — fix tsconfig paths instead

CHECKLIST
  - [ ] Architecture doc parsed — all modules, processes, integrations identified
  - [ ] Layer plan presented and approved
  - [ ] Dependencies installed, tsconfig paths configured in BOTH backend AND frontend
  - [ ] Frontend tsconfig has paths for all backend libs used by router-trpc (check-frontend-paths.sh)
  - [ ] Layer 1: Shared contracts compile and export types
  - [ ] Layer 2: All providers have interface + real + in-memory implementations
  - [ ] Layer 3: Platform services registered and injecting providers
  - [ ] Layer 4: Communication protocols functional (handshake/stream verified)
  - [ ] Layer 5: Additional processes start and connect
  - [ ] API wired: all modules imported, plugins registered, env vars documented
  - [ ] Full build passes (backend + frontend + lint)
  - [ ] All processes start without crashes
  - [ ] progress.json and PLANS.md updated
