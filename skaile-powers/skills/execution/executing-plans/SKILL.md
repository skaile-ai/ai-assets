---
name: executing-plans
description: Use when you have a written implementation plan to execute in a separate session with review checkpoints
---

> Read `skaile-powers/references/config.md` before proceeding.

# Executing Plans

## Overview

Load the plan overview, review critically, execute the beads in dependency order, report when complete.

**Announce at start:** "I'm using the executing-plans skill to implement this plan."

**Note:** This skill works much better with subagent support. If subagents are available, use `skaile-powers:subagent-driven-development` (the default execution mode) instead of this skill. Use `executing-plans` only for inline same-session execution without subagents.

## The Process

### Step 1: Load and Review the Plan
1. Read `overview.md` for the bead list and dependency graph (see `writing-plans` / `config.md`)
2. Read every bead file
3. Review critically — identify any questions or concerns
4. If concerns: raise them with your human partner before starting
5. If no concerns: create a TodoWrite entry per bead and proceed

### Step 2: Execute Beads

Work beads in dependency order. For each bead:
1. Confirm every `depends_on` bead has `status: done` — skip beads with unmet dependencies until their blockers finish
2. **HITL beads** (`type: HITL`) — pause and hand off to the user rather than executing
3. Set the bead's frontmatter `status: in-progress`; mark its TodoWrite entry in_progress
4. Follow each step exactly (beads have bite-sized steps)
5. Run verifications as specified
6. Set the bead's frontmatter `status: done`; update its row in `overview.md`; mark the TodoWrite entry completed. When a bead reaches `status: done`, append (or extend the current day's entry in) `devlog/DEVLOG.md` (see `config.md` → Devlog) listing the completed bead.

**Follow-up work discovered mid-bead:** don't expand the current bead. Write a new follow-up bead — next free sub-number, `status: pending`, honest `depends_on`, a note that it was discovered during implementation — and link it into `overview.md`.

### Step 3: Complete Development

After all tasks complete and verified:
- Announce: "I'm using the finishing-a-development-branch skill to complete this work."
- **REQUIRED SUB-SKILL:** Use skaile-powers:finishing-a-development-branch
- Follow that skill to verify tests, present options, execute choice

## When to Stop and Ask for Help

**STOP executing immediately when:**
- Hit a blocker (missing dependency, test fails, instruction unclear)
- Plan has critical gaps preventing starting
- You don't understand an instruction
- Verification fails repeatedly

**Ask for clarification rather than guessing.**

## When to Revisit Earlier Steps

**Return to Review (Step 1) when:**
- Partner updates the plan based on your feedback
- Fundamental approach needs rethinking

**Don't force through blockers** - stop and ask.

## Remember
- Review the plan critically first
- Follow bead steps exactly
- Respect the `depends_on` order; never start a bead with unmet dependencies
- Keep each bead's `status` frontmatter current
- Don't skip verifications
- Reference skills when a bead says to
- Stop when blocked, don't guess
- Never start implementation on main/master branch without explicit user consent

## Integration

**Required workflow skills:**
- **skaile-powers:using-git-worktrees** - REQUIRED: Set up isolated workspace before starting
- **skaile-powers:writing-plans** - Creates the plan this skill executes
- **skaile-powers:finishing-a-development-branch** - Complete development after all tasks
