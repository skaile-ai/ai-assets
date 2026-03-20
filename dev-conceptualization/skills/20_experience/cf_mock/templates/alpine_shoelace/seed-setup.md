---
name: alpine-shoelace-seed-setup
description: Plain JS seed data with scenario switcher for Alpine.js mockups
---

# Seed Data Setup — Alpine.js + Shoelace

Convert `seed.json` scenarios to a JS module used by all screen pages.

## Pattern (`cf__shared/seed.js`)

```javascript
// seed.js — seed data for prototype rendering
const SEED = {
  populated: { /* ... from seed.json populated scenario */ },
  empty: { /* ... from seed.json empty scenario */ },
  edge_cases: { /* ... from seed.json edge_cases scenario */ }
};

let CURRENT_SCENARIO = 'populated';

function getSeedData() {
  return SEED[CURRENT_SCENARIO];
}

function setScenario(name) {
  CURRENT_SCENARIO = name;
  // Trigger Alpine reactivity by dispatching a custom event
  window.dispatchEvent(new CustomEvent('scenario-changed', { detail: name }));
}
```

## Scenario Switcher

Add a floating Shoelace radio group so reviewers can toggle scenarios:

```html
<div class="fixed bottom-4 right-4 z-50 bg-white shadow-lg rounded-lg p-3 border">
  <sl-radio-group label="Data Scenario" value="populated"
                   x-on:sl-change="setScenario($event.target.value)">
    <sl-radio-button value="populated">Populated</sl-radio-button>
    <sl-radio-button value="empty">Empty</sl-radio-button>
    <sl-radio-button value="edge_cases">Edge Cases</sl-radio-button>
  </sl-radio-group>
</div>
```
