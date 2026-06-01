---
name: "session-review"
description: "[skaile-development] Reviews the current Claude Code implementation
  session for the skaile-dev monorepo: reads the session JSONL to report token usage,
  cache efficiency, estimated cost, and workflow adherence against the skaile-development
  skill sequence. Outputs optimization tips and skillset improvement suggestions.
  Run at the end of any implementation session."
metadata:
  tags:
  - "session"
  - "review"
  - "tokens"
  - "cost"
  - "workflow"
  - "optimization"
  - "retrospective"
  - "skaile-development"
  source: "MERGED"
  stage: "beta"
  produces:
  - path: "_devlog/reports/session-review-<date>-<slug>.json"
    description: "Structured session report (optional, on user request)"
  user_inputs:
    dialog:
    - id: "session_id"
      label: "Session ID (leave blank to use the most recent session for this project)"
      type: "text"
      required: false
    files: []
---

# Session Review — Implementation Session Analysis

## Overview

Reads the Claude Code session JSONL file for the current project, computes real token usage
and estimated cost, assesses how well the skaile-development workflow was followed, and
provides session-specific optimization tips plus suggestions for improving the skillset.

Run this at the end of any implementation session for a full retrospective.

## When to Use

- At the end of any `implement` session (the `implement` skill suggests it automatically)
- When the user says "done", "that's all", or similar wrap-up signals
- After a heavy session with 3+ sub-agent dispatches (most valuable then)
- Any time you want to know what a session actually cost and how it went

## When NOT to Use

- Mid-session (the JSONL is still being written — run at the end)
- For sub-agent sessions (they have their own JSONL; this skill reads the parent session)

---

ROLE  Session retrospective analyst — reads raw Claude Code session data to produce an
      honest assessment of token efficiency, workflow adherence, and improvement opportunities.

READS
  ~/.claude/projects/<cwd-slug>/<session-id>.jsonl   — Claude Code session data

WRITES
  ? _devlog/reports/session-review-<date>-<slug>.json  — optional persisted report

MUST  locate and verify the session JSONL exists before parsing
MUST  use the monorepo root CWD for slug encoding, never a subdirectory
MUST  clearly separate "not applicable" steps from "skipped" steps in workflow grade
MUST  base optimization tips on actual session data — never generic advice
NEVER claim sub-agent costs are included — they are in separate JSONL files
NEVER hard-code the session ID — resolve it from context or the most recent file

EMIT [session-review] started session_id=<id>
EMIT [session-review] parsed turns=<N> agent_dispatches=<N>
EMIT [session-review] complete grade=<A|B|C|D>

---

# ── Step 0: Locate the Session JSONL ──────────────────────────────

The session JSONL path is:
```
~/.claude/projects/<cwd-slug>/<session-id>.jsonl
```

**Slug encoding:** Take the monorepo root absolute path and replace every `/` with `-`.
The leading `/` produces a leading `-`.

Example: `/mnt/localvault/workBench/SKAILE/skaile-dev`
→ `-mnt-localvault-workBench-SKAILE-skaile-dev`

**Session ID:** Read from the `sessionId` field in the first `system` JSONL entry visible
in Claude's current session context. If the user provided a `session_id` input, use that instead.

If the file does not exist, print:
```
Session file not found: <path>
Check ~/.claude/projects/<slug>/ for available sessions.
```
And stop.

EMIT [session-review] started session_id=<id>

# ── Step 1: Parse the Session JSONL ──────────────────────────────

Run this Python snippet via Bash (substitute the actual resolved path for SESSION_PATH):

