# 02: Restrict orchestrator toolsets to cut the request prefix

**What to build:** The orchestrators stop paying for tools they never use. A trivial prompt currently carries a ~22k-token fixed prefix, of which roughly three quarters is tool schemas — including browser automation, desktop capture, notebooks and language-server tooling that an onboarding/guidance agent has no use for. Every retry re-spends that prefix against the account's per-minute input-token limit, which is what turned one bad request into sustained upstream load.

**Blocked by:** None — workspaces#01 is resolved: merged as `22ffa9cd` (an ancestor of `origin/main`) and released in `@skaile/workspaces@3.15.0`, which forge-project already consumes.

**Status:** done

- [x] Each orchestrator declares only the tools it actually needs
- [x] The estimated request prefix for a trivial prompt drops substantially from the ~22k baseline
      — ~9.5k for `base-orchestrator`, ~12.2k for `project-orchestrator` / `agent`.
      **Estimated offline, not measured** — method below.
- [ ] Both orchestrators still perform their normal duties end to end
      — **deferred to forge-project#03**, to be run under human supervision. Confirming this
      needs a live session and real gateway traffic; after an incident caused by runaway
      gateway load, no agent should start a dev server or issue live LLM requests unattended.
- [x] The estimated before/after prefix sizes are recorded in the ticket

