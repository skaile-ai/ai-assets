# OMP RPC Protocol Reference

## Overview

omp's RPC mode (`--mode rpc`) exposes a JSON-lines protocol over stdin/stdout for embedding the full coding agent in any host application.

```
Host App ──stdin──▶ omp --mode rpc ──stdout──▶ Host App
         (commands)                  (events)
```

## Starting the Process

```bash
omp --mode rpc --no-session [--model provider/model] [flags...]
```

The process emits a **ready** event on stdout when initialized:
```json
{"type":"ready"}
```

**CRITICAL**: Wait for the `ready` event before sending any commands.

## Commands (stdin → omp)

All commands are single-line JSON objects terminated by `\n`.

### Prompt
```json
{"id":"req-1","type":"prompt","message":"Create a TODO app"}
```

### Abort
```json
{"id":"req-2","type":"abort"}
```

## Responses (omp → stdout)

### Command Acknowledgement
Sent immediately when a command is received:
```json
{"id":"req-1","type":"response","command":"prompt","success":true}
```

## Agent Events (omp → stdout)

Events are top-level JSON objects — **NOT** wrapped in `{"type":"event","data":...}`.

### Event Lifecycle

```
agent_start
  └─ turn_start
       ├─ message_start (user)
       ├─ message_end (user)
       ├─ message_start (assistant)
       ├─ message_update (thinking_start)
       ├─ message_update (thinking_delta) ×N
       ├─ message_update (thinking_end)
       ├─ message_update (text_start)
       ├─ message_update (text_delta) ×N
       ├─ message_update (text_end)
       ├─ message_end (assistant)
       └─ turn_end
  └─ [additional turns for tool use...]
agent_end
```

### Event Types

#### `agent_start`
```json
{"type":"agent_start"}
```

#### `turn_start`
```json
{"type":"turn_start"}
```

#### `message_start`
Fired for **both** user and assistant messages.
```json
{
  "type": "message_start",
  "message": {
    "role": "user",
    "content": [{"type":"text","text":"Hello"}],
    "attribution": "user",
    "timestamp": 1773261580273
  }
}
```

Assistant messages start with empty content:
```json
{
  "type": "message_start",
  "message": {
    "role": "assistant",
    "content": [],
    "api": "openai-completions",
    "provider": "openrouter",
    "model": "stepfun/step-3.5-flash:free",
    "usage": {"input":0,"output":0,"cacheRead":0,"cacheWrite":0,"totalTokens":0,"cost":{"input":0,"output":0,"cacheRead":0,"cacheWrite":0,"total":0}},
    "stopReason": "stop",
    "timestamp": 1773261580311
  }
}
```

#### `message_update`
Streaming content deltas. Contains both the delta event and the full accumulated message.

```json
{
  "type": "message_update",
  "assistantMessageEvent": {
    "type": "text_delta",
    "contentIndex": 1,
    "delta": "Hello!",
    "partial": { "role": "assistant", "content": [...] }
  },
  "message": {
    "role": "assistant",
    "content": [
      {"type":"thinking","thinking":"...accumulated...","thinkingSignature":"reasoning"},
      {"type":"text","text":"Hello!"}
    ],
    "api": "openai-completions",
    "provider": "openrouter",
    "model": "...",
    "usage": {...},
    "stopReason": "stop",
    "timestamp": 1773261580311
  }
}
```

**`assistantMessageEvent.type` values:**
- `thinking_start` — thinking block begins
- `thinking_delta` — thinking content chunk
- `thinking_end` — thinking block complete
- `text_start` — text block begins
- `text_delta` — text content chunk
- `text_end` — text block complete

#### `message_end`
Fired for **both** user and assistant messages. Contains the final message.

```json
{
  "type": "message_end",
  "message": {
    "role": "assistant",
    "content": [
      {"type":"thinking","thinking":"full thinking text","thinkingSignature":"reasoning"},
      {"type":"text","text":"Hello! How can I help?"}
    ],
    "api": "openai-completions",
    "provider": "openrouter",
    "model": "stepfun/step-3.5-flash:free",
    "usage": {"input":123,"output":45,...},
    "stopReason": "stop",
    "timestamp": 1773261580311,
    "duration": 2500
  }
}
```

**GOTCHA**: `message_end` fires for user messages too! Don't persist user messages separately in `prompt()` — rely on `message_end` to avoid duplicates.

#### `turn_end`
Marks the end of an agent turn. Always fires, even on errors.

```json
{
  "type": "turn_end",
  "message": { "role": "assistant", "content": [...], ... },
  "toolResults": [
    {
      "role": "toolResult",
      "toolCallId": "call_abc123",
      "toolName": "bash",
      "content": [{"type":"text","text":"output here"}]
    }
  ]
}
```

Use `turn_end` to reset streaming/loading state.

#### `agent_end`
Contains the full message array for the entire conversation.

```json
{
  "type": "agent_end",
  "messages": [
    {"role":"user","content":[...],...},
    {"role":"assistant","content":[...],...}
  ]
}
```

**GOTCHA**: `agent_end` may fire BEFORE the final `message_end` and `turn_end`.

### Error Messages
When the model returns an error, the assistant message includes:
```json
{
  "role": "assistant",
  "content": [],
  "stopReason": "error",
  "errorMessage": "400 model does not support tools\nraw-http-request=...",
  "duration": 192
}
```

## Message Content Format

Content is always an array of typed blocks:

```typescript
type ContentBlock =
  | { type: "text"; text: string }
  | { type: "thinking"; thinking: string; thinkingSignature: "reasoning" }
  | { type: "toolCall"; id: string; name: string; arguments: Record<string, any> }
```

## Node.js Integration Pattern

```typescript
import { spawn } from 'node:child_process';
import { createInterface } from 'node:readline';
import { EventEmitter } from 'node:events';

class OmpProcess extends EventEmitter {
  private proc;
  private rl;
  private reqId = 0;

  constructor(cwd: string, env: Record<string,string>, args: string[]) {
    super();
    this.proc = spawn('omp', args, { cwd, env, stdio: ['pipe','pipe','pipe'] });

    this.rl = createInterface({ input: this.proc.stdout });
    this.rl.on('line', (line) => {
      const event = JSON.parse(line);
      if (event.type === 'ready') this.emit('ready');
      else if (event.type === 'response') this.emit(`response:${event.id}`, event);
      else this.emit('agent-event', event);
    });
  }

  prompt(message: string) {
    const id = `req-${++this.reqId}`;
    this.proc.stdin.write(JSON.stringify({ id, type: 'prompt', message }) + '\n');
  }

  abort() {
    const id = `req-${++this.reqId}`;
    this.proc.stdin.write(JSON.stringify({ id, type: 'abort' }) + '\n');
  }
}
```