```bash
python3 - <<'PYEOF'
import json, sys
from pathlib import Path

SESSION_PATH = "~/.claude/projects/<slug>/<session-id>.jsonl"  # substitute before running

path = Path(SESSION_PATH).expanduser()
if not path.exists():
    print(json.dumps({"error": f"File not found: {path}"}))
    sys.exit(1)

input_tokens = cache_read = cache_creation_1h = cache_creation_5m = output_tokens = 0
agent_dispatches = 0
skill_calls = []
bash_samples = []
compact_indices = []
agent_indices = []
worktree_used = False

events = []
with open(path) as f:
    for line in f:
        try:
            events.append(json.loads(line.strip()))
        except Exception:
            pass

for i, d in enumerate(events):
    t = d.get("type", "")
    if t == "system" and d.get("subtype") == "compact_boundary":
        compact_indices.append(i)
    if t == "assistant" and "message" in d:
        msg = d["message"]
        if isinstance(msg, dict) and "usage" in msg:
            u = msg["usage"]
            input_tokens += u.get("input_tokens", 0)
            cache_read += u.get("cache_read_input_tokens", 0)
            cc = u.get("cache_creation", {})
            if isinstance(cc, dict):
                cache_creation_1h += cc.get("ephemeral_1h_input_tokens", 0)
                cache_creation_5m += cc.get("ephemeral_5m_input_tokens", 0)
            output_tokens += u.get("output_tokens", 0)
        if isinstance(msg, dict) and isinstance(msg.get("content"), list):
            for block in msg["content"]:
                if not isinstance(block, dict) or block.get("type") != "tool_use":
                    continue
                name = block.get("name", "")
                inp = block.get("input", {}) or {}
                if name == "Agent":
                    agent_dispatches += 1
                    agent_indices.append(i)
                elif name == "Skill":
                    skill_calls.append(f"{inp.get('skill','?')} {inp.get('args','')}")
                elif name == "Bash":
                    cmd = str(inp.get("command", ""))
                    bash_samples.append(cmd[:100])
                    if "git worktree add" in cmd:
                        worktree_used = True

cache_creation_total = cache_creation_1h + cache_creation_5m
if cache_read + cache_creation_total > 0:
    cache_eff = round(cache_read / (cache_read + cache_creation_total) * 100, 1)
else:
    cache_eff = 0.0

# Pricing: claude-sonnet-4-6 — update in validator.py when model changes
cost = (
    input_tokens * 3.00 / 1_000_000 +
    output_tokens * 15.00 / 1_000_000 +
    cache_read * 0.30 / 1_000_000 +
    cache_creation_total * 3.75 / 1_000_000
)

# Coarse metric: did any compact precede any agent dispatch in the session?
# Note: this checks if compaction ever happened before agents were dispatched,
# not whether each individual dispatch was immediately preceded by a compact.
# First-version approximation — see validator.py Known Limitations.
compact_before_agent = (
    len(compact_indices) > 0 and
    len(agent_indices) > 0 and
    any(ci < ai for ci in compact_indices for ai in agent_indices)
)

print(json.dumps({
    "input_tokens": input_tokens,
    "cache_read": cache_read,
    "cache_creation_1h": cache_creation_1h,
    "cache_creation_5m": cache_creation_5m,
    "cache_creation_total": cache_creation_total,
    "output_tokens": output_tokens,
    "cache_efficiency_pct": cache_eff,
    "estimated_cost_usd": round(cost, 4),
    "agent_dispatches": agent_dispatches,
    "compact_count": len(compact_indices),
    "compact_before_agent": compact_before_agent,
    "skill_calls": skill_calls,
    "bash_samples": bash_samples[:15],
    "worktree_used": worktree_used,
    "total_events": len(events)
}, indent=2))
PYEOF
```

Parse the JSON output. If `"error"` key is present, report the error and stop.

EMIT [session-review] parsed turns=<total_events> agent_dispatches=<agent_dispatches>

# ── Step 2: Assess Workflow Adherence ────────────────────────────

From `skill_calls` and `bash_samples`, determine which skaile-development steps were taken.
Mark each as ✅ done / ⚠️ skipped / — not applicable.

**Detection logic:**

| Step | Detected if |
|---|---|
| Branch created | any `skill_calls` entry starts with `git` and contains `mode=branch` |
| `implement` used | any `skill_calls` entry starts with `implement` |
| `test` run | any `skill_calls` entry starts with `test` |
| `audit` run | any `skill_calls` entry starts with `audit` |
| `doc --mode update` run | any `skill_calls` entry starts with `doc` and contains `update` |
| `devlog` added | any `skill_calls` entry starts with `devlog` |
| `/compact` before Agent dispatch | `compact_before_agent` = true (coarse — see Known Limitations) |
| Worktree used | `worktree_used` = true |

