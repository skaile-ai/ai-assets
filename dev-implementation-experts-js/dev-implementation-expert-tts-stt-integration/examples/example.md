# Example: Prog Expert TTS/STT Integration

## Input
"Add voice input to the chat — user clicks mic, speaks, and the transcript appears in the input box in real time."

## Expected Output
1. `server/routes/api/transcribe-stream.ts` — Deepgram WebSocket proxy
2. `server/api/speak.post.ts` — ElevenLabs TTS endpoint
3. Vue 3 component with `startRecording()`, `stopRecording()`, `handleSttTranscript()`, `playMessage()`
4. Settings UI additions for STT/TTS provider and language selection
5. `nuxt.config.ts` updated with `experimental.websocket: true`
