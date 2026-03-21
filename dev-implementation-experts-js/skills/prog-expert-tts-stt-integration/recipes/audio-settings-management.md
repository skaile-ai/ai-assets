---
name: audio-settings-management
description: Multi-provider audio settings pattern with environment variable fallback, persisted to a JSON settings file, with masked API key display in UI.
libraries_used: ["zod (settings validation)", "vue 3 (settings UI)"]
---

# Audio Settings Management

## When to Use
Use when implementing configurable STT/TTS providers where API keys can come from env vars or user-provided runtime settings.

## Settings Interface

```typescript
interface Settings {
  apiKeys: Record<string, string>   // { deepgram: '...', elevenlabs: '...' }
  defaultSttProvider: string        // 'deepgram'
  defaultTtsProvider: string        // 'elevenlabs'
  sttLanguage?: string              // 'auto' | 'multi' | 'en' | 'de' | ...
}
```

## Key Resolution Pattern (Server)

```typescript
// Always: env var → settings file → throw
function resolveKey(envVar: string, settingsKey: string, settings: Settings): string {
  const key = process.env[envVar] || settings.apiKeys?.[settingsKey]
  if (!key) throw new Error(`${settingsKey} API key not configured`)
  return key
}

// Usage
const deepgramKey = resolveKey('DEEPGRAM_API_KEY', 'deepgram', settings)
const elevenLabsKey = resolveKey('ELEVENLABS_API_KEY', 'elevenlabs', settings)
```

## Language Options

```typescript
const STT_LANGUAGES = [
  { label: 'Auto Detect', value: 'auto' },
  { label: 'Multi-language', value: 'multi' },
  { label: 'English', value: 'en' },
  { label: 'German', value: 'de' },
  { label: 'French', value: 'fr' },
  { label: 'Spanish', value: 'es' },
  { label: 'Italian', value: 'it' },
  { label: 'Portuguese', value: 'pt' },
  { label: 'Dutch', value: 'nl' },
  { label: 'Japanese', value: 'ja' },
  { label: 'Korean', value: 'ko' },
  { label: 'Chinese', value: 'zh' },
]
```

## Settings UI — API Key Masking

```vue
<!-- Show masked value when key comes from env, allow override -->
<template>
  <div v-for="provider in audioProviders" :key="provider.key">
    <label>{{ provider.label }} API Key</label>
    <input
      :placeholder="isEnvKey(provider.envVar) ? '****** (from env)' : 'Enter key...'"
      :disabled="isEnvKey(provider.envVar)"
      v-model="settings.apiKeys[provider.key]"
    />
  </div>
</template>

<script setup>
function isEnvKey(envVar: string) {
  // Backend returns masked flag; never expose actual env key to frontend
  return envKeyStatus[envVar] === true
}
</script>
```

## Settings API (Nitro + Zod)

```typescript
// server/api/settings.put.ts
import { z } from 'zod'

const SettingsSchema = z.object({
  apiKeys: z.record(z.string()).optional().default({}),
  defaultSttProvider: z.string().default('deepgram'),
  defaultTtsProvider: z.string().default('elevenlabs'),
  sttLanguage: z.string().optional(),
})

export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  const settings = SettingsSchema.parse(body)
  await writeFile('data/settings.json', JSON.stringify(settings, null, 2))
  return { success: true }
})
```

## Key Details
- Never return actual API keys from env vars to the frontend — only a masked/boolean indicator
- `sttLanguage: 'auto'` → add `&detect_language=true` to Deepgram URL
- `sttLanguage: 'multi'` → same as `auto` (Deepgram handles multi-language with detect_language)
- Settings file: `data/settings.json` (gitignored)
