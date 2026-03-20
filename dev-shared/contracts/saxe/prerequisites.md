# Prerequisites Check

How to verify external tools are available before a skill starts work.
Skills that depend on external tools must run a prerequisites check as their
first step. If a tool is missing, inform the user with the install command
and offer to continue without it (where possible) or stop.

## External Tool Registry

Each tool has a check command, install command, and classification.

### pnpm (Package Manager)

- **Check:** `pnpm --version`
- **Install:** `npm install -g pnpm` (or via Corepack: `corepack enable && corepack prepare pnpm@latest --activate`)
- **Classification:** hard — cannot proceed without it
- **Used by:** all implementation skills

### git

- **Check:** `git --version`
- **Install:** System package manager (`apt install git`, `brew install git`)
- **Classification:** hard — cannot proceed without it
- **Used by:** all implementation skills

### @postxl/cli (pxl)

- **Check:** `pnpx @postxl/cli --version` (or `pxl --version` if globally installed)
- **Install:** `pnpm add -D @postxl/cli` (in the project, or `pnpm add -g @postxl/cli` globally)
- **Classification:** hard — cannot scaffold or generate without it
- **Used by:** implement-1-setup-1-scaffold, implement-generate

### docker / docker compose

- **Check:** `docker --version && docker compose version`
- **Install:** [Docker Desktop](https://docs.docker.com/get-docker/) or system package manager
- **Classification:** hard for stateful mode (database, Keycloak); soft for stateless dev
- **Used by:** implement-1-setup-1-scaffold (database setup), implement (Phase 2), implement-3-verify
- **Note:** Stateless dev mode (`pnpm run dev`) works without Docker. Docker is
  required for `pnpm prisma migrate dev`, Keycloak auth, and stateful E2E tests.

### agent-browser

- **Check:** `agent-browser --version`
- **Install (two steps):**

  ```bash
  # 1. Install globally
  npm install -g agent-browser

  # 2. Install browser dependencies (needed on Linux/WSL)
  agent-browser install --with-deps
  ```

- **Classification:** soft — visual verification is skipped without it, but
  implementation can proceed. Screenshots and browser checks will not be available.
- **Used by:** implement-1-setup-2-foundation (Step 9), implement-2-features (Step 7), implement-3-verify (Step 5)

### python3

- **Check:** `python3 --version`
- **Install:** System package manager (`apt install python3`, `brew install python3`)
- **Classification:** soft — only needed for auto-review mode in concept.
  Manual review mode works without it.
- **Used by:** concept (auto-review), concept-review

## How to Run the Check

Run each required tool's check command. Collect results into a summary:

```
Prerequisites check:
  ✓ pnpm 10.14.0
  ✓ git 2.43.0
  ✓ @postxl/cli 1.8.2
  ✓ docker 27.0.3 / docker compose v2.29.1
  ✗ agent-browser — not found
  ✓ python3 3.12.0
```

### On failure (hard prerequisite)

Stop and inform the user:

```
Missing required tool: @postxl/cli

Install with:
  pnpm add -D @postxl/cli

Cannot proceed without this tool. Please install and re-run.
```

### On failure (soft prerequisite)

Warn and offer to continue:

```
Optional tool not found: agent-browser

Install with:
  npm install -g agent-browser && agent-browser install --with-deps

Without agent-browser, visual verification (browser screenshots and
walkthrough checks) will be skipped. You can still approve features
based on E2E test results and Storybook stories.

Continue without agent-browser? (yes / install first)
```

If the user chooses to continue, note the skip in `_implementation/decisions.md`:

```
YYYY-MM-DD: Proceeding without agent-browser — visual verification skipped
```

## Per-Skill Prerequisites

| Skill          | Hard                   | Soft                       |
| -------------- | ---------------------- | -------------------------- |
| concept    | —                      | python3 (auto-review only) |
| implement  | pnpm, git              | docker, agent-browser      |
| implement-1-setup-1-scaffold   | pnpm, git, @postxl/cli | docker                     |
| implement-generate   | pnpm, @postxl/cli      | —                          |
| implement-1-setup-2-foundation | pnpm, git              | agent-browser              |
| implement-2-features    | pnpm, git              | agent-browser              |
| implement-3-verify     | pnpm                   | docker, agent-browser      |
