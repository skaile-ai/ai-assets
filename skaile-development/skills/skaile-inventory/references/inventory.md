# skaile-ai inventory

Surveyed 2026-11-22 from `gh repo list skaile-ai` (85 repos), each repo's README /
`AGENTS.md` / `package.json`, and the local checkouts. Status words: **active** (pushed in
the last month and in use), **prototype**, **stale** (no push for months, or old
dependencies), **superseded** (frozen; its code lives on elsewhere), **archived**. Rows marked
*(guess)* are inferred, not read from a file. The repo is the truth; the row is a pointer.

## I need… → reach for

| I need… | Reach for | Where |
|---|---|---|
| one function exposed to UI, HTTP, MCP, CLI and agent tools | `@skaile/forge-app-actions` (`defineServerAction`) | forge |
| a user action that agents can also invoke (platform) | `agentAction(...)` on the tRPC procedure, `platform.find_actions` / `invoke` | platform `docs/agent-action-declarations.md` |
| a Cmd+K command palette with agent-callable actions | platform `<feature>.actions.tsx` pattern; in Nuxt apps `defineForgeAction` + `<ForgeCommandPalette>` | platform / forge |
| an agent to ask a person and wait for the answer or approval | platform approvals and autonomy grants; outside the platform `@skaile/forge-agent-inbox` (lib) and the deployed agent-inbox service | platform / forge |
| a chat UI over an agent or LLM | `@skaile/forge-chat` (Vue; skaile-protocol, AI SDK, OpenAI transports); React: `@skaile/workspaces/store/react` | forge / workspaces |
| an "ask this app" assistant inside an app | `@skaile/forge-layer-chat` (app's actions as tools) or `@skaile/forge-layer-workspace-agent` (full `skaile` agent over the app's `/mcp`) | forge |
| an existing app that agents can operate (views, forms, entities) | `@skaile/workspaces/app-sdk` + `skailify-adapters` (Angular, React, Vue adapters; EMF/JPA MCP backends preliminary); platform `libs/app-channel` | workspaces / skailify-adapters / platform |
| to run a coding agent (Claude SDK, Codex, Gemini, omp, …) | `@skaile/workspaces/bridge` + `/harnesses/*` | workspaces |
| to spawn and manage `skaile serve` from a Node app | `@skaile/forge-agent-runtime` | forge |
| a UI on a self-deployed `skaile serve` | `forge/apps/forge-attach` | forge |
| a multi-step agent pipeline (DAG) | workspaces flow engine (`flow.v2`), platform flows, run groups | workspaces / platform |
| batch over many inputs | `@skaile/workspaces/run-group-engine`, platform run groups | workspaces / platform |
| session-to-session or cross-org agent messaging | platform `libs/agent-to-agent`, `libs/org-connection` | platform |
| a local Claude Code (or other harness) talking to platform agents | `harness-connectors` (Claude Code plugin + MCP; protocol `platform/docs/protocol/harness-channel-v1.md`), platform `libs/remote-agents` | harness-connectors / platform |
| to expose the platform to Claude Code / Cursor / ChatGPT as MCP | platform `libs/mcp-server` (`/mcp/o/<org>`) | platform |
| cloud files mounted into an agent container | `sync` (Go FUSE data plane; SharePoint/OneDrive, Box, WebDAV/Nextcloud) | sync |
| a file-provider connector (browse, OAuth, refresh) | platform `libs/remote-provider` (`IRemoteProvider`) + `libs/{sharepoint,googledrive,box,nextcloud,git}-provider` | platform |
| mail, calendar, tasks, contacts | platform `libs/{mail,task,contacts}-provider` boundaries + `exchange-`, `imap-`, `google-provider` | platform |
| outbound system email | platform `libs/mail` | platform |
| Teams / chat-app integration | `@skaile/workspaces/channels/msteams`, platform `libs/channel-bridge` | workspaces / platform |
| a Mattermost bot bridged to agents | `forge-tui` headless connector *(stale)*, ai-assets `forge-mattermost` agent, workspaces `mattermost` connector | forge-tui / ai-assets / workspaces |
| speech-to-text / text-to-speech | platform `libs/voice` (Deepgram STT, TTS); ai-assets `use-elevenlabs`; `forge-editor-audio` | platform / ai-assets / forge |
| LLM provider, API-key and model settings | platform `libs/credential` + `libs/secrets`; in Nuxt apps `@skaile/forge-settings` | platform / forge |
| a transparent LLM proxy that rewrites or pseudonymizes prompts | `@skaile/transform-proxy` | forge |
| PII anonymization with reversible pseudonyms (German) | `@skaile/forge-anon` | forge |
| generic CRUD / aggregate with an LLM-writable filter | platform: PostXL-generated layer (never hand-roll); Drizzle apps: `@skaile/forge-data` + `@skaile/forge-layer-data-browser` | platform / forge |
| a new internal data app | `forge/templates/app` (Nuxt 4, actions catalogue, Drizzle, auth); offline phone app: `forge/templates/mobile`; PostXL app: `pxl create-project` (skill `postxl`) | forge / PostXL |
| background jobs without a new service | `@skaile/forge-layer-jobs` (pg-boss in the app DB) | forge |
| an installable, offline, push-enabled web app | `@skaile/forge-pwa`; platform `libs/notifications` (Web Push) | forge / platform |
| a rich-text / block document editor | `@skaile/forge-editor` (TipTap v3, Vue + React) + `forge-editor-*` add-ons | forge |
| document → PDF (Typst), slides, charts, audio | `forge-editor-typst`, `-slides`, `-charts`, `-audio` (mostly unpublished) | forge |
| auditable human sign-off on content | `@skaile/forge-editor-signoff` | forge |
| trigram + pgvector search over blocks | `forge-editor-index-pg` (unpublished) | forge |
| text composed from example documents and facts | `@skaile/forge-compose`, `@skaile/forge-editor-compose` | forge |
| tags with an AND/OR/NOT query grammar | `@skaile/forge-tags` | forge |
| a knowledge graph per agent / person / project / org | platform `libs/knowledge-graph` | platform |
| BM25 search, DE/EN number parsing over documents | `wmc-knowledgebase` (Python, SQLite FTS5) | wmc-knowledgebase |
| slides or a pitch deck from text | `@skaile/decks` (`deck.md` → HTML/PDF; skill `use/decks`); client themes in `decks-templates` | decks |
| read/write .xlsx locally | `excel-mcp` (79 tools, POI) | excel-mcp |
| Excel on SharePoint / OneDrive with the real calc engine | `excel-graph-mcp` *(scaffold)*; platform `libs/excel-io`, `libs/xlport` | excel-graph-mcp / platform |
| edit .pptx / export slides to PDF or images | `powerpoint-mcp` (52 tools) | powerpoint-mcp |
| edit .docx keeping formatting, tracked changes, comments | `word-mcp` | word-mcp |
| permission-scoped SQL for an agent | `sql-mcp` (Postgres, MySQL, SQLite, MSSQL) | sql-mcp |
| an HTML report, dashboard dump, code-flow diagram | `ai-assets-report` (`report-*` skills) | ai-assets-report |
| a product video with voice-over | `ai-assets-video` (filmkit CLI, Remotion) + `filmkit` app | ai-assets-video / filmkit |
| PR stories / a "what landed" page | `ai-assets-pr-story` | ai-assets-pr-story |
| crawl a running app like a user and report UX | `ai-assets-app-explore` | ai-assets-app-explore |
| UI feedback from people as file:line notes for agents | `@skaile/forge-annotation-inbox`; platform `docs/ui-annotation-workflow.md` | forge-annotation-inbox / platform |
| PR / review / landing orchestration for agents (task hub) | `forge-delivery-hub` | forge-delivery-hub |
| process mapping where the agent proposes and a person accepts | `process-canvas` | process-canvas |
| the platform running locally, offline, or on a phone | `skaile-desktop` (Tauri; Android in `mobile/`); thin wrapper: `skaile-tauri-shell-prototype` | skaile-desktop |
| to test, judge or auto-improve skills | `ai-assets-skill-development` (`validate`, `judge`, `improve`, `compile-validators`) | ai-assets-skill-development |
| a concept → build → quality pipeline | `ai-assets-skaileup` (93 skills, 15 domains); data model `@skaile/model` | ai-assets-skaileup / forge |
| agent personality / tone | `ai-assets-personas` (30 PERSONA.md) | ai-assets-personas |
| web search / research / document parsing skills | ai-assets `use-*` (`use-exa`, `use-perplexity`, `use-docling`, …) and `knowledge-research` | ai-assets |
| to publish or resolve skills and agents | skaile.store (`store/`), `skaile source` / `store` CLI, `store-sidecars` for repos we don't own | store / workspaces |
| Skaile colours, fonts, logos | `@skaile/brand` (platform, marketing); `@skaile/forge-brand` (Nuxt UI apps) | brand / forge |
| host provisioning, firewall, Postgres + backups | `ansible` roles | ansible |
| a parser for any language or file format | `grammars-v4` (ANTLR fork) | grammars-v4 |
| prior research on a tool or market | `skaile-research`, `skaile-markt` | skaile-research / skaile-markt |

