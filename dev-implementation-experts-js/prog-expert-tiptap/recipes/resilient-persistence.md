---
name: Resilient Persistence
description: Robust Tiptap state management with local storage drafts and debounced server sync
libraries_used: "@tiptap/vue-3, @vueuse/core"
---

# Resilient Persistence

## Objective
Ensure the user never loses their work by implementing a multi-layered persistence strategy: immediate local draft saving and debounced server synchronization.

## Prerequisites
- Nuxt 3 project
- `@vueuse/core`

## Instructions
1.  **Local Draft**: Use `@vueuse/core`'s `useLocalStorage` to maintain an immediate, client-side copy of the JSON content.
2.  **Debounced Sync**: Use a debounced watch or function to send the content to your backend API to minimize server pressure and prevent race conditions.
3.  **Hydration**: On mount, prioritize the local draft if it exists and is newer than the server version (or simply fallback to server if local is empty).

## Code Example

```vue
<script setup lang="ts">
import { useEditor, EditorContent } from '@tiptap/vue-3'
import { useLocalStorage, useDebounceFn } from '@vueuse/core'
import { watch, onMounted } from 'vue'

const props = defineProps<{ documentId: string, initialContent: any }>()

// 1. Local Storage Draft
const localDraft = useLocalStorage(`editor-draft-${props.documentId}`, props.initialContent)

const editor = useEditor({
  content: localDraft.value,
  onUpdate: ({ editor }) => {
    // Immediate local sync
    localDraft.value = editor.getJSON()
    // Debounced server sync
    debouncedSave()
  },
})

// 2. Debounced Server Sync
const debouncedSave = useDebounceFn(async () => {
  if (!editor.value) return
  
  await $fetch(`/api/documents/${props.documentId}`, {
    method: 'PUT',
    body: { content: editor.value.getJSON() }
  })
}, 1500)

// 3. Hydration Logic (optional override if server content changes externally)
watch(() => props.initialContent, (newContent) => {
  if (editor.value && !editor.value.isFocused) {
    editor.value.commands.setContent(newContent)
  }
})
</script>

<template>
  <div class="editor-wrapper">
    <EditorContent :editor="editor" />
  </div>
</template>
```
