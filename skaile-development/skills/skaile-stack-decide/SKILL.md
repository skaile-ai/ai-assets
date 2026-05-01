---
name: "skaile-stack-decide"
description: "[skaile-development] Use when reasoning about whether and how to build an app idea on the Skaile/PostXL stack -- choosing between platform skills+flows, forge integration tiers, the agent runtime, or PostXL alone. Loads the four context packs (agent framework, ecosystem, platform, PostXL) on demand and routes the question to the relevant doc(s). Trigger when the user asks 'should we build X with Skaile?', 'where in the stack does this fit?', 'platform or standalone app?', 'PostXL or just NestJS?', or any architectural orientation question across the Skaile/PostXL surface."
metadata:
  version: "1.0.0"
  tags:
    - "skaile"
    - "postxl"
    - "architecture"
    - "app-design"
    - "stack-decision"
    - "orientation"
    - "decision-rubric"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    inputs_required:
      - id: idea
        label: "What is the app idea or architectural question?"
        type: text
        hint: "Plain-language description of the app or the decision you're trying to make"
    inputs_optional:
      - id: scope
        label: "Decision scope (helps narrow which references to load)"
        type: select
        options:
          - "auto"
          - "agent-runtime"
          - "ecosystem"
          - "platform"
          - "postxl"
          - "all"
        default: "auto"
        hint: "auto = pick references from the question; all = load every pack"
  reads_from:
    - "references/"
  writes_to: []
---

## Purpose

Provide stack-literate guidance for any question that crosses the Skaile
or PostXL surface. The skill carries four condensed context packs in
`references/`. They are not loaded into context until the skill fires --
each one is between 200-700 lines, so loading all four costs significant
tokens. Pick the minimum set that answers the question.

## When to Use

Trigger on questions like:

- "Can the Skaile platform do X?" / "Should we build X on the platform?"
- "How would I add AI to this existing app?"
- "Which forge tier matches my use case?"
- "Should we use PostXL for this admin tool?"
- "Where in the skaile-dev monorepo does this idea live?"
- "Platform skills+flows or a standalone forge app?"
- "What's already on the shelf for X?" (mounts, connectors, MCP, skills)
- "Walk me through the trade-offs between PostXL alone and the Skaile platform."

Skip when the user has a concrete code-level task that does not require
architectural orientation (use `implement`, `audit`, `test`, etc.).

## How to Use

### Step 1 -- Classify the question

Read the user's prompt and decide which of the four reference packs
apply. Most questions need 1-2; rarely all four.

| Question pattern | Load |
|---|---|
| "How do I embed an AI agent in app X?" | `skaile-agent-framework.md` |
| "What's the right integration tier?" | `skaile-agent-framework.md` |
| "Where in skaile-dev does idea X live?" | `skaile-ecosystem.md` |
| "Which forge app should I mimic?" | `skaile-ecosystem.md` |
| "Can the Skaile platform do X without code changes?" | `skaile-platform.md` |
| "Skills + flows + AAP components -- what fits?" | `skaile-platform.md` |
| "Should I use PostXL for this admin tool?" | `postxl-framework.md` |
| "Will PostXL fit our stack?" | `postxl-framework.md` |
| "Compare PostXL alone vs Skaile platform for my idea" | `skaile-platform.md` + `postxl-framework.md` |
| "Add AI to a PostXL app" | `skaile-agent-framework.md` + `postxl-framework.md` |
| "Build a brand-new product on Skaile" | usually `skaile-ecosystem.md` first, then narrow |

When `scope` input is not `auto`, load only what `scope` indicates. When
`scope=all`, load all four.

### Step 2 -- Load the chosen references

Read each selected `references/<name>.md` in full. Do not paraphrase
upstream until you have read what is there. The packs are written so
that one read suffices -- they include decision rubrics, decision
trees, and concrete app-shape -> solution mappings.

### Step 3 -- Apply the rubrics, do not freelance

Each pack ships with explicit decision rubrics:

- `skaile-agent-framework.md` Section 5 -- 8 questions for picking an
  integration tier
