# Backend Implementation Patterns

Where to put custom backend logic in a PostXL project, how to test it,
and how to integrate external services.

## PostXL Backend Architecture

The generated backend follows a layered dispatch architecture:

```
tRPC Route → Dispatcher → UpdateService → Repository → Database/InMemory
                             ↑                ↑
                       custom logic      custom queries
```

Read operations bypass the dispatcher:

```
tRPC Route → ViewService → Repository → Database/InMemory
                  ↑
           custom transforms
```

All layers run identically in both stateless (in-memory) and stateful (Postgres)
modes. Custom backend logic works in both — no special setup needed for testing.

## Where Custom Logic Goes

### 1. Custom tRPC procedures

Add new endpoints to existing route files using `@custom-start` blocks:

```typescript
// backend/libs/router-trpc/src/routes/cloudConnection.route.ts

export const cloudConnections = router({
  // ... generated CRUD routes ...

  // @custom-start:customRoutes
  validateCredentials: procedure
    .use(authMiddleware)
    .input(z.object({ connectionId: z.string() }))
    .mutation(({ input, ctx }) =>
      ctx.dispatch({
        scope: 'cloudConnection',
        type: 'validateCredentials',
        payload: input,
      })
    ),
  // @custom-end:customRoutes
})
```

### 2. Custom business logic (mutations)

Extend the UpdateService's dispatch method and add private implementation methods:

```typescript
// backend/libs/update/src/cloudConnection.update.service.ts

public async dispatch<A extends Action_CloudConnection>({
  action,
  execution,
}): Promise<ActionResult_CloudConnection[A['type']]> {
  switch (action.type) {
    case 'create':
      return this.create({ data: action.payload, execution })
    // ... generated cases ...

    // @custom-start:dispatchCases
    case 'validateCredentials':
      return this.validateCredentials({ data: action.payload, execution })
    // @custom-end:dispatchCases
  }
}

// @custom-start:customMethods
private async validateCredentials({ data, execution }): Promise<CloudConnectionViewModel> {
  this.authorizeWriteOperation({ execution, operation: 'update' })

  const connection = await this.data.get({ id: data.connectionId, execution })
  if (!connection) throw new Error('Connection not found')

  // Call external adapter (mockable via DI)
  const isValid = await this.cloudAdapter.testConnection(connection)

  const updated = await this.data.update({
    item: { id: connection.id, status: isValid ? 'Active' : 'Failed' },
    execution,
  })

  return this.viewService.cloudConnections.toViewModel(updated)
}
// @custom-end:customMethods
```

### 3. Custom data transformations (queries)

Override `toViewModel()` in ViewService to add computed fields or load relations:

```typescript
// backend/libs/view/src/cloudConnection.view.service.ts

// @custom-start:toViewModel
public async toViewModel(item: CloudConnection): Promise<CloudConnectionViewModel> {
  const base = await super.toViewModel(item)
  return {
    ...base,
    regionCount: item.regions ? JSON.parse(item.regions).length : 0,
    lastValidatedAgo: item.lastValidatedAt
      ? formatDistanceToNow(item.lastValidatedAt)
      : null,
  }
}
// @custom-end:toViewModel
```

### 4. Custom authorization rules

Override `evaluateCustomPolicy()` for row-level authorization:

```typescript
// backend/libs/actions/src/authorization/authorization-policy.service.ts

// @custom-start:authorizationPolicyRowRules
protected evaluateCustomPolicy(params: PolicyContext): boolean | undefined {
  // Only workspace members can access workspace resources
  if (params.scope === 'app' && params.operation === 'read') {
    return params.viewer.workspaceIds.includes(params.item.workspaceId)
  }
  return undefined // fall through to default policy
}
// @custom-end:authorizationPolicyRowRules
```

### 5. Custom validation/decoding

Fill decoder stubs for JSON fields or complex validation:

```typescript
// backend/libs/decoders/src/cloudConnection.model.decoder.ts

// @custom-start:decodeCredentialsJson
function decodeCredentialsJson(value: unknown): AwsCredentials | AzureCredentials {
  const parsed = JSON.parse(String(value))
  if (parsed.provider === 'aws') {
    return AwsCredentialsSchema.parse(parsed)
  }
  return AzureCredentialsSchema.parse(parsed)
}
// @custom-end:decodeCredentialsJson
```

## External Service Adapter Pattern