## Core product

### platform — active
Enterprise app (NestJS + React 19 + Postgres + Keycloak, generated by PostXL). Orgs and
Spaces, agent sessions in containers, connectors and mounts, flows, A2A, approvals, the
personal assistant, run groups, mail and calendar, channels, voice, previews, Skailify apps.
Start at `features/SKAILE-PLATFORM-CAPABILITIES.md` (21 sections, "use when" index), then
`docs/README.md`, `decisions/` (ADRs) and the lib's `CLAUDE.md`. Backend libs worth knowing
by name: `session`, `session-materializer`, `agent-gateway`, `capabilities` (`platform.*`
agent tools), `authorization-verdict` (`may.ts`), `agent-to-agent`, `org-connection`,
`remote-agents`, `mcp-server`, `credential`, `secrets`, `remote-provider` + `*-provider`,
`channel-bridge`, `voice`, `classifier`, `knowledge-graph`, `library`,
`asset-catalog-client`, `preview-manager`, `public-share`, `link-preview`, `app-channel`,
`ui-state-mediator`, `notifications`, `usage`, `session-logs`, `log-sink`, `workspace-file`,
`upload`, `s3`, `excel-io`, `feedback`. `apps/mount-helper` is the legacy mount plane, frozen
in favour of `sync`.

