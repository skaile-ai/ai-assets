---
name: PrimeVue Forms + Zod Validation
description: Form handling with @primevue/forms and Zod schema validation in Nuxt, including error display and toast notifications.
libraries_used: ["@primevue/forms", "primevue", "zod"]
---

# PrimeVue Forms + Zod Validation

## 1. Install

```bash
pnpm add @primevue/forms zod
```

## 2. Form with Zod Resolver

```vue
<script setup lang="ts">
import { z } from 'zod';
import { zodResolver } from '@primevue/forms/resolvers/zod';
import { useToast } from 'primevue/usetoast';
import { Form, FormField } from '@primevue/forms';

const toast = useToast();

const schema = zodResolver(
  z.object({
    username: z.string().min(3, 'Username must be at least 3 characters'),
    email: z.string().email('Please enter a valid email address'),
    age: z.number().min(18, 'You must be at least 18 years old'),
    feedback: z.string().optional(),
  })
);

const initialValues = {
  username: '',
  email: '',
  age: undefined,
  feedback: '',
};

const onFormSubmit = ({ valid, values }: { valid: boolean; values: any }) => {
  if (valid) {
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Form submitted successfully!',
      life: 3000,
    });
  } else {
    toast.add({
      severity: 'error',
      summary: 'Validation Error',
      detail: 'Please fix the highlighted fields.',
      life: 3000,
    });
  }
};
</script>

<template>
  <Form
    :resolver="schema"
    :initialValues="initialValues"
    @submit="onFormSubmit"
    class="flex flex-col gap-4 max-w-md"
  >
    <FormField v-slot="$field" name="username" initialValue="">
      <InputText
        type="text"
        placeholder="Username"
        :class="{ 'p-invalid': $field.invalid }"
      />
      <Message
        v-if="$field.invalid"
        severity="error"
        size="small"
        variant="simple"
      >
        {{ $field.error?.message }}
      </Message>
    </FormField>

    <FormField v-slot="$field" name="email" initialValue="">
      <InputText
        type="email"
        placeholder="Email"
        :class="{ 'p-invalid': $field.invalid }"
      />
      <Message
        v-if="$field.invalid"
        severity="error"
        size="small"
        variant="simple"
      >
        {{ $field.error?.message }}
      </Message>
    </FormField>

    <FormField v-slot="$field" name="age">
      <InputNumber
        placeholder="Age"
        :class="{ 'p-invalid': $field.invalid }"
      />
      <Message
        v-if="$field.invalid"
        severity="error"
        size="small"
        variant="simple"
      >
        {{ $field.error?.message }}
      </Message>
    </FormField>

    <FormField v-slot="$field" name="feedback" initialValue="">
      <Textarea
        placeholder="Feedback (optional)"
        rows="3"
      />
    </FormField>

    <Button type="submit" label="Submit" />
  </Form>
</template>
```

## Key Patterns

- **`zodResolver()`** wraps a Zod schema for PrimeVue Form's resolver prop
- **`FormField v-slot="$field"`** exposes `$field.invalid`, `$field.error` for inline validation
- **`p-invalid` class** triggers PrimeVue's built-in error styling on inputs
- **`Message` component** with `severity="error"` and `variant="simple"` for inline errors
- **Toast** for submission-level feedback (success/error)
- **`initialValues`** sets form defaults; matches schema shape

## Nuxt Config for Required Components

```typescript
// nuxt.config.ts
primevue: {
  components: {
    include: ['Button', 'InputText', 'InputNumber', 'Textarea', 'Message', 'Toast']
  }
}
```
