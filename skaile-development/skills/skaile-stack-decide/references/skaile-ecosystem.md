# Skaile Dev Ecosystem -- Agent Context Pack

A condensed reference for AI agents reasoning about how to add AI to a new
or existing application using the full Skaile dev ecosystem. Companion to
`skaile-agent-framework.md` (which covers the runtime stack only);
this file zooms out to the whole monorepo: agent-framework + ai-assets +
ai-assets-skaile + ai-assets-skaileup + forge + platform.

Hand both docs to a fresh agent before discussing app ideas; together they
capture the runtime primitives, the skill catalog, the reference apps, and
the enterprise platform without requiring the agent to read every CLAUDE.md
in the repo.

Out of scope for this doc: `infra/` (Hetzner / Docker / Jenkins IaC) and
`marketing/` (Astro standalone site).

---

## 1. The Ecosystem in One Picture

```
                      AI-augmented app you want to build
                                    |
                                    v
                  +-------------------------------------+
                  |  Pick an integration shape (sec 4)  |
                  +-------------------------------------+
                          |          |           |
                  Tier 1-2 |    Tier 3 |     Tier 4
                  (in-proc)|  (subproc)| (multi-tenant)
                          v          v           v
                       agent-framework -- the runtime stack
                          |          |           |
                          v          v           v
              +-------------------------------------------+
              | Capabilities provided by the framework:   |
              |  - drivers (omp, claude-sdk)              |
              |  - mounts + connectors (resources)        |
              |  - sessions, persistence, fan-out         |
              |  - flow engine                            |
              |  - skill model + agent definitions        |
              +-------------------------------------------+
                          |
                          | configured by
                          v
              +-------------------------------------------+
              | skaile.yaml (per workspace)               |
              +-------------------------------------------+
                          ^
                          |
        +-----------------+---------------------------+
        |                 |                           |
   ai-assets/        ai-assets-skaile/         ai-assets-skaileup/
   (skills/agents/   (built-in mount drivers   (concept + build +
   experts/research  + connector adapters)     quality pipelines)
   /writing/use)     -> @skaile/base-assets    -> domain SKILL.md files
                          ^                           ^
                          |                           |
                          +--------- referenced by ---+
                                       |
                          +------------+-------------+
                          |                          |
                        forge/                   platform/
                  reference apps L2-L5      enterprise web app
                  (TUI, Nuxt apps,          (NestJS + React 19 +
                  collaborative editor)     vm-agent containers)
```

Two natural reading paths through this doc:

1. **Greenfield**: "I have an app idea." -> Section 2 (vocabulary) -> Section 4
   (where in the ladder you land) -> Section 7 (patterns).
2. **Brownfield**: "I have an existing app, what can I bolt onto it?" ->
   Section 2 -> Section 5 (capability inventory) -> Section 7.

---

## 2. The Repo Map

The dev shell repo (`skaile-dev`) aggregates 9 submodules; we cover 7 of them
here.

| Submodule | Role | When you reach for it |
|---|---|---|
| `agent-framework/` | Runtime stack (16 packages, `@skaile/agent-*`) | Always -- everything else builds on it |
| `ai-assets/` | Skill catalog: dev workflow, experts, research, writing, external service wrappers | When the agent needs domain capabilities or you want to author your own |
| `ai-assets-skaile/` | `@skaile/base-assets`: built-in mount drivers + connector adapters with manifests | When the agent needs to reach data sources; or you write a new connector |
| `ai-assets-skaileup/` | Concept/build/quality pipeline skills (14 domains, flow-driven) | When you want to *generate* an app or run quality gates on one |
| `forge/` | Reference apps L2-L5: TUI, Nuxt assistant, collaborative editor | Templates to copy-paste from |
| `platform/` | Enterprise platform (NestJS + React 19 + vm-agent containers) | Multi-tenant SaaS / reference for enterprise patterns |
| `theme/` | Design tokens (colors, fonts, Tailwind preset) | Brand consistency |

Excluded by this doc: `infra/` (deploy infra), `marketing/` (Astro site).

Shared resources that live in the shell repo itself: `theme/`, `docs/` (the
Starlight site at the monorepo root), `_scripts/`.

