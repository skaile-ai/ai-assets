# TTS/STT Patterns — Pichi Reference

Extracted from the Pichi codebase. Use these as canonical implementation templates.

---

## 1. Deepgram STT — WebSocket Proxy (Nitro)

**File**: `server/routes/api/transcribe-stream.ts`

```typescript
// nuxt.config.ts — required
experimental: { websocket: true }

// server/routes/api/transcribe-stream.ts
export default defineWebSocketHandler({
  async open(peer) {
    const settings = await useAgentManager().getSettings()
    const { apiKey, language } = await useAgentManager().getDeepgramStreamConfig()

    let dgUrl = `wss://api.deepgram.com/v1/listen?model=nova-3&smart_format=true&interim_results=true&endpointing=300&vad_events=true`
    if (language === 'auto' || language === 'multi') {
      dgUrl += `&detect_language=true`
    } else if (language) {
      dgUrl += `&language=${language}`
    }

    const dgSocket = new WebSocket(dgUrl, { headers: { Authorization: `Token ${apiKey}` } })

    dgSocket.on('open', () => peer.send(JSON.stringify({ type: 'ready' })))

    dgSocket.on('message', (data: Buffer) => {
      const msg = JSON.parse(data.toString())
      if (msg.type === 'Results') {
        const transcript = msg.channel?.alternatives?.[0]?.transcript ?? ''
        if (transcript) {
          peer.send(JSON.stringify({
            type: 'transcript',
            transcript,
            is_final: msg.is_final,
            speech_final: msg.speech_final,
          }))
        }
      }
    })

    dgSocket.on('close', () => peer.send(JSON.stringify({ type: 'closed' })))
    dgSocket.on('error', (err: Error) => {
      peer.send(JSON.stringify({ type: 'error', message: err.message }))
    })

    // Store on peer context for access in message/close handlers
    peer.ctx.dgSocket = dgSocket
  },

  message(peer, message) {
    const dgSocket = peer.ctx.dgSocket as WebSocket
    if (!dgSocket || dgSocket.readyState !== WebSocket.OPEN) return

    if (message.text) {
      const parsed = JSON.parse(message.text())
      if (parsed.type === 'stop') {
        dgSocket.close()
        return
      }
    }
    // Forward raw binary audio to Deepgram
    dgSocket.send(message.rawData)
  },

  close(peer) {
    peer.ctx.dgSocket?.close()
  },

  error(peer, error) {
    console.error('[transcribe-stream] error:', error)
    peer.ctx.dgSocket?.close()
  }
})
```

---

## 2. Deepgram STT — One-Shot REST (Nitro)

**File**: `server/api/transcribe.post.ts`

```typescript
// server/api/transcribe.post.ts
export default defineEventHandler(async (event) => {
  const form = await readFormData(event)
  const audioFile = form.get('audio') as File | null
  if (!audioFile) throw createError({ statusCode: 400, message: 'No audio file provided' })

  const buffer = Buffer.from(await audioFile.arrayBuffer())
  const transcript = await useAgentManager().transcribe(buffer)
  return { transcript }
})
```

**AgentManager.transcribe()**:

```typescript
async transcribe(audioData: Buffer): Promise<string> {
  const { apiKey, language } = await this.getDeepgramStreamConfig()
  let url = `https://api.deepgram.com/v1/listen?model=nova-3&smart_format=true`
  if (language === 'auto' || language === 'multi') {
    url += `&detect_language=true`
  } else if (language) {
    url += `&language=${language}`
  }

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      Authorization: `Token ${apiKey}`,
      'Content-Type': 'audio/wav',
    },
    body: audioData,
  })

  if (!response.ok) {
    const err = await response.json()
    throw new Error(`Deepgram error: ${err.err_msg ?? response.statusText}`)
  }

  const data = await response.json()
  return data.results.channels[0].alternatives[0].transcript ?? ''
}
```

---

## 3. ElevenLabs TTS — Streaming (Nitro)

**File**: `server/api/speak.post.ts` + `AgentManager.speak()`

```typescript
// server/api/speak.post.ts
export default defineEventHandler(async (event) => {
  const { text } = await readBody(event)
  if (!text) throw createError({ statusCode: 400, message: 'Missing text' })

  const stream = await useAgentManager().speak(text)
  return sendStream(event, stream)
})
```

**AgentManager.speak()**:

```typescript
async speak(text: string): Promise<ReadableStream> {
  const settings = await this.getSettings()
  const apiKey = process.env.ELEVENLABS_API_KEY || settings.apiKeys?.['elevenlabs']
  if (!apiKey) throw new Error('ElevenLabs API key not configured')

  const voiceId = 'pNInz6obpgDQGcFmaJgB' // Adam voice
  const url = `https://api.elevenlabs.io/v1/text-to-speech/${voiceId}?output_format=mp3_44100_128`

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'xi-api-key': apiKey,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      text,
      model_id: 'eleven_multilingual_v2',
    }),
  })

  if (!response.ok) {
    const err = await response.json()
    throw new Error(`ElevenLabs error: ${err.detail?.message ?? response.statusText}`)
  }

  return response.body!
}
```

---

## 4. Settings Resolution Pattern

```typescript
// Consistent key resolution: env var → settings file
interface AudioSettings {
  apiKeys: Record<string, string>
  defaultSttProvider: string  // 'deepgram'
  defaultTtsProvider: string  // 'elevenlabs'
  sttLanguage?: string        // 'auto' | 'multi' | 'en' | 'de' | ...
}

