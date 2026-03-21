# Custom Actions Pattern

Domain-specific actions that combine multiple models or implement custom business logic.

## When to Use

Use custom actions instead of raw CRUD when:
- An operation affects multiple models (e.g., "deploy project" creates deployment, updates status, notifies users)
- Business logic goes beyond simple create/update/delete (e.g., "archive" with cascade rules)
- The action describes **intent** rather than **mechanics** (e.g., "complete" vs "update status to completed")

## Schema Definition

Define actions on the closest model in `postxl-schema.json`:

```json
{
  "models": {
    "Project": {
      "description": "A software project managed on the platform.",
      "fields": { ... },
      "actions": {
        "deploy": "Deploy the project to the target environment.",
        "archive": "Archive the project and all its artifacts.",
        "duplicate": "Create a copy of the project with a new name."
      }
    }
  }
}
```

## What Gets Generated

After `pnpm run generate`:

### Backend

**Action types** in `backend/libs/update/src/project.update.service.ts`:
```typescript
export type Action_Project_Deploy = {
  scope: 'project'
  type: 'deploy'
  payload: DeployPayload
}
```

**Dispatcher** routes actions by scope → model → type:
```typescript
dispatch({ action, execution }) → routes to ProjectUpdateService.dispatch()
```

**Update service placeholder** with `@custom-start` / `@custom-end` markers:
```typescript
// @custom-start deploy
async deploy(payload: DeployPayload, execution: IActionExecution): Promise<DeployResult> {
  // TODO: Implement deploy logic
  throw new Error('Not implemented')
}
// @custom-end deploy
```

### Frontend

**tRPC mutation hook** auto-generated:
```typescript
// Usage in React component
const deployMutation = trpc.project.deploy.useMutation()
await deployMutation.mutateAsync({ projectId: '...', target: '...' })
```

## Implementation Pattern

1. Define the action in the schema
2. Regenerate: `pnpm run generate`
3. Implement in the update service (between `@custom-start` / `@custom-end` markers)
4. Inject existing providers from infrastructure phase (never create duplicate adapters)
5. Use mock/in-memory implementations — e2e tests always run against mocks

## Naming Convention

Actions should describe **intent**, not mechanics:
- `deploy` not `createDeploymentAndUpdateStatus`
- `archive` not `setArchivedTrue`
- `complete` not `updateStatusToCompleted`
- `invite` not `createUserAndSendEmail`
