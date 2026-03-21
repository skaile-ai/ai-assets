# PrimeVue Unstyled Mode with Tailwind CSS: A Specific Recipe

This document provides a detailed, step-by-step recipe for using PrimeVue's "Unstyled Mode" in a Nuxt 3 project, specifically with Tailwind CSS. This approach gives you complete control over the look and feel of your components.

## 1. What is Unstyled Mode?

Unstyled Mode strips all default visual styling from PrimeVue components. It delivers components as "functional skeletons" that include logic, accessibility, and structure, but no cosmetic CSS. [3] This is the ideal choice when you want to use a utility-first CSS framework like Tailwind CSS to implement a custom design system.

The core mechanism for styling in this mode is the **`pt` (Pass Through)** property, which allows you to "pass through" HTML attributes and CSS classes directly to the internal elements of a component.

## 2. Initial Setup

This guide assumes you have a Nuxt 3 project and have already installed the `@primevue/nuxt-module`.

### Step 1: Install Tailwind CSS
First, add the `@nuxtjs/tailwindcss` module to your project.

```bash
npx nuxi@latest module add tailwindcss
```
This will install the module and create a `tailwind.config.js` and an `assets/css/tailwind.css` file.

### Step 2: Configure Unstyled Mode
In your `nuxt.config.ts`, enable the `unstyled` option within the `primevue` configuration. This single setting disables all default PrimeVue themes and styling.

```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  modules: [
    '@primevue/nuxt-module',
    '@nuxtjs/tailwindcss'
  ],

  primevue: {
    options: {
      unstyled: true // Enable Unstyled Mode
    }
  }
})
```

## 3. Styling Components: The `pt` Property

The `pt` property is the cornerstone of styling in unstyled mode. It's an object where each key corresponds to a specific internal element of a PrimeVue component (e.g., `root`, `label`, `input`). The value is a string of CSS classes you want to apply.

To find the available `pt` sections for any component, refer to the "Pass Through" section in that component's official documentation.

### Inline Styling Example: Button

Here's how to style a single Button component directly in your template using Tailwind CSS classes.

```vue
<template>
  <div class="p-8">
    <Button
      label="Primary Action"
      icon="pi pi-check"
      :pt="{
        root: 'bg-blue-600 border border-blue-600 text-white font-semibold py-2 px-4 rounded-lg hover:bg-blue-700 transition-colors duration-200 flex items-center gap-2',
        label: 'text-base',
        icon: 'text-lg'
      }"
    />
  </div>
</template>

<script setup>
// No script logic needed for styling
</script>
```
In this example:
*   `root`: Styles the main `<button>` element.
*   `label`: Styles the `<span>` containing the button's text.
*   `icon`: Styles the `<i>` element for the icon.

## 4. Reusable Styling: Global Pass Through

Applying styles inline is great for one-off components, but it's not scalable. For a consistent design system, you should define your styles globally. This is done by creating a custom Pass Through object and registering it in `nuxt.config.ts`.

### Step 1: Create a Global PT File
Create a new file to house your global styles. A good location is `presets/MyPreset.js` or a similar directory. This file will export an object containing the styles for each component.

```javascript
// presets/MyPreset.js

export default {
  button: {
    root: 'bg-blue-600 border border-blue-600 text-white font-semibold py-2 px-4 rounded-lg hover:bg-blue-700 transition-colors duration-200 flex items-center gap-2',
    label: 'text-base',
    icon: 'text-lg'
  },
  panel: {
    root: 'border border-gray-300 rounded-lg shadow-md',
    header: 'bg-gray-100 text-gray-800 p-4 font-bold rounded-t-lg border-b border-gray-300',
    content: 'p-4'
  },
  inputtext: {
    root: 'border border-gray-300 rounded-md p-2 focus:outline-none focus:ring-2 focus:ring-blue-500'
  }
  // ... add styles for other components
}
```

### Step 2: Register the Global PT in `nuxt.config.ts`
Import your preset file and add it to the `primevue.pt` option.

```typescript
// nuxt.config.ts
import MyPreset from './presets/MyPreset';

export default defineNuxtConfig({
  modules: [
    '@primevue/nuxt-module',
    '@nuxtjs/tailwindcss'
  ],

  primevue: {
    options: {
      unstyled: true
    },
    pt: MyPreset // Register the global Pass Through object
  }
})
```

Now, any `<Button>`, `<Panel>`, or `<InputText>` component used in your application will automatically have these styles applied without needing to specify the `pt` prop inline.

## 5. Component Usage Examples with Global Styles

With the global styles configured, using the components in your templates becomes incredibly clean.

### Form Components

```vue
<template>
  <div class="w-full max-w-md p-8 space-y-4">
    <h2 class="text-2xl font-bold">User Profile</h2>
    
    <!-- InputText will use global styles -->
    <div class="flex flex-col gap-2">
      <label for="username">Username</label>
      <InputText id="username" />
    </div>

    <!-- Dropdown (assuming you added 'select' styles to your preset) -->
    <div class="flex flex-col gap-2">
      <label for="city">City</label>
      <Select id="city" :options="cities" optionLabel="name" placeholder="Select a City" class="w-full" />
    </div>

    <!-- Button will use global styles -->
    <Button label="Save Profile" />
  </div>
</template>

<script setup>
import { ref } from 'vue';
const cities = ref([
    { name: 'New York', code: 'NY' },
    { name: 'London', code: 'LDN' }
]);
</script>
```

### Data Table

```vue
<template>
  <div class="p-8">
    <!-- Panel will use global styles -->
    <Panel header="Product List">
      <!-- DataTable styles would be defined in the global preset -->
      <DataTable :value="products">
        <Column field="name" header="Name"></Column>
        <Column field="category" header="Category"></Column>
        <Column field="price" header="Price"></Column>
      </DataTable>
    </Panel>
  </div>
</template>

<script setup>
import { ref } from 'vue';
const products = ref([
    { name: 'Laptop', category: 'Electronics', price: 1200 },
    { name: 'Book', category: 'Stationery', price: 25 }
]);
</script>
```

By using the unstyled mode with a global pass-through configuration, you achieve a highly maintainable and fully customized component library that integrates seamlessly with Tailwind CSS.
