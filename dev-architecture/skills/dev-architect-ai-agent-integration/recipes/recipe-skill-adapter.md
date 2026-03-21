---
name: recipe-skill-adapter
description: Full implementation of the SkillAdapter — state machine, action routing, consumer presence, bidirectional pending action resolution, and input derivation from pipeline.json.
libraries_used: [node:events, node:crypto, nuxt4/nitro]
---

# Recipe: Skill Adapter (`server/utils/skill-adapter.ts`)

The central bridge between raw omp events and the typed Typed Action Protocol.

## Step 1: Shared Types (`shared/types/skill-actions.ts`)

```typescript
// shared/types/skill-actions.ts

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

export interface DialogField {
  id: string
  label: string
  type: 'text' | 'textarea' | 'select' | 'multiselect' | 'toggle'
  placeholder?: string
  options?: Array<{ value: string; label: string }>
  required?: boolean
  default?: unknown
}

export type SkillAction =
  | { type: 'request_input'; actionId: string; fields: DialogField[]; context?: string; skill: string; stepId: string | null }
  | { type: 'show_progress'; skill: string; stepId: string | null; message: string; percent?: number }
  | { type: 'request_approval'; actionId: string; skill: string; stepId: string | null; artifacts: string[]; summary: string }
  | { type: 'notify'; level: 'info' | 'warn' | 'error'; message: string }
  | { type: 'file_changed'; path: string; action: 'created' | 'updated' | 'deleted' }
  | { type: 'skill_state'; state: SkillState; skill: string; stepId: string | null; runId: string; message?: string }
  | { type: 'open_file'; path: string }
  | { type: 'show_diff'; path: string; before: string; after: string }

export type ConsumerResponse =
  | { type: 'input_response'; actionId: string; values: Record<string, unknown> }
  | { type: 'approval_response'; actionId: string; approved: boolean; feedback?: string }
  | { type: 'cancel'; reason?: string }

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

## Step 2: Skill Adapter (`server/utils/skill-adapter.ts`)

```typescript
// server/utils/skill-adapter.ts
import { EventEmitter } from 'node:events'
import { randomUUID } from 'node:crypto'
import type { SkillAction, SkillRunState, SkillState, ConsumerResponse, StreamEvent, DialogField } from '~/shared/types/skill-actions'

// Re-export types for convenience
export type { SkillAction, SkillRunState, SkillState, ConsumerResponse, StreamEvent }

// ─── Emitter ───────────────────────────────────────────────────────────────
// Emits on channel `action:${conceptName}` with a StreamEvent payload.
// Replace any legacy `agentEmitter` with this.
export const adapterEmitter = new EventEmitter()
adapterEmitter.setMaxListeners(100)

// ─── State Machine ──────────────────────────────────────────────────────────
const runStates = new Map<string, SkillRunState>()

function createRunState(skillName: string, stepId: string | null): SkillRunState {
  return {
    skillName,
    stepId,
    runId: randomUUID(),
    state: 'idle',
    startedAt: Date.now(),
    toolCallCount: 0,
    currentTool: null,
  }
}

function transitionState(conceptName: string, state: SkillState, message?: string) {
  const run = runStates.get(conceptName)
  if (!run) return
  run.state = state
  if (message) run.message = message
  emitStreamEvent(conceptName, { type: 'run_state', runState: { ...run } })
  emitAction(conceptName, {
    type: 'skill_state',
    state,
    skill: run.skillName,
    stepId: run.stepId,
    runId: run.runId,
    message,
  })
}

export function getRunState(conceptName: string): SkillRunState | null {
  return runStates.get(conceptName) ?? null
}

// ─── Consumer Presence ──────────────────────────────────────────────────────
const consumerCounts = new Map<string, number>()

export function registerConsumer(conceptName: string): () => void {
  consumerCounts.set(conceptName, (consumerCounts.get(conceptName) ?? 0) + 1)
  return () => {
    const count = consumerCounts.get(conceptName) ?? 0
    if (count <= 1) consumerCounts.delete(conceptName)
    else consumerCounts.set(conceptName, count - 1)
  }
}

export function hasConsumer(conceptName: string): boolean {
  return (consumerCounts.get(conceptName) ?? 0) > 0
}

// ─── Event Emission Helpers ─────────────────────────────────────────────────
function emitStreamEvent(conceptName: string, event: StreamEvent) {
  adapterEmitter.emit(`action:${conceptName}`, event)
}

function emitAction(conceptName: string, action: SkillAction) {
  emitStreamEvent(conceptName, { type: 'action', action })
}

// ─── Pending Action Resolution ───────────────────────────────────────────────
// Bidirectional channel: adapter emits request_input/request_approval,
// consumer responds via /api/agent/respond → handleConsumerResponse().
const pendingActions = new Map<string, { conceptName: string; resolve: (r: ConsumerResponse) => void; reject: (e: Error) => void }>()

