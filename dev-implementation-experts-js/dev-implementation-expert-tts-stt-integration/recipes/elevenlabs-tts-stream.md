---
name: elevenlabs-tts-stream
description: ElevenLabs TTS REST integration in Nitro, streaming MP3 audio directly to the browser client using sendStream.
libraries_used: ["elevenlabs (optional — raw fetch used)"]
---

# ElevenLabs TTS Streaming (Nitro)

## When to Use
Use when you need server-side text-to-speech that streams MP3 audio to the browser without buffering the full response.

## Steps

### 1. API route
```typescript
// server/api/speak.post.ts
export default defineEventHandler(async (event) => {
  const { text } = await readBody(event)
  if (!text) throw createError({ statusCode: 400, message: 'Missing text' })
  const stream = await useAgentManager().speak(text)
  return sendStream(event, stream)
})
```

### 2. AgentManager.speak()
```typescript
async speak(text: string): Promise<ReadableStream> {
  const settings = await this.getSettings()
  const apiKey = process.env.ELEVENLABS_API_KEY || settings.apiKeys?.['elevenlabs']
  if (!apiKey) throw new Error('ElevenLabs API key not configured')

  const voiceId = 'pNInz6obpgDQGcFmaJgB' // Adam
  const res = await fetch(
    `https://api.elevenlabs.io/v1/text-to-speech/${voiceId}?output_format=mp3_44100_128`,
    {
      method: 'POST',
      headers: { 'xi-api-key': apiKey, 'Content-Type': 'application/json' },
      body: JSON.stringify({ text, model_id: 'eleven_multilingual_v2' }),
    }
  )
  if (!res.ok) throw new Error(`ElevenLabs error: ${(await res.json()).detail?.message}`)
  return res.body!
}
```

### 3. Browser playback
```typescript
async function playMessage(text: string) {
  const blob = await $fetch('/api/speak', { method: 'POST', body: { text }, responseType: 'blob' })
  const url = URL.createObjectURL(blob as Blob)
  const audio = new Audio(url)
  audio.onended = () => URL.revokeObjectURL(url)
  await audio.play()
}
```

## Key Details
- `sendStream()` pipes the ReadableStream directly — no memory buffering
- Always revoke blob URLs after playback to free memory
- Model `eleven_multilingual_v2` handles 29 languages automatically
- Switch to `eleven_turbo_v2_5` for lower latency if quality can be reduced
