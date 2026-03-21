---
name: DataTable Mastery
description: Server-side pagination, sorting, and filtering patterns.
libraries_used: primevue/datatable
---

# DataTable Mastery

## Objective
Implement high-performance DataTables that handle large datasets via server-side logic.

## Prerequisites
- Nuxt 3
- `DataTable`, `Column` components

## Instructions

### 1. Enable Lazy Loading
Set `lazy` to `true` on the `DataTable`.

### 2. Handle State
Listen to `@page`, `@sort`, and `@filter` events to trigger API calls.

### 3. Sync with URL (Optional)
Use Nuxt `useRoute` query parameters to keep the table state shareable.

## Code Example

```vue
<script setup>
const loading = ref(false);
const totalRecords = ref(0);
const customers = ref([]);
const lazyParams = ref({
    first: 0,
    rows: 10,
    page: 1,
    sortField: null,
    sortOrder: null,
    filters: {}
});

const loadLazyData = async () => {
    loading.value = true;
    try {
        const { data } = await $fetch('/api/customers', { params: lazyParams.value });
        customers.value = data.records;
        totalRecords.value = data.total;
    } finally {
        loading.value = false;
    }
};

const onPage = (event) => {
    lazyParams.value = event;
    loadLazyData();
};
</script>

<template>
    <DataTable 
        :value="customers" 
        lazy 
        paginator 
        :rows="10" 
        :totalRecords="totalRecords" 
        :loading="loading"
        @page="onPage"
        @sort="onSort"
        @filter="onFilter"
    >
        <Column field="name" header="Name" sortable filter />
        <Column field="country.name" header="Country" sortable filter />
    </DataTable>
</template>
```
