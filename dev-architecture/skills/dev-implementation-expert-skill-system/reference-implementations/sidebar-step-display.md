# Reference Implementation: Sidebar Step Display

Vue component showing pipeline step status in a sidebar. From concept-forge's `SidebarPipelineStep.vue`.

## Component

```vue
<template>
  <div class="flex items-center gap-1.5 px-5 py-1.5 cursor-pointer select-none transition-colors group"
    :class="[containerClasses, step.optional ? 'opacity-60' : '']"
    @click="$emit('click')"
  >
    <!-- Status icon -->
    <UIcon :name="iconName" class="w-3.5 h-3.5 flex-shrink-0"
      :class="[iconColor, isActive ? 'animate-spin' : '']" />

    <!-- Step name -->
    <span class="text-xs font-medium flex-1 truncate" :class="nameColor">
      {{ step.name }}
      <span v-if="step.optional" class="text-[10px] text-gray-400 font-normal">(opt)</span>
    </span>

    <!-- impl_status badge (features/screens) -->
    <span v-if="step.implStatus && (step.implStatus.implemented > 0 || step.implStatus.tested > 0)"
      class="text-[9px] tabular-nums flex-shrink-0 px-1 py-0.5 rounded"
      :class="implBadgeClass" :title="implBadgeTitle">
      {{ implBadgeLabel }}
    </span>

    <!-- File count (only when no implStatus) -->
    <span v-if="step.fileCount > 0 && !step.implStatus"
      class="text-[10px] text-gray-400 tabular-nums flex-shrink-0">
      {{ step.fileCount }}
    </span>

    <!-- Expand chevron -->
    <UIcon v-if="step.fileCount > 0 && step.folder"
      :name="isFolderOpen ? 'i-heroicons-chevron-down' : 'i-heroicons-chevron-right'"
      class="w-3 h-3 text-gray-400 flex-shrink-0" />
  </div>
</template>
```

## Status Icon Mapping

```typescript
const iconName = computed(() => {
  if (props.isActive) return 'i-heroicons-arrow-path'       // spinning
  if (status === 'complete' || status === 'approved') return 'i-heroicons-check-circle'
  if (status === 'in_progress') return 'i-heroicons-ellipsis-horizontal-circle'
  if (status === 'blocked') return 'i-heroicons-lock-closed'
  if (props.step.canRun) return 'i-heroicons-play-circle'
  return 'i-heroicons-minus-circle'                          // not_started, deps not met
})

const iconColor = computed(() => {
  if (props.isActive) return 'text-primary-500'
  if (status === 'complete' || status === 'approved') return 'text-green-500'
  if (status === 'in_progress') return 'text-yellow-500'
  if (status === 'blocked') return 'text-gray-300'
  if (props.step.canRun) return 'text-primary-400'
  return 'text-gray-300'
})
```

## impl_status Badge

```typescript
const implBadgeLabel = computed(() => {
  const s = props.step.implStatus
  if (!s) return ''
  const total = s.pending + s.implemented + s.tested
  if (s.tested === total) return `${total} tested`          // all tested
  if (s.implemented + s.tested === total) return `${total} impl`  // all implemented
  return `${s.implemented + s.tested}/${total}`              // partial
})

const implBadgeClass = computed(() => {
  const s = props.step.implStatus
  const total = s.pending + s.implemented + s.tested
  if (s.tested === total) return 'bg-green-100 text-green-700'     // all tested
  if (s.implemented + s.tested === total) return 'bg-blue-100 text-blue-700'  // all impl
  return 'bg-yellow-100 text-yellow-700'                            // partial
})
```

## Phase Grouping

Steps are grouped by phase in the sidebar:

```vue
<div v-for="phase in sortedPhases" :key="phase.id">
  <!-- Phase header (collapsible) -->
  <button @click="togglePhase(phase.id)">
    {{ phase.name }}
    <span>{{ phaseCompletedCount(phase) }}/{{ phaseStepCount(phase) }}</span>
  </button>

  <!-- Steps in this phase (optionally grouped by sub_phase) -->
  <div v-if="isPhaseOpen(phase.id)">
    <SidebarPipelineStep v-for="step in getPhaseSteps(phase.id)" ... />
  </div>
</div>
```