**Grade:**
- **A** — All applicable steps done (or marked —)
- **B** — 1 applicable step skipped
- **C** — 2 applicable steps skipped
- **D** — 3+ applicable steps skipped on a non-trivial session

**Known Limitations (include in report):**
- The `compact_before_agent` metric is a coarse first-version check — it confirms compaction
  happened at some point before some agent dispatch, not that each dispatch was immediately
  preceded by a compact.
- Sub-agent token costs are not included — they live in separate JSONL files at
  `~/.claude/projects/<slug>/<session-id>/subagents/`. Only the parent session is parsed here.

# ── Step 3: Render the Report ────────────────────────────────────

Output the four blocks:

```markdown
## Session Review

### Block 1 — Token & Cost

| Metric | Value |
|---|---|
| Input tokens (direct) | {input_tokens:,} |
| Cache read tokens | {cache_read:,} |
| Cache creation tokens | {cache_creation_total:,} (1h: {cache_creation_1h:,} / 5m: {cache_creation_5m:,}) |
| Output tokens | {output_tokens:,} |
| Cache efficiency | {cache_efficiency_pct}% |
| Estimated cost | ${estimated_cost_usd} |
| Agent dispatches | {agent_dispatches} |

> Pricing: claude-sonnet-4-6 — $3.00/M input, $15.00/M output, $0.30/M cache-read,
> $3.75/M cache-creation (flat rate for both ephemeral tiers).
> Sub-agent costs are in separate JSONL files and not included here.

---

### Block 2 — Workflow Adherence

| Step | Status | Notes |
|---|---|---|
| Branch created before code changes | ✅/⚠️/— | ... |
| `implement` used for non-trivial changes | ✅/⚠️/— | ... |
| `test` run after implementation | ✅/⚠️/— | ... |
| `audit scope=diff` run | ✅/⚠️/— | ... |
| `doc --mode update` run | ✅/⚠️/— | ... |
| `devlog` added | ✅/⚠️/— | ... |
| `/compact` before Agent dispatch | ✅/⚠️/— | (coarse check — see Known Limitations) |
| Worktree used for isolation | ✅/⚠️/— | ... |

**Grade: {A/B/C/D}**

**Known Limitations**
- The `/compact` check confirms compaction occurred before some agent dispatch in the session,
  not that each specific dispatch was preceded by a compact.
- Sub-agent token costs live in separate JSONL files and are not summed here.

---

### Block 3 — Optimization Tips

(3–5 tips based on actual session data. Examples:)
- If cache_efficiency_pct < 40%: "Cache efficiency was {N}% — consolidate repeated file reads"
- If agent dispatches were sequential: "Agents dispatched sequentially — batch in parallel"
- If compact_before_agent = false and agent_dispatches > 0: "No pre-dispatch compaction detected"
- If worktree_used = false and multiple packages changed: "Consider worktree for cross-package isolation"
- If any workflow step ⚠️: "The {skill} skill was skipped"

Do NOT generate tips for steps that went well.

---

### Block 4 — Skillset Improvement Suggestions

(2–4 bullets on friction no existing skill covered. Be honest. If no friction, say so plainly.)
```

# ── Step 4: Optional Persistence ─────────────────────────────────

Ask: "Save this report to `_devlog/reports/session-review-<date>-<slug>.json`? (yes/no)"

IF yes:
  Write the raw parsed data + grade + tips as JSON to that path.
  $ git add _devlog/reports/ && git commit -m "docs: add session review report <date>"

EMIT [session-review] complete grade=<grade>

---

## Integration

- **Suggested by:** `implement` skill (after devlog phase), `skaile-development` agent (on wrap-up)
- **Reads:** Claude Code session JSONL at `~/.claude/projects/<slug>/<session-id>.jsonl`
- **Sub-agent costs:** In `~/.claude/projects/<slug>/<session-id>/subagents/` — not parsed here

*See also: `skills/implement/SKILL.md`*
