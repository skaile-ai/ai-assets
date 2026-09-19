# 01: Repoint orchestrator model pins and revisit timeouts

**What to build:** The forge-project orchestrators no longer pin a model that cannot serve their own request size. Three manifests under the forge-project asset tree reference the implicated model — two pin it as `preferred`, a third carries it as a `fallback`. Its per-minute input-token limit is smaller than the prefix these agents send, so requests can never succeed by retrying — this caused a sustained upstream retry loop. Note the manifest validator rejects current single-digit-major IDs, so use a family alias unless workspaces#03 has landed. The runtime timeouts on these agents also match the observed request-drop window and should be reviewed in the same pass.

**Blocked by:** None (can start immediately)

**Status:** ready-for-human

- [x] No manifest under the forge-project asset tree names the implicated model, as `preferred` or as `fallback`
- [x] The chosen model ID validates against the manifest schema as it exists today
- [x] Runtime timeouts have been reviewed against realistic request duration and either changed or explicitly justified
- [ ] A session started from each affected agent resolves the intended model
      — **deferred to forge-project#03**, to be run under human supervision. Verifying
      this requires a live app and real gateway traffic; after an incident caused by
      runaway gateway load, no agent should start a dev server or issue live LLM
      requests unattended. Everything else here was verified statically.

## Work done

### Manifests changed

All three under `forge-project/`:

| Manifest | `model.preferred` | `model.fallback` | `runtime.timeout` |
|---|---|---|---|
| `agent/agent.yaml` | `claude-opus-4-6` → `opus` | dropped `claude-sonnet-4-6`; kept `claude-haiku-4-5-20251001` | 600 → 1800 |
| `base-orchestrator/agent.yaml` | `claude-sonnet-4-6` → `opus` | unchanged (`claude-haiku-4-5-20251001`) | 300 → 900 |
| `project-orchestrator/agent.yaml` | `claude-sonnet-4-6` → `opus` | unchanged (`claude-haiku-4-5-20251001`) | 600 → 1800 |

`claude-sonnet-4-6` no longer appears anywhere under `forge-project/`.

### Why the `opus` alias, and why `agent/agent.yaml` moved too

`claude-opus-5` — the model forge-project's own `DEFAULT_MODELS` now targets — **fails**
`ModelIdSchema` today: `ANTHROPIC_FULL_ID_PATTERN` requires a two-segment version, so a
single-digit major has no second segment to match. The family alias `opus` is in
`ANTHROPIC_MODEL_ALIASES` and passes unconditionally, so the manifests validate against
the schema as it exists now and stay valid however workspaces#03 lands. The alias also
auto-rolls to the current top Opus model, so this cannot go stale again.

`agent/agent.yaml`'s `preferred` was `claude-opus-4-6` — not the implicated model, and it
does validate, but it is a hard pin to a release that `DEFAULT_MODELS` implies is
superseded. Leaving it was leaving the same latent failure mode (a pin to a model the
gateway may not serve), so it was moved to the alias for consistency with the other two.

The `claude-haiku-4-5-20251001` fallbacks were deliberately **left as full IDs**. They are
not implicated, they validate, and — unlike an alias — they are resolvable verbatim by
every backend. Keeping one literal ID in each fallback list means that if any backend
fails to resolve the `opus` alias, there is still a concrete model for it to fall back to.

### Timeout decision

Changed, not just justified. The old values gave every one of these agents a **6 s/turn**
budget (100 turns / 600 s; 50 turns / 300 s), which is not a realistic request duration:
a single call carrying the ~22k-token prefix plus tool round-trips runs tens of seconds on
its own, so even a handful of turns blew the budget.

That is the wrong direction for this incident. A timeout that fires mid-flight does not
reduce upstream load — it abandons an in-flight request that the gateway has already paid
the full input-token cost for, and the client then retries and re-spends the same ~22k
prefix. A too-short timeout is an amplifier of the retry loop, not a guard against it.
The guard against runaway load is `max_turns` plus the concurrency guard added in
forge-project, both of which stay as they were.

