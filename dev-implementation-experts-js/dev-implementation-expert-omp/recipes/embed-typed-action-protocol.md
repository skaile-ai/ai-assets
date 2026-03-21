---
name: Typed Action Protocol for Web Embedding
description: Bidirectional typed events between omp sidecar and frontend via Skill Adapter
libraries_used: node:events, node:crypto, nuxt 4, nitro
learned_from: CF-PROJECT/concept-forge feature/typed-action-protocol
---

# Typed Action Protocol for Web Embedding

## Objective

Replace the naive raw-event SSE bridge with a typed bidirectional protocol:
- omp events → SkillAdapter → typed SkillActions → SSE → frontend
- Frontend → POST /api/agent/dispatch or /api/agent/respond → SkillAdapter → omp

## Prerequisites

- omp running as RPC sidecar (see `rpc-node-sidecar.md`)
- Nitro/Nuxt 4 server
- `shared/types/skill-actions.ts` with protocol types

## Instructions

### 1. Define the Protocol Types

Create `shared/types/skill-actions.ts`:

```typescript
export type SkillState = 'idle' | 'collecting_inputs' | 'running' | 'checkpoint' | 'awaiting_approval' | 'completed' | 'failed' | 'blocked'

export interface SkillRunState {
  runId: string
  skill: string
  stepId: string | null
  state: SkillState
  startedAt: number
  message: string
  toolCallCount: number
  currentTool: string | null
  pendingActionId: string | null
}

export interface DialogField {
  id: string
  label: string
  type: 'text' | 'textarea' | 'select' | 'multiselect' | 'boolean' | 'number'
  required?: boolean
  options?: string[]
  default?: string
  hint?: string
}

export type SkillAction =
  | { type: 'request_input'; actionId: string; fields: DialogField[]; context?: string; skill: string; stepId: string | null }
  | { type: 'show_progress'; skill: string; stepId: string | null; message: string; percent?: number }
  | { type: 'request_approval'; actionId: string; skill: string; stepId: string | null; artifacts: string[]; summary: string; diff?: string }
  | { type: 'notify'; level: 'info' | 'warn' | 'error'; message: string; skill?: string }
  | { type: 'file_changed'; path: string; action: 'created' | 'updated' | 'deleted'; skill?: string }
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

### 2. Create the Skill Adapter

Create `server/utils/skill-adapter.ts` (core bridge module):

```typescript
import { EventEmitter } from 'node:events'
import { randomUUID } from 'node:crypto'
import type { SkillState, SkillRunState, SkillAction, ConsumerResponse, StreamEvent } from '../../shared/types/skill-actions'

export const adapterEmitter = new EventEmitter()
adapterEmitter.setMaxListeners(50)

const runStates = new Map<string, SkillRunState>()
const consumerCounts = new Map<string, number>()
const pendingActions = new Map<string, { resolve: (r: ConsumerResponse) => void; conceptName: string }>()

// Consumer presence
export function registerConsumer(conceptName: string): () => void {
  consumerCounts.set(conceptName, (consumerCounts.get(conceptName) ?? 0) + 1)
  return () => {
    const n = (consumerCounts.get(conceptName) ?? 1) - 1
    n <= 0 ? consumerCounts.delete(conceptName) : consumerCounts.set(conceptName, n)
  }
}
export function hasConsumer(conceptName: string) { return (consumerCounts.get(conceptName) ?? 0) > 0 }
export function getRunState(conceptName: string) { return runStates.get(conceptName) ?? null }

function emitAction(conceptName: string, action: SkillAction) {
  adapterEmitter.emit(`action:${conceptName}`, { type: 'action', action } as StreamEvent)
}
function emitStreamEvent(conceptName: string, event: StreamEvent) {
  adapterEmitter.emit(`action:${conceptName}`, event)
}

function transitionState(conceptName: string, newState: SkillState, message?: string) {
  const run = runStates.get(conceptName)
  if (!run) return
  run.state = newState
  if (message !== undefined) run.message = message
  emitAction(conceptName, { type: 'skill_state', state: newState, skill: run.skill, stepId: run.stepId, runId: run.runId, message: run.message })
}

