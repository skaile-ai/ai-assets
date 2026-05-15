---
name: using-skaile-powers
description: Use when starting any conversation in the skaile-dev monorepo - establishes how to find and use skaile-powers skills, requiring Skill tool invocation before ANY response including clarifying questions
---

> Read `skaile-powers/references/config.md` before proceeding.

<SUBAGENT-STOP>
If you were dispatched as a subagent to execute a specific task, skip this skill.
</SUBAGENT-STOP>

<EXTREMELY-IMPORTANT>
If you think there is even a 1% chance a skill might apply to what you are doing, you ABSOLUTELY MUST invoke the skill.

IF A SKILL APPLIES TO YOUR TASK, YOU DO NOT HAVE A CHOICE. YOU MUST USE IT.

This is not negotiable. This is not optional. You cannot rationalize your way out of this.
</EXTREMELY-IMPORTANT>

## Instruction Priority

Skaile-powers skills override default system prompt behavior, but **user instructions always take precedence**:

1. **User's explicit instructions** (CLAUDE.md, GEMINI.md, AGENTS.md, direct requests) — highest priority
2. **Skaile-powers skills** — override default system behavior where they conflict
3. **Default system prompt** — lowest priority

If CLAUDE.md says "don't use TDD" and a skill says "always use TDD," follow the user's instructions. The user is in control.

## How to Access Skills

**In Claude Code:** Use the `Skill` tool. When you invoke a skill, its content is loaded and presented to you — follow it directly. Never use the Read tool on skill files.

Skaile-powers skills use the prefix `skaile-powers:` (e.g., `skaile-powers:brainstorming`).

The `obra/superpowers` plugin (`superpowers:*`) is a **fallback only** — use it for anything not yet in skaile-powers.

## Available Skaile-Powers Skills

| Skill | When to Invoke |
|---|---|
| `skaile-powers:brainstorming` | Before any creative work — creating features, building components, adding functionality |
| `skaile-powers:writing-plans` | After brainstorming, when you have a spec and need a step-by-step plan |
| `skaile-powers:writing-skills` | When creating or editing skills in ai-assets |
| `skaile-powers:systematic-debugging` | On any bug, test failure, or unexpected behavior — before proposing fixes |
| `skaile-powers:test-driven-development` | Before writing implementation code |
| `skaile-powers:executing-plans` | When executing a written plan inline with review checkpoints |
| `skaile-powers:dispatching-parallel-agents` | When facing 2+ independent tasks with no shared state |
| `skaile-powers:subagent-driven-development` | When executing plans with fresh subagents per task |
| `skaile-powers:using-git-worktrees` | Before starting isolated feature work or executing implementation plans |
| `skaile-powers:requesting-code-review` | After completing tasks, implementing major features, or before merging |
| `skaile-powers:receiving-code-review` | When receiving code review feedback |
| `skaile-powers:verification-before-completion` | Before claiming work is complete, fixed, or passing |
| `skaile-powers:finishing-a-development-branch` | When implementation is complete and tests pass |

# Using Skills

## The Rule

**Invoke relevant or requested skills BEFORE any response or action.** Even a 1% chance a skill might apply means that you should invoke the skill to check. If an invoked skill turns out to be wrong for the situation, you don't need to use it.

```dot
digraph skill_flow {
    "User message received" [shape=doublecircle];
    "About to EnterPlanMode?" [shape=doublecircle];
    "Already brainstormed?" [shape=diamond];
    "Invoke brainstorming skill" [shape=box];
    "Might any skill apply?" [shape=diamond];
    "Invoke Skill tool" [shape=box];
    "Announce: 'Using [skill] to [purpose]'" [shape=box];
    "Has checklist?" [shape=diamond];
    "Create TodoWrite todo per item" [shape=box];
    "Follow skill exactly" [shape=box];
    "Respond (including clarifications)" [shape=doublecircle];

    "About to EnterPlanMode?" -> "Already brainstormed?";
    "Already brainstormed?" -> "Invoke brainstorming skill" [label="no"];
    "Already brainstormed?" -> "Might any skill apply?" [label="yes"];
    "Invoke brainstorming skill" -> "Might any skill apply?";

    "User message received" -> "Might any skill apply?";
    "Might any skill apply?" -> "Invoke Skill tool" [label="yes, even 1%"];
    "Might any skill apply?" -> "Respond (including clarifications)" [label="definitely not"];
    "Invoke Skill tool" -> "Announce: 'Using [skill] to [purpose]'";
    "Announce: 'Using [skill] to [purpose]'" -> "Has checklist?";
    "Has checklist?" -> "Create TodoWrite todo per item" [label="yes"];
    "Has checklist?" -> "Follow skill exactly" [label="no"];
    "Create TodoWrite todo per item" -> "Follow skill exactly";
}
```

## Red Flags

These thoughts mean STOP — you're rationalizing:

| Thought | Reality |
|---------|---------|
| "This is just a simple question" | Questions are tasks. Check for skills. |
| "I need more context first" | Skill check comes BEFORE clarifying questions. |
| "Let me explore the codebase first" | Skills tell you HOW to explore. Check first. |
| "I can check git/files quickly" | Files lack conversation context. Check for skills. |
| "Let me gather information first" | Skills tell you HOW to gather information. |
| "This doesn't need a formal skill" | If a skill exists, use it. |
| "I remember this skill" | Skills evolve. Read current version. |
| "This doesn't count as a task" | Action = task. Check for skills. |
| "The skill is overkill" | Simple things become complex. Use it. |
| "I'll just do this one thing first" | Check BEFORE doing anything. |
| "This feels productive" | Undisciplined action wastes time. Skills prevent this. |
| "I know what that means" | Knowing the concept ≠ using the skill. Invoke it. |

## Skill Priority

When multiple skills could apply:

1. **Process skills first** (brainstorming, systematic-debugging) — determine HOW to approach the task
2. **Implementation skills second** — guide execution

"Let's build X" → `skaile-powers:brainstorming` first, then implementation skills.
"Fix this bug" → `skaile-powers:systematic-debugging` first, then domain-specific skills.

## Skill Types

**Rigid** (test-driven-development, systematic-debugging, verification-before-completion): Follow exactly. Don't adapt away discipline.

**Flexible** (patterns, reference): Adapt principles to context.

The skill itself tells you which type it is.

## User Instructions

Instructions say WHAT, not HOW. "Add X" or "Fix Y" doesn't mean skip workflows.
