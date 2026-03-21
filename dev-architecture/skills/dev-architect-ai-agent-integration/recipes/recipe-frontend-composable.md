---
name: recipe-frontend-composable
description: Full implementation of useSkillActions composable — SSE connection management, typed action routing, reactive state, dispatchSkill/sendPrompt/submitInput/submitApproval methods.
libraries_used: [vue3, nuxt4, typescript]
---

# Recipe: `useSkillActions` Composable

Manages the `/api/agent/actions` SSE connection and provides reactive state + action methods.

## `app/composables/useSkillActions.ts`

```typescript
import { ref, computed, watch, onUnmounted } from 'vue'
import type { Ref } from 'vue'
import type {
  StreamEvent,
  SkillRunState,
  SkillAction,
  ConsumerResponse,
  DialogField,
} from '~/shared/types/skill-actions'

// ---- Types ----
export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
  timestamp: number
}

export interface Notification {
  id: string
  level: 'info' | 'warn' | 'error'
  message: string
  timestamp: number
}

export type RequestInputAction = Extract<SkillAction, { type: 'request_input' }>
export type RequestApprovalAction = Extract<SkillAction, { type: 'request_approval' }>

// ---- Composable ----
export function useSkillActions(conceptRef: Ref<string | null | undefined>) {
  // SSE connection
  let eventSource: EventSource | null = null

  // Reactive state
  const runState = ref<SkillRunState | null>(null)
  const messages = ref<ChatMessage[]>([])
  const notifications = ref<Notification[]>([])
  const changedFiles = ref<string[]>([])
  const pendingInput = ref<RequestInputAction | null>(null)
  const pendingApproval = ref<RequestApprovalAction | null>(null)
  const isConnected = ref(false)
  const error = ref<string | null>(null)

  // Derived
  const isRunning = computed(() => {
    const s = runState.value?.state
    return s === 'running' || s === 'collecting_inputs'
  })
  const activeSkill = computed(() => runState.value?.skillName ?? null)
  const currentTool = computed(() => runState.value?.currentTool ?? null)
  const needsAttention = computed(() =>
    pendingInput.value !== null || pendingApproval.value !== null
  )

  // Streaming message accumulator
  let streamingMsgId: string | null = null

  // ── Connection ──────────────────────────────────────────────────────────
  function connect() {
    const concept = conceptRef.value
    if (!concept || eventSource) return

    eventSource = new EventSource(`/api/agent/actions?concept=${encodeURIComponent(concept)}`)
    isConnected.value = false

    eventSource.onmessage = (e) => {
      try {
        const event: StreamEvent = JSON.parse(e.data)
        handleStreamEvent(event)
      } catch {
        console.warn('[useSkillActions] Failed to parse event:', e.data)
      }
    }

    eventSource.onerror = () => {
      isConnected.value = false
      error.value = 'SSE connection lost'
      // Reconnect after delay
      setTimeout(() => {
        if (conceptRef.value) {
          disconnect()
          connect()
        }
      }, 3000)
    }
  }

  function disconnect() {
    eventSource?.close()
    eventSource = null
    isConnected.value = false
  }

  // ── Event Routing ───────────────────────────────────────────────────────
  function handleStreamEvent(event: StreamEvent) {
    switch (event.type) {
      case 'connected':
        isConnected.value = true
        error.value = null
        runState.value = event.runState
        break

      case 'run_state':
        runState.value = event.runState
        // Clear pending actions when skill completes/fails
        if (event.runState.state === 'completed' || event.runState.state === 'failed' || event.runState.state === 'idle') {
          pendingInput.value = null
          pendingApproval.value = null
        }
        break

      case 'action':
        handleSkillAction(event.action)
        break

      case 'agent_message':
        if (!event.streaming) {
          // Complete message — update or add
          const existing = messages.value.find(m => m.id === streamingMsgId)
          if (existing) {
            existing.content = event.content
            existing.streaming = false
          } else {
            messages.value.push({
              id: streamingMsgId ?? Date.now().toString(),
              role: 'assistant',
              content: event.content,
              streaming: false,
              timestamp: Date.now(),
            })
          }
          streamingMsgId = null
        } else {
          // Start streaming
          streamingMsgId = Date.now().toString()
          messages.value.push({
            id: streamingMsgId,
            role: 'assistant',
            content: '',
            streaming: true,
            timestamp: Date.now(),
          })
        }
        break

      case 'agent_message_delta': {
        const msg = streamingMsgId ? messages.value.find(m => m.id === streamingMsgId) : null
        if (msg) msg.content += event.content
        break
      }

      case 'tool_call':
        if (runState.value) {
          runState.value = { ...runState.value, currentTool: event.name, toolCallCount: event.toolCallCount }
        }
        break

      case 'error':
        error.value = event.error
        break
    }
  }

  function handleSkillAction(action: SkillAction) {
    switch (action.type) {
      case 'request_input':
        pendingInput.value = action
        break

      case 'request_approval':
        pendingApproval.value = action
        break

      case 'notify':
        notifications.value.push({
          id: Date.now().toString(),
          level: action.level,
          message: action.message,
          timestamp: Date.now(),
        })
        break

      case 'file_changed':
        if (!changedFiles.value.includes(action.path)) {
          changedFiles.value.push(action.path)
        }
        break

      case 'skill_state':
        // Already handled via run_state events
        break
    }
  }

  // ── Actions ─────────────────────────────────────────────────────────────
  async function dispatchSkill(skill: string, inputs?: Record<string, unknown>): Promise<void> {
    const concept = conceptRef.value
    if (!concept) return

    // Optimistically add user message
    messages.value.push({
      id: Date.now().toString(),
      role: 'user',
      content: inputs ? `Running ${skill}...` : `Run ${skill}`,
      timestamp: Date.now(),
    })

    await $fetch('/api/agent/dispatch', {
      method: 'POST',
      body: { concept, skill, inputs },
    })
  }

  async function sendPrompt(prompt: string): Promise<void> {
    const concept = conceptRef.value
    if (!concept) return

    messages.value.push({
      id: Date.now().toString(),
      role: 'user',
      content: prompt,
      timestamp: Date.now(),
    })

    await $fetch('/api/agent/prompt', {
      method: 'POST',
      body: { concept, prompt },
    })
  }

  async function submitInput(actionId: string, values: Record<string, unknown>): Promise<void> {
    const concept = conceptRef.value
    if (!concept) return

    pendingInput.value = null

    const response: ConsumerResponse = { type: 'input_response', actionId, values }
    await $fetch('/api/agent/respond', {
      method: 'POST',
      body: { concept, response },
    })
  }

  async function submitApproval(actionId: string, approved: boolean, feedback?: string): Promise<void> {
    const concept = conceptRef.value
    if (!concept) return

    pendingApproval.value = null

    const response: ConsumerResponse = { type: 'approval_response', actionId, approved, feedback }
    await $fetch('/api/agent/respond', {
      method: 'POST',
      body: { concept, response },
    })
  }

  async function cancel(): Promise<void> {
    const concept = conceptRef.value
    if (!concept) return

    pendingInput.value = null
    pendingApproval.value = null

    const response: ConsumerResponse = { type: 'cancel' }
    await $fetch('/api/agent/respond', {
      method: 'POST',
      body: { concept, response },
    }).catch(() => {})
  }

  async function abort(): Promise<void> {
    const concept = conceptRef.value
    if (!concept) return

    await $fetch(`/api/agent/abort`, {
      method: 'POST',
      body: { concept },
    }).catch(() => {})
  }

  function clearChat() {
    messages.value = []
    notifications.value = []
    changedFiles.value = []
    error.value = null
  }

  // ── Lifecycle ───────────────────────────────────────────────────────────
  watch(
    conceptRef,
    (newConcept, oldConcept) => {
      if (oldConcept !== newConcept) {
        disconnect()
        if (newConcept) connect()
      }
    },
    { immediate: true }
  )

  onUnmounted(() => {
    disconnect()
  })

  return {
    // State
    runState: runState as Ref<SkillRunState | null>,
    isRunning,
    isConnected,
    activeSkill,
    currentTool,
    needsAttention,
    pendingInput,
    pendingApproval,
    messages,
    notifications,
    changedFiles,
    error,

    // Actions
    dispatchSkill,
    sendPrompt,
    submitInput,
    submitApproval,
    cancel,
    abort,
    clearChat,
    connect,
    disconnect,
  }
}
```

