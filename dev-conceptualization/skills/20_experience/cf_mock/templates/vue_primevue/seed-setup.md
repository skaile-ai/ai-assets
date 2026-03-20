---
name: vue-primevue-seed-setup
description: Vue.ref scenario switcher for PrimeVue mockups
---

# Seed Data Setup — Vue 3 + PrimeVue

Convert `seed.json` scenarios to a JS module with Vue reactivity.

## Pattern (`cf__shared/seed.js`)

```javascript
// seed.js — seed data with Vue.ref scenario switcher
const SEED = {
  populated: { /* ... from seed.json populated scenario */ },
  empty: { /* ... from seed.json empty scenario */ },
  edge_cases: { /* ... from seed.json edge_cases scenario */ }
};

const CURRENT_SCENARIO = Vue.ref('populated');
```

## Scenario Switcher

Use PrimeVue `SelectButton` component as a floating switcher:

```html
<div class="fixed bottom-4 right-4 z-50 bg-white shadow-lg rounded-lg p-3 border">
  <SelectButton v-model="scenario"
                :options="['populated', 'empty', 'edge_cases']"
                :allow-empty="false" />
</div>
```

In the page setup:

```javascript
setup() {
  const scenario = CURRENT_SCENARIO;
  const data = computed(() => SEED[scenario.value]);
  return { data, scenario };
}
```

## Notes

- `CURRENT_SCENARIO` is a Vue `ref` — changes automatically propagate to all computed properties.
- All pages share the same `seed.js` file — switching scenario updates every page on reload.
- Empty scenario should show PrimeVue empty states (DataTable empty message, Skeleton placeholders).
