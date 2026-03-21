# AI Agent Integration Patterns

## 1. Architecture Overview

Two integration depths exist:

### Naive (Raw Event Bridge) — DO NOT USE
```
omp process → stdout JSON → agentEmitter → SSE → frontend
Frontend → POST /api/agent/prompt with natural language "Run skill X"
```
Problems:
- Skill dispatch via regex: `/Run skill\s+(\S+)/i` — brittle string matching
- No structured input collection — LLM asks questions as free text
- No approval flows — user types text to approve
- Frontend hardcodes knowledge that should come from skills (startQuestionsMap, etc.)
- One-way events: frontend cannot send structured signals back

### Typed Action Protocol (Correct)
```
omp process → SkillAdapter.processAgentEvent() → typed SkillActions → adapterEmitter → SSE
Frontend → POST /api/agent/dispatch (structured) OR POST /api/agent/respond (typed response)
```
Key properties:
- Bidirectional typed messages (SkillAction / ConsumerResponse)
- State machine per skill run (idle→collecting_inputs→running→checkpoint→awaiting_approval→completed)
- Input forms derived from pipeline.json metadata (no hardcoded maps)
- Consumer presence detection (skill adapter knows if frontend is connected)
- Skills are pure SKILL.md prompts — adapter derives actions from metadata + LLM output heuristics

---

## 2. Protocol Types (`shared/types/skill-actions.ts`)

```typescript
// ---- State Machine ----
export type SkillState =
  | 'idle'
  | 'collecting_inputs'
  | 'running'
  | 'checkpoint'
  | 'awaiting_approval'
  | 'completed'
  | 'failed'
  | 'blocked'

export interface SkillRunState {
  skillName: string
  stepId: string | null
  runId: string
  state: SkillState
  startedAt: number
  toolCallCount: number
  currentTool: string | null
  message?: string
}

// ---- Dialog Field (from pipeline.json) ----
export interface DialogField {
  id: string
  label: string
  type: 'text' | 'textarea' | 'select' | 'multiselect' | 'toggle'
  placeholder?: string
  options?: Array<{ value: string; label: string }>
  required?: boolean
  default?: unknown
}

// ---- Skill → Consumer (outbound) ----
export type SkillAction =
  | { type: 'request_input'; actionId: string; fields: DialogField[]; context?: string; skill: string; stepId: string | null }
  | { type: 'show_progress'; skill: string; stepId: string | null; message: string; percent?: number }
  | { type: 'request_approval'; actionId: string; skill: string; stepId: string | null; artifacts: string[]; summary: string }
  | { type: 'notify'; level: 'info' | 'warn' | 'error'; message: string }
  | { type: 'file_changed'; path: string; action: 'created' | 'updated' | 'deleted' }
  | { type: 'skill_state'; state: SkillState; skill: string; stepId: string | null; runId: string; message?: string }
  | { type: 'open_file'; path: string }
  | { type: 'show_diff'; path: string; before: string; after: string }

// ---- Consumer → Skill (inbound) ----
export type ConsumerResponse =
  | { type: 'input_response'; actionId: string; values: Record<string, unknown> }
  | { type: 'approval_response'; actionId: string; approved: boolean; feedback?: string }
  | { type: 'cancel'; reason?: string }

// ---- SSE Stream Events ----
export type StreamEvent =
  | { type: 'connected'; runState: SkillRunState | null; hasConsumer: boolean }
  | { type: 'action'; action: SkillAction }
  | { type: 'agent_message'; role: 'assistant'; content: string; streaming: boolean }
  | { type: 'agent_message_delta'; content: string }
  | { type: 'tool_call'; name: string; toolCallCount: number }
  | { type: 'tool_result'; name: string; success: boolean; summary?: string }
  | { type: 'run_state'; runState: SkillRunState }
  | { type: 'error'; error: string }
```

---

## 3. Skill Adapter (server-side)

Single server-side module (`server/utils/skill-adapter.ts`) that bridges omp events to typed actions.

Key responsibilities:
- Translate raw omp events → typed `SkillAction`s via `adapterEmitter`
- Track state machine per concept name
- Track consumer presence (is frontend connected?)
- Resolve pending input/approval Promises (bidirectional channel)
- Derive input forms from `pipeline.json`