---

## 3. The Layers Stack (mental model)

Think in five horizontal layers; each one builds on the layers beneath:

```
+---------------------------------------------------------------+
| 5. APPLICATION                                                |
|    forge/L2-tui, forge/L4-project, forge/L5-concept,          |
|    platform/, your app                                        |
+---------------------------------------------------------------+
| 4. ORCHESTRATION                                              |
|    flows (multi-step pipelines), bundles (group-installed     |
|    skill+agent sets), domains (skill catalogs)                |
+---------------------------------------------------------------+
| 3. CAPABILITIES                                               |
|    skills (SKILL.md), agent definitions (SOUL/RULES/agent.yaml|
|    /knowledge), MCP servers, framework fragments              |
+---------------------------------------------------------------+
| 2. RESOURCES                                                  |
|    mounts (filesystem) + connectors (tool calls), declared in |
|    skaile.yaml; secrets via env/forge/oauth/op/kp providers   |
+---------------------------------------------------------------+
| 1. RUNTIME                                                    |
|    drivers (omp, claude-sdk), sessions (LocalRuntime,         |
|    AgentClient), transports (in-process, WebSocket)           |
+---------------------------------------------------------------+
```

`skaile-agent-framework.md` covers Layers 1-2 in depth (with code).
This document focuses on Layers 3-5 and how the ecosystem repos populate them.

---

## 4. Where Your App Idea Lands

A two-step decision: pick a **runtime tier** (how the agent process lives),
then pick a **reference app** to mimic in `forge/` or `platform/`.

### Tier matrix

| Tier | Pattern | Concurrent sessions | Persistence | Process model | Reference |
|---|---|---|---|---|---|
| 1 | `createAgentSession()` from `@skaile/agent-sdk` | 1 | Buffer only | In-process | `skaile repl` (in `agent-framework/tui/`) |
| 2 | Custom session class + `LocalRuntime` + registry | N | Buffer or your own | In-process | `forge/L2-tui` |
| 3 | `AgentProcessManager` + `SessionDispatcher` + `MessageStore` | N per user/entity | JSONL or your DB | Subprocess (WS IPC) | `forge/L4-project`, `forge/L5-concept` |
| 4 | Container-per-session, gateway, container manager | N x M (multi-tenant) | Postgres + durable mirrors | Container (Docker / vm-agent) | `platform/` |

### Reference-app cheat sheet

| Your app idea looks like... | Mimic | Why |
|---|---|---|
| CLI tool, single AI session | `skaile repl` (Tier 1) | One factory call; covered by `@skaile/agent-tui` |
| Operator console with N AI sessions | `forge/L2-tui` (Tier 2) | Profile YAML pattern, ink layout, `SessionRegistry` |
| Slack/Mattermost/Discord bot | (deprecated `forge/L3-mattermost`) | Lazy session per thread, dual output routing |
| Personal AI web assistant | `forge/L4-project` (Pichi) or `forge/L4-assistant` | SSE + JSONL + cookie auth + asset catalog UI |
| Multi-user real-time collaborative AI editor | `forge/L5-concept` | Hocuspocus + Yjs + flow engine + per-document sessions |
| Multi-tenant SaaS with org/project/session hierarchy | `platform/` | Container manager, gateway, durable flow mirror, voice IO |
| AI co-design / spec generation pipeline | `ai-assets-skaileup/` flows | Concept pipeline produces structured `_concept/` from a brief |
| Vertical domain expert (legal/medical/ops) | New `ai-assets/<my-domain>/` | Author SKILL.md + agent definition + bundle |
| Add data source to an existing agent | New connector in `ai-assets-skaile/` | Adapter + `CONNECTOR.md` + register in framework |

### Forge ladder (one-line each)

| Level | App | Pattern |
|---|---|---|
| L2 | `L2-tui` | Multi-session ink TUI: profile YAML -> `LocalRuntime`, `SessionRegistry`, log viewer, settings |
| L3 | (archived `L3-mattermost`) | Bot per thread: lazy `LocalRuntime` start on first message, SQLite-per-channel state |
| L4 | `L4-project` (Pichi) | Personal AI web assistant: subprocess agent + JSONL + SSE, SQLite multi-user auth, WebDAV/SSH access |
| L4 | `L4-assistant` | Simpler personal AI assistant variant focused on chat |
| L5 | `L5-concept` | Multi-user collaborative concept editor: Hocuspocus + Yjs + flow engine, voice IO, AI flows generate `_concept/` specs |

