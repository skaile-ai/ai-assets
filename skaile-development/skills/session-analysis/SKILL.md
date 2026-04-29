---
name: "session-analysis"
description: "[skaile-development] Cross-session analysis of Claude Code session data for the skaile-dev monorepo. Reads a pre-computed digest (≤10KB) from the Python pipeline, triages improvement opportunities, reads implicated SKILL.md files, and proposes concrete diffs to reduce token waste and improve workflow adherence. Run after ./run.sh in _scripts/session-analysis/."
metadata:
  version: "1.0.0"
  tags:
    - "session"
    - "analysis"
    - "tokens"
    - "optimization"
    - "skill-improvement"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  produces:
    - path: "_devlog/reports/session-analysis-<date>.md"
      description: "Structured analysis report with proposed SKILL.md diffs"
  user_inputs:
    dialog:
      - id: "focus"
        label: "Focus area (leave blank for full triage): prompt-bloat | workflow | redundancy | subagents"
        type: "text"
        required: false
    files: []
---

# Session Analysis — Cross-Session Skill Optimization

## Overview

Reads the pre-computed digest from the Python pipeline (`_scripts/session-analysis/.cache/digest.json`)
and proposes concrete SKILL.md edits to reduce token waste and improve workflow adherence.

The Python pipeline (extract.py + reduce.py) does all the heavy data crunching at zero token cost.
This skill only sees the compact digest (≤10KB) and the specific SKILL.md files it needs to edit.

## Prerequisites

Run the Python pipeline first:
```bash
cd _scripts/session-analysis && ./run.sh
```

## When to Use

- After accumulating 10+ sessions and wanting to tune skills
- When token costs feel higher than they should be
- When workflow steps keep getting skipped across sessions
- Periodically (weekly/bi-weekly) as part of skill maintenance

## When NOT to Use

- For single-session retrospectives — use `skaile-dev-session-retro` instead
- Mid-session — the current session's JSONL is still being written

---

ROLE  Cross-session analyst — reads aggregated session data to identify systemic token waste
      and propose surgical SKILL.md improvements.

READS
  _scripts/session-analysis/.cache/digest.json           — pre-computed analysis digest
  ai-assets/skaile-development/skills/<name>/SKILL.md    — only the skills flagged by triage

WRITES
  ? _devlog/reports/session-analysis-<date>.md            — analysis report (on user request)

MUST  verify the digest exists before starting — if not, tell user to run ./run.sh
MUST  present the triage ranking and wait for user input before reading any SKILL.md
MUST  base all proposals on evidence from the digest — never generic advice
MUST  show estimated token savings for each proposed diff
NEVER read raw JSONL files — only the pre-computed digest
NEVER apply changes — only print proposed diffs
NEVER propose changes without citing the specific digest data that justifies them

EMIT [session-analysis] started sessions=<N> focus=<area|all>
EMIT [session-analysis] triage opportunities=<N>
EMIT [session-analysis] complete proposals=<N>

---

# ── Step 0: Load the Digest ──────────────────────────────────────

Read: `_scripts/session-analysis/.cache/digest.json`

IF file does not exist:
  STOP with:
  > "Digest not found. Run the Python pipeline first:
  > ```bash
  > cd _scripts/session-analysis && ./run.sh
  > ```"

Parse the JSON. Print the meta summary:

```
Sessions analyzed: {meta.sessions_analyzed}
Date range: {meta.date_range[0]} → {meta.date_range[1]}
Total cost: ${meta.total_cost_usd}
Total input tokens: {meta.total_tokens_in:,}
Total output tokens: {meta.total_tokens_out:,}
```

EMIT [session-analysis] started sessions={meta.sessions_analyzed} focus={user_focus|all}

# ── Step 1: Triage ───────────────────────────────────────────────

Combine signals from all 5 digest sections to rank improvement opportunities.
Each opportunity gets a type, severity, and brief evidence summary.

**Signal mapping:**

