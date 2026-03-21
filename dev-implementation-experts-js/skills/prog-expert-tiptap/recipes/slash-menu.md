---
name: Slash Menu
description: Customizable command palette triggered by / using @tiptap/suggestion and Vue components
libraries_used: "@tiptap/vue-3, @tiptap/suggestion, tippy.js"
---

# Slash Menu

## Objective
Implement a "Slash Menu" (command palette) that appears when the user types `/`, allowing for quick insertion of blocks and execution of commands.

## Prerequisites
- Tiptap Vue 3
- `@tiptap/suggestion`
- `tippy.js` (for positioning the popover)

## Instructions
1.  **Define Suggestions**: Create a list of items (Heading, Bullet List, etc.) with their associated commands.
2.  **Suggestion Extension**: Configure the `Suggestion` extension to trigger on `/`.
3.  **Vue Component**: Build a `SlashCommandList.vue` component to handle keyboard navigation (ArrowUp, ArrowDown, Enter) and item selection.
4.  **Integration**: Combine the extension and component using a `render` function that utilizes `tippy.js`.

## Code Example

### `slash-command.ts` (Extension Configuration)
```typescript
import { Extension } from '@tiptap/core'
import Suggestion from '@tiptap/suggestion'
import { VueRenderer } from '@tiptap/vue-3'
import tippy from 'tippy.js'
import SlashCommandList from './SlashCommandList.vue'

export const SlashCommand = Extension.create({
  name: 'slashCommand',
  addOptions() {
    return {
      suggestion: {
        char: '/',
        command: ({ editor, range, props }) => {
          props.command({ editor, range })
        },
        render: () => {
          let component: any
          let popup: any

          return {
            onStart: (props) => {
              component = new VueRenderer(SlashCommandList, {
                props,
                editor: props.editor,
              })

              popup = tippy('body', {
                getReferenceClientRect: props.clientRect,
                appendTo: () => document.body,
                content: component.element,
                showOnCreate: true,
                interactive: true,
                trigger: 'manual',
                placement: 'bottom-start',
              })
            },
            onUpdate(props) {
              component.updateProps(props)
              popup[0].setProps({
                getReferenceClientRect: props.clientRect,
              })
            },
            onKeyDown(props) {
              if (props.event.key === 'Escape') {
                popup[0].hide()
                return true
              }
              return component.ref?.onKeyDown(props)
            },
            onExit() {
              popup[0].destroy()
              component.destroy()
            },
          }
        },
      },
    }
  },
  addProseMirrorPlugins() {
    return [
      Suggestion({
        editor: this.editor,
        ...this.options.suggestion,
      }),
    ]
  },
})
```

### `SlashCommandList.vue` (The UI)
```vue
<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{ items: any[], command: Function }>()
const selectedIndex = ref(0)

const selectItem = (index: number) => {
  const item = props.items[index]
  if (item) {
    props.command(item)
  }
}

const onKeyDown = ({ event }: { event: KeyboardEvent }) => {
  if (event.key === 'ArrowUp') {
    selectedIndex.value = (selectedIndex.value + props.items.length - 1) % props.items.length
    return true
  }
  if (event.key === 'ArrowDown') {
    selectedIndex.value = (selectedIndex.value + 1) % props.items.length
    return true
  }
  if (event.key === 'Enter') {
    selectItem(selectedIndex.value)
    return true
  }
  return false
}

defineExpose({ onKeyDown })
</script>

<template>
  <div class="slash-menu">
    <button
      v-for="(item, index) in items"
      :key="index"
      :class="{ 'is-selected': index === selectedIndex }"
      @click="selectItem(index)"
    >
      {{ item.title }}
    </button>
  </div>
</template>
```