### workspaces — active
`@skaile/workspaces` (npm, 100+ subpath exports) and the `skaile` CLI. Harness bridge
(omp, claude-sdk, codex, gemini, qwen, opencode, goose, continue), runner, flow engine
(v2), run-group engine, connectors (memory, postgres, sqlite, gmail, minio, mattermost,
devserver, tunnel, deploy, box, git, s3, webdav, sharepoint, googledrive, …), plugin
registry, deploy targets (local, docker, podman; plugins for Fly, Vercel sandbox, k8s),
protocol types + client + WS transport, React/Vue stores and bridges, app SDK, channels,
asset manager, secrets, telemetry (OTel → Langfuse). CLI command families are in
`packages/workspaces/cli/CLAUDE.md`; breaking changes in `MIGRATION.md`.

### sync — active
Cloud-mount plane extracted from the platform: Go lazy-read FUSE data plane plus TypeScript
control, shipped as the `sync-plugin` Docker plugin. SharePoint/OneDrive (Graph), Box,
WebDAV/Nextcloud; no Google Drive adapter. Admission budgets per tenant, journaled
mutations, release harnesses. Credentials stay in the platform. `sync/CLAUDE.md`,
`sync/decisions/`.

### store (`skaile-store`) — active
skaile.store, the public pointer-only asset catalog (PostXL backend, admin console, Astro
storefront, `@skaile/store-shared` contract, CLI). Admin curation, community namespaces,
escrow import, auto-publish via poller and webhook.