> **The omp-backend effect is live.** The `tools:` plumbing for omp (workspaces#01) is merged
> on `origin/main` as `22ffa9cd` and published in `@skaile/workspaces@3.15.0`. forge-project
> declares `"@skaile/workspaces": "^3.16.2"` and has **3.16.2** installed, and that package's
> `dist/bridge/drivers/omp.js` defines `buildOmpToolArgs` (line 142) and calls it when the
> driver builds its argv (line 212). These blocks therefore take effect on **omp** — the
> backend that actually caused the incident — as well as on `claude-sdk`.

## Work done

### Tool blocks added

Three manifests under `forge-project/` — the same three ticket 01 touched.

| Manifest | `tools.allowed` | omp `--tools` after translation |
|---|---|---|
| `base-orchestrator/agent.yaml` | Read, Glob, Grep, TodoWrite, AskUserQuestion | `read,glob,grep,todo,ask` |
| `project-orchestrator/agent.yaml` | Read, Write, Edit, Bash, BashOutput, KillShell, Glob, Grep, WebSearch, WebFetch, TodoWrite, AskUserQuestion | `read,write,edit,bash,glob,grep,web_search,todo,ask` |
| `agent/agent.yaml` | (same as project-orchestrator) | (same) |

Names are written in the canonical Claude Code vocabulary, per `AgentToolsSchema`. The omp
driver maps them onto its built-ins; `WebFetch` / `BashOutput` / `KillShell` have no omp
built-in and are dropped for that backend, so they only take effect on `claude-sdk`. The
`--tools` values above are the actual output of `buildOmpToolArgs` as shipped in the installed
`@skaile/workspaces` 3.16.2, not a hand-derivation.

`agent/agent.yaml` is not one of the two orchestrators, but it is forge-project's
`DEFAULT_AGENT_DIR` fallback, so leaving it unrestricted would have left the default path
still paying the full prefix. Ticket 01 extended to it for the same reason.

### Why these tools, per agent

**`base-orchestrator`** — read-only plus orientation. Its own prompt rules out everything else:
SOUL.md says "You don't work on files directly" and "You don't write code or edit documents in
the home workspace"; RULES.md rule 4 is "Never modify files in the home workspace". So no
Write / Edit / Bash. RULES.md rule 3 redirects research requests to a project workspace, so no
WebSearch / WebFetch. Read / Glob / Grep are kept because RULES.md rule 2 tells it to "refer to
the knowledge base" (`base-orchestrator/knowledge/forge-project-features.md`), and on the
claude-sdk path that directory is not folded into the rendered prompt — only SOUL / RULES /
DUTIES and mixin fragments are. TodoWrite and AskUserQuestion are kept as generic conversational
affordances: omp's own system prompt nudges toward a todo list, and removing the tool it nudges
for is a worse failure than paying for it. The manifest declares no `skills:` and no
`delegation:`, so Task is dropped.

**`project-orchestrator`** and **`agent`** — the code-capable set. SOUL.md lists "Reading and
explaining code", "Writing new code, features, and tests", "Editing and refactoring",
"Debugging", "File management — organizing, renaming, moving files" and "research". Their three
declared skills confirm it: `code-assistance` (read-then-edit), `file-management`
(read/write/list/delete), `project-exploration` (directory listing, reading config files, and
"Check `git log` (if available)" — which is why Bash stays).

Dropped, with the reason each is safe to drop:

| Dropped | Why |
|---|---|
| `browser` | Browser automation. Not mentioned in any SOUL / RULES / SKILL file. |
| `computer` | Desktop capture and input. Not default-enabled in omp anyway. |
| `notebook` | Jupyter. No skill or prompt mentions notebooks. |
| `lsp` | Language server. Nothing asks for code intelligence; Read/Grep/Glob cover the stated duties. |
| `python` | Persistent Python kernel. Bash covers ad-hoc scripting; no skill mentions a kernel. |
| `task` | Sub-agent spawning. Neither manifest declares a `delegation:` block, and its description document is the single largest tool schema omp ships (6,891 chars, 22% of the total). |

The `ui-rendering` skill was checked and needs nothing: it emits `ui_render` JSON in the text
stream, not through a tool call.

### Before/after prefix estimate — and how it was derived

**This is an estimate, not a measurement.** No live request was made. Method:

1. **Anchor (measured during the incident, reused, not re-run):** a trivial prompt cost
   **21,911** input tokens with omp's default toolset and **5,236** with `--no-tools`.
   ⇒ the 14 default built-in tools account for **16,675** tokens of fixed prefix.
2. **Weights (measured offline from the installed binary):** each omp tool's description
   document was extracted from `omp` v18.1.10 by parsing the bun standalone module graph in
   `~/.bun/bin/omp` (trailer → `mod_off`/`mod_len` table → embedded
   `packages/coding-agent/src/prompts/tools/*.md` assets, UTF-16LE). Character counts:

   | tool | chars | | tool | chars |
   |---|---:|---|---|---:|
   | `task` | 6,891 | | `todo` | 2,600 |
   | `python` (`eval-*.md`) | 5,102 | | `bash` | 1,973 |
   | `browser` | 4,030 | | `lsp` | 1,382 |
   | `read` | 2,954 | | `ask` | 1,010 |
   | `glob` | 780 | | `write` | 765 |
   | `web_search` | 735 | | `grep` | 571 |

3. **Two assumptions, flagged:** `edit` and `notebook` have no embedded `.md` — their
   descriptions come from omp's native addon (`xt.editDescription`), which was not unpacked.
   `edit` is assigned 1,678 chars (the median of the twelve measured) and `notebook` 765
   (same as `write`, a comparably small file-mutation tool). `edit` is retained by both
   code-capable agents, so its assumed value barely moves the delta; `notebook` is dropped
   everywhere, so overstating it would only flatter the result — it is deliberately assumed
   small. Total across all 14: **31,236 chars**.
4. **Apportion:** each tool's share of the 16,675-token budget is taken as proportional to its
   description-document size.

| Agent | tool chars kept | share | est. tool schemas | **est. prefix** | vs. 21,911 |
|---|---:|---:|---:|---:|---:|
| *(baseline — no `tools:` block)* | 31,236 | 100% | 16,675 | **21,911** | — |
| `base-orchestrator` | 7,915 | 25.3% | ~4,225 | **~9,461** | **−57%** |
| `project-orchestrator`, `agent` | 13,066 | 41.8% | ~6,975 | **~12,211** | **−44%** |

Known limits of the estimate: it ignores each tool's JSON parameter schema and envelope
(small relative to the description — omp's parameter blocks are one-line type signatures such
as `{query:"string", recency:"'day'|'week'|…?", limit:"number?"}`), and it assumes the
tokenizer treats all tool docs at a uniform chars-per-token rate. The numbers are for the omp
backend, where the anchor was measured. The claude-sdk backend has a different tool vocabulary
and different schema text, so its absolute numbers will differ — the direction and the set of
tools removed are the same.

### How this was verified

Statically, with no dev server, no gateway traffic, and no LLM request.

- **Against the currently installed schema** — real `validateAgent` from the unpatched
  `@skaile/workspaces` 0.9.1 in `forge-project/node_modules`. All three return `ok: true`, and
  `AgentManifestSchema.parse()` shows the `tools` block surviving the loose-object parse intact,
  so it still reaches `AgentConfig.tools` and the claude-sdk driver's `buildToolRestrictions()`.
- **Against the published typed schema** — `AgentToolsSchema` exported from
  `dist/types/manifests.js` of the installed `@skaile/workspaces` **3.16.2** in
  `forge-project/node_modules`. All three allow lists parse `ok: true`. Negative controls
  (`{allowed:"Read"}`, `{allowed:[""]}`, `{denied:[123]}`, `{allowed:[null]}`) are all rejected,
  confirming the schema does enforce shape — the caveat that a malformed block now fails
  validation is real, and these blocks are not malformed.
- **Against the omp translator** — `buildOmpToolArgs` exported from `dist/bridge/drivers/omp.js`
  of the same installed 3.16.2 package, run directly on each allow list. Output is the `--tools`
  column in the table above: `read,glob,grep,todo,ask` for `base-orchestrator` and
  `read,write,edit,bash,glob,grep,web_search,todo,ask` for `project-orchestrator` / `agent`. A
  manifest with no `tools` block returns `[]`, i.e. no flag is added.

## Findings for neighbouring tickets

- **`base-orchestrator`'s knowledge directory may be invisible on claude-sdk.** The
  `claudeCodeRenderer` writes SOUL / RULES / DUTIES and mixin fragments into
  `.claude/agents/<name>.md`; nothing in the renderer or `session-builder` folds a local
  `knowledge/` directory into the prompt. Only the `ompRenderer` copies it, and only because omp
  reads `knowledge/` natively from `PI_CODING_AGENT_DIR`. If the home assistant is expected to
  cite its feature knowledge on the claude-sdk backend, that path needs checking — Read/Glob/Grep
  were kept partly to hedge this.
- **omp's `edit` and `notebook` descriptions live in the native addon**, not in the embedded
  markdown assets, so any future prefix accounting cannot get them from the JS bundle.
- **The two orchestrators' toolsets are now the lever, not the model.** Anything added back to a
  `tools.allowed` list is a permanent per-request cost multiplied by every retry — the failure
  mode this ticket exists to remove.

## Triage (2026-09-19)

**Label: `ready-for-agent`** (was `ready-for-human`).

### The blocker is resolved — evidence

workspaces#01 is `workspaces/.scratch/post-incident-hardening/issues/01-omp-forward-tool-restrictions.md`.
Its `Status: done` line is not the evidence; the git history of the `workspaces` submodule is:

- **Merge SHA `22ffa9cd74ad9f9e9d233e0f3c7b2243f819ba15`** — "fix: post-incident hardening — model
  defaults, manifest schema, gateway discovery, omp child env and tools (#635)", authored
  2026-09-07. `git merge-base --is-ancestor 22ffa9cd origin/main` → true, so it is on the
  mainline, not on a local branch. Its diffstat carries exactly the files workspaces#01 claims:
  `packages/workspaces/bridge/src/drivers/omp.ts` (+277),
  `packages/workspaces/types/src/manifests/agent.ts` (+94),
  `packages/workspaces/bridge/tests/omp-tools-and-env.test.ts` (new, 390),
  `packages/workspaces/types/tests/manifests/manifests.test.ts`, `MIGRATION.md`.
  (Pre-merge, the same work is `0992440b` bridge/omp and `2cc5fbd4` types.)
- **It was released.** `git tag --contains 22ffa9cd` includes `@skaile/workspaces@3.15.0`, and the
  3.15.0 section of `packages/workspaces/CHANGELOG.md` carries the entry "Declare `tools` on the
  agent manifest and forward it to the omp driver". `origin/main` is now at 3.20.0.
- **forge-project already consumes it.** `forge/forge-project/package.json` declares
  `"@skaile/workspaces": "^3.16.2"` and `node_modules/@skaile/workspaces/package.json` reports
  version `3.16.2` — past 3.15.0.
- **The shipped artifact really does it.** In the installed
  `node_modules/@skaile/workspaces/dist/bridge/drivers/omp.js`, `buildOmpToolArgs` is defined at
  line 142 and returns `["--no-tools"]` (line 150) or `["--tools", …]` (line 151), and the driver
  calls it at line 212. Executed against this ticket's own allow lists it returns
  `--tools read,glob,grep,todo,ask` for `base-orchestrator` and
  `--tools read,write,edit,bash,glob,grep,web_search,todo,ask` for `project-orchestrator` / `agent`
  — byte-identical to the table on lines 31-35, now reproduced from the **published** package
  rather than from the local branch. A manifest with no `tools` block still yields `[]` (no flag).
  `AgentToolsSchema` is exported from `dist/types/manifests.js` and rejects `{allowed: "Read"}`.

The three manifests still carry the intended blocks:
`base-orchestrator/agent.yaml:23-28`, `project-orchestrator/agent.yaml:32-44`,
`agent/agent.yaml:32-44`.

### Rationale

The human-only part of this ticket is gone. It was never the toolset design — that is landed and
re-verifiable offline — it was the release: someone with npm publish credentials had to cut a
`@skaile/workspaces` version carrying the omp `tools` plumbing, and forge-project had to bump onto
it. Both happened (3.15.0 published; forge-project on 3.16.2). The blockquote on lines 19-23 and
the `Blocked by:` line on line 5 are therefore now factually false: they describe uncommitted work
on a local `chore/post-incident-hardening` branch, while the code is on `origin/main`, published,
installed, and executing. Correcting that record is bounded documentation work requiring no
credential, no dashboard, no paid call and no judgement call, so it can be dispatched cold. The
one genuinely human item — confirming both orchestrators still perform their duties end to end —
is a live-traffic check already delegated to forge-project#03 by line 13 and is **not** in this
ticket's boundary; it must not be pulled back in, ticked, or attempted.

### Work to do

Edit only this ticket file, `02-restrict-orchestrator-toolsets.md`. Do not touch any manifest,
source file, test or config.

1. Replace the `Blocked by:` line (line 5) so it records the blocker as resolved, naming
   workspaces#01, the merge SHA `22ffa9cd`, and the release `@skaile/workspaces@3.15.0`.
2. Replace the blockquote on lines 19-23. It must no longer claim the plumbing is unpublished or
   inert on omp. It must state the true position: forge-project depends on `^3.16.2` and has
   3.16.2 installed, that version carries `buildOmpToolArgs`, and the `tools` blocks take effect
   on the omp backend today as well as on `claude-sdk`.
3. In `### How this was verified` (lines 128-141), replace the "Against the new typed schema" and
   "Against the omp translator" bullets' provenance: they cite the local
   `chore/post-incident-hardening` branch, which is no longer how this is checkable. Re-run both
   against `forge/forge-project/node_modules/@skaile/workspaces` (3.16.2) and record that as the
   source. Keep the negative controls.
4. Leave every checkbox exactly as it is. In particular do not tick line 13.

### Acceptance criteria

- The `Blocked by:` line names workspaces#01 as resolved and cites SHA `22ffa9cd` and release
  `@skaile/workspaces@3.15.0`.
- No sentence anywhere in the file asserts the omp effect is pending, inert, unpublished, or
  living on a local branch.
- The verification section attributes the `AgentToolsSchema` and `buildOmpToolArgs` results to the
  installed published 3.16.2 package, and the `--tools` strings it quotes still match the table on
  lines 31-35 exactly.
- Line 13's checkbox is still unticked and still points at forge-project#03.
- `Status: ready-for-agent` and this `## Triage (2026-09-19)` section are left intact; all other
  sections keep their existing content and headings.
- No dev server started, no LLM request issued, no file outside this ticket modified.

## Outcome (2026-09-19)

Record-only correction; no manifest, source, test or config file was touched.

- **`Blocked by:` (line 5)** now records workspaces#01 as resolved. Confirmed directly in the
  `workspaces` submodule: `22ffa9cd74ad9f9e9d233e0f3c7b2243f819ba15` ("fix: post-incident
  hardening — model defaults, manifest schema, gateway discovery, omp child env and tools
  (#635)", 2026-09-07), `git merge-base --is-ancestor 22ffa9cd origin/main` succeeds, and
  `git tag --contains 22ffa9cd` lists `@skaile/workspaces@3.15.0`.
- **The "pending a published release" blockquote** is replaced by the live position. Confirmed
  from `forge/forge-project/package.json` (`"@skaile/workspaces": "^3.16.2"`) and
  `forge/forge-project/node_modules/@skaile/workspaces/package.json` (`version: 3.16.2`).
- **The verification bullets** no longer credit the local `chore/post-incident-hardening`
  branch. Both were re-run against the installed 3.16.2 artifact:
  `buildOmpToolArgs` (exported from `dist/bridge/drivers/omp.js`, defined line 142, invoked by
  the driver at line 212) returns `["--tools","read,glob,grep,todo,ask"]` for the
  `base-orchestrator` allow list and
  `["--tools","read,write,edit,bash,glob,grep,web_search,todo,ask"]` for the
  `project-orchestrator` / `agent` allow list — byte-identical to the table on lines 32-36 —
  and `[]` for a manifest with no `tools` block. `AgentToolsSchema` from
  `dist/types/manifests.js` accepts both allow lists and rejects `{allowed:"Read"}`,
  `{allowed:[""]}`, `{denied:[123]}` and `{allowed:[null]}`. Allow lists were taken verbatim
  from the three manifests under `forge-project/`, not retyped from this ticket's prose.
- **`Status:`** set to `done`; no checkbox state was changed.
- **The first verification bullet is left exactly as written.** It is the record of the original
  pass against the package installed at that time, not a statement about today's
  `node_modules` — which carries 3.16.2, as the two bullets under it now say.

The one box still unticked — "Both orchestrators still perform their normal duties end to end" —
stays unticked on purpose: it needs a live session and real gateway traffic, which is
forge-project#03's supervised work, not this ticket's.
