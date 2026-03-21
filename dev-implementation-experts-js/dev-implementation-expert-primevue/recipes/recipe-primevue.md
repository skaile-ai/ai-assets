# PrimeVue v4 with Nuxt 3: A Programming Recipe

This guide provides a comprehensive recipe for installing, configuring, and using PrimeVue v4 in a Nuxt 3 application. It covers both styled and unstyled (with Tailwind CSS) theming approaches and includes code examples for common components.

## 1. Installation and Configuration

First, you need to add the `@primevue/nuxt-module` to your Nuxt project and install the required dependencies.

### Step 1: Add the Nuxt Module
Use the Nuxt CLI to automatically add and configure the module:```bash
npx nuxi@latest module add primevue
```

### Step 2: Install Dependencies
Install PrimeVue and the default theme presets.
```bash
# Using npm
npm install primevue @primeuix/themes

# Using yarn
yarn add primevue @primeuix/themes
```

### Step 3: Configure `nuxt.config.ts`
The Nuxt module will handle most of the configuration. Your `nuxt.config.ts` should look like this:

```typescript
export default defineNuxtConfig({
  modules: [
    '@primevue/nuxt-module'
  ],
  primevue: {
    // Options
  }
})
```
The module automatically handles component and directive registration with tree-shaking support. [2]

---

## 2. Theming: Styled vs. Unstyled

PrimeVue v4 offers two distinct theming approaches: a pre-styled, token-based system and an unstyled mode for full control with CSS libraries like Tailwind CSS.

### Styled Mode Analysis [2]

Styled mode is the default and recommended way to use PrimeVue if you want a beautiful theme out-of-the-box.

**Architecture:**
The styling is decoupled from the components and managed by themes. A theme consists of a "base" (CSS rules) and a "preset" (design tokens). You can think of design tokens as CSS variables for colors, spacing, and other visual elements. PrimeVue offers several built-in presets like Aura (default), Lara, and Material.

**Configuration:**
To configure a theme, you specify it in your `nuxt.config.ts`. The module comes with the `Aura` theme by default.

```typescript
// nuxt.config.ts
import Aura from '@primeuix/themes/aura';

export default defineNuxtConfig({
  modules: [
    '@primevue/nuxt-module'
  ],
  primevue: {
    theme: {
      preset: Aura,
      options: {
        prefix: 'p',
        darkModeSelector: 'system', // or '.dark-mode'
        cssLayer: false
      }
    }
  }
})
```

**Customization:**
You can customize a preset by overriding its design tokens. For example, to change the primary color to indigo:

```typescript
// nuxt.config.ts
import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

const MyPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{indigo.50}',
      100: '{indigo.100}',
      200: '{indigo.200}',
      300: '{indigo.300}',
      400: '{indigo.400}',
      500: '{indigo.500}',
      600: '{indigo.600}',
      700: '{indigo.700}',
      800: '{indigo.800}',
      900: '{indigo.900}',
      950: '{indigo.950}'
    }
  }
});


export default defineNuxtConfig({
  // ...
  primevue: {
    theme: {
      preset: MyPreset
    }
  }
});
```

### Unstyled Mode with Tailwind CSS Analysis [1]

Unstyled mode removes all built-in PrimeVue styling, giving you complete control. This is ideal when you want to use a utility-first CSS framework like Tailwind CSS.

**Architecture:**
You enable `unstyled: true` in the configuration. Then, you style each component using the `pt` (Pass Through) property. This property gives you access to the component's internal structure (like `root`, `label`, `icon`, etc.) and allows you to apply CSS classes directly to them.

**Configuration:**
First, ensure you have Tailwind CSS installed and configured in your Nuxt project. Then, enable unstyled mode in `nuxt.config.ts`.

```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  modules: [
    '@primevue/nuxt-module'
  ],
  primevue: {
    options: {
      unstyled: true
    }
  }
})
```

**Usage with Tailwind CSS:**
You can apply Tailwind classes directly in your components.

```vue
<template>
  <div class="p-8">
    <Button
      label="Search"
      icon="pi pi-search"
      :pt="{
        root: 'bg-teal-500 hover:bg-teal-700 active:bg-teal-900 cursor-pointer py-2 px-4 rounded-full border-0 flex gap-2',
        label: 'text-white font-bold text-lg',
        icon: 'text-white text-xl'
      }"
    />
  </div>
</template>

<script setup>
</script>
```

