---
name: Headless UI Selectors
description: Pro-grade Node, Link, and Color selectors for Tiptap using Headless UI
libraries_used: "@tiptap/vue-3, @headlessui/vue, lucide-vue-next"
---

# Headless UI Selectors

## Objective
Build a professional-grade editor toolbar or bubble menu using `@headlessui/vue` for accessible, unstyled UI components that fit perfectly into your design system.

## Prerequisites
- Tiptap Vue 3
- `@headlessui/vue`
- `lucide-vue-next` (icons)

## Instructions
1.  **Toolbar Structure**: Create a container for your selectors.
2.  **Node Selector**: Use `Menu` and `MenuItems` from Headless UI to create a dropdown for switching between Paragraph, Headings, and Lists.
3.  **Link Selector**: Use `Popover` to create an overlay for entering and editing URLs.
4.  **Color Picker**: Implement a grid of buttons within a `Menu` to apply colors via `editor.chain().focus().setColor(color).run()`.

## Code Example

### `NodeSelector.vue`
```vue
<script setup lang="ts">
import { Menu, MenuButton, MenuItems, MenuItem } from '@headlessui/vue'
import { ChevronDown } from 'lucide-vue-next'
import { Editor } from '@tiptap/vue-3'

const props = defineProps<{ editor: Editor }>()

const nodes = [
  { name: 'Text', command: () => props.editor.chain().focus().setParagraph().run(), isActive: () => props.editor.isActive('paragraph') },
  { name: 'Heading 1', command: () => props.editor.chain().focus().toggleHeading({ level: 1 }).run(), isActive: () => props.editor.isActive('heading', { level: 1 }) },
  // ... more nodes
]
</script>

<template>
  <Menu as="div" class="relative inline-block text-left">
    <MenuButton class="inline-flex w-full justify-center px-4 py-2 text-sm font-medium">
      Node Type <ChevronDown class="ml-2 h-4 w-4" />
    </MenuButton>
    <MenuItems class="absolute right-0 mt-2 w-56 origin-top-right bg-white shadow-lg ring-1 ring-black ring-opacity-5">
      <MenuItem v-for="node in nodes" :key="node.name" v-slot="{ active }">
        <button
          @click="node.command"
          :class="[active ? 'bg-gray-100' : '', node.isActive() ? 'font-bold text-blue-600' : '', 'group flex w-full items-center px-2 py-2 text-sm']"
        >
          {{ node.name }}
        </button>
      </MenuItem>
    </MenuItems>
  </Menu>
</template>
```
