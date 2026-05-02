# Capability Authoring Guide

Skills, flows, MCP servers, connectors, and mounts contribute to the agent's
tool surface through the **Capability Registry** introduced in Protocol v2.
A capability is a typed, documented, optionally render-bearing tool that the
LLM can call during a session. The runner owns one `CapabilityRegistry` per
session; everything the LLM can invoke goes through it.

This guide covers how to author capabilities from a skill (or any agent-side
producer) using the `defineCapability` helper from `@skaile/agent-runner`.

---

## When To Reach For This

You are writing a capability when:

- A skill needs to expose a callable tool to the LLM beyond the bundled
  flow / connector / mount surfaces.
- A platform-side handler (reactions, custom messages, domain pickers) must
  be invoked from the agent and the bridge needs the typed schema, logging,
  and `fireAndForget` / `requiresApproval` semantics.
- A render-bearing component should be triggered by the LLM and projected
  onto a target surface (chat / preview / modal / input-extension) by the
  frontend.

You are NOT writing a capability when:

- You are adding a connector or mount adapter — those produce capabilities
  automatically in their own lifecycle. Do not double-register.
- You are adding a flow node — flow nodes are advanced via the FlowAdapter
  connector tools (`get_state`, `start_node`, ...), not as standalone
  capabilities.
- You are exposing a one-shot non-LLM-callable hook — capabilities exist to
  be called by the model.

---

## Anatomy of a Capability

```typescript
import { z } from 'zod'
import { defineCapability } from '@skaile/agent-runner'

export const greet = defineCapability({
  name: 'demo.greet',
  description: 'Greet a user by name and optionally append a tagline.',
  side: 'agent',
  origin: { kind: 'skill', skillId: 'demo-greet' },
  scope: 'session',
  kind: 'effect',
  fireAndForget: false,
  requiresApproval: false,
  promptFragment: [
    'Use `demo.greet` whenever the user asks to be greeted by name.',
    'Pass the literal name as `name`; pass an empty `tagline` if none.',
  ].join('\n'),
  input: z.object({
    name: z.string().min(1),
    tagline: z.string().optional(),
  }),
  output: z.object({
    greeting: z.string(),
  }),
  handler: async ({ name, tagline }, ctx) => {
    ctx.log.info('greeting', { name, hasTagline: !!tagline })
    const tail = tagline ? ` — ${tagline}` : ''
    return { greeting: `Hello, ${name}${tail}` }
  },
})
```

### Field reference

| Field | Required | Notes |
|---|---|---|
| `name` | yes | LLM-visible tool name. Lowercase, dotted; reserve `__name` for built-ins. |
| `description` | yes | One-sentence, LLM-readable. Don't restate the name. |
| `side` | yes | `'agent'` (handler runs in the runner) or `'client'` (handler runs platform-side; see below). |
| `origin` | yes | Discriminated union; `{kind: 'skill', skillId}` for skills, `{kind: 'flow', flowId}` for flows, etc. |
| `input` | yes | Zod schema; the registry validates input on every invoke. |
| `output` | optional | Zod schema for the handler's return. Drop for fire-and-forget. |
| `scope` | optional | `'session'` (default) or `'turn'` (deregistered after the current turn). |
| `kind` | optional | `'effect' \| 'render' \| 'query'`; metadata for filtering / UI. |
| `fireAndForget` | optional | When true, the bridge resolves to `{}` immediately and runs the handler in the background. Errors are logged but do NOT propagate to the LLM. |
| `requiresApproval` | optional | When true, the platform routes the invocation through the approval flow before the handler runs. |
| `promptFragment` | optional | Markdown text appended into the system prompt's `<CAPABILITIES>` block. Keep short — terse usage hints, not a manual. |
| `render` | optional | `RenderSpec` for render-bearing capabilities (see below). |
| `handler` | yes for `side: 'agent'` | Async function; receives `(args, ctx)`. |

### Origin trust

The registry enforces origin trust on register:

- `source: 'agent'` may declare any origin **except** `client`.
- `source: 'client'` may declare **only** `client`.

