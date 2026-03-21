---
name: RPC to SSE Bridge
description: Bridge omp RPC events to Server-Sent Events for web frontends
libraries_used: node:events, nitro (or express)
---

# RPC to SSE Bridge

## Objective
Stream omp agent events to a web frontend via Server-Sent Events.

## Prerequisites
- OmpProcess class (see `rpc-node-sidecar` recipe)
- Nitro/Nuxt server or Express

## Instructions

### 1. Session Manager Pattern

```typescript
import { EventEmitter } from 'events';

class AgentManager extends EventEmitter {
  private sessions = new Map<string, OmpSession>();

  async getSession(slug: string): Promise<OmpSession> {
    let session = this.sessions.get(slug);
    if (session?.process.isRunning) return session;
    return this._createSession(slug);
  }

  private async _createSession(slug: string) {
    const omp = new OmpProcess(workspacePath, env, args);
    const session = { process: omp, messages: [], isStreaming: false };

    omp.on('agent-event', (event) => {
      // Track state
      if (event.type === 'message_start' && event.message?.role === 'assistant')
        session.isStreaming = true;
      if (event.type === 'turn_end' || event.type === 'agent_end')
        session.isStreaming = false;

      // Persist
      if (event.type === 'message_end' && event.message)
        session.messages.push(event.message);
      if (event.type === 'turn_end' && event.toolResults)
        event.toolResults.forEach(r => session.messages.push(r));

      // Forward to SSE listeners
      this.emit(`event:${slug}`, event);
    });

    await omp.start();
    this.sessions.set(slug, session);
    return session;
  }
}
```

### 2. Nitro SSE Endpoint

```typescript
// server/api/projects/[slug]/stream.get.ts
export default defineEventHandler(async (event) => {
  const slug = getRouterParam(event, 'slug')!;
  await agentManager.getSession(slug);

  const eventStream = createEventStream(event);
  const listener = (agentEvent: any) => {
    eventStream.push({ data: JSON.stringify(agentEvent) });
  };

  agentManager.on(`event:${slug}`, listener);
  eventStream.onClosed(() => agentManager.off(`event:${slug}`, listener));
  return eventStream.send();
});
```

### 3. Prompt Endpoint

```typescript
// server/api/projects/[slug]/prompt.post.ts
export default defineEventHandler(async (event) => {
  const slug = getRouterParam(event, 'slug')!;
  const { prompt } = await readBody(event);

  // Fire-and-forget — events flow through SSE
  agentManager.prompt(slug, prompt).catch((e) => {
    agentManager.emit(`event:${slug}`, { type: 'error', error: e.message });
  });

  return { success: true };
});
```

### 4. Frontend EventSource

```typescript
const eventSource = new EventSource(`/api/projects/${slug}/stream`);
eventSource.onmessage = (e) => {
  const event = JSON.parse(e.data);

  if (event.type === 'message_start' || event.type === 'message_update') {
    streamMessage.value = event.message; // .message has accumulated content
  } else if (event.type === 'message_end') {
    messages.value.push(event.message);
    streamMessage.value = null;
  } else if (event.type === 'turn_end') {
    event.toolResults?.forEach(r => messages.value.push(r));
  } else if (event.type === 'error') {
    // Show error to user
  }
};
```

## Important
- Don't persist user messages in `prompt()` — omp's `message_end` handles both roles
- Use `turn_end` to reset loading spinners (fires even on errors)
- `message_update.message` contains the full accumulated content (not just the delta)
