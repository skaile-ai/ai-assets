---
name: vue-primevue-setup
description: PrimeVue plugin registration + component setup pattern
---

# PrimeVue Setup — Plugin + Component Registration

## Pattern (`cf__shared/primevue-setup.js`)

```javascript
// primevue-setup.js — register PrimeVue plugin + components
function setupPrimeVue(app) {
  app.use(PrimeVue.default);

  // Register all needed components globally
  // (determined by reading screen specs)
  app.component('Button', PrimeVue.Button);
  app.component('DataTable', PrimeVue.DataTable);
  app.component('Column', PrimeVue.Column);
  app.component('Dialog', PrimeVue.Dialog);
  app.component('InputText', PrimeVue.InputText);
  app.component('Dropdown', PrimeVue.Dropdown);
  app.component('Toolbar', PrimeVue.Toolbar);
  app.component('Panel', PrimeVue.Panel);
  app.component('Toast', PrimeVue.Toast);
  app.component('Skeleton', PrimeVue.Skeleton);
  app.component('ProgressBar', PrimeVue.ProgressBar);
  app.component('Tag', PrimeVue.Tag);
  app.component('SelectButton', PrimeVue.SelectButton);
  app.component('Sidebar', PrimeVue.Sidebar);
  app.component('ConfirmDialog', PrimeVue.ConfirmDialog);
  app.component('OverlayPanel', PrimeVue.OverlayPanel);
  // ... add components as needed by screen specs
}
```

## Rules

- Register **all** PrimeVue components used across any screen page.
- Scan screen specs to determine which components are needed.
- Every component must be registered — unregistered components cause "Unknown component" warnings.
- Call `setupPrimeVue(app)` before `app.mount('#app')` in every page.
