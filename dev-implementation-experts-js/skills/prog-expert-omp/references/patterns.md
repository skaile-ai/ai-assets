# OMP Patterns & Best Practices

## 1. RPC Sidecar Integration

### Process-per-Project
- Spawn one omp process per project/workspace
- Set `cwd` to the project's workspace root so omp tools operate on the correct files
- Pass API keys via environment variables, model via `--model` CLI flag
- Use `--no-session` for embedding (host app manages persistence)

### Ready-Before-Send
- Always await the `{"type":"ready"}` event before writing commands to stdin
- Use a timeout fallback (3-5s) in case ready event is delayed
- Pattern: `once('ready')` + `setTimeout()` race

### Event-Driven State Machine
```
idle → prompt sent → isStreaming=true → turn_end → isStreaming=false
                                       → agent_end → isStreaming=false
                                       → error → isStreaming=false
```
- Set `isStreaming=true` only on assistant `message_start` (not user)
- Reset on `turn_end` (always fires) OR `agent_end` OR error

### Message Persistence
- **DO NOT** persist user messages in `prompt()` — omp sends `message_end` for user messages
- Persist on `message_end` for both roles
- Persist tool results from `turn_end.toolResults`
- Use JSONL format (one message per line)

### Graceful Process Restart
- omp exits on invalid `--model` — handle `exit` event and recreate
- Clear stale session reference on exit but keep messages (loaded from disk)
- Use a pending-promise pattern to prevent duplicate starts

## 2. Agent Architecture

### Model Roles
omp supports multiple model roles for different task types:
| Role | Flag | Env Var | Use Case |
|------|------|---------|----------|
| default | `--model` | — | General coding tasks |
| smol | `--smol` | `PI_SMOL_MODEL` | Quick, lightweight tasks |
| slow | `--slow` | `PI_SLOW_MODEL` | Deep analysis, debugging |
| plan | `--plan` | `PI_PLAN_MODEL` | Architecture, planning |
| commit | — | — | Commit message generation |

### Context Management
- **Auto-compaction**: omp automatically compresses context when approaching limits
- **TTSR**: Time-traveling streamed rules — rules can be injected at different context positions
- **Branching**: Conversations can fork into branches

### Subagent Isolation
The `task` tool spawns subagents with isolation:
- **Worktrees**: Git worktree for independent file operations
- **FUSE overlay**: Filesystem overlay for sandboxed modifications
- Subagents run in parallel with independent context

## 3. SSE/WebSocket Bridge — Typed Action Protocol (Recommended)

For web apps, do NOT forward raw omp events directly to the frontend.
Use the Skill Adapter pattern instead: translate raw events into typed SkillActions.

See **`../prog-expert-integration-ai-agents/`** for the full specification:
- **Architecture & types**: `references/patterns.md` — protocol types, state machine, API key resolution, gotchas
- **Skill Adapter**: `recipes/recipe-skill-adapter.md` — server-side event translation + state machine
- **API Endpoints**: `recipes/recipe-dispatch-endpoint.md` — dispatch, respond, actions SSE
- **Frontend Composable**: `recipes/recipe-frontend-composable.md` — `useSkillActions` composable
- **UI Components**: `recipes/recipe-action-components.md` — input/approval dialogs, state indicators

**Why raw event forwarding is wrong:**
- Skill dispatch via regex: `/Run skill\s+(\S+)/i` — brittle
- No structured input collection (LLM asks in free text)
- No approval flows — user types text to approve
- Frontend hardcodes knowledge from skills (startQuestionsMap, etc.)
- One-way — frontend can't send structured signals back

**Typed Action Protocol (correct):**
```
omp → processAgentEvent() → typed SkillActions → adapterEmitter → SSE
Frontend → POST /api/agent/dispatch (skill) OR POST /api/agent/respond (response)
```

```typescript
// Server: route ALL omp events through the Skill Adapter
import { processAgentEvent, adapterEmitter } from './skill-adapter'

omp.on('agent-event', (event) => {
  processAgentEvent(conceptName, event)  // emits typed SkillActions to adapterEmitter
})

// SSE endpoint subscribes to adapterEmitter (not omp directly)
adapterEmitter.on(`action:${conceptName}`, (streamEvent) => {
  sseStream.push({ data: JSON.stringify(streamEvent) })
})
```

