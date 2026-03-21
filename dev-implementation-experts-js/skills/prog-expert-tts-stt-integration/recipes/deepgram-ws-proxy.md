---
name: deepgram-ws-proxy
description: Nitro WebSocket proxy that relays browser audio to Deepgram's streaming STT API, returning real-time transcripts with interim results.
libraries_used: ["@deepgram/sdk (optional — raw WS used)", "nuxt nitro experimental.websocket"]
---

# Deepgram WebSocket Proxy (Nitro)

## When to Use
Use when you need real-time speech-to-text in the browser without exposing the Deepgram API key to the client.

## Steps

### 1. Enable WebSocket in Nuxt config
```typescript
// nuxt.config.ts
nitro: { experimental: { websocket: true } }
```

### 2. Create the WebSocket handler
Place at `server/routes/api/transcribe-stream.ts` (note: `routes/` not `api/` — needed for WS).

```typescript
export default defineWebSocketHandler({
  async open(peer) {
    const { apiKey, language } = await getDeepgramConfig()
    let url = `wss://api.deepgram.com/v1/listen?model=nova-3&smart_format=true&interim_results=true&endpointing=300&vad_events=true`
    if (language === 'auto' || language === 'multi') url += '&detect_language=true'
    else if (language) url += `&language=${language}`

    const dg = new WebSocket(url, { headers: { Authorization: `Token ${apiKey}` } })
    dg.on('open', () => peer.send(JSON.stringify({ type: 'ready' })))
    dg.on('message', (data) => {
      const msg = JSON.parse(data.toString())
      if (msg.type === 'Results') {
        const transcript = msg.channel?.alternatives?.[0]?.transcript ?? ''
        if (transcript) peer.send(JSON.stringify({ type: 'transcript', transcript, is_final: msg.is_final, speech_final: msg.speech_final }))
      }
    })
    dg.on('close', () => peer.send(JSON.stringify({ type: 'closed' })))
    peer.ctx.dg = dg
  },
  message(peer, message) {
    const dg = peer.ctx.dg
    if (!dg || dg.readyState !== 1) return
    if (message.text) {
      const parsed = JSON.parse(message.text())
      if (parsed.type === 'stop') { dg.close(); return }
    }
    dg.send(message.rawData)
  },
  close(peer) { peer.ctx.dg?.close() },
  error(peer) { peer.ctx.dg?.close() }
})
```

### 3. Client message protocol
- Server → Client: `{ type: 'ready' }` when Deepgram connection opens
- Client → Server: binary audio chunks (WebM/Opus, 250ms intervals)
- Client → Server: `{ type: 'stop' }` to end session
- Server → Client: `{ type: 'transcript', transcript, is_final, speech_final }`
- Server → Client: `{ type: 'closed' }` when Deepgram closes

## Key Details
- Audio MIME: `audio/webm;codecs=opus`
- Chunk interval: 250ms (via `mediaRecorder.start(250)`)
- Always wait for `{ type: 'ready' }` before starting MediaRecorder