function waitForResponse(actionId: string, conceptName: string, timeoutMs = 300_000): Promise<ConsumerResponse> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      pendingActions.delete(actionId)
      reject(new Error(`Action ${actionId} timed out after ${timeoutMs}ms`))
    }, timeoutMs)

    pendingActions.set(actionId, {
      conceptName,
      resolve: (r) => { clearTimeout(timer); pendingActions.delete(actionId); resolve(r) },
      reject: (e) => { clearTimeout(timer); pendingActions.delete(actionId); reject(e) },
    })
  })
}

export function handleConsumerResponse(response: ConsumerResponse): boolean {
  if (response.type === 'cancel') {
    // Cancel all pending actions for any concept
    for (const [id, pending] of pendingActions) {
      pending.resolve(response)
      pendingActions.delete(id)
    }
    return true
  }
  if ('actionId' in response) {
    const pending = pendingActions.get(response.actionId)
    if (pending) {
      pending.resolve(response)
      return true
    }
  }
  return false
}

// ─── Pipeline Input Derivation ───────────────────────────────────────────────
let _pipelineInputsCache: Map<string, { dialog: DialogField[]; files: string[] }> | null = null

function getPipelineInputs(): Map<string, { dialog: DialogField[]; files: string[] }> {
  if (_pipelineInputsCache) return _pipelineInputsCache

  const map = new Map<string, { dialog: DialogField[]; files: string[] }>()
  try {
    // loadPipelineDefinition() reads your pipeline.json — adjust import as needed
    const pipeline = loadPipelineDefinition()
    for (const step of pipeline.steps ?? []) {
      const dialog = step.user_inputs?.dialog ?? []
      const files = step.user_inputs?.files ?? []
      if (dialog.length > 0) {
        map.set(step.id, { dialog, files })
      }
    }
  } catch {
    // pipeline.json not available — no input forms
  }

  _pipelineInputsCache = map
  return map
}

// ─── Skill Dispatch ───────────────────────────────────────────────────────────
export async function dispatchSkill(
  conceptName: string,
  skillName: string,
  promptFn: (prompt: string) => Promise<void>,
  providedInputs?: Record<string, unknown>
): Promise<void> {
  // Determine step ID from skill name (convention: skill name = step id)
  const stepId = skillName ?? null

  const run = createRunState(skillName, stepId)
  runStates.set(conceptName, run)

  // Emit initial state
  emitStreamEvent(conceptName, { type: 'run_state', runState: { ...run } })

  // Check if inputs needed
  const inputs = getPipelineInputs()
  const stepInputs = stepId ? inputs.get(stepId) : null

  if (stepInputs?.dialog && stepInputs.dialog.length > 0 && !providedInputs && hasConsumer(conceptName)) {
    // Collect inputs from frontend
    transitionState(conceptName, 'collecting_inputs')
    const actionId = randomUUID()
    emitAction(conceptName, {
      type: 'request_input',
      actionId,
      fields: stepInputs.dialog,
      context: `Running skill: ${skillName}`,
      skill: skillName,
      stepId,
    })

    let response: ConsumerResponse
    try {
      response = await waitForResponse(actionId, conceptName)
    } catch {
      transitionState(conceptName, 'failed', 'Input collection timed out')
      runStates.delete(conceptName)
      return
    }

    if (response.type === 'cancel') {
      transitionState(conceptName, 'idle')
      runStates.delete(conceptName)
      return
    }

    providedInputs = (response as { type: 'input_response'; actionId: string; values: Record<string, unknown> }).values
  }

  // Build prompt
  let prompt = `Run skill ${skillName}`
  if (providedInputs && Object.keys(providedInputs).length > 0) {
    prompt += '\n\nInputs:\n' + Object.entries(providedInputs)
      .map(([k, v]) => `- ${k}: "${v}"`)
      .join('\n')
  }

  transitionState(conceptName, 'running')

  try {
    await promptFn(prompt)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e)
    transitionState(conceptName, 'failed', msg)
    runStates.delete(conceptName)
  }
}

export async function dispatchPrompt(
  conceptName: string,
  prompt: string,
  promptFn: (prompt: string) => Promise<void>
): Promise<void> {
  const run = createRunState('_prompt', null)
  runStates.set(conceptName, run)
  transitionState(conceptName, 'running')
  try {
    await promptFn(prompt)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e)
    transitionState(conceptName, 'failed', msg)
    runStates.delete(conceptName)
  }
}