To avoid repetition, you can define global pass-through styles in `nuxt.config.ts`:

```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  // ...
  primevue: {
    options: {
      unstyled: true
    },
    pt: {
      button: {
        root: 'bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded'
      },
      panel: {
        header: 'bg-gray-800 text-white p-4 rounded-t-lg',
        content: 'p-4 border border-gray-800 rounded-b-lg'
      }
    }
  }
})
```

---

## 3. Component Usage Examples

Here are examples for some of the most common components, grouped by category. [3]

### Form

```vue
<template>
  <div class="flex flex-col gap-4 p-4">
    <!-- InputText -->
    <InputText v-model="textValue" placeholder="Enter text" />

    <!-- Calendar -->
    <DatePicker v-model="dateValue" showIcon />

    <!-- Checkbox -->
    <div class="flex items-center gap-2">
      <Checkbox v-model="checked" :binary="true" />
      <label>I agree</label>
    </div>

    <!-- Dropdown -->
    <Select v-model="selectedCity" :options="cities" optionLabel="name" placeholder="Select a City" />
  </div>
</template>

<script setup>
import { ref } from 'vue';

const textValue = ref('');
const dateValue = ref(null);
const checked = ref(false);
const selectedCity = ref();
const cities = ref([
    { name: 'New York', code: 'NY' },
    { name: 'Rome', code: 'RM' },
    { name: 'London', code: 'LDN' },
    { name: 'Istanbul', code: 'IST' },
    { name: 'Paris', code: 'PRS' }
]);
</script>
```

### Button

```vue
<template>
  <div class="flex gap-2 p-4">
    <!-- Standard Button -->
    <Button label="Submit" icon="pi pi-check" />

    <!-- Severity -->
    <Button label="Warning" severity="warning" />

    <!-- Rounded -->
    <Button label="Info" severity="info" rounded />
  </div>
</template>
```

### Data

```vue
<template>
  <div class="p-4">
    <!-- DataTable -->
    <DataTable :value="products">
      <Column field="code" header="Code"></Column>
      <Column field="name" header="Name"></Column>
      <Column field="category" header="Category"></Column>
      <Column field="quantity" header="Quantity"></Column>
    </DataTable>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const products = ref([
    { code: 'P01', name: 'Product A', category: 'Electronics', quantity: 10 },
    { code: 'P02', name: 'Product B', category: 'Books', quantity: 5 },
]);
</script>```

### Panel

```vue
<template>
  <div class="flex flex-col gap-4 p-4">
    <!-- Panel -->
    <Panel header="Basic Panel">
      <p>
        Lorem ipsum dolor sit amet, consectetur adipiscing elit...
      </p>
    </Panel>

    <!-- Accordion -->
    <Accordion>
      <AccordionTab header="Header I">
        Content for Accordion Tab 1
      </AccordionTab>
      <AccordionTab header="Header II">
        Content for Accordion Tab 2
      </AccordionTab>
    </Accordion>

    <!-- Card -->
    <Card>
        <template #title>Simple Card</template>
        <template #content>
            <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit.</p>
        </template>
    </Card>
  </div>
</template>
```

### Overlay

```vue
<template>
  <div class="p-4">
    <Button label="Show Dialog" @click="dialogVisible = true" />

    <!-- Dialog -->
    <Dialog v-model:visible="dialogVisible" modal header="Header" :style="{ width: '50rem' }">
        <p>
            Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
        </p>
    </Dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const dialogVisible = ref(false);
</script>
```

### Messages

```vue
<template>
  <div class="p-4 flex flex-col gap-4">
    <!-- Toast -->
    <Toast />
    <Button @click="showToast" label="Show" />

    <!-- Message -->
    <Message severity="info">This is an info message.</Message>
  </div>
</template>

<script setup>
import { useToast } from "primevue/usetoast";
const toast = useToast();

const showToast = () => {
    toast.add({ severity: 'info', summary: 'Info', detail: 'Message Content', life: 3000 });
};
</script>
```