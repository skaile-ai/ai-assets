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