A skill cannot register a `client`-origin capability and the platform cannot
register a `skill`-origin capability. The registry drops mismatches with a
warning instead of throwing.

### Why `side: 'agent'` vs `side: 'client'`

- `side: 'agent'` — the handler runs inside the runner's process. Use this
  for skill / flow / MCP / connector / mount surfaces.
- `side: 'client'` — the handler runs platform-side. The runner stores a wire
  stub; the bridge dispatches via the round-trip protocol
  (`capability_invoked` event → platform handler → `capability_result`
  command). Skill authors normally never produce a `client`-side capability;
  these are declared by the platform in its `ConfigureCommand.capabilities`
  payload.

---

## Logging — The `ctx.log` Convention

Every handler receives a `HandlerContext` with a pre-wired `log: Logger`.
The registry constructs it as:

```typescript
createLogger({
  kind: 'capability',
  subkind: cap.origin.kind,
  instance: cap.name,
})
```

So your handler's emissions land under
`capability:<origin-kind>:<capability-name>` and are queryable from the
debug panel and the CLI:

```bash
skaile session logs <session-id> --kind capability --subkind skill
skaile session logs <session-id> --kind capability --instance demo.greet
skaile session logs <session-id> --tail --kind capability
```

**Hard rules** (not enforced at runtime; verify in code review):

- MUST emit through `ctx.log` instead of `console.*`, `consola`, or a
  hand-rolled logger.
- MUST NOT log sensitive payloads (API keys, raw secrets); the LogStore is
  forwarded to the platform debug panel and persisted to SQLite.
- SHOULD use structured `data` arguments — `ctx.log.info('summary', {...})`
  — not interpolated message strings.

The registry itself emits an `info "invoking"` and an `info "ok"` (or
`error "failed"`) bracketing every handler call automatically; you don't
need to log entry / exit yourself.

---

## Render Capabilities

A capability with a `render` block triggers a UI render in addition to the
ordinary tool-call return. The bridge:

1. Emits `RenderInvokedEvent { capabilityName, props, callId }` over the
   transport.
2. (When `render.fallback` is set) emits a parallel `text` event derived
   from the fallback template.
3. Continues the standard invoke / result path.

```typescript
defineCapability({
  name: 'ui.gif',
  description: 'Render a GIF in the chat surface.',
  side: 'client',
  origin: { kind: 'client' },
  kind: 'render',
  render: {
    component: 'gif',                           // Component tag name
    target: 'chat',                             // 'chat' | 'preview' | 'modal' | 'input-extension'
    fallback: 'Showed a GIF: {{url}} ({{caption}})',
  },
  input: z.object({ url: z.string().url(), caption: z.string().optional() }),
  handler: async () => ({}),
})
```

### `RenderSpec.fallback` placeholder convention

Fallbacks use the **bare-key** convention defined in the v2 spec:

| Form | Resolves to | Notes |
|---|---|---|
| `{{name}}` | `props.name` | Top-level key. Most common. |
| `{{user.name}}` | `props.user.name` | Dotted path traversal for nested records. |
| `{{props.x}}` | `props['props']['x']` | **Legacy** AAP form. No longer accepted as `props.x`; reads a literal `props` key. Update old fallbacks to `{{x}}`. |

Missing keys collapse to an empty string so callers can ship best-effort
templates without conditional logic. The bridge implementation lives in
`agent-framework/bridge/src/capability-dispatch.ts` (`renderFallback`).

### `target`

The frontend dispatches the render to the surface named by `target`. Common
values:

- `chat` — inline in the message stream.
- `preview` — sidebar preview pane.
- `modal` — full-screen modal.
- `input-extension` — affordance attached to the message composer.

Unknown targets currently fall through to `chat`; the platform may grow new
named targets without breaking existing capabilities.

---

## `fireAndForget` and `requiresApproval`