- `skaile-ecosystem.md` Section 8 -- 12 questions for the whole
  ecosystem
- `skaile-platform.md` Section 11 -- decision tree for the five
  extension layers
- `postxl-framework.md` Section 10 -- 7-question rubric for PostXL fit

Walk the user's idea through the relevant rubric out loud. Do not
shortcut to a recommendation without naming the rubric and the answers.

### Step 4 -- Map to concrete shapes

Each pack has a "patterns by app shape" or equivalent table. After the
rubric, pick the matching shape:

- `skaile-agent-framework.md` Section 10 -- common app shapes
- `skaile-ecosystem.md` Section 7 -- patterns by app shape
- `skaile-platform.md` Section 12 -- 10 idea -> platform-shape mappings
- `postxl-framework.md` Sections 8/9 -- when PostXL fits / does not fit

### Step 5 -- Surface trade-offs explicitly

When the answer spans multiple stacks (which it often does -- e.g.
"Skaile platform with a custom AAP component vs forge L5-concept app"),
list the trade-offs in a short table before recommending. Do not pick
for the user without showing the choice.

### Step 6 -- Point to deeper reading

Each pack ends with a "Pointers" section linking to deeper docs in the
upstream repos (skaile-dev, postxl). Forward those links when the user
needs more depth than a context pack provides.

## Output Conventions

Structure your response in this order:

1. **Restate the question** in one sentence so the user can verify you
   classified it correctly.
2. **State which references you loaded** and why (one sentence each).
3. **Walk the rubric**: name it, list the questions, give the answers
   for the user's idea.
4. **Map to a shape**: pick from the relevant patterns table.
5. **Surface trade-offs** (if the answer spans stacks).
6. **Recommend** with a one-paragraph rationale.
7. **Point to deeper docs** for follow-up.

Skip steps that do not apply; do not pad. The user wants a stack-literate
answer, not an essay.

## Anti-Patterns

- **Loading all four packs by default.** Each is 200-700 lines. Pick
  with intent. Use `scope=all` only when the user explicitly asks for
  full coverage.
- **Paraphrasing the rubric instead of running it.** The rubrics exist
  to be applied, not summarized. Walk the user's idea through them.
- **Recommending PostXL or the platform without checking fit.** Both
  have explicit weaknesses sections. Honor them.
- **Forgetting the cross-stack option.** Many ideas land at "Skaile
  platform extending a PostXL-generated codebase" or "forge app using
  the agent framework with mounts to a PostXL backend." Do not
  prematurely collapse to a single stack.
- **Answering from training data.** The packs reflect the current state
  of the codebase. If a question references a feature the packs do not
  cover, say so and suggest reading the upstream CLAUDE.md.

## References

| File | What it covers |
|---|---|
| `references/skaile-agent-framework.md` | Agent runtime stack: drivers, sessions, mounts, connectors, flows, integration tiers |
| `references/skaile-ecosystem.md` | Full skaile-dev monorepo: forge ladder, ai-assets, ai-assets-skaile, ai-assets-skaileup, platform overview |
| `references/skaile-platform.md` | Enterprise platform deep dive: skills+flows, AAP components, app actions, governance, protocol extensions |
| `references/postxl-framework.md` | PostXL code generator: schema model, generated stack, decision rubric, customization model |

## Maintenance

Sources of the four references (re-sync when upstream changes):

| Source (upstream repo) | Destination here |
|---|---|
| `skaile-dev/agent-framework/AGENT-CONTEXT.md` | `references/skaile-agent-framework.md` |
| `skaile-dev/AGENT-CONTEXT.md` | `references/skaile-ecosystem.md` |
| `skaile-dev/platform/AGENT-CONTEXT.md` | `references/skaile-platform.md` |
| `pxl/docs/postxl-for-agents.md` | `references/postxl-framework.md` |

After copying, re-apply the cross-reference rewrites so each pack
references the others by their `references/` filename (which matches
the upstream rewrite already done in `~/repos/system/`).