New values restore the house convention of ~18 s/turn, taken from this repo's own
canonical example in `docs/agents.md` (`max_turns: 100` ↔ `timeout: 1800`) and consistent
with the healthiest peer manifest, `skaile-development` (150 turns / 2400 s ≈ 16 s/turn):

- 100-turn agents (`agent`, `project-orchestrator`) → **1800 s**, matching the documented pairing exactly.
- 50-turn agent (`base-orchestrator`) → **900 s**, the same 18 s/turn budget scaled to its turn count.

### How this was verified

Statically, with no dev server and no gateway traffic. Each edited manifest was parsed and
run through the real `validateAgent` from the **installed, unpatched** `@skaile/workspaces`
0.9.1 in `forge-project/node_modules` — i.e. the schema as it exists today, not the local
`workspaces` checkout and not a post-workspaces#03 version. All three return `ok: true`.

A negative control was run through the same harness to prove it actually rejects things:
`claude-opus-5`, `claude-sonnet-5` and `not-a-model` all return `ok: false`. This also
independently reconfirms the constraint that motivated the alias choice.

Note `claude-sonnet-4-6` itself still returns `ok: true` — it is a schema-valid ID. The
schema was never going to catch this; removing it from the manifests is the fix.

## Findings for neighbouring tickets

- **The `runtime.timeout` field may be inert.** Across the `workspaces` packages, only
  `runtime.max_turns` is consumed (`runner/src/session-builder.ts`, `asset-manager/src/renderers.ts`);
  `runtime.timeout` appears solely in the manifest schema, and the `claude-code` renderer maps
  `max_turns` but not `timeout`. The `omp` renderer does copy the whole `runtime` block verbatim
  into the deployed `agent.yaml`, so omp may honour it — unconfirmed. The new values are correct
  either way, but nobody should assume this field is what bounds a request.
- **The omp renderer's alias comment looks stale.** `asset-manager/src/renderers.ts` asserts omp
  "does not understand Claude Code aliases (`opus` / `sonnet` / `haiku`)". The installed omp
  (v18.1.10) documents `--model` as a *fuzzy match* and gives `"opus"` as its own example. Worth
  confirming before anyone acts on that comment.
- **A stale `claude-sonnet-4-6` remains outside this repo**, in forge-project's
  `server/utils/agent-manager.ts` (bootstrap `agent-config` default template). Out of scope here;
  flagged for the forge-project tickets.

## Triage (2026-09-19)

**Label: `ready-for-human`** (unchanged).

### Human action required

A human must run one live, supervised session per affected agent — `forge-project/agent`,
`forge-project/base-orchestrator`, `forge-project/project-orchestrator` — against the real
gateway and confirm the `opus` alias resolves to an actual Opus model on the **omp** backend,
because that is a billable request against the gateway this incident overloaded and is the one
check in this ticket that cannot be made offline.

### What is unblocked around it

Everything else. All three `[x]` boxes were re-verified independently of the ticket's own prose:

- **The implicated model is gone from the asset tree.** A search for `claude-sonnet-4-6` and
  `claude-opus-4-6` across `ai-assets/forge-project/` returns no matches, so the claim on line 30
  holds. The three manifests now read `model.preferred: "opus"` with a
  `claude-haiku-4-5-20251001` fallback and the stated timeouts —
  `base-orchestrator/agent.yaml:8,10,31` (50 turns / 900 s),
  `project-orchestrator/agent.yaml:8,10,51` and `agent/agent.yaml:8,10,51` (100 turns / 1800 s).
- **The chosen ID validates against the schema forge-project actually runs.**
  `ModelIdSchema` from the installed, published `@skaile/workspaces@3.16.2` in
  `forge/forge-project/node_modules` accepts `opus`, and rejects `not-a-model`.
