# Sub-agent Dispatch Patterns

Reference for any skill that spawns child agents (Agent tool calls). Defines the
fan-out amplification problem and the three mitigation techniques used across
skaile-development skills.

---

## The Fan-out Amplification Problem

When a parent agent spawns a child agent, Claude Code passes a snapshot of the
**full parent conversation** as the child's starting context. The child pays the
full token cost of everything the parent has read and done — before doing any
useful work of its own.

```
total_input_tokens = parent_context_tokens × number_of_sub_agents
                   + Σ(each sub-agent's own work)
```

**Sequential dispatch makes this worse.** Each returned result gets appended to
the parent context. Sub-agent 5 therefore receives sub-agents 1–4's results as
context, even though it has no use for them. The cost grows with every iteration.

**Example:** A parent session with 80K tokens dispatches 20 sequential sub-agents.
By the time agent 20 starts, the parent context is ~180K tokens (80K + 19 results).
Total initialization cost alone: ~2.5M tokens — before agent 20 writes a line of code.

---

## Technique 1 — Minimum Viable Context (MVC) Prompts

A sub-agent prompt must contain exactly what the sub-agent needs and nothing else.
Never pass the parent conversation or all files read so far. Construct the prompt
explicitly.

### MVC prompt structure

```
# Task: <one-line summary>

## Context
<Package name, file paths, relevant type signatures — only what this task touches.>
<Max 3–5 files or excerpts. If a file is large, paste only the relevant section.>

## What to implement
<Precise description of the change. Acceptance criteria as a checklist.>

## Constraints
<Naming conventions, patterns to follow, patterns to avoid — specific to this task.>
<Reference the relevant package CLAUDE.md section if needed, but paste the key rule,
 don't ask the sub-agent to go read the full file.>

## What NOT to do
<Explicitly rule out scope that could pull in extra files or work.>

## Output
<What files to create or modify. Expected result.>
```

**Anti-patterns to avoid in sub-agent prompts:**

| Anti-pattern | Why it burns tokens |
|---|---|
| "Based on our conversation, implement…" | Forces Claude to re-process the entire parent context |
| "Read CLAUDE.md and then implement…" | Sub-agent re-reads a file the parent already loaded |
| "Implement tasks 3–7 from the plan" | Sub-agent must re-read the plan; paste the task text instead |
| Pasting the full source file when only one function needs changing | Every token in the paste is re-encoded |

---

## Technique 2 — Batch + Parallel Dispatch

Instead of one sub-agent per task (sequential), group related tasks into batches
and dispatch batches in parallel.

```
Sequential (bad):           Parallel batches (good):
agent1 → wait → agent2      [agent1, agent2, agent3] → wait
→ wait → agent3             [agent4, agent5, agent6] → wait
→ wait → ...
```

### Grouping rules

- **Group by package boundary.** Tasks in the same package share context (imports,
  type definitions, naming conventions) and can share the per-package CLAUDE.md
  excerpt in their batch prompt without duplication.
- **Group by independence.** Two tasks are independent if neither reads files the
  other writes. Dependent tasks must be sequential within the batch or in separate
  batches.
- **Batch size:** 4–8 tasks per agent. Fewer → too many agents; more → the agent
  prompt becomes overloaded and quality drops.
- **Parallel width:** 3–5 concurrent agents. More → rate-limit pressure and harder
  to triage failures.

### Batch prompt structure (extends MVC)

```
# Batch: <package or feature area> — Tasks <N>–<M>

## Shared context (applies to all tasks below)
<Package name, key imports, naming convention excerpt — read once, reused for all tasks.>

## Task N: <title>
### Files
<file paths>
### What to implement
<precise description>
### Acceptance criteria
- [ ] ...

## Task N+1: <title>
...
```

---

## Technique 3 — Pre-dispatch Compaction

Before spawning any sub-agents, the parent should have a compact context.

**Rule:** Call `/compact` immediately before any Agent tool call when the parent
session has done non-trivial work (read multiple files, ran tests, made prior edits).

The compaction summary replaces the full message history with a structured summary,
dramatically reducing the snapshot that each sub-agent inherits.

**When to compact:**

| Situation | Action |
|---|---|
| About to dispatch the first of N≥3 agents | `/compact` then dispatch all in parallel |
| Mid-sequence: prior batch returned, about to dispatch next batch | `/compact` between batches |
| Parent session has been running >30 minutes | `/compact` before any new Agent call |
| Parent context contains large file reads (>500 lines total) | `/compact` before dispatch |

**When compaction is NOT needed:**

- Dispatching a single one-off agent from a fresh session (context is small)
- The sub-agent's task is so narrow that parent context is irrelevant anyway
  (e.g., a pure research agent with a fully self-contained prompt)

---

## Decision Table — Which Pattern to Apply

| Number of tasks | Task relationship | Apply |
|---|---|---|
| 1 | Any | MVC prompt only, no batching needed |
| 2–3 | Independent | Parallel, one agent each, MVC prompts |
| 4–8 | Same package | Single batch agent with batch prompt |
| 4–8 | Different packages | Parallel agents, one per package, MVC per agent |
| 9–20 | Mixed | Group into 3–4 batch agents by package, run in parallel |
| 20+ | Sequential-looking ("implement task N of M") | Restructure: decompose into 4–5 batch agents, all parallel |

---

## Skills That Use This Reference

- `skaile-dev-implement` — primary user; applies all three techniques in Phase 3
- `skaile-dev-test` (construct mode) — applies MVC when delegating to `skaile-dev-test-unit` / `skaile-dev-test-integration`
- Any skill that calls `Agent(...)` with more than one invocation in a session
