# Skaile Agent Framework -- Agent Context Pack

A condensed reference for AI agents reasoning about how to add AI capabilities
to a new or existing application using the Skaile agent framework. Hand this
file to an agent before discussing app ideas; it captures the framework's
mental model, primitives, integration tiers, and reference apps in roughly
one read.

---

## 1. What the Framework Is

A TypeScript runtime stack (`@skaile/agent-*` on npm) that turns any app into
an AI-augmented one without reinventing the LLM plumbing. It bundles:

- A **driver abstraction** so the same agent can run on Claude SDK, oh-my-pi,
  or any future backend with no code change.
- A **resource model** -- declare data sources (filesystems, git, S3, Postgres,
  Redis, MCP servers, ...) in `skaile.yaml`, the framework connects, mounts,
  injects tools, and resolves secrets.
- A **session model** with state machine, persistence, fan-out, and a single
  `IAgentRuntime` interface that scales from in-process to subprocess to
  remote container without changing call sites.
- A **flow engine** for multi-step pipelines (research -> draft -> review)
  with dependency tracking and approval/input gates.
- A **skill model** -- agent capabilities are markdown files (SKILL.md) that
  the agent reads and follows; no compilation, no deploy.
- **Portable agent identity** -- SOUL.md + RULES.md + knowledge/ as a plain
  directory rendered into Claude Code, OMP, or Codex shapes on install.

The unifying principle: *write the agent once, swap drivers/transports/hosts
through configuration*.

---

## 2. Core Concepts (the vocabulary)

### Runtime

| Concept | What it is |
|---|---|
| **AgentDriver** | Wraps an LLM backend (`omp`, `claude-sdk`). Single interface: `prompt(message) -> events`. |
| **IAgentRuntime** | The fundamental session interface: `prompt`, `reply`, `abort`, `dispose`, `onEvent`. Two impls: `LocalRuntime` (in-process) and `AgentClient` (remote/WebSocket). |
| **AgentSession** | Higher-level wrapper around a runtime that converts raw events into renderable `OutputLine`s and tracks state machine. |
| **AgentEvent** | Discriminated union of everything the agent emits: `text`, `tool_call`, `tool_result`, `question`, `finished`, `status`, `subagent`, `state_changed`, `resources_available`, `ui_render`, `connector_status`, `error`. Some are persistent, some transient. |
| **AgentCommand** | What the host sends in: `prompt`, `reply`, `cancel`, `shutdown`, `start_flow`, `configure`. |
| **SessionState** | `starting -> idle <-> active <-> waiting -> stopped`. `waiting` means the agent asked a question; the next `send()` becomes a `reply()`. |

### Configuration