For features that integrate with external services (cloud providers, git platforms,
AI models, email, etc.), use the adapter pattern so business logic is testable
without real external calls.

### Define the adapter interface

```typescript
// backend/libs/cloud-adapter/src/cloud-adapter.interface.ts

export interface CloudAdapter {
  testConnection(credentials: CloudCredentials): Promise<boolean>
  listRegions(provider: string): Promise<Region[]>
  provisionResource(config: ProvisionConfig): Promise<ProvisionResult>
}
```

### Implement the real adapter

```typescript
// backend/libs/cloud-adapter/src/aws-cloud.adapter.ts

@Injectable()
export class AwsCloudAdapter implements CloudAdapter {
  async testConnection(credentials: CloudCredentials): Promise<boolean> {
    const sts = new STSClient({ credentials: { ... } })
    try {
      await sts.send(new GetCallerIdentityCommand({}))
      return true
    } catch {
      return false
    }
  }
}
```

### Implement the in-memory adapter (for dev/test)

```typescript
// backend/libs/cloud-adapter/src/in-memory-cloud.adapter.ts

@Injectable()
export class InMemoryCloudAdapter implements CloudAdapter {
  // Configurable responses for testing different scenarios
  private responses = new Map<string, boolean>()

  setTestResponse(connectionId: string, result: boolean): void {
    this.responses.set(connectionId, result)
  }

  async testConnection(credentials: CloudCredentials): Promise<boolean> {
    // Return configured response, or true by default
    return this.responses.get(credentials.id) ?? true
  }

  async listRegions(): Promise<Region[]> {
    return [
      { id: 'us-east-1', name: 'US East (N. Virginia)' },
      { id: 'eu-west-1', name: 'Europe (Ireland)' },
    ]
  }
}
```

### Wire via DI (same pattern as repositories)

```typescript
// backend/libs/cloud-adapter/src/cloud-adapter.module.ts

@Module({})
export class CloudAdapterModule {
  static forRoot({ useInMemory }: { useInMemory: boolean }): DynamicModule {
    return {
      module: CloudAdapterModule,
      providers: [{
        provide: CloudAdapter,
        useClass: useInMemory ? InMemoryCloudAdapter : AwsCloudAdapter,
      }],
      exports: [CloudAdapter],
    }
  }
}
```

The in-memory adapter runs automatically in stateless mode. E2E tests exercise the
real business logic through the full tRPC → Dispatcher → UpdateService → Adapter
chain — only the final external call is swapped.

## Backend Testing Strategy

### Unit tests for custom services (Jest)

Test business logic in isolation. Mock repositories and adapters.

```typescript
// backend/libs/update/src/cloudConnection.update.service.spec.ts

import { Test } from '@nestjs/testing'
import { CloudConnectionUpdateService } from './cloudConnection.update.service'

describe('CloudConnectionUpdateService', () => {
  let service: CloudConnectionUpdateService
  let mockRepo: jest.Mocked<CloudConnectionRepository>
  let mockAdapter: jest.Mocked<CloudAdapter>

  beforeEach(async () => {
    mockRepo = {
      get: jest.fn(),
      update: jest.fn(),
    } as any
    mockAdapter = {
      testConnection: jest.fn(),
    } as any

    const module = await Test.createTestingModule({
      providers: [
        CloudConnectionUpdateService,
        { provide: CloudConnectionRepository, useValue: mockRepo },
        { provide: CloudAdapter, useValue: mockAdapter },
      ],
    }).compile()

    service = module.get(CloudConnectionUpdateService)
  })

  describe('validateCredentials', () => {
    it('sets status to Active when provider confirms', async () => {
      mockRepo.get.mockResolvedValue({ id: 'conn-1', provider: 'aws' })
      mockAdapter.testConnection.mockResolvedValue(true)
      mockRepo.update.mockImplementation(async ({ item }) => item)

      const result = await service.dispatch({
        action: { scope: 'cloudConnection', type: 'validateCredentials', payload: { connectionId: 'conn-1' } },
        execution: mockExecution(),
      })

      expect(result.status).toBe('Active')
      expect(mockAdapter.testConnection).toHaveBeenCalledWith(
        expect.objectContaining({ id: 'conn-1' })
      )
    })

    it('sets status to Failed when provider rejects', async () => {
      mockRepo.get.mockResolvedValue({ id: 'conn-1', provider: 'aws' })
      mockAdapter.testConnection.mockResolvedValue(false)
      mockRepo.update.mockImplementation(async ({ item }) => item)

      const result = await service.dispatch({
        action: { scope: 'cloudConnection', type: 'validateCredentials', payload: { connectionId: 'conn-1' } },
        execution: mockExecution(),
      })

      expect(result.status).toBe('Failed')
    })

    it('throws when connection not found', async () => {
      mockRepo.get.mockResolvedValue(null)

      await expect(
        service.dispatch({
          action: { scope: 'cloudConnection', type: 'validateCredentials', payload: { connectionId: 'missing' } },
          execution: mockExecution(),
        })
      ).rejects.toThrow('Connection not found')
    })
  })
})
```