async getDeepgramStreamConfig() {
  const settings = await this.getSettings()
  const apiKey = process.env.DEEPGRAM_API_KEY || settings.apiKeys?.['deepgram']
  if (!apiKey) throw new Error('Deepgram API key not configured')
  return { apiKey, language: settings.sttLanguage ?? 'auto' }
}
```

---

## 5. Browser MediaRecorder + WebSocket STT (Vue 3)

```typescript
// State
const isRecording = ref(false)
const isTranscribing = ref(false)
let mediaRecorder: MediaRecorder | null = null
let sttSocket: WebSocket | null = null
let sttCursorPos = 0
let sttUtterance = ''
let sttInterimText = ''
let sttInsertedLength = 0

async function startRecording() {
  // Save cursor for text insertion
  sttCursorPos = inputTextarea.value?.selectionStart ?? inputValue.value.length
  sttUtterance = ''
  sttInterimText = ''
  sttInsertedLength = 0

  // Open WebSocket to Nitro proxy
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  sttSocket = new WebSocket(`${protocol}//${location.host}/api/transcribe-stream`)

  // Wait for 'ready' before capturing audio
  await new Promise<void>((resolve, reject) => {
    sttSocket!.onmessage = (e) => {
      const msg = JSON.parse(e.data)
      if (msg.type === 'ready') resolve()
      else if (msg.type === 'error') reject(new Error(msg.message))
    }
    sttSocket!.onerror = reject
  })

  sttSocket.onmessage = (e) => handleSttTranscript(JSON.parse(e.data))

  // Start mic capture
  const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
  mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm;codecs=opus' })
  mediaRecorder.ondataavailable = (e) => {
    if (e.data.size > 0 && sttSocket?.readyState === WebSocket.OPEN) {
      sttSocket.send(e.data)
    }
  }
  mediaRecorder.start(250) // 250ms chunks
  isRecording.value = true
}

function stopRecording() {
  if (mediaRecorder && isRecording.value) {
    mediaRecorder.stop()
    mediaRecorder.stream.getTracks().forEach(t => t.stop())
    mediaRecorder = null
  }
  if (sttSocket?.readyState === WebSocket.OPEN) {
    sttSocket.send(JSON.stringify({ type: 'stop' }))
  }
  isRecording.value = false
  isTranscribing.value = true
}

function handleSttTranscript(msg: { type: string; transcript: string; is_final: boolean; speech_final: boolean }) {
  if (msg.type !== 'transcript') {
    if (msg.type === 'closed') isTranscribing.value = false
    return
  }

  const base = inputValue.value
  const before = base.slice(0, sttCursorPos + sttUtterance.length)
  const after = base.slice(sttCursorPos + sttUtterance.length + sttInsertedLength)

  if (msg.is_final || msg.speech_final) {
    sttUtterance += (sttUtterance ? ' ' : '') + msg.transcript
    sttInterimText = ''
    sttInsertedLength = 0
  } else {
    sttInterimText = msg.transcript
    sttInsertedLength = msg.transcript.length
  }

  const insert = sttUtterance + (sttInterimText ? ' ' + sttInterimText : '')
  inputValue.value = before + (insert ? ' ' + insert : '') + after
}

// TTS playback
async function playMessage(text: string) {
  const response = await $fetch('/api/speak', {
    method: 'POST',
    body: { text },
    responseType: 'blob',
  })
  const url = URL.createObjectURL(response as Blob)
  const audio = new Audio(url)
  audio.onended = () => URL.revokeObjectURL(url)
  await audio.play()
}

// Cleanup
onUnmounted(() => {
  if (sttSocket) { sttSocket.close(); sttSocket = null }
  if (mediaRecorder && isRecording.value) {
    mediaRecorder.stop()
    mediaRecorder.stream.getTracks().forEach(t => t.stop())
  }
})
```

---

## 6. Deepgram WebSocket URL Parameters

| Param | Value | Purpose |
|---|---|---|
| `model` | `nova-3` | Latest model, best accuracy |
| `smart_format` | `true` | Punctuation, numerals, etc. |
| `interim_results` | `true` | Real-time partial transcripts |
| `endpointing` | `300` | ms of silence before speech_final |
| `vad_events` | `true` | Voice activity detection events |
| `detect_language` | `true` | Auto language detection |
| `language` | `en`, `de`, etc. | Force specific language |

---

## 7. ElevenLabs Voice IDs & Models

| Voice | ID |
|---|---|
| Adam (default) | `pNInz6obpgDQGcFmaJgB` |
| Rachel | `21m00Tcm4TlvDq8ikWAM` |

| Model | Use Case |
|---|---|
| `eleven_multilingual_v2` | Multi-language, high quality |
| `eleven_turbo_v2_5` | Low latency |
| `eleven_flash_v2_5` | Fastest, minimal latency |

Output formats: `mp3_44100_128` (default), `mp3_44100_64`, `pcm_16000`

---

## 8. Nuxt Config — WebSocket Enable

```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  nitro: {
    experimental: {
      websocket: true,
    },
  },
})
```
