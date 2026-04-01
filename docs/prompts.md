---
title: Prompts
description: What a prompt is, how it differs from a skill, and how prompts are used in ai-assets.
---

A **prompt** is a reusable, standalone text fragment — a focused instruction or persona that can be injected into a larger context without being a full skill. Prompts are lighter-weight than skills: no `STEPS`, no `CHECKLIST`, no I/O declarations.

## Two Forms

### Standalone prompt file

A `*.prompt.md` file anywhere in a domain directory. Discovered by `arm` as `kind: prompt`:

```
speaker-persona-alex.prompt.md
```

```yaml
---
name: speaker-persona-alex
description: Podcast host persona — curious, concise, asks follow-up questions
type: prompt
---

You are Alex, a podcast host known for...
```

### SKILL.md with `type: prompt`

A skill directory whose frontmatter declares `type: prompt`. Treated by `arm` as a prompt rather than a skill, but can still have the full directory structure:

```yaml
---
name: tone-casual
type: prompt
description: Casual, conversational tone instruction for writing tasks
---

Write in a casual, conversational tone...
```

## Where Prompts Live

Domains that produce content (writing, podcasts, conceptualization) keep reusable prompt fragments in a `prompts/` subdirectory:

```
knowledge-writing/
└── prompts/
    ├── speaker-persona-host.prompt.md
    ├── speaker-persona-guest.prompt.md
    └── chapter-structure.prompt.md
```

## Prompts vs Skills

| | Prompt | Skill |
|---|---|---|
| Purpose | Reusable fragment, injected into context | Full agent instruction with procedure |
| Format | `*.prompt.md` or `type: prompt` in frontmatter | `SKILL.md` with STEPS/CHECKLIST |
| Executed by runner | No — composed into other prompts or agents | Yes — sent directly as `driver.prompt()` |
| Discovered by `arm` | Yes (`kind: prompt`) | Yes (`kind: skill`) |
| Has `reads_from` / `writes_to` | Rarely | Typically yes |

## Using Prompts

Prompts are referenced by skills in their prompt body or in `resources/`. They are not executed as flow nodes — a skill composes them, or an agent loads them as part of its imprint.

```bash
# Install a prompt
skaile add speaker-persona-host
```
