---
name: techstack
description: "Use when the project brief exists and tech stack hasn't been chosen yet. Also when user says 'tech stack', 'framework', 'what should we build with', 'database choice'."
keywords: [techstack, framework, database, hosting, architecture, nuxt, next, primevue, radix]
user_inputs:
  dialog:
    - id: framework_experience
      label: "Framework experience"
      type: text
      required: false
      hint: "Vue, React, or starting fresh?"
    - id: platform
      label: "Platform"
      type: select
      required: true
      options: [web, mobile, api, desktop]
      hint: "What are you building?"
    - id: data_heavy
      label: "Data-heavy app?"
      type: boolean
      required: false
      hint: "Lots of tables, lists, sorting, and filtering?"
    - id: managed_vs_selfhosted
      label: "Hosting preference"
      type: select
      required: false
      options: [self-hosted, managed/cloud]
      hint: "Manage your own server or use hosted services?"
  files: []
---

# Tech Stack — Stack Selection

## Overview

The **techstack** skill is the Tech Stack Advisor. It helps the user choose the
right tools for their project through plain-language questions. It discovers
available stacks at runtime from `tech-stack/*/SKILL.md` rather than using
hardcoded presets, and recommends the best match based on the user's answers.

**Phase:** conceptualization / tech-stack
**Pipeline ID:** `cf_concept_techstack` (see `cf__shared/pipeline.json`)
**Writes to:** `_concept/05_techstack/`

## When to Use

- The project brief exists and no tech stack has been chosen yet
- The user says "tech stack", "framework", "what should we build with", "database choice"
- The user wants to change or re-evaluate their tech stack
- The orchestrator dispatches this after the project brief is approved

## When NOT to Use

- `_concept/05_techstack/stack.md` already exists and is approved — proceed with downstream skills
- No project brief exists yet — run **overview** first
- The user already has a codebase and just needs auditing — use **audit** instead

## Prerequisites

### HARD-GATE

`_concept/01_project/brief.md` must exist. If not:

> "No project brief found. Run the **overview** skill first."

### Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid paths
- `cf__shared/frontmatter.md` — stack.md frontmatter fields
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** `_concept/01_project/brief.md` must exist
**If gates fail:** Run `cf_concept_overview` first.
**On completion:** Present summary, then orchestrator suggests next steps.

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | `_concept/01_project/brief.md` | Yes |
| **Must read** | `tech-stack/*/SKILL.md` (all available) | Yes (to build the options list) |
| **Optional** | `_concept/03_features/**/*.md` _(fallback: skip_if_absent)_ | No (helps assess complexity) |
| **Optional** | `_concept/_research/onboarding-info.md` | No (skip questions already answered) |
| **Optional** | `_concept/_research/techstack/user_input.json` | No |
| **Never load** | `_concept/06_datamodel/`, `_concept/07_screens/`, source code | — |

## Available Stacks

Stacks are discovered at runtime from `tech-stack/*/SKILL.md`. Do not hardcode
stack details in this skill — always read from the `tech-stack/` directory.

### Discovery Steps

1. Scan `tech-stack/*/SKILL.md` — each subdirectory with a SKILL.md is an
   available stack
2. From each SKILL.md, read:
   - The **Identity table** (name, framework, UI library, backend, database, etc.)
   - The **"When to Use"** section (what project types this stack fits)
3. Build a comparison table of all discovered stacks to present to the user

### Presenting Options

After discovery, present the available stacks as a structured comparison:

> "Here are the stacks available for your project:
>
> | Stack | Framework | UI Library | Backend | Best For |
> |-------|-----------|------------|---------|----------|
> | nuxt-primevue | Nuxt 3 | PrimeVue | Directus | Data-heavy apps, admin panels |
> | nuxt-ui | Nuxt 3 | @nuxt/ui | Directus | Design-forward, branded products |
> | nextjs-radix | Next.js 15 | Radix UI | Directus | React ecosystem, Vercel deploy |
> | ... | ... | ... | ... | ... |
>
> (Table above is built from actual files in tech-stack/ — content may differ)"

## Workflow

### Step 1: Read Context

Read `_concept/01_project/brief.md` for the app description.
Read `_concept/03_features/**/*.md` (if they exist) to understand feature complexity.

**Check onboarding hints first:** If `_concept/_research/onboarding-info.md` exists,
read it before asking any questions. The user may have already specified a frontend
framework, component library, or backend preference during onboarding. Skip any
questions whose answers are already provided there.

### Step 2: Discover Available Stacks

Scan `tech-stack/*/SKILL.md`. For each found:
- Read Identity table and "When to Use" section
- Build the comparison table
- Note which stack ID corresponds to each option

### Step 3: Assess Complexity

Based on the brief, features, and discovered stacks, determine:

| Signal | Points to |
|--------|-----------|
| Many data tables, filters, sorting | Stack with rich component library (e.g., PrimeVue) |
| Custom brand, unique visual identity | Stack with unstyled primitives (e.g., Radix UI / @nuxt/ui) |
| Team uses React | Next.js-based stacks |
| Team uses Vue or no preference | Nuxt-based stacks |
| Needs admin panel / CMS | Stacks with Directus backend |
| Wants managed hosting, no server | Stacks with Supabase or Vercel-native hosting |
| Simple app, few features | Minimal stack (e.g., nuxt-minimal) |
| Real-time collaboration | Check individual tech-stack skills for WebSocket/realtime support |