```typescript
// StreamEvent union — what the frontend receives:
type StreamEvent =
  | { type: 'connected'; runState: SkillRunState | null }
  | { type: 'action'; action: SkillAction }   // request_input, request_approval, notify, file_changed...
  | { type: 'agent_message'; role: 'assistant'; content: string; streaming: boolean }
  | { type: 'agent_message_delta'; content: string }
  | { type: 'tool_call'; name: string; toolCallCount: number }
  | { type: 'run_state'; runState: SkillRunState }  // state machine: idle→running→completed
  | { type: 'error'; error: string }
```

**Raw bridge (only for debugging / simple CLI consumers):**
```typescript
omp.on('agent-event', (event) => {
  sseStream.push({ data: JSON.stringify(event) });
});
```

## 4. API Key Resolution

### omp's own resolution order (CLI / standalone)
1. `--api-key` CLI flag
2. Environment variables: `{PROVIDER}_API_KEY`
3. `~/.omp/settings.json`
4. `.omp/settings.json` (project-level)

### Host app resolution order (embedding)
When embedding omp, resolve keys in the host app before injecting into the child process:
1. `process.env[{PROVIDER_UPPER}_API_KEY]` — already in process env (set by CLI or system)
2. `.env` files: `~/.env` → project root `.env` → app root `.env` (merge, later overrides earlier)
3. Stored in `omp-agent/config/settings.json` as `apiKeys.{provider}`

```typescript
// Convert provider id to env var name
function providerEnvKey(provider: string): string {
  return `${provider.toUpperCase().replace(/[^A-Z0-9]/g, '_')}_API_KEY`
}

// Resolution: env var takes priority over stored key
function resolveApiKey(provider: string, settings: AppSettings): string | undefined {
  return process.env[providerEnvKey(provider)] || settings.apiKeys[provider]
}

// Inject into omp child process env
const env: Record<string, string> = { ...process.env as Record<string, string> }
for (const [provider, key] of Object.entries(settings.apiKeys)) {
  if (key) env[providerEnvKey(provider)] = key
}
```

### Model Selection
Store model config in `omp-agent/config/settings.json`:
```json
{ "defaultProvider": "anthropic", "defaultModel": "claude-sonnet-4-6" }
```

Always pass `--model` explicitly when embedding — never let omp use its own default:
```typescript
if (settings.defaultModel) {
  const modelArg = settings.defaultProvider
    ? `${settings.defaultProvider}/${settings.defaultModel}`
    : settings.defaultModel
  ompArgs.push('--model', modelArg)
}
```

Model format is always `provider/model` (e.g. `anthropic/claude-sonnet-4-6`). Never `p-provider/model`.

## 5. Common Gotchas

| Gotcha | Fix |
|--------|-----|
| Model format `p-provider/model` | Use `provider/model` (no `p-` prefix) |
| Duplicate user messages | Don't persist in `prompt()`, rely on `message_end` |
| `--no-tools` still fails | Model must support tool-calling API format |
| `agent_end` before `turn_end` | Use `turn_end` as the authoritative "done" signal |
| Empty content on error | Check `stopReason === "error"` and `errorMessage` field |
| Process exits on bad model | Handle `exit` event, validate model exists first |
| `ready` event race condition | Use `once('ready')` + timeout race in start() |
| omp uses own default model | Always pass `--model` explicitly when embedding |

## 6. Extension & Hook Patterns

### TypeScript Extension
```typescript
// my-extension.ts
export default {
  name: 'my-extension',
  hooks: {
    'before:prompt': async (ctx) => {
      // Modify context before sending to model
    },
    'after:response': async (ctx) => {
      // Process model response
    }
  }
};
```

Load via: `omp -e my-extension.ts` or `omp --hook my-extension.ts`

### Extension Discovery
omp discovers extensions from 8 AI tool directories:
- Claude, Cursor, Windsurf, Continue, Cody, Copilot, Aider, omp-native