## Integration with `useAgentStatus` (global header badge)

```typescript
// app/composables/useAgentStatus.ts (additions)
import type { SkillState } from '~/shared/types/skill-actions'

export const useAgentStatus = () => {
  const isStreaming = useState<boolean>('agent-streaming', () => false)
  const skillState = useState<SkillState | null>('agent-skill-state', () => null)
  const activeSkillName = useState<string | null>('agent-active-skill', () => null)
  const needsAttention = computed(() =>
    skillState.value === 'collecting_inputs' || skillState.value === 'awaiting_approval'
  )

  // Called by useSkillActions on run_state events
  function setSkillState(state: SkillState | null, skillName: string | null) {
    skillState.value = state
    activeSkillName.value = skillName
    isStreaming.value = state === 'running' || state === 'collecting_inputs'
  }

  return { isStreaming, skillState, activeSkillName, needsAttention, setSkillState }
}
```

In `useSkillActions`, sync to global status:
```typescript
const agentStatus = useAgentStatus()

// In handleStreamEvent, 'run_state' case:
case 'run_state':
  runState.value = event.runState
  agentStatus.setSkillState(event.runState.state, event.runState.skillName)
  break
```

## Gotchas
- Always disconnect on `onUnmounted` to prevent memory leaks and stale consumer counts
- SSE reconnect: back off 3s, not immediately — avoid reconnect storms
- `agent_message_delta` accumulates content in the streaming message; set `streaming: false` on `agent_message` with `streaming: false` (the final event)
- `submitInput` / `submitApproval` must clear `pendingInput` / `pendingApproval` immediately (optimistic) to prevent duplicate form rendering