| Signal | Opportunity Type | Severity Formula |
|---|---|---|
| skill_usage: high cost_delta × high invocations | `prompt-bloat` | cost_delta × sessions_used_in |
| workflow_adherence: rate < 0.5 | `workflow-skip` | (1 - rate) × sessions_analyzed |
| redundancy: top_repeated_reads with avg > 2.0 | `redundant-reads` | avg_reads × sessions |
| redundancy: read_after_compact_ratio > 0.5 | `post-compact-reread` | ratio × sessions |
| subagent_patterns: sequential_ratio > 0.5 | `sequential-dispatch` | ratio × total_dispatches |

IF user provided a focus area:
  Filter to only opportunities matching that type.

Rank all opportunities by severity descending. Present as a numbered list:

```markdown
## Triage — Top Improvement Opportunities

| # | Type | Target | Severity | Evidence |
|---|---|---|---|---|
| 1 | prompt-bloat | implement | 46.5 | cost_delta $3.10 × 15 sessions |
| 2 | workflow-skip | audit | 26.0 | 34% adherence, skipped in 26 sessions |
| 3 | redundant-reads | CLAUDE.md | 22.4 | read 3.2x avg across 7 sessions |
| ... | ... | ... | ... | ... |
```

Then ask:

> "Which opportunities should I analyze? (e.g., 'all', '1,3', 'top 5', 'skip 2')"

Wait for user response.

EMIT [session-analysis] triage opportunities={N}

# ── Step 2: Targeted Reads ───────────────────────────────────────

For each selected opportunity, identify and read the implicated SKILL.md file(s):

| Opportunity Type | What to Read |
|---|---|
| `prompt-bloat` for skill X | `ai-assets/skaile-development/skills/{X}/SKILL.md` |
| `workflow-skip` for step X | The skill that should trigger step X (e.g., `skaile-dev-implement` triggers `skaile-dev-test`) + the skaile-development soul prompt if relevant |
| `redundant-reads` for file X | Search SKILL.md files that contain instructions to read that file (Grep for the filename) |
| `post-compact-reread` | Skills that instruct "read CLAUDE.md" without scoping |
| `sequential-dispatch` | Skills that dispatch agents (likely `skaile-dev-implement`) |

Read each implicated SKILL.md. Note:
- Total line count (proxy for prompt token size)
- Sections that overlap with other skills or with CLAUDE.md
- Instructions that force unnecessary file reads
- Missing instructions that would improve the weak signal

# ── Step 3: Propose Diffs ────────────────────────────────────────

For each opportunity, produce a structured proposal:

```markdown
### Opportunity {N}: {one-line summary}

**Type:** {prompt-bloat | workflow-skip | redundant-reads | post-compact-reread | sequential-dispatch}
**Target:** {skill name or file}
**Evidence:** {specific numbers from digest}

**Root cause:** {what in the SKILL.md causes this pattern}

**Proposed diff:**
```diff
--- a/ai-assets/skaile-development/skills/{skill}/SKILL.md
+++ b/ai-assets/skaile-development/skills/{skill}/SKILL.md
@@ ...
- {removed lines}
+ {added lines}
```

**Estimated savings:** {tokens saved per invocation} × {expected frequency} = {total}
```

**Diff guidelines:**
- For `prompt-bloat`: remove duplicated content, replace with cross-references
- For `workflow-skip`: strengthen trigger language, add explicit reminders
- For `redundant-reads`: add "if not already in context" guards, scope read instructions
- For `post-compact-reread`: add re-orientation sections that are compact-aware
- For `sequential-dispatch`: add parallel dispatch guidance with examples

# ── Step 4: Report ───────────────────────────────────────────────

After all proposals are presented, ask:

> "Save this analysis to `_devlog/reports/session-analysis-{date}.md`? (yes/no)"

IF yes:
  Write the full report (meta summary + triage table + all proposals) to that path.
  Do NOT commit automatically — the user may want to apply diffs first.

Print closing summary:

```
Proposals: {N}
Estimated total savings: {sum of per-proposal savings} tokens/session
Next step: Review and apply the diffs you agree with, then re-run ./run.sh
           after a few sessions to measure improvement.
```

EMIT [session-analysis] complete proposals={N}

---

## Integration

- **Prerequisite:** `_scripts/session-analysis/run.sh` (Python pipeline)
- **Complements:** `skaile-dev-session-retro` (single-session retrospective)
- **Reads:** Only the digest + implicated SKILL.md files
- **Suggests edits to:** Any skill under `ai-assets/skaile-development/skills/`