### Step 4: Ask Plain-Language Questions

If user_inputs are not already provided, ask one question at a time.
Adjust the next question based on the answer.

| # | Question | Why |
|---|----------|-----|
| 1 | Do you or your team have experience with a framework? (Vue, React, or starting fresh?) | Picks the ecosystem |
| 2 | Is this a web app, mobile app, desktop, or API only? | Narrows platform |
| 3 | How data-heavy is your app? (lots of tables/lists vs mostly content/forms) | Component library choice |
| 4 | Do you need a content management system or admin panel? | Backend choice |
| 5 | Do you want to manage your own server, or prefer hosted/cloud? | Hosting + backend |
| 6 | Any budget constraints? (self-hosted = free, cloud = monthly cost) | Hosting tier |

### Step 5: Recommend

Based on answers, recommend the best matching `tech-stack/*` skill. Present it clearly:

> "Based on your answers, I recommend:
>
> **nuxt-primevue** — Nuxt 3 + PrimeVue + Directus on PostgreSQL.
>
> Why:
> - Your app has data tables and an admin panel — PrimeVue's DataTable + Directus CMS
> - You prefer self-hosted — no monthly cloud costs
> - No framework preference — Vue/Nuxt is the easiest ramp-up
>
> Want me to go with this, or would you like to change anything?"

If the user is technical and wants to customize, let them override any field.
If they choose a stack not in the available list, note it as a custom stack and
set `tech_stack_skill: custom`.

### Step 6: Write Stack File

```bash
mkdir -p _concept/05_techstack
```

**Output: `_concept/05_techstack/stack.md`**

The `tech_stack_skill:` field is **required** — it is the reference all downstream
skills use to find implementation recipes. It must match a directory name under
`tech-stack/` (e.g., `tech-stack/nuxt-primevue`).

```yaml
---
platform: web
frontend: Nuxt 3
ui_library: PrimeVue + Tailwind CSS
backend: Directus
database: PostgreSQL
auth: Directus Auth
hosting: self-hosted
package_manager: bun
css: Tailwind CSS 4
tech_stack_skill: tech-stack/nuxt-primevue
last_updated: YYYY-MM-DD
---

# Tech Stack

## Frontend: <value>
<Summary from tech-stack skill "When to Use" or Identity table>

## UI Library: <value>
<Why this component library fits the project>

## Backend: <value>
<Why this backend was chosen>

## Database: <value>
<Production readiness notes>

## Auth: <value>
<Auth method and key features>

## Hosting: <value>
<Hosting setup summary>

## Package Manager: <value>
<Why this package manager>

## Trade-offs Considered
<Always include — explain what was weighed against what>
```

### Step 7: Emit Events

```
[techstack] started
  run_id: <uuid>
  reads: 01_project/brief.md, 03_features/
  stacks_discovered: N

[techstack] checkpoint phase=stacks_discovered
  available_stacks: [nuxt-primevue, nuxt-ui, nextjs-radix, ...]

[techstack] checkpoint phase=preset_recommended
  tech_stack_skill: tech-stack/nuxt-primevue

[techstack] completed
  run_id: <uuid>
  tech_stack_skill: tech-stack/nuxt-primevue
  overrides: none
```

## Outputs

| File | Description |
|------|-------------|
| `_concept/05_techstack/stack.md` | Full tech stack definition with `tech_stack_skill:` reference and reasoning |

## Completion Summary

Present to user: files produced, key decisions made (which `tech-stack/*` skill was
selected and why), suggested next steps (which skills are now unblocked).

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Recommending without asking questions | The agent assumes it knows best from the brief alone | Always ask the user, even if the answer seems obvious. Preferences matter. |
| Hardcoding stack details in stack.md | The agent embeds the full preset block | Use `tech_stack_skill:` reference only — details live in `tech-stack/<id>/SKILL.md` |
| Omitting `tech_stack_skill:` from stack.md | The agent forgets the new required field | This field is mandatory — all downstream skills depend on it to find their recipes |
| Using stack-specific types in the output | The agent writes Prisma schemas or SQL | This skill writes stack.md only. Stack translation is a separate step in datamodel. |
| Skipping user review | The agent auto-approves the stack | Always present the summary and let the user review. Tech stack is a high-impact decision. |
| Not explaining trade-offs | The agent picks without reasoning | Always include a "Trade-offs Considered" section so the user understands the choices. |
| Not discovering stacks at runtime | The agent uses a hardcoded list | Scan `tech-stack/*/SKILL.md` every time — new stacks may have been added |

## Integration

- **Called by:** orchestrator or standalone (parallel track after project brief)
- **Reads from:** `_concept/01_project/brief.md`, optionally `_concept/03_features/`, `tech-stack/*/SKILL.md`
- **Feeds into:** `cf_concept_datamodel` (database constraints), `cf_concept_mock` (component library), `implement/scaffold` (scaffolding recipe), `implement/foundation` (theming/auth recipe), `implement/design` (component library mapping)
- **Feedback loops:** None inbound. Outbound: all downstream skills read `05_techstack/stack.md` and resolve `tech_stack_skill:` for stack-specific details.