// Structured skill dispatch — collects inputs if needed, then calls omp
export async function dispatchSkill(conceptName: string, skillName: string, promptFn: (p: string) => Promise<void>, providedInputs?: Record<string, unknown>) {
  const run: SkillRunState = { runId: randomUUID(), skill: skillName, stepId: findStepId(skillName), state: 'idle', startedAt: Date.now(), message: '', toolCallCount: 0, currentTool: null, pendingActionId: null }
  runStates.set(conceptName, run)
  transitionState(conceptName, 'idle')

  const inputs = getInputsForSkill(skillName, run.stepId)
  if (inputs?.dialog.length && !providedInputs && hasConsumer(conceptName)) {
    transitionState(conceptName, 'collecting_inputs', `Waiting for input: ${skillName}`)
    const actionId = randomUUID()
    run.pendingActionId = actionId
    emitAction(conceptName, { type: 'request_input', actionId, fields: inputs.dialog, skill: skillName, stepId: run.stepId })
    const response = await waitForResponse(actionId, conceptName)
    if (response.type === 'cancel') { runStates.delete(conceptName); return }
    if (response.type === 'input_response') providedInputs = response.values as Record<string, unknown>
    run.pendingActionId = null
  }

  let prompt = `Run skill ${skillName}`
  if (providedInputs) {
    const lines = Object.entries(providedInputs).filter(([,v]) => v !== undefined && String(v).trim()).map(([k,v]) => `- ${k}: "${v}"`).join('\n')
    if (lines) prompt += ` with inputs:\n${lines}`
  }

  transitionState(conceptName, 'running', `Running ${skillName}`)
  await promptFn(prompt)
}

export async function dispatchPrompt(conceptName: string, prompt: string, promptFn: (p: string) => Promise<void>) {
  const run: SkillRunState = { runId: randomUUID(), skill: '_freeform', stepId: null, state: 'idle', startedAt: Date.now(), message: '', toolCallCount: 0, currentTool: null, pendingActionId: null }
  runStates.set(conceptName, run)
  transitionState(conceptName, 'running', 'Processing...')
  await promptFn(prompt)
}

// Translate raw omp events into typed SkillActions
export function processAgentEvent(conceptName: string, event: any) {
  const run = runStates.get(conceptName)

  if (event.type === 'message_start' && event.message?.role === 'assistant') {
    if (run && run.state !== 'running') transitionState(conceptName, 'running')
    emitStreamEvent(conceptName, { type: 'agent_message', role: 'assistant', content: '', streaming: true })
    return
  }
  if (event.type === 'message_update') {
    const content = extractText(event.message?.content)
    emitStreamEvent(conceptName, { type: 'agent_message_delta', content })
    return
  }
  if (event.type === 'message_end' && event.message?.role === 'assistant') {
    const content = extractText(event.message?.content)
    emitStreamEvent(conceptName, { type: 'agent_message', role: 'assistant', content, streaming: false })
    // Detect assistant question → emit request_input if frontend connected
    if (run && hasConsumer(conceptName) && isQuestion(content)) {
      const actionId = randomUUID()
      if (run) run.pendingActionId = actionId
      emitAction(conceptName, { type: 'request_input', actionId, fields: [{ id: 'response', label: 'Your answer', type: 'textarea', required: true }], context: content, skill: run?.skill ?? '_freeform', stepId: run?.stepId ?? null })
    }
    return
  }
  if (event.type === 'tool_call') {
    const name = event.name || event.tool?.name || 'unknown'
    if (run) { run.currentTool = name; run.toolCallCount++ }
    emitStreamEvent(conceptName, { type: 'tool_call', name, toolCallCount: run?.toolCallCount ?? 1 })
    return
  }
  if (event.type === 'tool_execution_end') {
    if (run) run.currentTool = null
    emitStreamEvent(conceptName, { type: 'tool_result', name: event.toolName || 'unknown', success: !event.error })
    // File write detection
    const path = event.input?.path || event.input?.file_path || ''
    if (path && (event.toolName === 'write' || event.toolName === 'write_concept')) {
      emitAction(conceptName, { type: 'file_changed', path, action: 'created', skill: run?.skill })
    }
    return
  }
  if (event.type === 'agent_end') {
    transitionState(conceptName, 'completed', 'Done')
    if (run?.stepId && hasConsumer(conceptName)) {
      emitAction(conceptName, { type: 'request_approval', actionId: randomUUID(), skill: run.skill, stepId: run.stepId, artifacts: [], summary: `${run.skill} completed. ${run.toolCallCount} tool calls.` })
    }
    runStates.delete(conceptName)
    return
  }
  if (event.type === 'error') {
    transitionState(conceptName, 'failed', event.error || 'Unknown error')
    runStates.delete(conceptName)
    emitStreamEvent(conceptName, { type: 'error', error: event.error || 'Unknown error' })
    return
  }
}