### Others
| Repo | Status | What it is |
|---|---|---|
| brand | active | `@skaile/brand`: tokens, `brand.css`, `theme.css`, Tailwind preset, logos |
| www.skaile.ai (`marketing/`) | active | skaile.ai website, Astro 6 SSR on Vercel |
| ansible | active | Hetzner provisioning; roles for docker, firewall, postgres + WAL-G, restic, nginx, registry, jenkins, sync-plugin, node-channel, log shipping, alerting, tang |
| skaile | active | Root repo pinning every submodule |
| decks / decks-templates | active | Deck renderer (being open-sourced) / private client themes, which must never leak into `decks` |
| skaile-framework (`framework/`) | stale | Old concept and devlog material; nothing reusable as code |

## forge monorepo (`skaile-ai/forge`) — active

Bun + Turborepo monorepo, Nuxt 4 / Vue / Nuxt UI / Nitro / Drizzle (PGlite or Postgres).
Each `@skaile/*` package is versioned and published on its own (changesets, npm trusted
publishing); most are 0.x and break without deprecation layers. Docs:
https://skaile-ai.github.io/forge/ (generated package table at
`apps/docs/content/2.packages/00.overview.md`, architecture in
`apps/docs/content/3.architecture/`, agent guide `packages/app-actions/llms.txt`),
self-assessment `docs/strategie/framework-analyse.md` (German, 2026-11-09).

| Package / app | npm | What it gives you |
|---|---|---|
| `packages/app-actions` | `@skaile/forge-app-actions` | One server action → HTTP, MCP, CLI, AI SDK tool; browser actions = palette rows + agent capabilities with approval dialog |
| `packages/app` | `@skaile/forge-app` | Nuxt layer wiring Nuxt UI, app-actions and brand |
| `packages/data` | `@skaile/forge-data` | Drizzle schema → `DataModel`; generic CRUD/aggregate; LLM-writable filter as `$ref`-free JSON Schema |
| `packages/layer-data-browser` | `@skaile/forge-layer-data-browser` | `/data` admin view over every entity |
| `packages/layer-jobs` | `@skaile/forge-layer-jobs` | pg-boss jobs, `defineJob`, web/worker roles |
| `packages/layer-chat` | `@skaile/forge-layer-chat` | In-app assistant drawer using the app's actions as tools |
| `packages/layer-workspace-agent` | `@skaile/forge-layer-workspace-agent` | `skaile` agent per conversation acting through the app's `/mcp`, with the agent inbox |
| `packages/chat` | `@skaile/forge-chat` | Chat core + Vue UI + skaile / AI SDK / OpenAI transports |
| `packages/agent-runtime` | `@skaile/forge-agent-runtime` | Spawn and connect to `skaile serve`, harness registry |
| `packages/agent-inbox` (+ `apps/agent-inbox-service`) | `@skaile/forge-agent-inbox` | Human-in-the-loop question/approval inbox over MCP, several stores; service deployed |
| `packages/settings` | `@skaile/forge-settings` | Provider registry, credential store + policy, model discovery and roles, providers panel |
| `packages/editor` (+ 12 `editor-*`) | `@skaile/forge-editor` | TipTap v3 editor; add-ons for audio, charts, compose, Directus, graph, pg index, PDF viewer, signoff, slides, Typst |
| `packages/compose` | `@skaile/forge-compose` | Corpus → composed text with example ranking |
| `packages/anon` | `@skaile/forge-anon` | German clinical PII anonymization, reversible ledger |
| `packages/transform-proxy` | `@skaile/transform-proxy` | Passthrough LLM proxy with text transforms, SSE-aware |
| `packages/tags` | `@skaile/forge-tags` | Hierarchical / flat / facet tags + filter grammar |
| `packages/pwa` | `@skaile/forge-pwa` | Installable, offline outbox, Web Push |
| `packages/brand` | `@skaile/forge-brand` | Brand tokens + Nuxt UI bridge for forge apps |
| `packages/model` | `@skaile/model` | skaileup `model.yaml` / `seed.yaml` schemas, loader, CLI |
| `apps/skaile-forge` | private | Assistant, workspace and concept modes over `skaile.yaml` workspaces |
| `apps/forge-attach` | private | Single-workspace UI on a standalone `skaile serve` |
| `templates/app`, `templates/mobile` | giget | Data-app starter (actions catalogue, Drizzle, auth, CLI) / offline phone PWA |