// ─── Raw omp Event Processing ────────────────────────────────────────────────
export function processAgentEvent(conceptName: string, event: unknown): void {
  const ev = event as Record<string, unknown>
  const run = runStates.get(conceptName)

  switch (ev.type) {
    case 'ready':
      // omp is ready — no action needed (startup handled by OmpProcess)
      break

    case 'message_start': {
      const msg = ev.message as Record<string, unknown> | undefined
      if (msg?.role === 'assistant') {
        transitionState(conceptName, 'running')
        emitStreamEvent(conceptName, {
          type: 'agent_message',
          role: 'assistant',
          content: '',
          streaming: true,
        })
      }
      break
    }

    case 'content_block_delta': {
      const delta = ev.delta as Record<string, unknown> | undefined
      if (delta?.type === 'text_delta' && typeof delta.text === 'string') {
        emitStreamEvent(conceptName, { type: 'agent_message_delta', content: delta.text })
      }
      break
    }

    case 'message_end': {
      const msg = ev.message as Record<string, unknown> | undefined
      if (msg?.role === 'assistant') {
        const content = typeof msg.content === 'string' ? msg.content : ''
        emitStreamEvent(conceptName, {
          type: 'agent_message',
          role: 'assistant',
          content,
          streaming: false,
        })
      }
      break
    }

    case 'tool_call': {
      if (run) {
        run.toolCallCount++
        run.currentTool = typeof ev.name === 'string' ? ev.name : null
      }
      emitStreamEvent(conceptName, {
        type: 'tool_call',
        name: typeof ev.name === 'string' ? ev.name : 'unknown',
        toolCallCount: run?.toolCallCount ?? 1,
      })
      break
    }

    case 'tool_execution_end': {
      const toolName = typeof ev.toolName === 'string' ? ev.toolName : ''
      const success = ev.success !== false
      emitStreamEvent(conceptName, { type: 'tool_result', name: toolName, success })

      // Detect file writes → emit file_changed
      if (['write', 'write_file', 'edit', 'edit_file'].includes(toolName)) {
        const input = ev.input as Record<string, unknown> | undefined
        const filePath = (input?.path ?? input?.file_path ?? '') as string
        if (filePath) {
          emitAction(conceptName, { type: 'file_changed', path: filePath, action: 'created' })
        }
      }
      break
    }

    case 'turn_end': {
      // Authoritative "turn done" signal
      if (run) run.currentTool = null
      break
    }

    case 'agent_end': {
      // Skill completed — optionally request approval
      if (run) {
        const needsApproval = run.stepId && hasConsumer(conceptName)
        if (needsApproval) {
          transitionState(conceptName, 'awaiting_approval')
          const actionId = randomUUID()
          emitAction(conceptName, {
            type: 'request_approval',
            actionId,
            skill: run.skillName,
            stepId: run.stepId,
            artifacts: [],  // populate from file_changed events if tracked
            summary: `${run.skillName} completed with ${run.toolCallCount} tool calls`,
          })
          // Don't await — approval handled asynchronously via /api/agent/respond
        } else {
          transitionState(conceptName, 'completed')
          runStates.delete(conceptName)
        }
      }
      break
    }

    case 'error': {
      const errMsg = typeof ev.message === 'string' ? ev.message : 'Unknown error'
      transitionState(conceptName, 'failed', errMsg)
      emitStreamEvent(conceptName, { type: 'error', error: errMsg })
      runStates.delete(conceptName)
      break
    }
  }
}
```

## Step 3: Wire into concept-agent.ts

```typescript
// server/utils/concept-agent.ts (relevant section)
import { processAgentEvent, dispatchSkill, dispatchPrompt } from './skill-adapter'

// In event handler:
omp.on('agent-event', (event) => {
  // Track streaming state locally
  if (event.type === 'message_start' && event.message?.role === 'assistant') {
    session.isStreaming = true
  }
  if (event.type === 'turn_end' || event.type === 'agent_end' || event.type === 'error') {
    session.isStreaming = false
  }

  // Route ALL events through the adapter (emits typed SkillActions)
  processAgentEvent(conceptName, event)
})

// Exported dispatch functions:
export async function dispatchSkillViaAdapter(
  conceptName: string,
  skill: string,
  inputs?: Record<string, unknown>
): Promise<void> {
  const session = await getConceptSession(conceptName)
  await dispatchSkill(conceptName, skill, (prompt) => session.process.prompt(prompt), inputs)
}

export async function promptConceptViaAdapter(
  conceptName: string,
  prompt: string
): Promise<void> {
  const session = await getConceptSession(conceptName)
  await dispatchPrompt(conceptName, prompt, (p) => session.process.prompt(p))
}
```

## Gotchas
- `processAgentEvent()` must be called for EVERY omp event, not just agent events
- `tool_execution_end` vs `tool_call`: `tool_call` fires when the model requests a tool; `tool_execution_end` fires after the tool runs
- `agent_end` fires before `turn_end` — use `turn_end` as the authoritative "done" signal for streaming state
- Clear `_pipelineInputsCache` if pipeline.json can change at runtime
