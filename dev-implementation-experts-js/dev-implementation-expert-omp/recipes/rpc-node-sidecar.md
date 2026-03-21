---
name: RPC Node.js Sidecar
description: Embed omp as an RPC sidecar in a Node.js/Nitro/Express application
libraries_used: node:child_process, node:readline, node:events
---

# RPC Node.js Sidecar

## Objective
Spawn and manage an omp child process for full coding-agent capabilities in a Node.js host app.

## Prerequisites
- `omp` installed globally via `bun install -g @oh-my-pi/pi-coding-agent`
- Node.js 18+

## Instructions

### 1. Create OmpProcess class

```typescript
import { spawn, type ChildProcess } from 'node:child_process';
import { EventEmitter } from 'node:events';
import { createInterface, type Interface } from 'node:readline';

export class OmpProcess extends EventEmitter {
  private proc: ChildProcess | null = null;
  private rl: Interface | null = null;
  private reqId = 0;
  private cwd: string;
  private env: Record<string, string>;
  private args: string[];
  private ready = false;
  private startPromise: Promise<void> | null = null;

  constructor(cwd: string, env?: Record<string, string>, args?: string[]) {
    super();
    this.cwd = cwd;
    this.env = env ?? (process.env as Record<string, string>);
    this.args = args ?? ['--mode', 'rpc', '--no-session'];
  }

  async start(): Promise<void> {
    if (this.proc) return;
    if (this.startPromise) return this.startPromise;
    this.startPromise = this._start();
    return this.startPromise;
  }

  private async _start(): Promise<void> {
    return new Promise<void>((resolve, reject) => {
      this.proc = spawn('omp', this.args, {
        cwd: this.cwd,
        env: this.env,
        stdio: ['pipe', 'pipe', 'pipe'],
      });

      this.proc.on('error', (err) => {
        this.emit('error', { type: 'error', error: err.message });
        if (!this.ready) reject(err);
      });

      this.proc.on('exit', (code, signal) => {
        this.cleanup();
        this.emit('exit', { code, signal });
      });

      // Parse JSON events from stdout
      if (this.proc.stdout) {
        this.rl = createInterface({ input: this.proc.stdout });
        this.rl.on('line', (line) => {
          if (!line.trim()) return;
          try {
            const event = JSON.parse(line);
            if (event.type === 'ready') {
              this.emit('ready');
            } else if (event.type === 'response') {
              this.emit(`response:${event.id}`, event);
            } else {
              this.emit('agent-event', event);
            }
          } catch {
            // Non-JSON output
          }
        });
      }

      // Log stderr
      if (this.proc.stderr) {
        createInterface({ input: this.proc.stderr }).on('line', (line) => {
          console.error('[omp stderr]', line);
        });
      }

      // Wait for ready event or timeout
      const timeout = setTimeout(() => {
        if (!this.ready) { this.ready = true; resolve(); }
      }, 3000);

      this.once('ready', () => {
        clearTimeout(timeout);
        if (!this.ready) { this.ready = true; resolve(); }
      });
    });
  }

  prompt(message: string) {
    const id = `req-${++this.reqId}`;
    this.proc?.stdin?.write(JSON.stringify({ id, type: 'prompt', message }) + '\n');
  }

  abort() {
    if (!this.proc) return;
    this.proc.stdin?.write(JSON.stringify({ id: `req-${++this.reqId}`, type: 'abort' }) + '\n');
  }

  get isRunning() { return this.proc !== null && !this.proc.killed; }

  kill() {
    this.proc?.kill('SIGTERM');
    this.cleanup();
  }

  private cleanup() {
    this.rl?.close();
    this.rl = null;
    this.proc = null;
    this.ready = false;
    this.startPromise = null;
  }
}
```

### 2. Build CLI args with model/provider

```typescript
const args: string[] = ['--mode', 'rpc', '--no-session'];
if (model) {
  // Format: provider/model — e.g. "openrouter/anthropic/claude-sonnet-4"
  const modelArg = provider ? `${provider}/${model}` : model;
  args.push('--model', modelArg);
}
```

### 3. Inject API keys via environment

```typescript
const env: Record<string, string> = { ...process.env };
for (const [provider, key] of Object.entries(apiKeys)) {
  env[`${provider.toUpperCase().replace(/[^A-Z0-9]/g, '_')}_API_KEY`] = key;
}
const omp = new OmpProcess(workspacePath, env, args);
```

### 4. Forward events to SSE

```typescript
omp.on('agent-event', (event) => {
  sseStream.push({ data: JSON.stringify(event) });
});
```

## Key Gotchas
- Model format: `provider/model` NOT `p-provider/model`
- `message_end` fires for user AND assistant — don't double-persist
- `turn_end` always fires — use it to reset streaming state
- Process exits on invalid model — handle `exit` event gracefully
