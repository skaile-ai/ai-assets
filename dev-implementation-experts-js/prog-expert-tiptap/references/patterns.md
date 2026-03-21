# Tiptap x Nuxt 3 Patterns

## 1. SSR & Hydration
- ALWAYS set `immediatelyRender: false` in `useEditor`.
- Wrap editor components in `<client-only>`.
- Isolate Tiptap logic in dedicated components to avoid polluting main layouts with browser-only dependencies.

## 2. Interactive Content (Custom Node Views)
- Use Vue components for complex interactivity (drag handles, form fields inside nodes).
- Leverage `NodeViewWrapper` and `NodeViewContent` for structural integrity.
- Define custom attributes in `addAttributes()` to keep node state serializable.

## 3. Real-time Collaboration (Yjs)
- Sync content using `ydoc` and a provider (e.g., Hocuspocus, Tiptap Cloud).
- **Package name**: `@tiptap/extension-collaboration-caret` (NOT `-cursor`). Export is `CollaborationCaret`.
- Use `CollaborationCaret` for presence indicators.
- Ensure the editor content is initialized from the Yjs document, not from a local string, once the connection is established.
- **Disable `history` in StarterKit** when Collaboration is active — Yjs provides its own undo manager.
- Prefer **conditional collaboration**: check server health first, start solo, reconfigure to collaborative on success.
- Seed empty Y.Doc in `onSynced` callback if the Yjs XML fragment is empty.

## 4. Embedded Hocuspocus (Nitro Plugin)
- Run Hocuspocus in-process via a Nitro plugin instead of a separate WebSocket server process.
- Use `WebSocketServer({ noServer: true })` and attach to the Node HTTP server's `upgrade` event.
- Cannot use Nitro's `defineWebSocketHandler` because Hocuspocus requires raw `ws.WebSocket`, not crossws `Peer`.
- The HTTP server is created after plugins run — poll `process._getActiveHandles()` to find it.
- Nitro plugins are ESM — never use `require()`, always use `import` at top level.
- Expose a `getHocuspocus()` export from the plugin for use in API routes (health checks, stats).

## 5. WebSocket URL Resolution
- Never hardcode `ws://localhost:1234` — it breaks in production.
- If `NUXT_PUBLIC_HOCUSPOCUS_URL` is set, use it. Otherwise derive from `window.location` on the client.
- Use the same-origin `/_ws` path to avoid CORS and extra port exposure.

## 6. Context-Aware Menus
- Use `BubbleMenu` for selection-based actions.
- Use `FloatingMenu` for actions in empty lines.
- Implement Slash Commands using `@tiptap/suggestion` for block-level shortcuts.

## 7. Performance & Persistence
- Destroy editor instances on unmount.
- Tear down HocuspocusProvider and Y.Doc on unmount to avoid memory leaks.
- Sync Markdown/JSON fields with a backend (e.g., Directus) using `v-model` or `onUpdate` debouncing.
- In collaborative mode, skip emitting `update:modelValue` — Yjs is the source of truth.
