---
name: Nuxt Content 3
description: Nuxt Content 3 setup with typed collections, markdown pages, JSON data, navigation generation, and content rendering.
libraries_used: ["@nuxt/content"]
---

# Nuxt Content 3

## 1. Install

```bash
pnpm add @nuxt/content
```

```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  modules: ['@nuxt/content'],
});
```

## 2. Content Config with Collections

Create `content.config.ts` in project root:

```typescript
import { defineContentConfig, defineCollection, z } from '@nuxt/content';

export default defineContentConfig({
  collections: {
    // Markdown collections
    sites: defineCollection({
      type: 'page',
      source: 'sites/**',
    }),
    blog: defineCollection({
      type: 'page',
      source: 'blog/**',
    }),
    content: defineCollection({
      type: 'page',
      source: 'content/**',
    }),
    // JSON/YAML data collections
    faq: defineCollection({
      type: 'data',
      source: 'faq/**',
      schema: z.object({
        question: z.string(),
        answer: z.string(),
      }),
    }),
  },
});
```

## 3. Directory Structure

```
content/
├── sites/
│   ├── about.md
│   └── features.md
├── blog/
│   ├── first-post.md     # frontmatter: title, date, description
│   └── second-post.md
├── content/
│   ├── impressum.md
│   └── datenschutz.md
└── faq/
    └── general.json       # [{ "question": "...", "answer": "..." }]
```

## 4. Querying Collections

### Fetch all items from a collection

```vue
<script setup lang="ts">
const { data: posts } = await useAsyncData('blog', () =>
  queryCollection('blog').all()
);
</script>
```

### Fetch with sorting and limiting

```vue
<script setup lang="ts">
const { data: latestPosts } = await useAsyncData('latest-blog', () =>
  queryCollection('blog')
    .order('date', 'DESC')
    .limit(3)
    .all()
);
</script>
```

### Fetch by path

```vue
<script setup lang="ts">
const { data: page } = await useAsyncData('page', () =>
  queryCollection('content')
    .path('/impressum')
    .first()
);
</script>
```

## 5. Navigation Generation

Build navigation trees from content collections:

```vue
<script setup lang="ts">
const { data: navigation } = await useAsyncData('nav', () =>
  queryCollectionNavigation('sites')
);
</script>
```

## 6. Rendering Content

### Markdown pages

```vue
<template>
  <ContentRenderer v-if="page" :value="page" />
</template>

<script setup lang="ts">
const { data: page } = await useAsyncData('page', () =>
  queryCollection('sites').path('/about').first()
);
</script>
```

### Content in a modal (dynamic loading)

```vue
<script setup lang="ts">
const contentPath = ref('/datenschutz');
const { data: content } = await useAsyncData(
  `content-${contentPath.value}`,
  () => queryCollection('content').path(contentPath.value).first()
);
</script>

<template>
  <Dialog v-model:visible="isVisible">
    <ContentRenderer v-if="content" :value="content" />
  </Dialog>
</template>
```

## 7. Composable for Modal Content

Pattern for reusable content modal state:

```typescript
// composables/useContentModal.ts
export const useContentModal = () => {
  const isVisible = useState<boolean>('contentModal:visible', () => false);
  const contentPath = useState<string>('contentModal:path', () => '');
  const contentTitle = useState<string>('contentModal:title', () => '');

  const showModal = (path: string, title: string) => {
    contentPath.value = path;
    contentTitle.value = title;
    isVisible.value = true;
  };

  const hideModal = () => {
    isVisible.value = false;
  };

  return { isVisible, contentPath, contentTitle, showModal, hideModal };
};
```

## Tips

- Use `type: 'page'` for markdown, `type: 'data'` for JSON/YAML
- Content collections are typed — use Zod schemas for data collections
- `queryCollectionNavigation()` auto-builds nav trees from directory structure
- `useState()` shares state across components without Pinia