Common backend pieces are extracted into `forge/common-backend` (auth,
settings, workspace service, `AgentProcessManager`, `JsonlMessageStore`) and
common UI pieces into `forge/common-ui` (TipTap editor, catalog browser,
settings manager).

---

## 5. Capability Inventory (what's already on the shelf)

Use this as the "do I need to build it?" checklist. If a capability appears
here, you reuse it; you don't reimplement it.

### From the runtime (`agent-framework/`)

- Pluggable LLM drivers (omp, claude-sdk) selectable by config
- Session state machine (`starting -> idle <-> active <-> waiting -> stopped`)
- Subprocess + WebSocket IPC (process isolation)
- Message persistence (`JsonlMessageStore`, custom `MessageStore`)
- Subscriber fan-out (SSE, multiple listeners per session)
- Flow engine (DAG nodes, approval/input gates, pure state machine)
- Settings resolution (CLI -> env -> project -> global -> defaults)
- Secret resolution chain (env / forge / oauth / op / kp)
- Telemetry (Noop or OTLP -> Langfuse)
- Prompt fragments injected per installed capability

### Built-in mounts (`ai-assets-skaile/mounts/`)

| Driver | Purpose |
|---|---|
| `local` | Bind-mount a host directory |
| `git` | Clone + sync a git repository (with session lifecycle hooks) |
| `s3` | S3 bucket sync |
| `webdav` | WebDAV server sync |
| `sharepoint` | SharePoint delta sync |

### Built-in connectors (`ai-assets-skaile/connectors/`)

Data:

| Adapter | Role |
|---|---|
| `memory` | In-process KV (testing, ephemeral state) |
| `postgres` | Query/schema/execute against PostgreSQL |
| `redis` | KV ops (get/set/keys/del) |
| `sqlite` | Embedded SQL (sql.js, no external server) |
| `xstate` | State machine driven via tools |
| `xstate-store` | Lightweight reactive KV |
| `yjs` | CRDT shared state for real-time collaboration |
| `gmail` | OAuth + Gmail API |
| `minio` | S3-compatible object store |
| `mattermost` | REST API v4 (no npm deps) |

Service:

| Adapter | Role |
|---|---|
| `devserver` | Manage dev server processes (Vite, Nuxt, ...) |
| `static-server` | Bun static file server |
| `tunnel` | Network tunnels (cloudflare, ngrok) |
| `deploy` | Build + push to remote (fly, vercel, ...) |

Each adapter has a `CONNECTOR.md` declaring npm peer deps, configuration
fields (typed, with `sensitive: true` for secrets), and a health-check
operation. The framework reads these to auto-install peers and validate
config.

### Skills and domains (`ai-assets/`)

10 domains shipped today; each has a `DOMAIN.md` index. Highlights:

| Domain | What it gives you |
|---|---|
| `skaile-development/` | 30+ workflow skills for the monorepo (git, test, audit, implement, doc, devlog, review, release, ...) -- many usable on any TS monorepo with light edits |
| `skaile-platform/` | Platform-specific tasks (e2e harness, preview ops) |
| `forge-project/` | Forge app management skills |
| `dev-implementation-experts-js/` | Deep JS/TS expertise: Nuxt, Directus, TipTap, PrimeVue, SDK building, AI integration patterns |
| `dev-implementation-experts-python/` | Python, Pydantic AI, Marimo |
| `dev-implementation-experts-typst/` | Typst document generation + expert advisor router |
| `knowledge-research/` | Deep research, paper extraction |
| `knowledge-writing/` | Long-form writing from research (books, podcasts) |
| `use/` | External service wrappers (Exa, Perplexity, Outline, ElevenLabs, Deepgram) |
| `ai-asset-management/` | Meta: build new skills/domains/CLIs, navigate the catalog |

