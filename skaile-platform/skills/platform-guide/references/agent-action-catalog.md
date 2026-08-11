# Agent Action Catalog — `platform.act` / `platform.act_batch`

`platform.act` is default-deny. This reference documents its **sole allowlisted action**;
it is not a data-model CRUD catalog. Prefer a dedicated capability whenever one exists.

## Allowed request

`{ scope: "project", type: "markAllSessionsRead", payload: { id: "<projectId>" }, rationale }`

- `payload.id` is the canonical target project id.
- `rationale` must state why clearing the unread indicators is useful.
- The effect resets unread indicators for every member of every session in the target project.
- Risk is low/routine and target-scoped. This is the sole descriptor eligible for batch
  execution.

## Allowed batch request

```json
{
  "rationale": "Why the complete batch is needed",
  "steps": [
    {
      "scope": "project",
      "type": "markAllSessionsRead",
      "payload": { "id": "<projectId>" },
      "rationale": "Optional reason for this step"
    }
  ]
}
```

- The batch contains 1–20 ordered steps, all from the same compatible
  `project-read-state` family.
- `payload.id` may use `{ "$ref": [earlierStepIndex, "id"] }`. The referenced `id` is a
  descriptor-declared string projected from the earlier canonical project target.
- References must point backward and name a declared output whose type matches the exact
  receiving field. The platform rejects the entire request before approval or execution
  if any shape, action, target, or reference is invalid.
- The proposal lists the normalized steps, symbolic dependencies, exact project targets,
  per-step and aggregate risk, and all consequences.

## Approval and authorization

The platform prepares the request before it shows a card: it parses the exact payload,
resolves the canonical project and organization ancestry, writes the approval description,
and authorizes the current **session owner** on the target project. Malformed or unknown
requests fail without a card.

Project `User` or `Owner` is required. An explicit ProjectMember or team `Viewer`
role wins over a broader organization role and is denied; no target-project access is also
denied. PlatformAdmin is the explicit break-glass role.

Approval records consent only. The person who clicks Approve does not become the actor.
Immediately before single-action execution—and before every individual batch step—the
platform reloads the current session owner and target, verifies the prepared request and
policy version, and recomputes the target-project role.

A batch requires an explicit approval card. Wildcard and standing grants do not authorize
`platform.act_batch`. Once approved, execution is ordered and best-effort: the first
failure stops the batch, the receipt identifies completed/failed/unexecuted steps, and no
completed effect is rolled back. Treat every retry as a new batch. Batch steps have no
process-local timeout because the dispatcher cannot cancel an in-flight effect: the batch
waits for the authoritative result rather than producing a false terminal receipt. A slow
step can therefore keep the batch pending; durable cancellation and recovery belong to the
operation workflow, not this capability.

## Everything else is unavailable

Do not construct generic create, update, delete, lifecycle, membership,
credential/provider-link, runtime-internal, or destructive actions through either generic
action capability. Every unlisted scope/type pair is blocked, and blocked workflows cannot
be made available by putting them in a batch.