```typescript
// Emits on channel: `action:${conceptName}`
export const adapterEmitter = new EventEmitter()

// State machine tracking
const runStates = new Map<string, SkillRunState>()

// Consumer presence
const consumerCounts = new Map<string, number>()
export function registerConsumer(conceptName: string): () => void { ... }
export function hasConsumer(conceptName: string): boolean { ... }

// Core entrypoints
export async function dispatchSkill(
  conceptName: string,
  skillName: string,
  promptFn: (prompt: string) => Promise<void>,
  providedInputs?: Record<string, unknown>
): Promise<void>

export async function dispatchPrompt(
  conceptName: string,
  prompt: string,
  promptFn: (prompt: string) => Promise<void>
): Promise<void>

export function processAgentEvent(conceptName: string, event: unknown): void
export function handleConsumerResponse(response: ConsumerResponse): boolean
```

See `recipes/recipe-skill-adapter.md` for the full implementation.

---

## 4. Three API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/agent/dispatch` | POST | Structured skill dispatch (`{ concept, skill, inputs? }`) |
| `/api/agent/respond` | POST | Consumer response (`{ concept, response: ConsumerResponse }`) |
| `/api/agent/actions` | GET | Typed SSE stream (`?concept=X`) |

Old endpoints to **remove**:
- `GET /api/agent/stream` → replaced by `/api/agent/actions`
- `POST /api/agent/prompt` with skill detection → replaced by `/api/agent/dispatch`

See `recipes/recipe-dispatch-endpoint.md` for full implementation.

---

## 5. Frontend Composable

`useSkillActions(conceptRef)` manages the SSE connection and all action state:

```typescript
const skills = useSkillActions(conceptRef)

// State (all reactive)
skills.runState       // SkillRunState | null
skills.isRunning      // computed bool
skills.activeSkill    // computed string | null
skills.currentTool    // computed string | null
skills.pendingInput   // RequestInputAction | null → auto-render as form
skills.pendingApproval // RequestApprovalAction | null → auto-render as dialog
skills.messages       // ChatMessage[]
skills.notifications  // { level, message }[]
skills.changedFiles   // string[]

// Actions
await skills.dispatchSkill('cf_overview', { app_name: 'MyApp' })
await skills.sendPrompt('Improve the brief')
await skills.submitInput(actionId, { app_name: 'MyApp' })
await skills.submitApproval(actionId, true)
await skills.cancel()
await skills.abort()
await skills.clearChat()
```

See `recipes/recipe-frontend-composable.md` for full implementation.

---

## 6. API Key Resolution

### Resolution Order (priority high → low)
1. Environment variable: `{PROVIDER_UPPER}_API_KEY` (e.g. `ANTHROPIC_API_KEY`)
2. `.env` files: `~/.env` → project root `.env` → app root `.env` (merged, later overrides earlier)
3. Stored in `omp-agent/config/settings.json` as `apiKeys.{provider}`

### Implementation Pattern
```typescript
// server/utils/settings.ts
export function providerEnvKey(provider: string): string {
  return `${provider.toUpperCase().replace(/[^A-Z0-9]/g, '_')}_API_KEY`
}

export function resolveApiKey(provider: string, settings: AppSettings): string | undefined {
  return process.env[providerEnvKey(provider)] || settings.apiKeys[provider]
}

// Injecting into omp child process env:
const env: Record<string, string> = { ...process.env }
for (const [provider, key] of Object.entries(settings.apiKeys)) {
  if (key) env[providerEnvKey(provider)] = key
}
```

### Supported Providers
```typescript
export const ALL_PROVIDERS = [
  'anthropic', 'openai', 'google', 'mistral', 'groq', 'openrouter',
  'deepseek', 'xai', 'together', 'fireworks',
  'deepgram', 'elevenlabs',
] as const
```

---

## 7. Default Model Selection

### Storage
Model config stored in `omp-agent/config/settings.json`:
```json
{
  "defaultProvider": "anthropic",
  "defaultModel": "claude-sonnet-4-6"
}
```