### Concept-to-app pipelines (`ai-assets-skaileup/`)

14 domains forming a concept -> implementation pipeline. Two reading paths:

**Concept pipeline (greenfield):**
```
grounding -> discovery -> experience -> architecture -> datamodel
                                    \-> concept-mockup
                                    \-> concept-storybook
```
Produces a structured `_concept/` directory: brief, journeys, features,
screens, components, techstack choice, data model.

**Build pipeline (uses concept output):**
```
build (autonomous) | build-supervised (git-prepare/brainstorm/plan/finish)
                                    -> quality (audit, tests, readiness)
                                    -> lab (validate skills themselves)
```

Cross-cutting: `concept-ops` (review/eval/drift/sync), `contracts` (shared
schemas referenced by every skill).

When useful: any time you want an AI to design and build (or audit) an app
end-to-end. The flows are JSON definitions consumable by the runner's flow
engine.

---

## 6. The Platform -- Enterprise Reference

`platform/` is what production-grade Tier 4 looks like. It is also a
reference for which enterprise concerns the framework alone does not solve.

### Stack

- Backend: NestJS + Fastify + Prisma + tRPC + PostgreSQL + Keycloak
- Frontend: Vite + React 19 + TanStack (Router/Query/Form/Table) + Tailwind 4
- Agent host: vm-agent Docker containers, WebSocket IPC via `agent-gateway`
- Generated by PostXL (`postxl-schema.json` is source of truth -- never edit `prisma/generated/` directly)
- Test: Jest (backend), Vitest (frontend), Playwright (E2E)

### Data hierarchy

```
Organization -> Project -> Session -> Conversation -> Message
                  |
                  +-- source type: Git | SharePoint | LocalFolder | Empty
                  +-- mounts (filesystem-bound)
                  +-- connectors (tool-bound)
```

### Notable backend libraries (`platform/backend/libs/`)

| Library | Role |
|---|---|
| `session` | Session workspace lifecycle: hibernate/wake, idle detect, context restore |
| `agent-gateway` | WebSocket bridge to vm-agent containers; connection pool; resource/state cache |
| `ai` / `ai-provider` | Provider-agnostic LLM dispatch |
| `skill-registry` | Catalog of installed skills/agents/flows |
| `deployment-manager` | Phase-based pipeline engine (PostXL generation runs) |
| `preview-manager` / `preview-proxy` | Generated-app preview lifecycle (start/refresh/stop) |
| `voice` | STT (Deepgram) + TTS (ElevenLabs) WebSocket protocol |
| `git-provider` / `sharepoint-provider` / `nextcloud-provider` / `s3` | Project source backends |
| `credential` / `secrets-provider` | Multi-provider secret resolution |
| `artifact-manager` / `excel-extraction` / `excel-io` / `xlport` | Document handling |
| `import` / `mail` / `upload` | Ingest / outbound mail / upload |

### Flow execution surface (tRPC)

`workspace.startFlow`, `approveFlowNode`, `provideFlowInput`, `cancelFlow`,
`setFlowAutonomousMode`, `retryFlowNode`, `getActiveFlow`. All resolve the
active container via `sessionLifecycle.resolveWorkspaceSession` and call
into `FlowExecutionSyncService`, which mirrors runtime `state_changed`
events into Prisma `FlowExecution` rows.

### Protocol extensions on top of base agent-framework messaging

Documented at `platform/docs/protocol-extensions.md`:

- Inline text markers in agent output (`[REACT]`, `[PASS]`, `[CUSTOM]`)
- `MessageReaction` table (per-user reactions on agent messages)
- Voice protocol (TTS playback + STT mic streaming over WebSocket)
- Custom AAP (Agent Augmented Page) components rendered from agent events
- Multi-user features (mentions, reply threading, sender attribution)

### Architecture documents to reach for

