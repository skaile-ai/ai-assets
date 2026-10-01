# Agent Action Catalogue — `platform.find_actions`, `platform.invoke`, `platform.batch`

Beyond its dedicated capabilities, the agent can run **platform actions**: the things a user
does in the Skaile UI that the platform has declared for agents. Each is one existing tRPC
procedure (or an adapted upload/download route) carrying a declaration — title, description,
keywords, target, risk. Undeclared, generated CRUD and browser-only procedures are never in it.
The catalogue grows every deploy, so **search it; never recall it.** The three capabilities are
offered in ordinary project sessions as well as the personal assistant. Prefer a dedicated
capability whenever one exists.

## Discover — `platform.find_actions({ query, limit? })`

A read-only lexical search: `query` is a few words of what you want ("mute notifications",
"rename session"), `limit` 1–20 (default 8). Each result carries `action` (the key), `title`,
`description`, `kind` (`read` or `write`) and `inputSchema`. An empty result means nothing fits
**on this turn**: rephrase once with other words, then tell the user. On a turn the platform
cannot attribute to anyone it is always empty — do not rephrase; say so in your output.
Results are filtered by who asked the turn (see *Shared sessions* below), never by whether a
specific target exists — so a result is not proof you may act on a given project.

Examples of what it finds today: starring and unstarring, renaming or describing a session,
marking every session in a project read, the owner's own notification preferences. Treat that
list as illustrative only. Catalogue actions always act on the **owner's** things: a member who
asks to change *their own* notification mode for this session needs the dedicated
`platform.set_notification_mode`, not a catalogue action.

## Run one — `platform.invoke({ action, input })`

`input` must match the result's `inputSchema`. It runs **as the session owner**, through the
procedure's own authorization, exactly as in the UI.

- **Every call asks the owner first — reads included** — unless a standing grant the owner made
  from an earlier card covers that action on that target. The card is generic: the action's
  title, its input, and a note when it is not idempotent. Grants reach that one target only (for
  an action on the owner's own settings, the calling session). So never tell the user a card
  will appear, nor that the call will run without one.
- **Refusals are uniform.** An unknown or undeclared key, a target that does not resolve or that
  someone cannot reach, a malformed or oversized input all read the same. Do not retry with
  other ids or keys. A procedure's own client error (bad input, conflict) comes back with its
  message.
- Each call runs at most once. A grant that stopped mid-session means the next call is carded,
  not that something broke.

## Several at once — `platform.batch({ steps })`

Up to 20 steps, run as one plan. Each step is `{ id, read: "<key>", input }` for a `read` action
or `{ id, invoke: "<key>", input }` for a `write` action. Any input value may be
`{ "$ref": ["<step id>", "<field>", ...] }` — that field of an earlier step's result. Step ids,
not indices; references point backwards only, and a read may reference only earlier reads.

- **Reads run first**, before anyone is asked, so a batch accepts only reads declared visible to
  every session reader. A read of the owner's private data is refused as a batch step in every
  session, the personal assistant included — run it with `platform.invoke`. Batch read results
  are not returned to you.
- **Consent is per step.** Each write is matched against its own action's standing grants. One
  card lists only the steps no grant covers; with none uncovered, there is no card. A step the
  policy would refuse outright refuses the whole batch. A batch is never granted whole.
- **Drift.** At run time the reads run again; if any answers differently from what the card
  showed, nothing runs (`batch_drifted`) — call `platform.batch` again.
- **Order and failure.** Writes run in order and stop at the first failure; the result names the
  `completed`, `failed` and `unexecuted` steps. Nothing is rolled back; a retry is a new batch.

## Files: pass a reference, never bytes

A file is named by reference, `{ sessionId?, resourceId?, path }` — `sessionId` defaults to this
session, `resourceId` to `workspace`, and `path` is mount-relative (`reports/Q3.pdf`, not
`workspace/reports/Q3.pdf`). The platform reads the bytes itself. Another session can be named
only from the personal assistant, only its workspace, and only one the owner can reach (find it
with `platform.search_my_sessions` / `platform.read_session_history`); that read is audited, and a
sleeping session is never woken for it — if it cannot be read, ask the owner to open it.

Two catalogue actions move files, at most 10 MiB each, and the bytes never reach you:

- a **download** writes the stored file into `downloads/` of this session's workspace (a taken
  name gets a numbered sibling) and returns `{ file: { sessionId, resourceId, path }, name, size,
  contentType }` — open it (`platform.open_file`) or attach it by that path;
- an **upload** takes `file: { sessionId?, path }`; a file from another organization is refused.
  The card shows the reference, not the content.

`platform.add_draft_attachment` takes the same reference — see
`references/exchange-mail-calendar.md`.

## Shared sessions

The platform records who set off each turn; the agent cannot set it. All three capabilities key
on it:

- **Grants cover only the owner's own turns** (and triggers the owner set up). A member's turn,
  several members at once, or an unattributable turn always gets a card.
- `find_actions` hides actions on the owner's own things from anyone but the owner, and shows
  nothing on a turn it cannot attribute.
- When a member asks, they must be able to do it themselves as well; the owner's reach is not
  enough. Actions that would act *as the asker* are not runnable through `invoke` yet.
- An action on the owner's own things is decided by the owner only; one on a shared project or
  session can also be decided by a co-owner.

Grounded in: `platform/docs/agent-action-declarations.md`,
`platform/docs/personal-assistant-control-plane.md` §4 and §5.7–5.8,
`platform/decisions/2026-09-30-agent-action-catalogue.md`, and
`platform/backend/libs/capabilities/src/handlers/` (`find-actions.handler.ts`,
`invoke.handler.ts`, `batch.handler.ts`) and `resource-reference.ts`.