### Injecting into omp
Always pass `--model` explicitly — omp will use its own default otherwise:
```typescript
if (settings.defaultModel) {
  const modelArg = settings.defaultProvider
    ? `${settings.defaultProvider}/${settings.defaultModel}`
    : settings.defaultModel
  ompArgs.push('--model', modelArg)
}
```

### Model Format Rules
- ALWAYS: `provider/model` (e.g. `anthropic/claude-sonnet-4-6`)
- NEVER: `p-provider/model` (wrong prefix)
- NEVER: bare model name without provider (unless no provider configured)

### AppSettings Interface
```typescript
export interface AppSettings {
  apiKeys: Record<string, string>   // provider → raw key (stored, not env)
  defaultProvider: string           // e.g. 'anthropic'
  defaultModel: string              // e.g. 'claude-sonnet-4-6'
  defaultSttProvider: string        // e.g. 'deepgram'
  defaultTtsProvider: string        // e.g. 'elevenlabs'
  sttLanguage: string               // e.g. 'de'
  ttsVoice: string                  // ElevenLabs voice ID
}
```

---

## 8. State Machine Visualization

```
State transitions:
idle → collecting_inputs → running → checkpoint → awaiting_approval → completed
                                   → failed
                                   → blocked
```

State → UI mapping:
- `collecting_inputs` → yellow pulsing dot + input form shown
- `running` → primary pulsing dot + tool call indicator
- `checkpoint` → blue dot + progress summary
- `awaiting_approval` → amber dot + approval dialog
- `completed` → green dot (briefly) → trigger pipeline refresh
- `failed` → red dot + error message
- `blocked` → orange dot + blocked message

---

## 9. Consumer Presence

When `hasConsumer() === false` (CLI / headless mode):
- Skip `request_input` actions (LLM asks in natural language)
- Skip `request_approval` actions (LLM handles in conversation)
- Still emit file_changed, notify, progress events

This allows the same skills to work both headlessly and with a connected frontend.

---

## 10. Input Derivation from pipeline.json

Never hardcode form fields in the frontend. Derive from `pipeline.json`:
```typescript
// pipeline.json step shape:
{
  "id": "01_project",
  "user_inputs": {
    "dialog": [
      { "id": "app_name", "label": "App Name", "type": "text", "required": true },
      { "id": "elevator_pitch", "label": "Elevator Pitch", "type": "textarea" }
    ],
    "files": []
  }
}

// server/utils/skill-adapter.ts
function getPipelineInputs(): Map<string, { dialog: DialogField[]; files: string[] }> {
  const pipeline = loadPipelineDefinition()  // reads pipeline.json
  const map = new Map()
  for (const step of pipeline.steps) {
    if (step.user_inputs?.dialog?.length > 0) {
      map.set(step.id, { dialog: step.user_inputs.dialog, files: step.user_inputs.files ?? [] })
    }
  }
  return map
}
```

---

## 11. Common Gotchas

| Gotcha | Fix |
|--------|-----|
| Raw SSE bridge forwarding omp events | Use `processAgentEvent()` → typed SkillActions |
| Skill dispatch via regex string matching | Use `POST /api/agent/dispatch { skill }` |
| Hardcoded startQuestionsMap in frontend | Derive from `pipeline.json` `user_inputs.dialog` |
| Model format `p-provider/model` | Use `provider/model` (no `p-` prefix) |
| Duplicate user messages persisted | Don't persist in `prompt()`, rely on `message_end` |
| `agent_end` before `turn_end` | Use `turn_end` as the authoritative "done" signal |
| omp uses own default model | Always pass `--model` explicitly when embedding |
| `ready` event race condition | Use `once('ready')` + timeout race in start() |
| Input forms shown headlessly | Check `hasConsumer()` before emitting `request_input` |
| API key not found by omp | Ensure keys are injected into child process env (not just process.env) |

---

## 12. Backward Compatibility

DO NOT maintain backward compat if you own all consumers. Remove:
- Raw `agentEmitter` and its SSE bridge (`/api/agent/stream`)
- String-matching dispatch (`/Run skill\s+(\S+)/i`)
- Hardcoded `startQuestionsMap` in frontend
- `promptConcept()` legacy function (replace with `dispatchSkill` / `dispatchPrompt`)
- 404 fallback in `runSkill()` to old prompt endpoint
