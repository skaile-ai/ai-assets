# PrimeVue Confirm Dialog Recipe

This recipe outlines the steps to integrate and use PrimeVue's `ConfirmDialog` component with the `ConfirmationService` in a Nuxt.js application.

## 1. Configure Nuxt.js for ConfirmationService

To enable the `ConfirmationService` globally, you need to add it to your `nuxt.config.ts` modules.

**`nuxt.config.ts`**
```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  // ... other configurations
  modules: [
    // ... other modules
    '@primevue/nuxt-module',
    'primevue/confirmationservice' // Add this line
  ],
  // ... other configurations
});
```

## 2. Implement ConfirmDialog in your Vue Component

In your Vue component where you want to use the confirmation dialog, you need to:
- Import `ConfirmDialog` and `useConfirm`.
- Add the `<ConfirmDialog></ConfirmDialog>` component to your template.
- Use the `useConfirm` composable to trigger the dialog.

**Example: `app/pages/notes/[id].vue`**

```vue
<template>
    <!-- ... existing template content ... -->

    <!-- Add the ConfirmDialog component -->
    <ConfirmDialog></ConfirmDialog>

    <!-- Button to trigger the confirmation -->
    <Button label="Delete" icon="pi pi-trash" severity="danger" @click="confirmDeleteNote" v-if="note.id" />

    <!-- ... rest of your template ... -->
</template>

<script setup lang="ts">
import type { Note } from '~/types/directus';
import { useConfirm } from "primevue/useconfirm"; // Import useConfirm
import ConfirmDialog from 'primevue/confirmdialog'; // Import ConfirmDialog

const toast = useToast();
const route = useRoute();
const confirm = useConfirm(); // Initialize useConfirm
const { fetchNote, createNote, updateNote, deleteNotes } = useDirectusNotes();

const note = ref<Note>({ title: '' });
const submitted = ref(false);

const noteId = computed(() => route.params.id as string);

// ... existing useAsyncData and saveNote functions ...

const confirmDeleteNote = () => {
    confirm.require({
        message: `Are you sure you want to delete <b>${note.value.title}</b>?`,
        header: 'Confirm Deletion',
        icon: 'pi pi-exclamation-triangle',
        acceptClass: 'p-button-danger',
        accept: async () => {
            try {
                if (note.value.id) {
                    await deleteNotes([note.value.id]);
                    toast.add({severity:'success', summary: 'Successful', detail: 'Note Deleted', life: 3000});
                    navigateTo('/notes');
                }
            } catch (e: any) {
                console.error("Error deleting note:", e);
                toast.add({severity:'error', summary: 'Error', detail: `Failed to delete note: ${e.message || 'Unknown error'}.`, life: 5000});
            }
        },
        reject: () => {
            toast.add({severity:'info', summary:'Rejected', detail:'You have rejected', life: 3000});
        }
    });
};
</script>
```

### Explanation of `confirm.require` options:
- `message`: The main message displayed in the dialog. HTML can be used for formatting (e.g., `<b>`).
- `header`: The title of the confirmation dialog.
- `icon`: An icon to display next to the message (e.g., `pi pi-exclamation-triangle` from PrimeIcons).
- `acceptClass`: CSS class for the accept button (e.g., `p-button-danger` for a red button).
- `accept`: A callback function executed when the user clicks the "Accept" button. This is where your action (e.g., deleting the note) should be performed.
- `reject`: A callback function executed when the user clicks the "Reject" button.