---
name: AI Autocomplete
description: Trigger-based AI completion using ++ pattern and Vercel AI SDK
libraries_used: "@tiptap/vue-3, ai, @ai-sdk/openai"
---

# AI Autocomplete

## Objective
Implement an AI completion feature that triggers automatically when the user types a specific character sequence (e.g., `++`), providing a seamless writing assistance experience.

## Prerequisites
- Nuxt 3 project
- Vercel AI SDK (`ai`, `@ai-sdk/openai`)
- Tiptap Vue 3 integration

## Instructions
1.  **Monitor for Trigger**: Use the `onUpdate` hook in the Tiptap editor to check the last few characters typed.
2.  **Trigger AI**: When the sequence is detected, delete the trigger characters and call the AI completion endpoint.
3.  **Stream Content**: Use a `watch` effect to monitor the incoming completion data and insert the delta into the editor.
4.  **Handle Interruptions**: Implement logic to stop the generation if the user presses "Escape" or clicks elsewhere.

## Code Example

### Component Implementation (`Editor.vue`)
```vue
<script setup lang="ts">
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { useCompletion } from 'ai/vue'
import { watch } from 'vue'

const { complete, completion, isLoading, stop } = useCompletion({
  api: '/api/generate',
})

const editor = useEditor({
  extensions: [StarterKit],
  onUpdate: ({ editor }) => {
    const { from } = editor.state.selection
    // Check last 2 characters for "++"
    const lastTwo = editor.state.doc.textBetween(from - 2, from)
    
    if (lastTwo === "++" && !isLoading.value) {
      // 1. Remove the trigger
      editor.commands.deleteRange({ from: from - 2, to: from })
      
      // 2. Trigger completion with preceding text for context
      const context = editor.getText()
      complete(context)
    }
  },
})

// 3. Delta-streaming insertion
watch(completion, (newVal, oldVal) => {
  const delta = newVal.slice(oldVal?.length || 0)
  if (delta && editor.value) {
    editor.value.commands.insertContent(delta)
  }
})

// 4. Interruption handling (example for Escape key)
if (process.client) {
  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && isLoading.value) {
      stop()
    }
  })
}
</script>

<template>
  <div class="editor-container">
    <EditorContent :editor="editor" />
    <div v-if="isLoading" class="ai-badge">AI is writing...</div>
  </div>
</template>
```

### Server API (`server/api/generate.ts`)
```typescript
import { streamText } from 'ai'
import { openai } from '@ai-sdk/openai'

export default defineEventHandler(async (event) => {
  const { prompt } = await readBody(event)

  const result = await streamText({
    model: openai('gpt-4o'),
    prompt: prompt,
    system: "You are a helpful writing assistant. Continue the user's text naturally.",
  })

  return result.toDataStreamResponse()
})
```