| Concept | What it is |
|---|---|
| **skaile.yaml** | Per-workspace config: agent settings, mounts, connectors, MCP servers, telemetry. |
| **Settings resolution** | Layered: CLI/API overrides -> env vars -> `.skaile/settings.json` (project) -> walked-up parent settings -> `~/.skaile/settings.json` -> `skaile.yaml` defaults -> built-in defaults. |
| **Two-root pattern** | `projectDir` (cwd for the agent, the user's working directory) and `agentDir` (the agent definition with SOUL.md/RULES.md). Independent and either can be omitted. |
| **Secret resolution** | `env:VAR` \| `forge:key` \| `oauth:key` \| `op:ref` \| `kp:ref`. Prefixed refs go to one provider; bare refs waterfall. |

### Resources -- the dual model

The framework deliberately separates two concerns, mirroring Docker's
volume/bind-mount split:

| Concern | **Mounts** | **Connectors** |
|---|---|---|
| Agent access | Filesystem (read/write files in mounted paths) | Tool calls (`connector_read`, `{id}__query`, ...) |
| Config key | `mounts:` (or legacy `volumes:`) | `connectors:` |
| Examples | `local`, `git`, `s3`, `webdav`, `sharepoint` | `postgres`, `redis`, `sqlite`, `memory`, `xstate`, `yjs`, `gmail`, `mattermost`, `minio`, `devserver`, `staticserver`, `tunnel`, `deploy` |
| Manager | `MountManager` (mount/sync/watch lifecycle) | `ConnectorManager` (connect/CRUD dispatch/tool registry/watch) |
| Watch mechanism | Chokidar filesystem watchers | Operation callbacks |

Adapters/drivers live in `@skaile/base-assets` (`ai-assets-skaile/`); the
`connectors` package contains base classes and managers only. Each adapter has
a `CONNECTOR.md` (or `MOUNT.md`) descriptor declaring npm deps, config fields,
and health checks -- the catalog reads these, allowing auto-install of npm
peers and field-schema validation.

### Skills, Agents, Domains

| Concept | What it is |
|---|---|
| **Skill** | A `SKILL.md` file with YAML frontmatter (`name`, `description`, `metadata.user_inputs.dialog`, `reads_from`, `writes_to`) plus markdown instructions. The agent reads and follows the markdown at execution time. No compilation. |
| **Domain** | A directory of related skills with a `DOMAIN.md` index, optional `agents/`, `contracts/`, `flows/`, and a `*.bundle.yaml` for group install. |
| **Agent definition** | `SOUL.md` (persona) + `RULES.md` (hard constraints) + `agent.yaml` (model, tools, max_turns, abilities) + `knowledge/` (priority-ordered domain knowledge). Renders to `.claude/agents/`, `.omp/agents/`, `.codex/agents/` via the asset-manager. |
| **Bundle** | A manifest grouping skills + agents under one install command (`skaile install bundle:my-domain`). |

### Flows

JSON/YAML pipelines with nodes (skills) and edges (dependencies). State per
node: `available -> running -> awaiting_approval | awaiting_input -> complete | failed | skipped`. Pure functional state machine -- the agent drives execution
via flow tools (`start_node`, `complete_node`, `request_approval`,
`request_input`). Flow snapshots flow as `state_changed` events with
`store: "flow:<runId>"` so hosts can persist them.

### Framework Fragments

Four conditional sections injected into the system prompt based on what is
installed:

| Fragment | Injected when | Purpose |
|---|---|---|
| `agent-mode` | Always | Solo vs orchestrator context |
| `handoff` | Subagents declared | Delegation guidelines |
| `skill-discovery` | Skills installed | Where to find abilities |
| `connector-usage` | Connectors configured | Available resources + access levels |

Each fragment is overridable per project. Add a connector and the prompt
gains the relevant tool descriptions automatically.

### Telemetry

Pluggable provider injected at construction time. `NoopTelemetryProvider`
(default, zero I/O) or `OtelTelemetryProvider` (OTLP -> Langfuse or any
collector). Configure via `telemetry:` in `skaile.yaml`.

---

## 3. The Package Layout

Bottom-up dependency flow, no cycles:

```
@skaile/agent-types          zero deps -- AgentEvent, AgentCommand, IAgentRuntime, transports
        v
@skaile/agent-core           manifest parsing, settings, discovery, scanDirectory()
@skaile/agent-bridge         driver abstraction (omp, claude-sdk)
        v
@skaile/agent-flow-engine    flow state machine (pure functions)
@skaile/agent-resolver       SKILL.md frontmatter parser + gates
@skaile/agent-transport      WebSocket + stdio adapters
@skaile/agent-client         AgentClient (implements IAgentRuntime over a transport)
        v
@skaile/agent-connectors     MountManager + ConnectorManager + registries + tool delivery
@skaile/agent-session        SessionDispatcher: persistence + subscriber fan-out
@skaile/agent-store          headless reactive UI store (React/Vue bindings)
        v
@skaile/agent-runner         flow execution, startAgentServer(), runAgentChat()
        v
@skaile/agent-sdk            high-level entry: createAgentSession(), AgentSession, LocalRuntime
@skaile/agent-cli            the `skaile` CLI -- run skills/flows, manage projects
@skaile/agent-lab            Docker-isolated skill testing
@skaile/asset-manager        catalog, install/remove skills+agents, scaffold workspaces
```

**Pick the highest-level package that meets your needs.** Most apps start
with `@skaile/agent-sdk` and only descend when they need specific behaviour.

---

## 4. Integration Tiers (which pattern fits)

### Tier 1 -- Factory session (1 session, simple)

```ts
import { createAgentSession } from '@skaile/agent-sdk'

const session = await createAgentSession({
  agent: { driver: 'claude-sdk', model: 'claude-sonnet-4-6', maxTurns: 50 },
  mounts: [{ id: 'workspace', driver: 'local', source: '/path', access: 'read-write' }],
})
session.onChange = () => render(session.output, session.state)
session.send('Hello')
// later: session.stop()
```

The factory creates a temp workspace, writes `skaile.yaml`, instantiates
`LocalRuntime`, starts it, and registers cleanup. Fits CLI tools, scripts,
single-user terminals.

### Tier 2 -- Custom session class (N sessions, in-process)

Manually create `LocalRuntime`, extend `AgentSession` with identity fields,
manage a `SessionRegistry` (Map/EventEmitter). Fits multi-session TUIs and
operator dashboards.

### Tier 3 -- Subprocess + persistence (web app)

```ts
const processManager = new AgentProcessManager()
const messageStore = new JsonlMessageStore('/path/to/messages')
const subscriberTransport = new EventEmitterSubscriberTransport()

const agentClient = processManager.createAgentClient(sessionId)
const dispatcher = new SessionDispatcher({
  sessionId, client: agentClient,
  store: messageStore, transport: subscriberTransport,
})

// HTTP: dispatcher.sendCommand({ type: 'prompt', prompt })
// SSE:  subscriberTransport.listen(subscriberId, event => res.write(...))
```

`AgentProcessManager` spawns `skaile serve` as child process with WebSocket
IPC. `SessionDispatcher` bridges agent client with persistence and
fan-out. Use for any HTTP/SSE-driven web app.

### Tier 4 -- Enterprise / multi-tenant

Container-managed agents, gateway with connection pool, session lifecycle
(start/hibernate/wake/close), pluggable `IContainerManager` (Mock, Local,
Docker, Nix), prompt decoration for multi-user, durable flow state mirror.
This is what the Skaile platform does.

### Decision matrix

| Concern | In-process | Subprocess | Remote |
|---|---|---|---|
| Process isolation | None | Yes | Yes |
| Crash isolation | None | Yes | Yes |
| Memory overhead | Shared | Per-process | Per-container |
| Session resume | No | Possible | Yes |
| Multi-tenant | No | Possible | Yes |
| Package | `@skaile/agent-sdk` | `@skaile/forge-common-backend` | `@skaile/agent-client` + `@skaile/agent-transport` |

---

## 5. The Decision Checklist (apply this when designing an integration)

1. **How many concurrent sessions?** 1 -> factory. N -> registry + manual runtime.
2. **Process isolation needed?** No -> `LocalRuntime`. Yes -> `AgentProcessManager` or remote.
3. **Message persistence?** No -> `AgentSession.output` buffer. Yes -> `SessionDispatcher` + `MessageStore`.
4. **Multiple frontends per session?** No -> direct event handler. Yes -> `SubscriberTransport` fan-out.
5. **Multi-step orchestration?** No -> prompt/response. Yes -> add `@skaile/agent-flow-engine`.
6. **Custom agent identity?** No -> omit `agentDir`. Yes -> create directory with SOUL.md + RULES.md.
7. **External data sources?** Filesystem -> mounts. APIs/databases -> connectors. Tool-providing services -> MCP.
8. **Multi-tenant?** No -> any tier. Yes -> Tier 4 with container manager + gateway.

---

## 6. What to Reuse vs Build

| Concern | Reuse from framework | Build yourself |
|---|---|---|
| LLM communication | `agent-bridge` (drivers) | Never |
| Session lifecycle | `agent-sdk` or `agent-session` | Only if radically different state machine |
| Message persistence | `JsonlMessageStore` or custom `MessageStore` impl | Use Prisma/Drizzle if JSONL doesn't fit |
| Streaming to frontends | `EventEmitterSubscriberTransport` (SSE), `agent-store` (React/Vue) | Only for non-SSE push |
| Data source access | Connectors | Proprietary APIs without an adapter |
| File system access | Mounts | Never -- mounts handle git/S3/WebDAV/SharePoint |
| Multi-step workflows | Flow engine | Only if your model is fundamentally different |
| Agent capabilities | Skills (SKILL.md) | Never -- this is the extension model |
| External tools | MCP servers in `skaile.yaml` | Only if no MCP exists |
| Configuration | `skaile.yaml` + `resolveSettings()` | Never -- merging is solved |
| Observability | `@skaile/agent-telemetry` | Only for non-OTLP backends |
| UI state | `@skaile/agent-store` | Only if React/Vue bindings don't fit |

---

## 7. Reference Implementations -- the Forge ladder

Each forge app is a working reference for one tier. Source under `forge/`.

| Level | App | Pattern | Transport | Sessions | Persistence | Key Lessons |
|---|---|---|---|---|---|---|
| L2 | `L2-tui` | `LocalRuntime` + custom session, profile YAML | In-process | N (registry) | None | Multi-session TUI, profile-based driver/connector selection, ink layout, focus mgmt, lazy start |
| L3 | (deprecated mattermost) | `LocalRuntime` + lazy start | In-process | N per-thread | SQLite per channel | Bot pattern, thread-as-session, custom event handler routing typing/post events |
| L4 | `L4-project` (Pichi) | `AgentProcessManager` + dispatcher | Subprocess (WS) | N per-user-project | JSONL | Personal AI assistant, settings UI, asset catalog, WebDAV/SSH access, multi-user SQLite auth |
| L4 | `L4-assistant` | Same shape, simpler chat UI | Subprocess (WS) | N | JSONL | Minimal personal assistant chat |
| L5 | `L5-concept` | `AgentProcessManager` + flow engine + Hocuspocus | Subprocess (WS) | N per-concept | JSONL + Git | Multi-user real-time concept editor, Yjs CRDT, AI flows produce `_concept/` specs, voice STT/TTS |

### When to mimic which forge app

| Your app idea looks like... | Mimic | Why |
|---|---|---|
| Terminal/CLI tool with one or N sessions | L2-tui | Profile YAML pattern, registry, ink components from `@skaile/agent-tui` |
| Slack/Mattermost/Discord bot | L3 mattermost (archived) | Lazy session start per thread, dual output routing, channel-scoped state |
| Personal web AI assistant | L4-project or L4-assistant | Subprocess + JSONL + SSE, settings UI, catalog browser, voice IO |
| Multi-user editor with AI co-design | L5-concept | Hocuspocus + Yjs + flow engine + per-document sessions |
| Enterprise multi-tenant platform | Skaile platform itself | Container manager, session lifecycle, gateway, durable flow mirror |

### Shared backend pieces in `forge/common-backend`

- `AgentProcessManager` -- subprocess lifecycle (spawns `skaile serve`)
- `JsonlMessageStore` -- append-only message persistence
- `EventEmitterSubscriberTransport` -- SSE/WebSocket fan-out
- `SessionDispatcher` (re-exported from `@skaile/agent-session`)
- Settings/auth/workspace service wrapping `@skaile/asset-manager`

### Shared UI pieces in `forge/common-ui`

Headless Vue composables: TipTap editor, catalog browser, settings manager,
workspace config editor. No UI framework dependency -- each app brings its
own component library.

---

## 8. The Skaile Platform -- Enterprise Reference

`platform/` is the full enterprise instance: NestJS + Fastify + Prisma + tRPC
backend, Vite + React 19 + TanStack frontend, Keycloak auth, PostgreSQL, vm-agent
Docker containers per session.

Data hierarchy: **Organization -> Project -> Session -> Conversation -> Message**.

Project source types determine isolation: `Git`, `SharePoint`, `LocalFolder`,
`Empty`. Mounts and connectors do the actual data binding.

Notable backend libs (all under `platform/backend/libs/`):

| Library | Role |
|---|---|
| `session` / session-manager | Session workspace lifecycle, hibernation/wake, idle detection |
| `agent-gateway` | WebSocket bridge to vm-agent containers |
| `ai` / `ai-provider` | Provider-agnostic LLM dispatch |
| `skill-registry` | Catalog of installed skills/agents/flows |
| `deployment-manager` | Pipeline-engine for phase-based generation runs |
| `preview-manager` / `preview-proxy` | Generated-app preview lifecycle |
| `voice` | STT (Deepgram) + TTS (ElevenLabs) WebSocket protocol |
| `git-provider` / `sharepoint-provider` / `nextcloud-provider` / `s3` | Project source backends |
| `credential` / `secrets-provider` | Multi-provider secret resolution |
| `artifact-manager` / `excel-extraction` / `excel-io` / `xlport` | Document handling |

Flow execution tRPC routes (`workspace.startFlow`, `approveFlowNode`,
`provideFlowInput`, `cancelFlow`, `setFlowAutonomousMode`, `retryFlowNode`,
`getActiveFlow`) sit on top of `FlowExecutionSyncService`, which mirrors
runtime `state_changed` events into Prisma `FlowExecution` rows.

Key extensibility hooks the platform layers on top of the framework's base
protocol: inline text markers (`[REACT]`, `[PASS]`, `[CUSTOM]`),
`MessageReaction`, voice WebSocket protocol, custom AAP components, multi-user
features (mentions, reply threading, sender attribution). See
`platform/docs/protocol-extensions.md`.

---

## 9. Driver Capability Matrix (when picking a driver matters)

| Capability | omp (oh-my-pi) | claude-sdk |
|---|---|---|
| Process model | Subprocess (JSON-RPC) | In-process |
| Models | Any provider (Anthropic, OpenAI, Google, xAI, Ollama, ...) via roles | Anthropic only |
| Subagent dispatch | `task` tool | `Agent` tool |
| Parallel subagents | Yes (configurable concurrency) | Yes (`run_in_background`) |
| Subagent isolation | worktree, fuse-overlay, fuse-projfs | worktree |
| Custom subagents | `.omp/agents/` | `.claude/agents/*.md` |
| Tool restriction | Per-agent registry | `tools` allowlist + `disallowedTools` denylist |
| Session persistence | Yes (`.omp/sessions/`) | Yes (disk-backed, resumable) |
| MCP tools | No (CLI fallback: `skaile res`) | Yes (`mcpServers` in config) |

The `AgentDriver` interface currently reduces both to `prompt(message) -> events`
-- subagents are not exposed to the runner. Pick `claude-sdk` for in-process
performance + native MCP. Pick `omp` for model flexibility + subprocess
isolation. Switch by config (`driver:` in `skaile.yaml` or env
`PREFERRED_DRIVER`).

---

## 10. Patterns for Common App Shapes

### "Add a chatbot to my web app"

- Tier 3, subprocess + dispatcher.
- Mount the relevant project files via `mounts` (`local` driver).
- Persist with `JsonlMessageStore`.
- Frontend: SSE -> `agent-store/react` for streaming text + tool events.
- Optional: add data connectors (`postgres`, `redis`) for app data.

### "AI co-pilot for our internal Postgres + S3 data"

- Connectors: `postgres` (auth `env:DATABASE_URL`) + `s3` mount for files.
- Provide an agent definition with RULES.md restricting destructive ops.
- Model: `claude-sdk` for native MCP tool use.
- Tier 1 if single-user, Tier 3 if multi-user.

### "Multi-step content pipeline (research -> draft -> review -> publish)"

- Define a flow JSON with skill nodes and edges.
- Each step is a SKILL.md with `reads_from` / `writes_to` declarations.
- Approval gates between draft and publish.
- Stream `state_changed` events to a progress UI.
- Mirror flow state to your DB if resume across restarts is required.

### "Real-time collaborative AI workspace"

- L5-concept pattern: Hocuspocus + Yjs for shared documents + a `yjs`
  connector so the agent reads/writes the same CRDT.
- Per-document sessions, `AgentProcessManager` for isolation.
- Voice IO via Deepgram + ElevenLabs (see `platform/backend/libs/voice`).

### "Domain expert in a vertical (legal, medical, ops)"

- Build a domain under `ai-assets/<my-domain>/` with skills + an agent
  definition (SOUL.md = expert persona, RULES.md = compliance constraints,
  knowledge/ = domain reference material).
- Bundle with `*.bundle.yaml`. Install on the project with
  `skaile install bundle:my-domain`.
- Add domain-specific connectors if external systems are involved (CRM,
  ticketing, knowledge base).

### "Embed an agent in an existing CLI"

- Tier 1: `createAgentSession()`. Mount the cwd as a `local` mount.
- For interactive REPL, reuse `@skaile/agent-tui` (`skaile repl`) instead
  of building from scratch.

---

## 11. Questions to Ask Before Designing an Integration

1. What is the user-facing surface? (terminal, web, mobile, voice, embed)
2. How many concurrent sessions does one process need to host?
3. What are the data sources -- filesystem, APIs, databases, third-party
   services? Which are mounts vs connectors vs MCP?
4. Are there secrets? Where do they live (env, OAuth, vault)?
5. Is there a multi-step workflow with human-in-the-loop checkpoints?
6. Does the agent need a specific persona/RULES, or is generic fine?
7. Which driver and which models? (Anthropic-only -> claude-sdk, multi-provider
   -> omp.)
8. How do messages get persisted? JSONL ok, or do you need a relational store?
9. Multi-user? Multi-tenant? If multi-tenant, you are at Tier 4.
10. Real-time collaboration on shared state -> Yjs connector. Turn-based state
    machine -> XState connector.

---

## 12. Pointers for Further Reading

Companion docs in this folder:

- `skaile-ecosystem.md` -- the whole skaile-dev monorepo (forge apps,
  ai-assets, ai-assets-skaile, ai-assets-skaileup, platform overview)
- `skaile-platform.md` -- enterprise platform deep dive (skills, flows,
  AAP components, app actions, governance)
- `postxl-framework.md` -- PostXL, the schema-driven code generator that
  produces the platform's backend + frontend scaffolding

When you need depth on a specific topic in the skaile-dev repo, point
the agent at these files (paths relative to the skaile-dev repo root):

| Topic | File |
|---|---|
| End-to-end implementation patterns | `agent-framework/IMPLEMENTATION-GUIDE.md` |
| Driver internals and capability matrix | `agent-framework/bridge/CLAUDE.md` |
| Mount/connector model, secret chain, FlowAdapter | `agent-framework/connectors/CLAUDE.md` |
| Runner orchestration, serve mode, two-root pattern | `agent-framework/runner/CLAUDE.md` |
| SDK exports and entry points | `agent-framework/sdk/package.json` (`exports`) |
| Flow execution model (universal) | `agent-framework/docs/flow-execution.md` |
| Client-server protocol | `agent-framework/docs/client-server-architecture.md` |
| Forge integration matrix | `forge/README.md`, `forge/CLAUDE.md` |
| Per-app forge architecture | `forge/<L2-tui\|L4-project\|L4-assistant\|L5-concept>/CLAUDE.md` |
| Platform architecture | `platform/CLAUDE.md` + `platform/docs/*.md` |
| Skill authoring conventions | `dev/CLAUDE.md` Skill Structure Convention + `ai-assets-skaileup/skaileup-contracts/contracts/` |

---

## 13. The Mental Model in One Paragraph

An app uses the framework by deciding three things: **which tier** (factory,
custom session, subprocess, enterprise), **which resources** (mounts +
connectors + MCP, declared in `skaile.yaml`), and **which agent** (generic, or
a SOUL/RULES/knowledge directory rendered for the chosen driver). Everything
else -- streaming, persistence, fan-out, prompt assembly, secret resolution,
flow orchestration -- is provided. New capabilities arrive as SKILL.md files;
new data sources arrive as connector adapters with a `CONNECTOR.md`
descriptor; new orchestration arrives as flow definitions. The same
`IAgentRuntime` interface is honoured from CLI tool to multi-tenant platform,
so an app can graduate from one tier to the next without rewriting how it
talks to the agent.