| Modifier | When to use | Result semantics |
|---|---|---|
| `fireAndForget: true` | The handler's return value is uninteresting to the LLM (telemetry, side effects, fan-out). | Bridge resolves the LLM tool call to `{}` immediately. Background errors logged at `capability:<origin>:<name>` only. |
| `requiresApproval: true` | The handler has user-visible consequences that must be authorized (e.g. writing to a connected service, executing a command). | Platform routes the invocation through the approval flow; handler runs only after a `capability_approve` decision lands. |

Don't combine the two — `fireAndForget` short-circuits before approval can be
collected. The registry doesn't enforce this today; treat it as a code-review
rule.

---

## Migrating from Old Skill MCP Tools

Skills used to register tools through ad-hoc MCP shims; v2 collapses that
into the capability registry. If you have a skill exposing tools today:

1. Replace each tool's input shape with a Zod schema (most JSON-schema-style
   tools translate one-to-one).
2. Wrap the tool's body in `defineCapability({ ..., side: 'agent', origin:
   { kind: 'skill', skillId } })` and move the body into `handler`.
3. Replace any logging through `console` / `consola` with `ctx.log`.
4. Drop the parallel MCP-tool registration — `composeLLMTools()` is now the
   only path the bridge sees. The runner has no direct skill-to-MCP-tool
   registration code today; if a skill ever ships an MCP server it would
   register through the v2 path the same way.

Connector / mount surfaces are migrated by the connectors package itself, not
by skill authors. Connector / mount tools become capabilities with
`origin: { kind: 'connector' | 'mount', connectorId | mountId }`.

---

## Author Checklist

Before submitting a capability:

- [ ] Name is dotted, lowercase, and unique within the session.
- [ ] `description` reads cleanly to the LLM in one sentence.
- [ ] `input` schema rejects every invalid case the handler would otherwise
      have to defend against.
- [ ] `output` schema (when present) matches what the handler actually
      returns.
- [ ] Handler emits through `ctx.log` only.
- [ ] `promptFragment` is brief — terse usage hints, not a manual.
- [ ] If `render` is set, `fallback` uses bare-key `{{name}}` placeholders
      (not the legacy `{{props.name}}`).
- [ ] `fireAndForget` and `requiresApproval` are not both set.
- [ ] Origin matches who is registering — skills register `kind: 'skill'`,
      connectors register `kind: 'connector'`, etc.

---

## Cross-References

- `agent-framework/runner/CLAUDE.md` § Capability Registry — registry surface,
  `HandlerContext.log`, hibernation snapshot semantics.
- `agent-framework/bridge/CLAUDE.md` § Capability Dispatch (Protocol v2) —
  bridge wiring, fire-and-forget, render emission, fallback rendering.
- `agent-framework/_devlog/specs/2026-04-30-protocol-v2-capability-protocol.md`
  — v2 spec, including `RenderSpec` / `Capability` shape and origin model.
- `agent-framework/_devlog/specs/2026-04-30-protocol-v2-follow-ups.md` —
  deferred work (embedding-based search, tRPC discovery surface, etc.).

---

## FAQ

**Q: Can a skill capability call another capability?**
Today, no — the registry exposes `invoke` only to the bridge. Cross-capability
composition is handled by the LLM (it calls one tool, then another). A
programmatic `compose` API is tracked under `AF-PV2` follow-ups.

**Q: How do I deregister a capability mid-session?**
Send a `capability_deregister` event from the runner side, or
`capability_deregister` command from the client side. The registry removes
the entry idempotently.

**Q: Where does the `<CAPABILITIES>` system-prompt block live?**
`agent-framework/runner/src/prompt-assembly.ts` (`buildCapabilitiesPromptSection`)
joins every registered capability's `promptFragment` into a single block,
ordered framework → client → agent-side. The block is appended via
`AgentSessionConfig.additionalPromptSections` in `serve.ts`.

**Q: How do I test a capability without spinning up the runner?**
Construct a `CapabilityRegistry`, register the capability, then call
`registry.invoke(name, input, baseCtx)` directly. The registry handles Zod
validation and logger wiring. See
`agent-framework/runner/tests/capability-registry.test.ts` for examples.