export function handleConsumerResponse(response: ConsumerResponse): boolean {
  if (response.type === 'cancel') {
    for (const [id, p] of pendingActions) { p.resolve(response); pendingActions.delete(id); return true }
    return false
  }
  const actionId = 'actionId' in response ? response.actionId : null
  if (!actionId) return false
  const p = pendingActions.get(actionId)
  if (!p) return false
  p.resolve(response); pendingActions.delete(actionId); return true
}

function waitForResponse(actionId: string, conceptName: string): Promise<ConsumerResponse> {
  return new Promise(resolve => pendingActions.set(actionId, { resolve, conceptName }))
}

function extractText(content: any): string {
  if (!content) return ''
  if (typeof content === 'string') return content
  if (Array.isArray(content)) return content.map((c: any) => c.text || '').join('')
  return ''
}
function isQuestion(text: string): boolean {
  if (!text || text.length < 10) return false
  const last = text.trim().split('\n').pop()?.trim() || ''
  return last.endsWith('?') && last.length > 15
}
function findStepId(skillName: string): string | null { return null /* implement with your pipeline.json loader */ }
function getInputsForSkill(skillName: string, stepId: string | null) { return null /* implement with your pipeline.json loader */ }
```

### 3. Wire into concept-agent.ts

```typescript
import { processAgentEvent, dispatchSkill, dispatchPrompt } from './skill-adapter'

// In omp event handler — replace raw agentEmitter forwarding with:
omp.on('agent-event', (event) => {
  if (event.type === 'message_start' && event.message?.role === 'assistant') session.isStreaming = true
  if (event.type === 'turn_end' || event.type === 'agent_end') session.isStreaming = false
  processAgentEvent(conceptName, event)
})

// Dispatch function
export async function dispatchSkillViaAdapter(conceptName: string, skillName: string, inputs?: Record<string, unknown>) {
  const session = await getConceptSession(conceptName)
  session.isStreaming = true
  await dispatchSkill(conceptName, skillName, (prompt) => session.process.prompt(prompt), inputs)
}

export async function promptConceptViaAdapter(conceptName: string, message: string) {
  const session = await getConceptSession(conceptName)
  session.isStreaming = true
  await dispatchPrompt(conceptName, message, (prompt) => session.process.prompt(prompt))
}
```

### 4. Create API Endpoints

**`POST /api/agent/dispatch`** — structured skill dispatch:
```typescript
import { isConceptStreaming, dispatchSkillViaAdapter } from '../../utils/concept-agent'

export default defineEventHandler(async (event) => {
  await requireWrite(event)
  const { concept, skill, inputs } = await readBody(event)
  if (!concept || !skill) throw createError({ statusCode: 400 })
  if (isConceptStreaming(concept)) throw createError({ statusCode: 409 })
  dispatchSkillViaAdapter(concept, skill, inputs).catch(console.error)
  return { ok: true, skill }
})
```

**`POST /api/agent/respond`** — consumer responses:
```typescript
import { handleConsumerResponse } from '../../utils/skill-adapter'
import type { ConsumerResponse } from '../../../shared/types/skill-actions'

export default defineEventHandler(async (event) => {
  await requireWrite(event)
  const { concept, response } = await readBody<{ concept: string; response: ConsumerResponse }>(event)
  const handled = handleConsumerResponse(response)
  return { ok: true, handled }
})
```

**`GET /api/agent/actions`** — typed SSE stream:
```typescript
import { adapterEmitter, getRunState, registerConsumer } from '../../utils/skill-adapter'
import { getConceptSession } from '../../utils/concept-agent'

