---
name: recipe-dispatch-endpoint
description: Three Nitro API endpoints for the Typed Action Protocol — POST /api/agent/dispatch, POST /api/agent/respond, GET /api/agent/actions (typed SSE stream).
libraries_used: [nuxt4/nitro, h3]
---

# Recipe: API Endpoints (Dispatch / Respond / Actions)

Three endpoints replace the old raw event bridge.

## 1. `POST /api/agent/dispatch` (`server/api/agent/dispatch.post.ts`)

Structured skill dispatch — replaces regex string matching.

```typescript
// server/api/agent/dispatch.post.ts
import { defineEventHandler, readBody } from 'h3'
import { requireAuth } from '../utils/auth'
import { dispatchSkillViaAdapter } from '../utils/concept-agent'

export default defineEventHandler(async (event) => {
  await requireAuth(event)

  const body = await readBody<{
    concept: string
    skill: string
    inputs?: Record<string, unknown>
  }>(event)

  if (!body.concept || !body.skill) {
    throw createError({ statusCode: 400, message: 'concept and skill are required' })
  }

  // Fire-and-forget — skill runs asynchronously, events flow via SSE
  dispatchSkillViaAdapter(body.concept, body.skill, body.inputs).catch((e) => {
    console.error(`[dispatch] Failed to run skill ${body.skill}:`, e)
  })

  return { ok: true, skill: body.skill, concept: body.concept }
})
```

## 2. `POST /api/agent/respond` (`server/api/agent/respond.post.ts`)

Consumer responses — input_response, approval_response, cancel.

```typescript
// server/api/agent/respond.post.ts
import { defineEventHandler, readBody } from 'h3'
import { requireAuth } from '../utils/auth'
import { handleConsumerResponse } from '../utils/skill-adapter'
import type { ConsumerResponse } from '~/shared/types/skill-actions'

export default defineEventHandler(async (event) => {
  await requireAuth(event)

  const body = await readBody<{
    concept: string
    response: ConsumerResponse
  }>(event)

  if (!body.concept || !body.response) {
    throw createError({ statusCode: 400, message: 'concept and response are required' })
  }

  const handled = handleConsumerResponse(body.response)

  if (!handled) {
    // Action may have timed out or already been handled
    console.warn(`[respond] No pending action for response type ${body.response.type}`)
  }

  return { ok: true, handled }
})
```

## 3. `GET /api/agent/actions` (`server/api/agent/actions.get.ts`)

Typed SSE stream — replaces the raw `/api/agent/stream`.

```typescript
// server/api/agent/actions.get.ts
import { defineEventHandler, getQuery } from 'h3'
import { createEventStream } from 'h3'
import { requireAuth } from '../utils/auth'
import { adapterEmitter, registerConsumer, getRunState } from '../utils/skill-adapter'
import type { StreamEvent } from '~/shared/types/skill-actions'

export default defineEventHandler(async (event) => {
  await requireAuth(event)

  const query = getQuery(event)
  const concept = query.concept as string | undefined

  if (!concept) {
    throw createError({ statusCode: 400, message: 'concept query param required' })
  }

  const stream = createEventStream(event)

  // Register consumer presence
  const unregisterConsumer = registerConsumer(concept)

  // Send initial connected event with current state
  const runState = getRunState(concept)
  const connectedEvent: StreamEvent = {
    type: 'connected',
    runState,
    hasConsumer: true,
  }
  stream.push({ data: JSON.stringify(connectedEvent) }).catch(() => {})

  // Forward typed events to SSE
  const handler = (streamEvent: StreamEvent) => {
    stream.push({ data: JSON.stringify(streamEvent) }).catch(() => {})
  }

  adapterEmitter.on(`action:${concept}`, handler)

  // Cleanup on disconnect
  stream.onClosed(() => {
    adapterEmitter.off(`action:${concept}`, handler)
    unregisterConsumer()
  })

  return stream.send()
})
```

## 4. Updated `POST /api/agent/prompt` (free-form only)

Keep this endpoint for direct free-form chat (not skill dispatch):

```typescript
// server/api/agent/prompt.post.ts
import { defineEventHandler, readBody } from 'h3'
import { requireAuth } from '../utils/auth'
import { promptConceptViaAdapter } from '../utils/concept-agent'

export default defineEventHandler(async (event) => {
  await requireAuth(event)

  const body = await readBody<{ concept: string; prompt: string }>(event)

  if (!body.concept || !body.prompt) {
    throw createError({ statusCode: 400, message: 'concept and prompt are required' })
  }

  // Fire-and-forget
  promptConceptViaAdapter(body.concept, body.prompt).catch((e) => {
    console.error('[prompt] Error:', e)
  })

  return { ok: true }
})
```

## Migration: Remove Old Endpoints

Delete `server/api/agent/stream.get.ts` — it forwards raw omp events without typing.

Frontend migration:
```typescript
// Before (raw stream)
const evtSource = new EventSource('/api/agent/stream?concept=X')
evtSource.onmessage = (e) => { /* parse raw omp events */ }

// After (typed actions)
const evtSource = new EventSource('/api/agent/actions?concept=X')
evtSource.onmessage = (e) => {
  const event: StreamEvent = JSON.parse(e.data)
  // Handle typed event
}
```
