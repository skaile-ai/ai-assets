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

## Practical notes for guiding users

- A preview runs against a **snapshot or live mount** of the session files — it is a
  sibling runtime, separate from the agent/session container. The agent edits the live
  workspace; the user refreshes the preview to see changes.
- Previews have resource caps (memory/CPU/PIDs) and are reconciled if orphaned.

Source of truth: `platform/docs/preview-contract.md`, `platform/docs/preview-lifecycle.md`.
