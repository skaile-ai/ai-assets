---
name: skaile-dev-kill-backend
description: Cleanly kills the Skaile platform backend and every process in its spawn chain (port 3001 listener, nest start --watch, dotenvx, bun run dev, sh wrappers). Use when the user says "kill the backend", "stop the backend", "free port 3001", "kill bun run dev", "clean up stray backend processes", or wants a clean slate before restarting `bun run dev` in `platform/backend`.
source: MIGRATED
version: 1.0.0
keywords: [kill, backend, port, nest, bun, process, cleanup, restart]
---

# Kill Backend

Terminates the Skaile platform backend and every process in its spawn chain so the user can cleanly restart it. Handles the common case where prior `bun run dev` sessions left orphaned `nest start --watch` / `dotenvx` / `bun` chains running in the background.

## When to Use

Invoke when the user wants a clean slate for the platform backend, including phrases like:

- "kill the backend"
- "stop the backend on port 3001"
- "free port 3001"
- "kill bun run dev"
- "clean up stray backend processes"
- "the backend is stuck, kill it"

## What Gets Killed

The skill targets any process matching:

- Listener on TCP port 3001
- Command line containing `skaile/dev/platform/backend`
- Command line containing `pnpm/bun run dev` (the sh wrapper spawned by pnpm's bun shim)
- Command line containing `nest start` (the NestJS watcher that respawns the backend on file changes)
- Command line containing `dotenvx run` when invoked under the backend chain

The skill does NOT touch:

- The current Claude Code shell (`zsh -c ...`) or the `pgrep` command itself
- Unrelated `bun` or `node` processes outside the backend chain
- The WSL `/init` parent

## Procedure

Run these steps in order. Do not skip the verification step - nest's watcher will re-spawn the backend if the parent chain is left alive.

### Step 1 - Show what is about to die

```bash
lsof -i :3001 -sTCP:LISTEN -t 2>/dev/null || echo "port 3001 free"
pgrep -af "skaile/dev/platform/backend|pnpm/bun run dev|nest start|dotenvx run" \
  | grep -v "zsh -c" | grep -v pgrep \
  || echo "no stray processes"
```

Report the list to the user before killing anything so they can see the blast radius.

### Step 2 - SIGTERM the whole chain

```bash
pgrep -f "skaile/dev/platform/backend" | xargs -r kill 2>/dev/null
pgrep -f "pnpm/bun run dev"            | xargs -r kill 2>/dev/null
pgrep -f "nest start"                  | xargs -r kill 2>/dev/null
sleep 1
```

### Step 3 - SIGKILL survivors

Some `bun` sh wrappers ignore SIGTERM. Force-kill anything still alive:

```bash
pgrep -f "skaile/dev/platform/backend" | xargs -r kill -9 2>/dev/null
pgrep -f "pnpm/bun run dev"            | xargs -r kill -9 2>/dev/null
pgrep -f "nest start"                  | xargs -r kill -9 2>/dev/null
sleep 1
```

### Step 4 - Verify

```bash
pgrep -af "skaile/dev/platform/backend|pnpm/bun run dev|nest start|dotenvx run" \
  | grep -v "zsh -c" | grep -v pgrep \
  || echo "none alive"
lsof -i :3001 -sTCP:LISTEN -t 2>/dev/null || echo "port 3001 free"
```

Both outputs must show the "free" / "none alive" branches. If anything survives, identify it with `ps -o pid,ppid,stat,command -p <pid>` and `kill -9` it individually. Do not give up after one round - stray `sh` wrappers sometimes need a direct `kill -9 <pid>`.

## Output to the User

Keep output minimal:

1. One line listing what was found before killing (or "nothing to kill")
2. One line confirming port 3001 is free and no survivors remain

Do not restart the backend automatically. The user will start it themselves in a fresh process.

## Gotchas

- **Never kill PID 1 or the WSL `/init` processes** (typically low PIDs under 40000 with `/init` in the command).
- **Exclude the current shell**: the `grep -v "zsh -c"` filter removes the Claude Code bash invocation from the match list so the skill does not try to kill its own parent shell.
- **`nest start --watch` is the respawner**: killing only the port 3001 listener is not enough - `nest` will restart it on the next file change. Always kill the full chain up through `bun run dev`.
- **Multiple orphan chains are common**: previous Claude sessions or terminals may have left several independent `bun run dev` -> `dotenvx` -> `nest` trees running. The pgrep-based approach catches all of them in one pass.
- **`xargs -r`** (GNU) prevents `kill` from being invoked with no arguments when pgrep returns nothing.