| Topic | File |
|---|---|
| Project / session model, hibernation, wake | `platform/docs/session-lifecycle.md` |
| Subfolder-scoped sessions with mount isolation | `platform/docs/scoped-sessions.md` |
| Sequence diagrams for message flow | `platform/docs/session-flow-diagrams.md` |
| Turn-based flow execution + durable mirror | `platform/docs/flow-execution-architecture.md` |
| Skill / flow catalog system | `platform/docs/asset-catalog-architecture.md` |
| Preview contract + lifecycle | `platform/docs/preview-contract.md`, `preview-lifecycle.md` |
| Storage architecture (virtual FS, mounts, sync) | `platform/docs/storage-architecture.md` |
| Integration architecture (auth, policy, security) | `platform/docs/integration_architecture.md` |
| Protocol extensions reference | `platform/docs/protocol-extensions.md` |

---

## 7. Patterns by App Shape

### "Add a chatbot to my web app"

- Tier 3, subprocess + dispatcher.
- Mount the relevant project files via `mounts:` (`local` driver).
- Persist with `JsonlMessageStore` (or roll your own `MessageStore` against your DB).
- Frontend: SSE -> `@skaile/agent-store/react` for streaming text + tool events.
- Optional: connectors for app data (`postgres`, `redis`).
- Reference: `forge/L4-project`.

### "AI co-pilot over our Postgres + S3 data"

- Connectors: `postgres` (`auth: env:DATABASE_URL`) + `s3` mount.
- Agent definition with `RULES.md` restricting destructive ops.
- `claude-sdk` driver for native MCP tool use.
- Tier 1 if single-user, Tier 3 if multi-user.

### "Multi-step content pipeline (research -> draft -> review -> publish)"

