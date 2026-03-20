---
name: preact-htm-seed-setup
description: Preact signals for scenario switching in Preact mockups
---

# Seed Data Setup — Preact + HTM

Convert `seed.json` scenarios to an ES module with Preact signals.

## Pattern (`cf__shared/seed.js`)

```javascript
// seed.js — seed data with Preact signals for scenario switching
import { signal } from '@preact/signals';

export const SEED = {
  populated: { /* ... from seed.json populated scenario */ },
  empty: { /* ... from seed.json empty scenario */ },
  edge_cases: { /* ... from seed.json edge_cases scenario */ }
};

export const currentScenario = signal('populated');
```

## Scenario Switcher Component

```javascript
import { html } from 'htm/preact';
import { currentScenario } from './seed.js';

export function ScenarioSwitcher() {
  const scenarios = ['populated', 'empty', 'edge_cases'];

  return html`
    <div class="fixed bottom-4 right-4 z-50 bg-white shadow-lg rounded-lg p-3 border flex gap-1">
      ${scenarios.map(s => html`
        <button
          class="px-3 py-1.5 rounded text-sm font-medium transition-colors ${
            currentScenario.value === s
              ? 'bg-brand-primary text-white'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          }"
          onClick=${() => { currentScenario.value = s; }}>
          ${s.replace('_', ' ')}
        </button>
      `)}
    </div>
  `;
}
```

## Notes

- `currentScenario` is a Preact signal — changes automatically re-render all subscribers.
- Include `<${ScenarioSwitcher} />` at the bottom of every page.
- All pages share the same `seed.js` module — switching scenario updates reactively.
