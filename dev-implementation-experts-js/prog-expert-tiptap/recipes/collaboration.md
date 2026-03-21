---
name: Real-time Collaboration with Conditional Hocuspocus
description: Embed Hocuspocus in Nitro, check availability, and enable Yjs collaboration only when the server is reachable. Falls back to solo editing.
libraries_used: "@tiptap/extension-collaboration, @tiptap/extension-collaboration-caret, @hocuspocus/server, @hocuspocus/provider, @hocuspocus/extension-sqlite, yjs, y-prosemirror, ws"
learned_from: concept-forge editor-next branch (2026-03-12)
---

# Real-time Collaboration (Conditional, Embedded Hocuspocus)

## Objective
Multi-user editing with cursors/presence that gracefully degrades to solo mode when collaboration is unavailable.

## Key Learnings

### Package naming
- The cursor/caret extension package is `@tiptap/extension-collaboration-caret` (NOT `-cursor`).
- The export is `CollaborationCaret` (NOT `CollaborationCursor`).

### Architecture: Embedded Hocuspocus in Nitro
Instead of running a separate Hocuspocus WebSocket server process, embed it as a **Nitro plugin** that attaches to the existing Node HTTP server's `upgrade` event. This gives:
- Single process, single port (ideal for Docker/Coolify/Nixpacks)
- WebSocket upgrades handled on `/_ws` path
- SQLite persistence for Y.Doc state

### Conditional collaboration
1. A health API endpoint (`/api/collaboration/health`) exposes whether Hocuspocus is running.
2. A composable (`useCollaboration`) checks the health endpoint once and caches the result.
3. The editor starts in **solo mode** (StarterKit with history).
4. On mount, if health check passes AND a `roomName` prop is provided, the editor reconfigures itself with Collaboration + CollaborationCaret extensions (history disabled, since Yjs handles undo/redo).

### WebSocket URL derivation
Don't hardcode `ws://127.0.0.1:1234`. Instead:
- If `NUXT_PUBLIC_HOCUSPOCUS_URL` is set, use it.
- Otherwise, derive from the current page origin: `wss://${location.host}/_ws`.

## Prerequisites
```
@hocuspocus/server @hocuspocus/provider @hocuspocus/extension-sqlite @hocuspocus/extension-logger
@tiptap/extension-collaboration @tiptap/extension-collaboration-caret
yjs y-prosemirror ws
```

## Server: Nitro Plugin (`server/plugins/hocuspocus.ts`)

```ts
import { Hocuspocus } from '@hocuspocus/server'
import { Logger } from '@hocuspocus/extension-logger'
import { SQLite } from '@hocuspocus/extension-sqlite'
import { WebSocketServer } from 'ws'
import { Server as HttpServer } from 'node:http'
import { Server as HttpsServer } from 'node:https'
import type { IncomingMessage } from 'node:http'
import type { Duplex } from 'node:stream'

let hocuspocus: Hocuspocus | null = null
export function getHocuspocus() { return hocuspocus }

export default defineNitroPlugin((nitroApp) => {
  hocuspocus = new Hocuspocus({
    quiet: true,
    extensions: [
      new Logger(),
      new SQLite({ database: '/path/to/hocuspocus.sqlite' }),
    ],
  })

  const wss = new WebSocketServer({ noServer: true })

  wss.on('connection', (ws, req) => {
    hocuspocus!.handleConnection(ws, req)
  })

  const onUpgrade = (req: IncomingMessage, socket: Duplex, head: Buffer) => {
    if (!req.url?.startsWith('/_ws')) return
    wss.handleUpgrade(req, socket, head, (ws) => {
      wss.emit('connection', ws, req)
    })
  }

  // Attach to the Node HTTP server's upgrade event.
  // The server may not exist yet when the plugin runs — retry briefly.
  const tryAttach = (): boolean => {
    const handles = (process as any)._getActiveHandles?.() || []
    for (const handle of handles) {
      if (
        (handle instanceof HttpServer || handle instanceof HttpsServer) &&
        !(handle as any).__hocuspocusAttached
      ) {
        handle.on('upgrade', onUpgrade)
        ;(handle as any).__hocuspocusAttached = true
        return true
      }
    }
    return false
  }

  if (!tryAttach()) {
    let attempts = 0
    const interval = setInterval(() => {
      attempts++
      if (tryAttach() || attempts > 50) clearInterval(interval)
    }, 200)
  }
})
```

### Why `_getActiveHandles()`?
Nitro plugins run before the HTTP server is created (both in dev via `listhen` and prod via `node-server.mjs`). There is no public API to access the server instance. Polling `process._getActiveHandles()` for an `http.Server` is the most reliable cross-environment approach.

