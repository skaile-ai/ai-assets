# Infrastructure Layer Patterns

Implementation patterns for each infrastructure layer. The skill DSL says
WHAT to build; this file explains HOW.

## Layer 1: Shared Contracts

Type-only packages with no runtime dependencies. Used by multiple modules
for message passing, protocol types, and validation.

```
backend/libs/<name>/src/
├── index.ts              — barrel export
├── schemas.ts            — Zod schemas for message types
├── codec.ts              — encode/decode with validation
└── types.ts              — TypeScript types (inferred from Zod)
```

Extract message types from the architecture doc's protocol specification
sections. Each message type gets a Zod schema and an inferred TypeScript type.

If the frontend also needs these types, create a frontend-only copy at
`frontend/src/lib/<name>.types.ts` with pure TypeScript types (no Zod,
no Node.js dependencies).

## Layer 2: Provider Abstractions

Interface + multiple real implementations + in-memory adapter + NestJS
dynamic module. This is the most common pattern for external integrations.

```
backend/libs/<name>/src/
├── index.ts                        — barrel export
├── <name>.interface.ts             — provider interface
├── <name>.config.ts                — typed config with env var mapping
├── <name>.module.ts                — NestJS dynamic module
├── implementations/
│   ├── <impl>-<name>.service.ts    — real implementation (e.g., claude-ai-provider.service.ts)
│   └── in-memory-<name>.service.ts — deterministic stub for dev/test
└── <name>.tokens.ts                — injection tokens
```

### Provider Interface Template

```typescript
// <name>.interface.ts
export interface <Name>Provider {
  // Extract method signatures from architecture doc
  methodName(params: MethodParams): Promise<MethodResult>
}

export const <NAME>_PROVIDER = Symbol('<Name>Provider')
```

### Dynamic Module Template

```typescript
// <name>.module.ts
import { DynamicModule, Module } from '@nestjs/common'

@Module({})
export class <Name>Module {
  static forRoot(config: <Name>Config): DynamicModule {
    const provider = config.useInMemory
      ? { provide: <NAME>_PROVIDER, useClass: InMemory<Name>Service }
      : { provide: <NAME>_PROVIDER, useFactory: () => new Real<Name>Service(config) }

    return {
      module: <Name>Module,
      providers: [provider],
      exports: [<NAME>_PROVIDER],
      global: true,
    }
  }
}
```

### In-Memory Implementation Rules

- Return deterministic responses (same input → same output)
- Support configurable responses for E2E test scenarios
- Never use `setTimeout` or fake delays
- Implement the full interface — no partial stubs
- Include reasonable default responses that exercise UI states

### Real Implementation Rules

- Gracefully degrade when credentials are missing: log warning, return stub response
- Use environment variables for all configuration (never hardcode)
- Handle rate limits, timeouts, and transient failures
- Wrap SDK errors in domain-specific error types

## Layer 3: Platform Services

NestJS modules that compose multiple providers into higher-level
orchestration. These implement the business logic described in the
architecture doc's data flow sections.

```
backend/libs/<name>/src/
├── index.ts
├── <name>.module.ts      — NestJS module
├── <name>.service.ts     — orchestration service
└── <name>.types.ts       — domain types
```

Inject providers via their tokens:

```typescript
@Injectable()
export class AgentVmManagerService {
  constructor(
    @Inject(AI_PROVIDER) private aiProvider: AiProvider,
    @Inject(GIT_PROVIDER) private gitProvider: GitProvider,
  ) {}
}
```

## Layer 4: Communication Infrastructure

### WebSocket (Fastify)

Register the plugin on the raw Fastify instance (not the NestJS adapter):

```typescript
// main.ts
import websocket from '@fastify/websocket'

const app = await NestFactory.create(ApiModule, new FastifyAdapter())
const fastify = app.getHttpAdapter().getInstance()
await fastify.register(websocket)
```

Create a Fastify route plugin for WebSocket endpoints:

```typescript
// ws-gateway.plugin.ts
export async function wsGatewayPlugin(fastify: FastifyInstance) {
  fastify.get('/ws/agent', { websocket: true }, (socket, req) => {
    // Session management, message routing, heartbeat
  })
}
```

### SSE (NestJS Controller)

```typescript
@Controller('events')
export class SseController {
  @Sse('progress')
  progress(): Observable<MessageEvent> {
    return this.service.progressStream()
  }
}
```

### Frontend Hooks

```typescript
// useAgentWebSocket.ts — auto-reconnect, exponential backoff
// useProgressSSE.ts    — EventSource with reconnect, graceful degradation
```

## Layer 5: Additional Processes

Standalone NestJS or Node.js applications with their own entry point.

```
backend/apps/<name>/
├── src/
│   └── main.ts           — entry point
├── tsconfig.json          — extends backend tsconfig, references shared libs
└── Dockerfile             — production container
```

Docker Compose service:

```yaml
<name>:
  build: ./docker/<name>
  ports:
    - "<port>:<port>"
  environment:
    - DATABASE_URL=${DATABASE_URL}
  depends_on:
    - postgres
```

Dev script in backend/package.json:

```json
"dev:<name>": "ts-node-dev --respawn apps/<name>/src/main.ts"
```
