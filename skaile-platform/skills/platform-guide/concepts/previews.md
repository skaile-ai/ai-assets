# App Previews

A session's workspace can contain a runnable app (or up to 8 apps). The platform can
build and serve them so the user sees a live preview in the workspace, without leaving the
platform.

## What the user experiences

- The workspace has a **preview** pane. Starting a preview builds and launches the app in a
  separate container and loads its URL in an embedded iframe.
- When a workspace exposes more than one app, a tab strip lets the user switch between them.
- Each app moves independently through `building -> starting -> ready -> stopped`. The user
  can start, stop, and refresh a preview (refresh = stop + start).

## What makes a workspace previewable

For an app to be previewable, its files must satisfy the **preview contract** — broadly: a
way to run it (a Dockerfile / Dockerfile.preview / static files / an npm script), a health
check, and optionally a `skaile.preview.json` declaring the app(s). If the user asks "why
can't I preview this", the answer is usually that the workspace does not yet meet the
contract — the agent can help create the missing pieces.

**Full-stack apps (separate frontend + backend) are fully supported — this is not a
static-HTML-only feature.** Never tell a user that previews only work for static
HTML/images; that is false, and it is only the lowest-priority fallback mode. Two ways to
wire up a frontend+backend app:

- **Auto-discovery**: `frontend/` and `backend/` directories at the workspace root are
  detected and run as independent sibling containers automatically — no config file needed.
- **Explicit `skaile.preview.json`**: declare each app's `path`, `role`
  (`frontend`/`backend`), and `port` — needed for non-default ports, more than two apps, or
  when an app lives at a nested path.

**The contract is checked at the session workspace root — never at an arbitrary nested
path.** If the agent scaffolds the generated project into a subdirectory (e.g. `app/`)
instead of putting it directly at the workspace root, detection will correctly report "no
preview contract detected" even though the app runs fine — that is a scaffolding mistake,
not a platform limitation. Fix it by scaffolding at the workspace root, or by adding a
`skaile.preview.json` at the root whose per-app `path` points into the subdirectory (e.g.
`"path": "app/frontend/"`).

**Before concluding *any* platform limitation, read the actual failure text** —
`PreviewRuntime.lastError`, or the `reason` from `checkPreviewable`. It names the specific
missing piece (missing contract, wrong port, failed health check, etc.) and, when the app
was scaffolded one level too deep, will name the nested directory it found. Guess only as a
last resort, after reading that message.

## Agent-controllable apps (Skailify)

An app in the workspace can opt into the Skailify protocol (`protocol: true` in
`skaile.preview.json`). It then opens a secure back-connection to the platform and
registers its own actions as capabilities — the session agent can read the app's state
and operate it (move a card, create a record, change a view) on the user's behalf,
through the same approval flow as any other capability. To the user this means: "ask the
agent to do it in the app" works for Skailify-enabled apps. Whether a given app supports
this is discovered at runtime like all capabilities — never assume it.

## Practical notes for guiding users

- A preview runs against a **snapshot or live mount** of the session files — it is a
  sibling runtime, separate from the agent/session container. The agent edits the live
  workspace; the user refreshes the preview to see changes.
- Previews have resource caps (memory/CPU/PIDs) and are reconciled if orphaned.

Source of truth: `platform/docs/preview-contract.md`, `platform/docs/preview-lifecycle.md`.
