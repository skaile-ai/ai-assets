# Dependency Mapping

Maps architecture doc entries to npm packages and TypeScript configuration.

## External Integration → npm Packages

| Architecture entry | npm packages | Notes |
|--------------------|-------------|-------|
| `claude-api` | `@anthropic-ai/sdk` | Anthropic Claude SDK |
| `openai-api` | `openai` | OpenAI SDK |
| `github-api` | `@octokit/rest` | GitHub REST API client |
| `gitlab-api` | `@gitbeaker/rest` | GitLab REST API client |
| `bitbucket-api` | `bitbucket` | Bitbucket REST API client |
| `aws-sdk` | `@aws-sdk/client-ec2`, `@aws-sdk/client-s3`, `@aws-sdk/client-sts` | AWS SDK v3 (install only needed clients) |
| `azure-sdk` | `@azure/arm-compute`, `@azure/identity` | Azure SDK (install only needed packages) |
| `gcp-sdk` | `@google-cloud/compute` | Google Cloud SDK |
| `stripe` | `stripe` | Payment processing |
| `resend` | `resend` | Email service |
| `sendgrid` | `@sendgrid/mail` | Email service |
| `twilio` | `twilio` | SMS/voice |
| `meilisearch` | `meilisearch` | Search engine |
| `redis` | `ioredis` | Redis client |
| `s3` | `@aws-sdk/client-s3`, `@aws-sdk/s3-request-presigner` | File storage |

## Protocol → npm Packages

| Protocol | npm packages | Dev packages |
|----------|-------------|-------------|
| `websocket` | `@fastify/websocket`, `ws` | `@types/ws` |
| `sse` | (built into NestJS) | — |
| `grpc` | `@grpc/grpc-js`, `@grpc/proto-loader` | — |
| `graphql` | `@nestjs/graphql`, `@nestjs/mercurius`, `graphql` | — |

## TypeScript Path Aliases

For each custom module, add a path alias to `backend/tsconfig.json`:

```json
{
  "compilerOptions": {
    "paths": {
      "@libs/<module-name>": ["libs/<module-name>/src"],
      "@libs/<module-name>/*": ["libs/<module-name>/src/*"]
    }
  }
}
```

Follow the existing PostXL path alias convention in the project.

## Environment Variables

Each external integration typically needs:

| Integration type | Env vars |
|-----------------|----------|
| API provider | `<NAME>_API_KEY`, `<NAME>_BASE_URL` (optional) |
| Database | `<NAME>_DATABASE_URL` |
| Cloud provider | `<NAME>_ACCESS_KEY_ID`, `<NAME>_SECRET_ACCESS_KEY`, `<NAME>_REGION` |
| OAuth provider | `<NAME>_CLIENT_ID`, `<NAME>_CLIENT_SECRET`, `<NAME>_REDIRECT_URI` |
| Service URL | `<NAME>_URL`, `<NAME>_PORT` |

Add all env vars to `.env.example` files with descriptive comments.
Add Zod schema validation to `api.config.ts` for required variables.
Use `.optional()` in Zod for credentials that enable graceful degradation.
