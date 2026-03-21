---
name: PrimeVue 4 Form & Validation
description: Using the new Form and FormField components with Valibot/Zod.
libraries_used: @primevue/forms, valibot
---

# PrimeVue 4 Form & Validation

## Objective
Learn how to use the modern `Form` component which manages its own state, moving away from manual `v-model` arrays.

## Prerequisites
- `@primevue/forms`
- Validation library (e.g., `valibot` or `zod`)

## Instructions

### 1. Define Initial Values
Create an object representing the form's starting state.

### 2. Define the Resolver
Create a validator using your library of choice.

### 3. Use `FormField`
Wrap your inputs in `FormField` to handle error messages and validation automatically.

## Code Example

```vue
<script setup>
import { ref } from 'vue';
import { v } from 'valibot';

const initialValues = ref({
    username: '',
    email: ''
});

const resolver = ({ values }) => {
    const schema = v.object({
        username: v.string([v.minLength(3, 'Username must be at least 3 chars')]),
        email: v.string([v.email('Invalid email address')])
    });
    // PrimeVue Forms expect a specific return structure for errors
    // Use the resolve function from @primevue/forms for standard adaptation
};

const onFormSubmit = ({ valid, values }) => {
    if (valid) {
        console.log('Form Submitted:', values);
    }
};
</script>

<template>
    <Form :initialValues="initialValues" :resolver="resolver" @submit="onFormSubmit" class="flex flex-col gap-4">
        <FormField name="username" v-slot="$field" class="flex flex-col gap-1">
            <InputText type="text" placeholder="Username" />
            <Message v-if="$field.invalid" severity="error" size="small" variant="simple">
                {{ $field.error.message }}
            </Message>
        </FormField>
        
        <Button type="submit" label="Submit" />
    </Form>
</template>
```
