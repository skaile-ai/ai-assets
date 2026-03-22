# Docker Stacks

Available compose stacks for skill execution.

| Stack | Base Image | Included Tools | Use For |
|---|---|---|---|
| base | alpine:3.20 | bash, git, curl | Generic skills |
| nuxt | node:22-alpine | Node.js 22, pnpm | Nuxt/Vue skills |
| nuxt-full | node:22-alpine | Node.js 22, pnpm, Redis | Skills needing DB/cache |
| python | python:3.12-slim | Python 3.12, uv | Python skills |
| typst | alpine:3.20 | typst, bash, git | Typst document skills |

## Stack Selection

1. Check `test-manifest.yaml` for `stack` field
2. Check `stack_override` for case-specific overrides
3. Fall back to skill's default stack

## Container Lifecycle

1. `docker compose -p lab-<skill>-<case> up -d --build --wait`
2. Copy scaffold to workspace: `/app → /app/output`
3. Execute skill via DockerProxyDriver
4. Run metrics in container
5. Collect artifacts if needed
6. `docker compose -p lab-<skill>-<case> down -v --remove-orphans`
