# 02: Restrict orchestrator toolsets to cut the request prefix

**What to build:** The orchestrators stop paying for tools they never use. A trivial prompt currently carries a ~22k-token fixed prefix, of which roughly three quarters is tool schemas — including browser automation, desktop capture, notebooks and language-server tooling that an onboarding/guidance agent has no use for. Every retry re-spends that prefix against the account's per-minute input-token limit, which is what turned one bad request into sustained upstream load.

**Blocked by:** workspaces#01 (for the omp backend; the Claude SDK path works today)

**Status:** ready-for-human

- [x] Each orchestrator declares only the tools it actually needs
- [x] The estimated request prefix for a trivial prompt drops substantially from the ~22k baseline
      — ~9.5k for `base-orchestrator`, ~12.2k for `project-orchestrator` / `agent`.
      **Estimated offline, not measured** — method below.
- [ ] Both orchestrators still perform their normal duties end to end
      — **deferred to forge-project#03**, to be run under human supervision. Confirming this
      needs a live session and real gateway traffic; after an incident caused by runaway
      gateway load, no agent should start a dev server or issue live LLM requests unattended.
- [x] The estimated before/after prefix sizes are recorded in the ticket

> **omp-backend effect is pending a published release.** The `tools:` plumbing for omp
> lives in workspaces#01, which exists only as uncommitted work on the local
> `chore/post-incident-hardening` branch and is **not published to npm**. Until a release
> carrying it ships and forge-project bumps to it, these blocks are **inert on omp** —
> the backend that actually caused the incident. On `claude-sdk` they take effect today.

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
`--tools` values above are the actual output of `buildOmpToolArgs` from the workspaces
branch, not a hand-derivation.

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
- **Against the new typed schema** — `AgentManifestSchema` / `AgentToolsSchema` from the local
  `chore/post-incident-hardening` branch of `workspaces`. All three parse `ok: true`. Negative
  controls (`{allowed:"Read"}`, `{allowed:[""]}`, `{denied:[123]}`, `{allowed:[null]}`) are all
  rejected, confirming the new schema does enforce shape — the caveat that a malformed block now
  fails validation is real, and these blocks are not malformed.
- **Against the omp translator** — `buildOmpToolArgs` from the same branch, run directly on each
  allow list. Output is the `--tools` column in the table above.

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