- **The constraint that forced the alias has since been lifted, and it changes nothing.**
  workspaces#03 (`workspaces/.scratch/post-incident-hardening/issues/03-model-id-schema-current-ids.md`,
  `Status: done`) landed in workspaces commit `22ffa9cd` — an ancestor of `origin/main`, released
  as `@skaile/workspaces@3.15.0` (git tag `@skaile/workspaces@3.15.0` contains that SHA). Against
  the installed 3.16.2, `claude-opus-5` and `claude-sonnet-5` now validate. The alias was chosen
  deliberately to stay correct "however workspaces#03 lands" (line 38) and still auto-rolls, so it
  remains the better pin and **no rework follows from this**.
- **The cross-repo residue flagged on line 98 is already resolved.** `claude-sonnet-4-6` no longer
  appears in `forge/forge-project/server/utils/agent-manager.ts`; that bootstrap template now
  carries an explicit comment leaving `model:` unset (`agent-manager.ts:267-269`).

### Rationale

The label does not change, but the reason for it narrows sharply. Previously the ticket read as
human-gated partly because its own text treated the manifest-schema constraint as an open
cross-repo dependency; that dependency has since shipped and been consumed, and every static
claim in the ticket now re-verifies against the released package rather than against a local
branch. What survives is not a blocked dependency but an irreducibly human step: confirming that
a family alias resolves to a real model requires issuing a real request through the gateway, and
post-incident policy puts that under human supervision. It is deliberately not pulled back into
this ticket — line 12 already defers it to forge-project#03, and that routing stands. No agent
work remains inside this ticket's boundary, so `ready-for-agent` would be wrong; the residual step
is a supervised paid API call, which is exactly what `ready-for-human` denotes.

## Pre-flight (2026-09-20) — the run was staged, and would have proved nothing

Preparing the supervised session found that **no forge-project session was loading any
agent manifest at all**, so the live check as written could not have tested what it says.

- Every project forge-project creates writes `agent: definition: agent:<name>` into its
  workspace `skaile.yaml` (`agent-manager.ts:301,341,655`).
- `resolveAgentDir` in the installed `@skaile/workspaces@3.16.2` returns `undefined` for
  any `agent:`-prefixed definition — `dist/chunk-OZ7UNMTK.js:1887-1889`, pinned upstream
  as "not yet supported" (`core/tests/workspace-config.test.ts:514-518`). It resolves
  `ai-assets://` refs and relative paths only.
- forge-project then fell back to a default directory that does not exist under the
  super-repo layout, so `agentDefinitionExists` was false and the manifest branch at
  `agent-manager.ts:1695` never ran.

Consequence for this ticket: the three manifests pin `model.preferred: "opus"`, but that
value was never reaching a session. A prompt sent under the old behaviour would have
resolved its model from forge-project's own `DEFAULT_MODELS`, answered normally, and left
box 4 looking tickable while the alias question stayed untested. Nothing would have
errored — which is why this was worth catching before spending the turns, not after.

Fixed app-side in forge-project (`server/utils/agent-definition.ts`, `agentDirForDefinition`,
covered by `tests/server/agent-definition-resolve.test.ts`): `agent:<name>` now resolves by
indexing the agent tree on each manifest's declared `name:`, which matters because the
directory name and the declared name disagree for two of the three (`agent/` declares
`forge-project-assistant`). The upstream behaviour is filed as
`workspaces/.scratch/post-incident-hardening/issues/06-resolve-agent-dir-drops-agent-refs.md`.

Two projects were staged so one session per agent is reachable, since all four pre-existing
projects bind to `base-orchestrator`:

| Project | Agent definition | Resolves to |
|---|---|---|
| `base` (existing) | `agent:forge-project-base-orchestrator` | `forge-project/base-orchestrator` |
| `alias-check-agent` | `agent:forge-project-assistant` | `forge-project/agent` |
| `alias-check-project-orchestrator` | `agent:forge-project-orchestrator` | `forge-project/project-orchestrator` |

Status unchanged: `ready-for-human`. The residual step is still one supervised prompt per
agent against the real gateway — it is now a check that can actually fail.
