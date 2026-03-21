---
name: vueuse-nuxt-setup
description: VueUse installation, configuration, and common usage patterns in Nuxt 3
libraries_used: ["@vueuse/core", "@vueuse/nuxt"]
---

# VueUse in Nuxt 3 — Setup & Common Patterns

## Installation

```bash
pnpm add @vueuse/nuxt @vueuse/core
```

`nuxt.config.ts`:
```typescript
export default defineNuxtConfig({
  modules: ['@vueuse/nuxt'],
})
```

## Usage — Auto-Imported (No Imports Needed)

```vue
<script setup lang="ts">
// All auto-imported — no import statements needed
const { x, y } = useMouse()
const isDark = useDark()
const isOnline = useOnline()
const { width, height } = useWindowSize()
</script>

<template>
  <div>
    <p>Mouse: {{ x }}, {{ y }}</p>
    <p>Dark: {{ isDark }} | Online: {{ isOnline }} | Size: {{ width }}x{{ height }}</p>
  </div>
</template>
```

## Conflict Resolution — Explicit Imports

```typescript
// These MUST be imported explicitly (disabled auto-imports)
import { useStorage, useFetch, useHead, useCookie } from '@vueuse/core'

// Then use normally
const theme = useStorage('theme', 'light')
```

## Common Patterns

### Dark Mode Toggle
```vue
<script setup lang="ts">
const isDark = useDark()
const toggleDark = useToggle(isDark)
</script>

<template>
  <button @click="toggleDark()">
    {{ isDark ? '☀️ Light' : '🌙 Dark' }}
  </button>
</template>
```

### Persistent User Preferences
```typescript
import { useStorage } from '@vueuse/core'

// Persists to localStorage automatically
const userPrefs = useStorage('user-prefs', {
  fontSize: 16,
  language: 'en',
  notifications: true,
})
```

### Clipboard Copy
```vue
<script setup lang="ts">
const { copy, copied } = useClipboard()
</script>

<template>
  <button @click="copy('Hello!')">
    {{ copied ? 'Copied!' : 'Copy' }}
  </button>
</template>
```

### Element Visibility (Lazy Loading / Infinite Scroll)
```vue
<script setup lang="ts">
const el = ref(null)
const { isVisible } = useElementVisibility(el)
</script>

<template>
  <div ref="el">
    <span v-if="isVisible">Now visible!</span>
  </div>
</template>
```

### Click Outside (Dropdown/Modal)
```vue
<script setup lang="ts">
const dropdown = ref(null)
const isOpen = ref(false)
onClickOutside(dropdown, () => { isOpen.value = false })
</script>

<template>
  <div ref="dropdown">
    <button @click="isOpen = !isOpen">Toggle</button>
    <ul v-if="isOpen">...</ul>
  </div>
</template>
```

### Debounced Search Input
```typescript
const searchQuery = useDebouncedRef('', 300)
// searchQuery.value only updates after 300ms of no input
watch(searchQuery, (q) => fetchResults(q))
```

### Async State (Loading/Error/Data)
```typescript
const { state, isLoading, error } = useAsyncState(
  fetch('/api/data').then(r => r.json()),
  null // initial value
)
```

### Undo/Redo with useRefHistory
```typescript
const text = ref('')
const { history, undo, redo, canUndo, canRedo } = useRefHistory(text)
```

### Throttled/Debounced Watch
```typescript
// Only runs at most once per 500ms
watchThrottled(source, (val) => save(val), { throttle: 500 })

// Waits 300ms after last change
watchDebounced(query, (val) => search(val), { debounce: 300 })
```

### v-model Helper
```typescript
// In a child component
const props = defineProps<{ modelValue: string }>()
const emit = defineEmits(['update:modelValue'])
const value = useVModel(props, 'modelValue', emit)
// Use `value` directly as a ref — reads and writes sync with parent
```
