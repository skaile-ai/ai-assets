---
name: "prog-expert-tts-stt-integration"
description: "Use when you need to implement TTS (text-to-speech) or STT (speech-to-text) integrations in Nuxt 3/Nitro applications, including Deepgram streaming STT via WebSocket, ElevenLabs TTS, browser MediaRecorder, and audio settings management. Expert-level programming and pattern management."
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "impl-experts-js-contract"
  env_vars:
    DEEPGRAM_API_KEY: "Required for STT. Your Deepgram API key."
    ELEVENLABS_API_KEY: "Required for TTS. Your ElevenLabs API key."
---

# Prog Expert TTS/STT Integration

## Goal
Expert-level implementation of audio I/O integrations in Nuxt 3 + Nitro applications. Covers Deepgram streaming STT via WebSocket proxy, ElevenLabs TTS, browser MediaRecorder API, audio settings, and multi-provider configuration.

## Core Workflow (Progressive Disclosure)

1. **Context Analysis**: Identify if the task is STT (input/transcription) or TTS (output/speech), and which provider is involved.
2. **Knowledge Retrieval**:
   - Check `recipes/` for matching implementation patterns.
   - Read `references/patterns.md` for architectural patterns and code snippets.
   - Check `references/versions.json` for current provider SDK versions.
3. **Implementation**: Follow the layered architecture (see below). Leverage `atomic-examples/` for isolated building blocks.
4. **Learning**:
   - After successful implementation, extract patterns via `programming/prog-expert-advisor/scripts/learn.py`.
   - Create/refine recipes in `recipes/` using `scripts/manage_recipes.py`.

## Architecture Overview

```
Browser                        Nitro Server                   External APIs
──────────────────────────────────────────────────────────────────────────
MediaRecorder (WebM/Opus)  →  WS /api/transcribe-stream  →  Deepgram WSS
                          ←  {type,transcript,is_final}  ←

POST /api/speak (text)     →  AgentManager.speak()        →  ElevenLabs REST
                          ←  ReadableStream (mp3)         ←

Audio() .play()            ←  sendStream(event, stream)   ←
```

## Instructions

- **Package manager**: Always use `bun` for this project.
- **Before starting**: Search `recipes/` for relevant patterns.
- **STT streaming**: Always proxy through Nitro WebSocket — never expose Deepgram API keys to the browser.
- **TTS streaming**: Use Nitro's `sendStream()` to pipe the ElevenLabs ReadableStream directly to the client.
- **Settings resolution**: API keys come from env vars first, then `data/settings.json`. Throw clear errors if missing.
- **Cleanup**: Always stop MediaRecorder tracks and close WebSocket on component unmount.
- **MIME type**: Use `audio/webm;codecs=opus` for MediaRecorder; Deepgram accepts it natively.
- **Language**: Support `auto` (detect_language=true), `multi` (detect_language=true&multi_channel=true), or specific codes.
- **After new recipe**: Add it to `recipes/README.md` index.

## Layered Implementation Checklist

### Backend (Nitro)
- [ ] `server/api/speak.post.ts` — TTS REST endpoint
- [ ] `server/api/transcribe.post.ts` — STT one-shot endpoint (optional)
- [ ] `server/routes/api/transcribe-stream.ts` — STT WebSocket proxy (preferred)
- [ ] `server/utils/agent-manager.ts` or equivalent — `speak()` and `transcribe()` methods
- [ ] `nuxt.config.ts` — enable `experimental.websocket: true`

### Frontend (Vue 3)
- [ ] Recording state: `isRecording`, `isTranscribing`, `mediaRecorder`, `audioChunks`
- [ ] WebSocket state: `sttSocket`, `sttCursorPos`, `sttUtterance`, `sttInterimText`
- [ ] `startRecording()` — mic access, WS connection, MediaRecorder setup
- [ ] `stopRecording()` — send `{type:'stop'}`, stop tracks
- [ ] `handleSttTranscript()` — merge interim + final results at cursor
- [ ] `playMessage(text)` — POST /speak, blob URL, Audio().play()
- [ ] `onUnmounted()` — full cleanup

### Settings
- [ ] `defaultSttProvider` (deepgram)
- [ ] `defaultTtsProvider` (elevenlabs)
- [ ] `sttLanguage` (auto | multi | en | de | fr | es | it | pt | nl | ja | ko | zh)
- [ ] API key masking in UI for env-sourced keys

## Self-Learning & Research
- Gather knowledge from web research and the `use-context7-api` skill.
- Learn from successful implementations and other codebases.
- Track latest SDK/API versions.
- Refine recipes based on implementation experience.

## Auto-Improvement
- Every time this skill is used, analyze the usage chat to find out if further improvement is advisable.
- Ask the user if those changes should be made.
- If approved, store improvement ideas in `resources/improvement_ideas.md`.

## References
- [Patterns](references/patterns.md) — Complete code patterns from Pichi: WebSocket proxy, MediaRecorder, TTS stream, settings.
- [Versions](references/versions.json) — Tracked provider/library versions.
- [Recipes](recipes/README.md) — Recipes index for recurring TTS/STT tasks.

## Example Code
When learning or implementing, use these code examples. ALWAYS load them via `view_file` to maintain Progressive Disclosure:

- **Atomic Examples** (Small code chunks from docs):
  - *(Add examples here)*: `view_file(<absolute_path>/atomic-examples/...)`
- **Recipes** (Larger patterns like Auth, Pages):
  - *(Add recipes here)*: `view_file(<absolute_path>/recipes/...)`
- **Reference Implementations** (Complex pseudo-code from existing real-world codebases):
  - *(Add references here)*: `view_file(<absolute_path>/reference-implementations/...)`

## Constraints
- Never expose provider API keys to the browser.
- Do not overwrite existing files without explicit user confirmation.
- Do not add Pinia/Vuex — use local `ref()` state only.

## Script Integration
- **Research**: `uv run scripts/research_knowledge.py "<query>"`
- **Version Tracking**: `uv run scripts/track_versions.py`
- **Recipe Management**: `uv run scripts/manage_recipes.py <action> [args]`
- **Example Management**: `uv run scripts/manage_examples.py <action> [args]`
- **Pattern Learning**: `uv run programming/prog-expert-advisor/scripts/learn.py <path/to/file>`
