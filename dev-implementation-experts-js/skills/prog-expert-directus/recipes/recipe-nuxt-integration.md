# Recipe: Nuxt 3 & Directus Integration

This recipe covers the best patterns for integrating Directus into a Nuxt 3 application using the official `@directus/sdk`.

## 1. Installation

```bash
npm install @directus/sdk
```

## 2. Nuxt Plugin Setup

Create a plugin to initialize the Directus client:

```typescript
// plugins/directus.ts
import { createDirectus, rest, staticToken, authentication } from '@directus/sdk';
import type { Schema } from '~/types/directus';

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig();
  
  const client = createDirectus<Schema>(config.public.directusUrl)
    .with(rest())
    .with(authentication())
    .with(staticToken(config.directusToken));

  return {
    provide: {
      directus: client
    }
  };
});
```

## 3. Configuration

Update `nuxt.config.ts`:

```typescript
export default defineNuxtConfig({
  runtimeConfig: {
    directusToken: process.env.DIRECTUS_TOKEN,
    public: {
      directusUrl: process.env.DIRECTUS_URL || 'http://localhost:8055'
    }
  }
});
```

## 4. Usage in Composables

Create a composable for type-safe data fetching:

```typescript
// composables/useDirectus.ts
import { readItems } from '@directus/sdk';

export const useDirectusItems = () => {
  const { $directus } = useNuxtApp();

  const getItems = async <T extends keyof Schema>(collection: T, query?: any) => {
    return await $directus.request(readItems(collection, query));
  };

  return { getItems };
};
```

## 5. Usage in Components

```vue
<script setup lang="ts">
const { getItems } = useDirectusItems();
const { data: posts } = await useAsyncData('posts', () => getItems('posts'));
</script>

<template>
  <div v-for="post in posts" :key="post.id">
    {{ post.title }}
  </div>
</template>
```