## Standalone apps and libraries (outside forge)

| Repo | Status | What it is |
|---|---|---|
| forge-annotation-inbox | active | `@skaile/forge-annotation-inbox`: dev toolbar, notes with SFC file:line for agents, HTTP + MCP |
| forge-delivery-hub | active | Delivery coordinator for platform work: work items, PRs, reviews, landing leases; first Task Hub implementation (platform#10990) |
| process-canvas | active | Company process model; the agent proposes changes, a person accepts, with audit and undo |
| filmkit | active | filmkit Studio, film production app on the forge template |
| skaile-desktop | active | Tauri supervisor running the whole platform locally; Android app for `/me` |
| skaile-tauri-shell-prototype | prototype | Thin Tauri window around the hosted platform |
| harness-connectors | active | Claude Code plugin + MCP bridging a local harness to Skaile sessions |
| skailify-adapters | active | Make an app agent-controllable: Angular, React, Vue adapters; EMF/JPA MCP backends |
| forge-site | stale | Markdown vault as a live website with an AI edit drawer; deprecated deps |
| forge-store | stale | Single-process, wire-compatible reimplementation of skaile.store |
| forge-tui | stale | Operator TUI over sessions; headless Mattermost connector |
| forge-ai-frameworks-guide | prototype | Comparison dashboard of 134 agent frameworks and tools |

## MCP servers

| Repo | Status | Surface |
|---|---|---|
| excel-mcp | active, most mature | 79 tools: workbooks, ranges, sheets, charts, tables, read-only VBA, Power Query (stored, not run), audit, real-Excel worker queue |
| excel-graph-mcp | scaffold | Excel Online via Graph, tool names matching excel-mcp |
| powerpoint-mcp | active | 52 `ppt.*` tools: slides, text, shapes, tables, charts, render, templates, Markdown outline import, transactions |
| word-mcp | active | ~40 tools: documents, styles, find/replace, tracked changes, comments, tables, images, render |
| sql-mcp | active | 8 tools; statement classification, read-only transactions, row/byte/time limits |
| harness-connectors | active | `ask_skaile`, `send_skaile`, `answer_skaile`, `check_skaile_inbox`, `list_skaile_peers` |

Install recipes for these and hosted MCPs (github, alma, ideogram, strava) are in
ai-assets `mcp/`.

## Skills and agent assets

| Repo | What it holds |
|---|---|
| ai-assets | Main library. `skaile-development` (ship, implement, review, test*, e2e-platform, verify-ui, postxl, doc, audit, quality, release, issue-bar, meat, skaile-stack-decide, this skill), `skaile-platform` (platform-guide, auto-ship), `use` (exa, perplexity, searxng, context7, elevenlabs, ideogram, docling, anydoc, outline, revealjs, slidev, decks, word-template, agent-browser, mcp2cli, skills-sh), `knowledge-research` (deep-research, paper extraction, podcast, book-writer), `dev-implementation-experts-*` (Nuxt, Directus, TipTap, PrimeVue, VueUse, omp, Python, PydanticAI, marimo, Typst), `github-ops`, `data-extraction` (Access VBA), `skaile-forge` and `forge-mattermost` agents, `mcp/` recipes. `catalogs/` is stale (its `build.ts` imports a removed path) |
| ai-assets-skaileup | Concept → build → quality pipeline, 93 skills in 15 domains |
| ai-assets-skill-development | Skill lab: validate, judge, improve, learn, compile-validators |
| ai-assets-app-explore | App crawler and UX report, app object model, guideline rules |
| ai-assets-report | `report-*` skills → one static HTML report |
| ai-assets-pr-story | PR stories and a "what landed" dispatch page |
| ai-assets-video | Product-film pipeline, filmkit CLI, Remotion engine, client packs |
| ai-assets-personas | 30 agent personas |
| store-sidecars | Curated manifest overlays for upstream skill repos |
| skaile-skill-migrator | TUI importing skills.sh skills into the store *(stale since 2026-06)* |

## Customer, pilot and research repos

These are customer work. Copy patterns, never data, across customers.

| Repo | What it is | Reusable |
|---|---|---|
| getec | GETEC pricing engine (Rust block DSL, golden-case harness) | the block-DSL + golden-case pattern *(guess)* |
| wmc-knowledgebase | Ingestion and BM25 search over M&A documents (Python) | DE/EN number parsing, slide segmentation, FTS5 search |
| use-cases | Presentation apps over the SharePoint use-case DB, Skailify-enabled | visibility filter + loader |
| brainlytix-eln | PostXL biopharma ELN MVP | example of a customer PostXL app |
| titans-prototype | PostXL recruiting app for a sports club | credits/licenses models *(guess)* |
| archimeda-lux-sta | Customer Access DB export | data only |
| experiment-zurich, experiment-orgdesign | Inputs / empty | — |
| skaile-research | Primary-source research reports, one per topic | check before researching again |
| skaile-markt | German market analysis, segments, funnels | GTM facts |
| youtube-partners | YouTube host-candidate research | — |
| screensavers, first-promo-video | Brand animations; an early promo video | brand geometry *(guess)* |
| coolify-teamclaude | Coolify deploy of the TeamClaude subscription proxy | — |
| demo-tools, throwaway, gsb-pilot, project-getec-bka, ecsplico-academy-ai-assets, skaile-dev-matthias | Empty shells, fixtures or personal repos | — |

## Superseded and archived

**Frozen, not archived; edit forge instead** (imported 2026-11-09, npm releases now come from
forge): forge-agent-inbox, forge-agent-runtime, forge-anon, forge-app, forge-app-actions,
forge-attach, forge-chat, forge-compose, forge-data, forge-editor, forge-settings, forge-tags,
skaile-forge, skaile-model → `forge/packages/*` or `forge/apps/*`. forge-app-template →
`forge/templates/app` *(guess: forge copy is canonical)*.

**Archived:**

| Repo | Replaced by |
|---|---|
| skaile-transform-proxy | `forge/packages/transform-proxy` |
| forge-common | split into forge-settings, forge-agent-runtime, forge-app, forge-app-actions, `@skaile/brand` (npm deprecated at 10.0.6) |
| forge-assistant, forge-project, forge-concept, forge-workspace | `forge/apps/skaile-forge` modes (forge-site is a fork of forge-workspace) |
| skaile-editor-adapters | `@skaile/forge-editor` *(guess)* |
| workspace-bridge-react, workspace-bridge-vue | `workspaces/packages/bridge-*` (same npm names) |
| agent-plugin | moved into the agent framework; now `@skaile/workspaces/workspace-plugin` *(guess)* |
| ai-assets-skaile, ai-assets-skaile-powers | built-in connectors in `@skaile/workspaces/factory-assets`; workflow skills in ai-assets *(guess)* |
| forge-lab | `ai-assets-skill-development` *(guess)* |
| forge-company | none (strategy docs) |
| skaile-dev, archive-skaile-initial-bus-dev-monorepo | the `skaile` root repo + standalone repos |
| skaile-business | SharePoint |
| infra | `ansible` *(guess)* |
| team-sub | `coolify-teamclaude` *(guess)* |

## Deeper indexes

- platform: `features/SKAILE-PLATFORM-CAPABILITIES.md`, `features/NN-*/`, `docs/README.md`,
  `decisions/`, `CONTEXT.md`.
- workspaces: `CLAUDE.md`, `packages/workspaces/CLAUDE.md`, `packages/workspaces/cli/CLAUDE.md`,
  `docs/workspaces-reference.md`, `MIGRATION.md`.
- sync: `CLAUDE.md`, `decisions/README.md`, `internal/README.md`.
- forge: `README.md`, `AGENTS.md`, the docs site above.
- Architecture choice across stacks: skill `skaile-stack-decide`.
