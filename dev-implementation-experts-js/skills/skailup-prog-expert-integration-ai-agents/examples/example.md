# Examples

Usage examples for `prog-expert-integration-ai-agents`.

## Minimal Integration

```typescript
// 1. Wire concept-agent.ts events through the adapter
omp.on('agent-event', (event) => {
  processAgentEvent(conceptName, event)
})

// 2. Frontend connects to the typed SSE stream
const evtSource = new EventSource('/api/agent/actions?concept=my-concept')

// 3. Dispatch a skill with structured inputs
await $fetch('/api/agent/dispatch', {
  method: 'POST',
  body: { concept: 'my-concept', skill: 'cf_overview', inputs: { app_name: 'MyApp' } },
})
```