export default defineEventHandler(async (event) => {
  await requireAuth(event)
  const concept = getQuery(event).concept as string
  if (!concept) throw createError({ statusCode: 400 })
  await getConceptSession(concept)
  const unregister = registerConsumer(concept)
  const stream = createEventStream(event)
  stream.push({ data: JSON.stringify({ type: 'connected', runState: getRunState(concept), hasConsumer: true }) })
  const handler = (e: any) => stream.push({ data: JSON.stringify(e) })
  adapterEmitter.on(`action:${concept}`, handler)
  stream.onClosed(() => { adapterEmitter.off(`action:${concept}`, handler); unregister() })
  return stream.send()
})
```

### 5. Frontend Composable (Vue 3 / Nuxt)

```typescript
// app/composables/useSkillActions.ts
export const useSkillActions = (conceptRef: Ref<string> | ComputedRef<string>) => {
  const runState = ref<SkillRunState | null>(null)
  const isRunning = computed(() => runState.value !== null && !['idle','completed','failed'].includes(runState.value.state))
  const pendingInput = ref<RequestInputAction | null>(null)
  const pendingApproval = ref<RequestApprovalAction | null>(null)
  const messages = ref<ChatMessage[]>([])
  const streamingMessage = ref('')

  let eventSource: EventSource | null = null

  function connect() {
    const url = `/api/agent/actions?concept=${encodeURIComponent(toValue(conceptRef))}`
    eventSource = new EventSource(url)
    eventSource.onmessage = (e) => handleStreamEvent(JSON.parse(e.data))
  }

  function handleStreamEvent(event: StreamEvent) {
    switch (event.type) {
      case 'connected': runState.value = event.runState; break
      case 'action': handleAction(event.action); break
      case 'agent_message': if (!event.streaming && event.content) messages.value.push({ role: 'assistant', content: event.content }); streamingMessage.value = ''; break
      case 'agent_message_delta': streamingMessage.value = event.content; break
      case 'tool_call': messages.value.push({ role: 'tool_call', content: '', toolName: event.name }); break
      case 'run_state': runState.value = event.runState; break
      case 'error': messages.value.push({ role: 'assistant', content: `Error: ${event.error}` }); break
    }
  }

  function handleAction(action: SkillAction) {
    if (action.type === 'request_input') pendingInput.value = action
    else if (action.type === 'request_approval') pendingApproval.value = action
    else if (action.type === 'skill_state' && runState.value) runState.value = { ...runState.value, state: action.state }
  }

  async function dispatchSkill(skill: string, inputs?: Record<string, unknown>) {
    messages.value.push({ role: 'user', content: `Run ${skill}` })
    await $fetch('/api/agent/dispatch', { method: 'POST', body: { concept: toValue(conceptRef), skill, inputs } })
  }

  async function submitInput(actionId: string, values: Record<string, unknown>) {
    await $fetch('/api/agent/respond', { method: 'POST', body: { concept: toValue(conceptRef), response: { type: 'input_response', actionId, values } } })
    pendingInput.value = null
  }

  async function submitApproval(actionId: string, approved: boolean, feedback?: string) {
    await $fetch('/api/agent/respond', { method: 'POST', body: { concept: toValue(conceptRef), response: { type: 'approval_response', actionId, approved, feedback } } })
    pendingApproval.value = null
  }

  watch(conceptRef, () => { eventSource?.close(); connect() })
  onBeforeUnmount(() => eventSource?.close())

  return { runState: readonly(runState), isRunning, pendingInput, pendingApproval, messages, streamingMessage, connect, dispatchSkill, submitInput, submitApproval }
}
```

## Gotchas

| Issue | Fix |
|-------|-----|
| Raw events forwarded directly to frontend | Route through `processAgentEvent()` first |
| Skill triggered via string `"Run skill X"` | Use `POST /api/agent/dispatch { skill }` |
| `request_input` never shown if no consumer | Check `hasConsumer()` before emitting input requests |
| `agent_end` fires before `turn_end` | `processAgentEvent` handles both — don't double-complete |
| Consumer disconnects while waiting for input | `registerConsumer` returns unregister fn — call on SSE close |
| Hardcoded input forms in frontend | Derive from `pipeline.json` `user_inputs.dialog` in skill-adapter |
