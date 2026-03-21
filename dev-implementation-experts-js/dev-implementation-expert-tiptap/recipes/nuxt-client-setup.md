---
name: Nuxt 3 Client-Only Setup
description: How to correctly initialize Tiptap in Nuxt 3 to avoid SSR hydration mismatches.
libraries_used: @tiptap/vue-3, @tiptap/starter-kit
---

# Nuxt 3 Client-Only Setup

## Objective
Tiptap relies on browser APIs (DOM) that aren't available during Nuxt's Server-Side Rendering (SSR). This recipe ensures the editor only mounts on the client.

## Prerequisites
- Nuxt 3
- `@tiptap/vue-3`
- `@tiptap/starter-kit`

## Instructions

### 1. Set `immediatelyRender: false`
In your component, when using the `useEditor` composable (or constructor), always set `immediatelyRender` to `false`. This prevents the editor from trying to attach to an element before it's in the DOM.

### 2. Use `<client-only>`
Wrap your Tiptap components in Nuxt's `<client-only>` tag to ensure they aren't rendered on the server.

### 3. Cleanup on Unmount
Always call `editor.destroy()` in `onBeforeUnmount` to prevent memory leaks.

## Code Example

```vue
<script setup>
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'

const editor = useEditor({
  content: '<p>Hello World!</p>',
  extensions: [StarterKit],
  // CRITICAL: MUST BE FALSE FOR SSR
  immediatelyRender: false,
})

onBeforeUnmount(() => {
  unref(editor).destroy()
})
</script>

<template>
  <client-only>
    <editor-content :editor="editor" />
  </client-only>
</template>
```
