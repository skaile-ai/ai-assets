# Agent Action Catalog — `platform.act`

`platform.act` is default-deny. This reference documents its **sole allowlisted action**;
it is not a data-model CRUD catalog. Prefer a dedicated capability whenever one exists.

## Allowed request

`{ scope: "project", type: "markAllSessionsRead", payload: { id: "<projectId>" }, rationale }`

- `payload.id` is the canonical target project id.
- `rationale` must state why clearing the unread indicators is useful.
- The effect resets unread indicators for every member of every session in the target project.
- Risk is low/routine, target-scoped, and not eligible for batch execution.

## Approval and authorization

The platform prepares the request before it shows a card: it parses the exact payload,
resolves the canonical project and organization ancestry, writes the approval description,
and authorizes the current **session owner** on the target project. Malformed or unknown
requests fail without a card.

Project `User` or `Owner` is required. An explicit ProjectMember or team `Viewer`
role wins over a broader organization role and is denied; no target-project access is also
denied. PlatformAdmin is the explicit break-glass role.

Approval records consent only. The person who clicks Approve does not become the actor.
Immediately before execution the platform reloads the current session owner and target,
verifies the prepared request and policy version, and recomputes the target-project role.

## Everything else is unavailable

Do not construct generic create, update, delete, lifecycle, membership,
credential/provider-link, runtime-internal, or destructive actions through `platform.act`.
Every unlisted scope/type pair is blocked. `platform.act_batch` is a separate legacy
capability and this reference does not authorize or describe batch actions.