### Why NOT `defineWebSocketHandler`?
Nitro's built-in WebSocket support uses `crossws`, which exposes `Peer` objects — not raw `ws.WebSocket` instances. Hocuspocus's `handleConnection()` requires a `ws.WebSocket`, so we need our own `WebSocketServer({ noServer: true })`.

## Server: Health Endpoint (`server/api/collaboration/health.get.ts`)

```ts
import { getHocuspocus } from '../../plugins/hocuspocus'

export default defineEventHandler(() => {
  const hocuspocus = getHocuspocus()
  if (hocuspocus) {
    return {
      available: true,
      connections: hocuspocus.getConnectionsCount(),
      documents: hocuspocus.getDocumentsCount(),
    }
  }
  return { available: false }
})
```

## Client: Composable (`composables/useCollaboration.ts`)

```ts
import { ref } from 'vue'

const collaborationAvailable = ref<boolean | null>(null)
let checked = false

export function useCollaboration() {
  function getWsUrl(): string {
    const config = useRuntimeConfig()
    const explicit = config.public.hocuspocusUrl as string
    if (explicit) return explicit
    if (import.meta.client) {
      const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      return `${proto}//${window.location.host}/_ws`
    }
    return 'ws://127.0.0.1:3333/_ws'
  }

  async function checkAvailability() {
    if (checked) return collaborationAvailable.value
    try {
      const data = await $fetch<{ available: boolean }>('/api/collaboration/health')
      collaborationAvailable.value = data.available
    } catch {
      collaborationAvailable.value = false
    }
    checked = true
    return collaborationAvailable.value
  }

  function reset() { checked = false; collaborationAvailable.value = null }

  return { collaborationAvailable, checkAvailability, getWsUrl, reset }
}
```

## Client: Editor with Conditional Collaboration

Key pattern — start solo, reconfigure on mount:

```vue
<script setup lang="ts">
import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCaret from '@tiptap/extension-collaboration-caret'
import { HocuspocusProvider } from '@hocuspocus/provider'
import * as Y from 'yjs'

const { collaborationAvailable, checkAvailability, getWsUrl } = useCollaboration()
const isCollaborative = ref(false)
const provider = ref<HocuspocusProvider | null>(null)
const ydoc = ref<Y.Doc | null>(null)

// Start with a solo editor (full StarterKit with history)
const editor = useEditor({
  content: '',
  extensions: [StarterKit, ...otherExtensions],
})

function setupCollaboration() {
  if (!props.roomName) return

  ydoc.value = new Y.Doc()
  provider.value = new HocuspocusProvider({
    url: getWsUrl(),
    name: props.roomName,
    document: ydoc.value,
    onSynced: () => {
      // Seed empty Y.Doc with initial content
      const yXmlFragment = ydoc.value!.getXmlFragment('default')
      if (yXmlFragment.length === 0 && props.modelValue) {
        editor.value?.commands.setContent(props.modelValue, { emitUpdate: false })
      }
    },
  })

  // Reconfigure: disable history (Yjs handles undo), add collaboration
  editor.value?.setOptions({
    extensions: [
      StarterKit.configure({ history: false }),
      ...otherExtensions,
      Collaboration.configure({ document: ydoc.value }),
      CollaborationCaret.configure({
        provider: provider.value,
        user: { name: props.userName || 'Anonymous', color: props.userColor || '#3b82f6' },
      }),
    ],
  })

  isCollaborative.value = true
}

onMounted(async () => {
  const available = await checkAvailability()
  if (available && props.roomName) {
    setupCollaboration()
  } else {
    // Solo mode: set initial content
    editor.value?.commands.setContent(props.modelValue, { emitUpdate: false })
  }
})
</script>
```

## Gotchas

| Issue | Fix |
|-------|-----|
| `CollaborationCursor` not found | Package is `@tiptap/extension-collaboration-caret`, export is `CollaborationCaret` |
| Undo/redo broken in collab mode | Disable `history` in StarterKit when Collaboration is active |
| Content lost on collab connect | Seed Y.Doc in `onSynced` callback if the fragment is empty |
| WS URL breaks in production | Derive from `window.location` instead of hardcoding `localhost` |
| Nitro plugin can't find HTTP server | Server is created after plugins; poll `_getActiveHandles()` |
| `require is not defined` in plugin | Nitro plugins are ESM — use `import` at top level, not `require()` |

## CSS for Collaboration Cursors

```css
.collaboration-cursor__caret {
  border-left: 1px solid #0d0d0d;
  border-right: 1px solid #0d0d0d;
  margin-left: -1px;
  margin-right: -1px;
  pointer-events: none;
  position: relative;
  word-break: normal;
}

.collaboration-cursor__label {
  border-radius: 3px 3px 3px 0;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  left: -1px;
  line-height: normal;
  padding: 0.1rem 0.3rem;
  position: absolute;
  top: -1.4em;
  user-select: none;
  white-space: nowrap;
}
```
