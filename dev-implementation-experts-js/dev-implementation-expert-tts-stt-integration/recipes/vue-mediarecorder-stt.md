---
name: vue-mediarecorder-stt
description: Complete Vue 3 Composition API pattern for browser audio recording with MediaRecorder, WebSocket STT, real-time interim results, and cursor-aware text insertion.
libraries_used: ["vue 3 (browser MediaRecorder API, WebSocket API)"]
---

# Vue 3 MediaRecorder + WebSocket STT

## When to Use
Use when building a voice input feature in a Vue 3 component that needs real-time transcription with interim results and seamless text insertion at the user's cursor position.

## Full Implementation

```typescript
// State
const isRecording = ref(false)
const isTranscribing = ref(false)
let mediaRecorder: MediaRecorder | null = null
let sttSocket: WebSocket | null = null

// Cursor tracking for text insertion
let sttCursorPos = 0
let sttUtterance = ''       // Accumulated finalized text
let sttInterimText = ''     // Current non-final transcript
let sttInsertedLength = 0   // Length of interim text currently in input

async function startRecording() {
  sttCursorPos = inputRef.value?.selectionStart ?? inputValue.value.length
  sttUtterance = ''
  sttInterimText = ''
  sttInsertedLength = 0

  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  sttSocket = new WebSocket(`${protocol}//${location.host}/api/transcribe-stream`)

  // Wait for server to confirm Deepgram connection
  await new Promise<void>((resolve, reject) => {
    sttSocket!.onmessage = (e) => {
      const msg = JSON.parse(e.data)
      if (msg.type === 'ready') resolve()
      else if (msg.type === 'error') reject(new Error(msg.message))
    }
    sttSocket!.onerror = () => reject(new Error('WebSocket error'))
    setTimeout(() => reject(new Error('WebSocket timeout')), 5000)
  })

  sttSocket.onmessage = (e) => handleSttTranscript(JSON.parse(e.data))
  sttSocket.onerror = (e) => console.error('[STT] socket error', e)

  const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
  mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm;codecs=opus' })
  mediaRecorder.ondataavailable = (e) => {
    if (e.data.size > 0 && sttSocket?.readyState === WebSocket.OPEN) {
      sttSocket.send(e.data)
    }
  }
  mediaRecorder.start(250)
  isRecording.value = true
}

function stopRecording() {
  mediaRecorder?.stop()
  mediaRecorder?.stream.getTracks().forEach(t => t.stop())
  mediaRecorder = null
  sttSocket?.readyState === WebSocket.OPEN && sttSocket.send(JSON.stringify({ type: 'stop' }))
  isRecording.value = false
  isTranscribing.value = true  // Show spinner until 'closed' received
}

function handleSttTranscript(msg: { type: string; transcript: string; is_final: boolean; speech_final: boolean }) {
  if (msg.type === 'closed') { isTranscribing.value = false; return }
  if (msg.type !== 'transcript') return

  const currentValue = inputValue.value
  const before = currentValue.slice(0, sttCursorPos + sttUtterance.length)
  const after = currentValue.slice(sttCursorPos + sttUtterance.length + sttInsertedLength)

  if (msg.is_final || msg.speech_final) {
    sttUtterance += (sttUtterance ? ' ' : '') + msg.transcript
    sttInterimText = ''
    sttInsertedLength = 0
  } else {
    sttInterimText = msg.transcript
    sttInsertedLength = msg.transcript.length + 1  // +1 for space
  }

  const insert = sttUtterance + (sttInterimText ? ' ' + sttInterimText : '')
  inputValue.value = before + (insert ? (before.endsWith(' ') ? '' : ' ') + insert : '') + after
}

onUnmounted(() => {
  sttSocket?.close()
  sttSocket = null
  if (mediaRecorder && isRecording.value) {
    mediaRecorder.stop()
    mediaRecorder.stream.getTracks().forEach(t => t.stop())
  }
})
```

## UI Pattern
```vue
<button @click="isRecording ? stopRecording() : startRecording()">
  <span v-if="isTranscribing">⏳</span>
  <span v-else-if="isRecording">🔴 Stop</span>
  <span v-else>🎤 Record</span>
</button>
```

## Key Behaviors
- `sttCursorPos`: snapshot cursor at recording start; all insertions relative to this
- `sttUtterance`: grows as `speech_final` results arrive; never replaced
- `sttInterimText`: replaced on each non-final message; removed on final
- `isTranscribing`: true from stop until `{ type: 'closed' }` — use for loading state