Run with: `cd backend && pnpm run test:jest`

### E2E tests verify backend through the UI (Playwright)

Backend behavior is verified via the same stateless E2E tests that test the UI.
The in-memory adapter provides predictable responses, so tests can assert on
outcomes without hitting real external services.

```typescript
// e2e/specs/06-deployment/cloud-connection.spec.ts

test('AC-B2: invalid credentials show error after validation', async ({ page }) => {
  // Navigate to cloud connections page
  await page.goto('/w/client-delivery/settings')
  await page.getByRole('tab', { name: /cloud/i }).click()

  // Add connection with test credentials
  await page.getByRole('button', { name: /add connection/i }).click()
  await page.getByLabel('Provider').selectOption('aws')
  await page.getByLabel('Access Key').fill('AKIAIOSFODNN7INVALID')
  await page.getByLabel('Secret Key').fill('test-secret')
  await page.getByRole('button', { name: /connect/i }).click()

  // The backend custom action runs, in-memory adapter returns failure
  // for keys starting with 'INVALID', UI shows the error
  await expect(page.getByText(/connection failed/i)).toBeVisible()
  await expect(page.getByText(/Failed/)).toBeVisible()
})
```

### When to write which test type

| Scenario | Test type | Why |
|----------|-----------|-----|
| Custom validation logic | Unit test (Jest) | Fast, covers edge cases, mock external deps |
| Custom authorization rules | Unit test (Jest) | Test permission matrix exhaustively |
| Complex data transformations | Unit test (Jest) | Verify computed fields, aggregations |
| External service integration | Unit test (Jest) | Mock adapter, test retry/error handling |
| Full user flow (UI + backend) | E2E test (Playwright) | Verifies the chain works end-to-end |
| API error → UI error message | E2E test (Playwright) | Verifies error propagation |
| Generated CRUD (no custom logic) | No custom test needed | Covered by PostXL's own generation tests |

### Test file locations

```
backend/
├── libs/
│   ├── update/src/
│   │   ├── cloudConnection.update.service.ts       ← implementation
│   │   └── cloudConnection.update.service.spec.ts  ← unit test
│   ├── view/src/
│   │   ├── app.view.service.ts
│   │   └── app.view.service.spec.ts                ← unit test for custom transforms
│   └── cloud-adapter/src/
│       ├── cloud-adapter.interface.ts
│       ├── aws-cloud.adapter.ts
│       ├── aws-cloud.adapter.spec.ts               ← adapter unit test (mocks AWS SDK)
│       └── in-memory-cloud.adapter.ts
```

## Features That Need Backend Implementation

Not every feature needs custom backend work. Use this decision tree:

```
Does the feature require ONLY standard CRUD?
  → YES: No custom backend needed. Generated code handles it.
  → NO: Does it call external services?
    → YES: Create adapter (interface + real + in-memory), write unit tests
    → NO: Does it have custom business rules?
      → YES: Extend UpdateService, write unit tests
      → NO: Does it need custom query transforms?
        → YES: Override toViewModel() in ViewService, write unit tests
        → NO: Generated code handles it.
```

Examples of features that typically need custom backend:
- Cloud provider integration (credential validation, resource provisioning)
- Git operations (commit, push, PR creation via API)
- AI agent orchestration (prompt chaining, streaming responses)
- File processing (upload, transform, S3 storage)
- Webhook delivery (event dispatch to external URLs)
- Complex domain rules (state machines, multi-step workflows)

Examples of features that typically DON'T need custom backend:
- CRUD list/detail/edit pages
- Search and filtering
- Settings pages (read/write config values)
- Navigation and layout
- Dashboard aggregations (if ViewService toViewModel suffices)