- Define a flow JSON with skill nodes + edges.
- Each step is a SKILL.md with `reads_from` / `writes_to` declared.
- Approval gate between draft and publish.
- Stream `state_changed` events to a progress UI.
- Mirror flow state to DB if cross-restart resume is needed (see platform's `FlowExecutionSyncService`).

### "Real-time collaborative AI workspace"

- L5-concept pattern: Hocuspocus + Yjs for shared docs + a `yjs` connector
  so the agent reads/writes the same CRDT.
- Per-document sessions via `AgentProcessManager`.
- Voice IO via Deepgram + ElevenLabs (see `platform/backend/libs/voice`).

### "Vertical domain expert (legal, medical, ops)"

- Author a domain at `ai-assets/<my-domain>/` with:
  - `skills/<task-name>/SKILL.md` for each capability
  - `agents/<expert-name>/` with `SOUL.md`, `RULES.md`, `agent.yaml`, `knowledge/`
  - `<my-domain>.bundle.yaml` for one-shot install
- Add domain-specific connectors (CRM, ticketing, KB) to `ai-assets-skaile/`
  if needed.
- Install on the project: `skaile install bundle:my-domain`.

### "Embed an agent in an existing CLI"

- Tier 1: `createAgentSession()`. Mount cwd as a `local` mount.
- For interactive REPL, reuse `@skaile/agent-tui` (`skaile repl`) instead
  of building from scratch.

### "Generate an entire app from a brief" (concept-to-code)

- Run the skaileup concept pipeline:
  - `skaileup-grounding` to onboard + research
  - `skaileup-discovery` for brief, brand identity
  - `skaileup-experience` for journeys, features, screens
  - `skaileup-architecture` + `skaileup-datamodel` for techstack + data
- Then `skaileup-build` (or supervised `skaileup-build-supervised`) emits
  scaffolding + features.
- `skaileup-quality` runs audit + test generation + readiness gates.
- This is the path the Skaile platform automates for end users.

### "Audit / improve an existing skill or codebase"

- `skaileup-quality` for code audit, test generation, readiness gates.
- `skaileup-lab` for skill testing in Docker-isolated runs (validators).
- `ai-assets/skaile-development/skills/audit`, `test`, `review` for
  monorepo-aware variants used inside skaile-dev.

### "Multi-tenant SaaS that hosts agents per customer"

- Tier 4 with the platform as reference.
- `IContainerManager` impl (Mock, Local, Docker, Nix) for your hosting env.
- Org/project/session data hierarchy (Postgres).
- Gateway with connection pool + resource/state cache.
- Durable flow mirror + breadcrumbs for resume across restarts.
- Keycloak (or equivalent OIDC) for auth.

---

## 8. Decision Cheat-sheet (12 questions to ask)

1. **User-facing surface?** Terminal / web / mobile / voice / embed.
2. **Concurrent sessions per process?** 1 -> Tier 1. N -> Tier 2-3. N x M -> Tier 4.
3. **Process isolation needed?** No -> `LocalRuntime`. Yes -> `AgentProcessManager` or container.
4. **Persistence?** None -> output buffer. Per-session -> JSONL. Cross-restart resume -> DB.
5. **Multiple frontends per session?** Yes -> `SubscriberTransport` fan-out (SSE / WS).
6. **Multi-step orchestration?** Yes -> flow engine + JSON flow def.
7. **Custom agent identity?** Yes -> author SOUL/RULES/agent.yaml/knowledge.
8. **External data sources?** Filesystem -> mount. APIs/databases -> connector. Tool service -> MCP.
9. **Secrets?** env / forge / oauth / op / kp providers; declare in YAML, never hardcode.
10. **Driver choice?** Anthropic only -> `claude-sdk` (in-process, native MCP). Multi-provider -> `omp` (subprocess, model roles).
11. **Real-time shared state?** Yjs connector (CRDT) or XState connector (turn-based machine).
12. **Multi-tenant?** Yes -> Tier 4: container manager, gateway, durable mirrors, OIDC.

---

## 9. What to Reuse vs Build (full ecosystem)

| Concern | Reuse | Repo |
|---|---|---|
| LLM driver | `agent-bridge` (omp, claude-sdk) | `agent-framework/` |
| Session state | `agent-sdk` or `agent-session` | `agent-framework/` |
| Persistence | `JsonlMessageStore` or custom `MessageStore` | `agent-framework/` |
| Streaming to UI | `EventEmitterSubscriberTransport` + `agent-store` (React/Vue) | `agent-framework/` |
| Process isolation | `AgentProcessManager` | `forge/common-backend` |
| Mounts (filesystem) | Built-ins: `local`, `git`, `s3`, `webdav`, `sharepoint` | `ai-assets-skaile/mounts/` |
| Connectors (tool-accessed) | Built-ins: postgres / redis / sqlite / yjs / gmail / minio / mattermost / xstate / etc. | `ai-assets-skaile/connectors/` |
| Skill capabilities | Existing skills in `ai-assets/` (or new ones in your own domain) | `ai-assets/` |
| Concept->code pipeline | Skaileup flows + skills | `ai-assets-skaileup/` |
| Code audit / tests / readiness | `skaileup-quality` + `skaile-development/skills/audit\|test\|review` | `ai-assets-skaileup/`, `ai-assets/` |
| Multi-user web app shell | `forge/L4-project` (cookie auth, SQLite, settings UI) | `forge/` |
| Real-time collab editor | `forge/L5-concept` (Hocuspocus, Yjs, voice) | `forge/` |
| Multi-tenant container hosting | `platform/` (vm-agent, gateway, container manager) | `platform/` |
| Voice IO | `platform/backend/libs/voice` (Deepgram + ElevenLabs) | `platform/` |
| Preview / sandboxing of generated apps | `platform/backend/libs/preview-manager` + `preview-proxy` | `platform/` |
| Brand styling | `theme/` design tokens + Tailwind preset | `dev/theme/` |

Build yourself only when nothing here fits. The bias is strong toward reuse.

---

## 10. Authoring Quick References

When the agent needs to *create* something new in this ecosystem.

### A new skill

Path: `ai-assets/<domain>/skills/<skill-name>/SKILL.md`. Use the
`skill-builder` skill for guided scaffolding.

```yaml
---
name: my-skill
description: "What it does and when to invoke (trigger keywords matter)"
metadata:
  version: "1.0.0"
  tags: [debugging, analysis]
  stage: alpha
  user_inputs:
    dialog:
      - id: target
        label: "What to analyze"
        type: text
        required: true
  reads_from: ["src/"]
  writes_to: ["_reports/my-skill.md"]
---

## Instructions
[Markdown body the agent reads at execution time. Keep <500 lines.
Move detailed reference material into ./references/.]
```

### A new domain

Use the `domain-builder` skill. Required structure:

```
ai-assets/<domain>/
├── DOMAIN.md
├── skills/
├── agents/        # optional
├── flows/         # optional
└── <domain>.bundle.yaml
```

### A new mount driver or connector adapter

Path: `ai-assets-skaile/mounts/<name>/` or `ai-assets-skaile/connectors/<name>/`.
Convention from `ai-assets-skaile/CLAUDE.md`:

1. Implement `driver.ts` extending `AbstractMountDriver` (or
   `AbstractConnectorAdapter`); export `createDriver()` (or `createAdapter()`).
2. Add `MOUNT.md` (or `CONNECTOR.md`) with `name`, `entry`, `fields`,
   `npm_deps`, `health_check`, `keywords`.
3. Add subpath export to `package.json` `"exports"`.
4. Register in `agent-framework/connectors/src/{mount,connector}-registry.ts`.

`name` in the manifest must match the registry key and the `driver:` /
`adapter:` field used in `skaile.yaml`.

### A new flow

JSON/YAML at `ai-assets-skaileup/<domain>/flows/<flow>.flow.yaml` (or
project-local `<projectDir>/.skaile/flows/<flow>/<flow>.flow.yaml`). Schema
at `ai-assets-skaileup/skaileup-contracts/flow.schema.json`. Nodes are
typed (`type: skill` is the common case); edges declare dependencies.

### A new agent definition

```
ai-assets/<domain>/agents/<agent-name>/
├── SOUL.md
├── RULES.md
├── agent.yaml
└── knowledge/
    ├── index.yaml
    └── *.md
```

Renderers in `@skaile/asset-manager` produce framework-specific outputs
(`.claude/agents/`, `.omp/agents/`, `.codex/agents/`) at install time.

---

## 11. The Pointers (where to read deeper)

| Topic | File |
|---|---|
| Runtime stack primitives + integration tiers | `skaile-agent-framework.md` (this folder) |
| End-to-end implementation patterns | `agent-framework/IMPLEMENTATION-GUIDE.md` |
| Driver internals + capabilities | `agent-framework/bridge/CLAUDE.md` |
| Mount/connector model + secret chain | `agent-framework/connectors/CLAUDE.md` |
| Built-in mounts/connectors source + manifests | `ai-assets-skaile/CLAUDE.md` |
| Skill catalog (domains overview) | `ai-assets/CLAUDE.md`, `ai-assets/README.md` |
| Concept/build/quality pipelines | `ai-assets-skaileup/CLAUDE.md`, `ai-assets-skaileup/README.md` |
| Skill authoring conventions | `dev/CLAUDE.md` (Skill Structure Convention section) |
| Per-app forge architecture | `forge/<app>/CLAUDE.md`, `forge/CLAUDE.md`, `forge/README.md` |
| Platform full developer guide | `platform/CLAUDE.md` |
| Platform protocol extensions | `platform/docs/protocol-extensions.md` |
| Platform session/scoped/flow lifecycle | `platform/docs/session-lifecycle.md`, `scoped-sessions.md`, `flow-execution-architecture.md` |
| Platform storage / integration architecture | `platform/docs/storage-architecture.md`, `integration_architecture.md` |
| Starlight (rendered docs site) | `docs/` at the dev repo root |

---

## 12. The Mental Model in One Paragraph

The Skaile dev ecosystem is a five-layer stack: a runtime
(`agent-framework`) under a resource model (mounts + connectors, with
adapters in `ai-assets-skaile`) under a capability layer (skills + agent
definitions + MCP, drawn from `ai-assets`) under an orchestration layer
(flows, bundles, domains, with the canonical concept-to-code pipeline in
`ai-assets-skaileup`) under an application layer (forge reference apps and
the enterprise platform). To add AI to an app, pick the runtime tier
(in-process, subprocess, or container), declare resources in `skaile.yaml`,
choose or compose an agent definition, and either reuse existing skills
or author new ones in your own domain. Mimic the forge app whose shape
matches yours, and reach into the platform for enterprise concerns
(multi-tenant containers, durable flow mirrors, voice IO, preview
sandboxing). The further left in this stack you build, the less you write;
the framework's job is to make the upper layers free.
