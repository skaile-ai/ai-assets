---
name: recipe-action-components
description: Vue 3 UI components for the Typed Action Protocol — SkillInputDialog (dynamic form), SkillApprovalDialog (approve/reject), SkillStateIndicator (colored dot), SkillActionRenderer (compositor), SkillAiDrawer (full chat panel).
libraries_used: [vue3, nuxt4, @nuxt/ui, typescript]
---

# Recipe: Action UI Components

## 1. `SkillInputDialog.vue`

Dynamic form rendered from `DialogField[]`. Shown when `pendingInput` is set.

```vue
<!-- app/components/SkillInputDialog.vue -->
<template>
  <UModal v-model:open="open" :title="title" prevent-close>
    <template #body>
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <div v-for="field in fields" :key="field.id">
          <label class="block text-sm font-medium mb-1">
            {{ field.label }}
            <span v-if="field.required" class="text-red-500 ml-1">*</span>
          </label>

          <UTextarea
            v-if="field.type === 'textarea'"
            v-model="values[field.id]"
            :placeholder="field.placeholder"
            :required="field.required"
            rows="3"
          />
          <USelect
            v-else-if="field.type === 'select'"
            v-model="values[field.id]"
            :options="field.options"
            :required="field.required"
          />
          <UToggle
            v-else-if="field.type === 'toggle'"
            v-model="values[field.id]"
          />
          <UInput
            v-else
            v-model="values[field.id]"
            :placeholder="field.placeholder"
            :required="field.required"
          />
        </div>
      </form>
    </template>
    <template #footer>
      <div class="flex gap-2 justify-end">
        <UButton variant="ghost" @click="handleCancel">Cancel</UButton>
        <UButton @click="handleSubmit" :disabled="!isValid">Continue</UButton>
      </div>
    </template>
  </UModal>
</template>

<script setup lang="ts">
import type { DialogField } from '~/shared/types/skill-actions'

const props = defineProps<{
  open: boolean
  fields: DialogField[]
  context?: string
  actionId: string
}>()

const emit = defineEmits<{
  submit: [values: Record<string, unknown>]
  cancel: []
}>()

const title = computed(() => props.context ?? 'Provide Inputs')
const values = reactive<Record<string, unknown>>({})

// Initialize defaults
watchEffect(() => {
  for (const field of props.fields) {
    if (!(field.id in values)) {
      values[field.id] = field.default ?? (field.type === 'toggle' ? false : '')
    }
  }
})

const isValid = computed(() =>
  props.fields.filter(f => f.required).every(f => {
    const v = values[f.id]
    return v !== '' && v !== null && v !== undefined
  })
)

function handleSubmit() {
  if (!isValid.value) return
  emit('submit', { ...values })
}

function handleCancel() {
  emit('cancel')
}
</script>
```

## 2. `SkillApprovalDialog.vue`

Review and approve/reject a completed skill run.

```vue
<!-- app/components/SkillApprovalDialog.vue -->
<template>
  <UModal v-model:open="open" title="Review & Approve" size="lg" prevent-close>
    <template #body>
      <div class="space-y-4">
        <p class="text-sm text-muted">{{ summary }}</p>

        <div v-if="artifacts.length > 0">
          <p class="text-sm font-medium mb-2">Changed Files:</p>
          <ul class="space-y-1">
            <li
              v-for="artifact in artifacts"
              :key="artifact"
              class="text-sm font-mono text-muted flex items-center gap-2"
            >
              <UIcon name="i-heroicons-document-text" class="w-4 h-4" />
              {{ artifact }}
            </li>
          </ul>
        </div>

        <UTextarea
          v-model="feedback"
          placeholder="Optional feedback..."
          rows="2"
        />
      </div>
    </template>
    <template #footer>
      <div class="flex gap-2 justify-end">
        <UButton variant="ghost" color="red" @click="handleReject">
          <UIcon name="i-heroicons-x-mark" /> Reject
        </UButton>
        <UButton color="green" @click="handleApprove">
          <UIcon name="i-heroicons-check" /> Approve
        </UButton>
      </div>
    </template>
  </UModal>
</template>

<script setup lang="ts">
const props = defineProps<{
  open: boolean
  summary: string
  artifacts: string[]
  actionId: string
}>()

const emit = defineEmits<{
  approve: [feedback?: string]
  reject: [feedback?: string]
}>()

const feedback = ref('')

function handleApprove() {
  emit('approve', feedback.value || undefined)
}

function handleReject() {
  emit('reject', feedback.value || undefined)
}
</script>
```

## 3. `SkillStateIndicator.vue`

Colored animated dot showing current skill execution state.

```vue
<!-- app/components/SkillStateIndicator.vue -->
<template>
  <div v-if="runState" class="flex items-center gap-2 text-sm">
    <!-- Colored dot -->
    <span
      class="w-2 h-2 rounded-full shrink-0"
      :class="[dotColor, isPulsing ? 'animate-pulse' : '']"
    />
    <!-- State label -->
    <span class="text-muted">{{ stateLabel }}</span>
    <!-- Active tool -->
    <span v-if="runState.currentTool" class="text-xs text-muted font-mono">
      · {{ runState.currentTool }}
    </span>
  </div>
</template>

<script setup lang="ts">
import type { SkillRunState } from '~/shared/types/skill-actions'

const props = defineProps<{
  runState: SkillRunState | null
}>()

const dotColor = computed(() => {
  switch (props.runState?.state) {
    case 'collecting_inputs': return 'bg-yellow-500'
    case 'running': return 'bg-primary-500'
    case 'checkpoint': return 'bg-blue-500'
    case 'awaiting_approval': return 'bg-amber-500'
    case 'completed': return 'bg-green-500'
    case 'failed': return 'bg-red-500'
    case 'blocked': return 'bg-orange-500'
    default: return 'bg-muted-300'
  }
})

const isPulsing = computed(() => {
  const s = props.runState?.state
  return s === 'running' || s === 'collecting_inputs'
})

const stateLabel = computed(() => {
  const skill = props.runState?.skillName
  switch (props.runState?.state) {
    case 'collecting_inputs': return `${skill} · collecting inputs`
    case 'running': return `${skill} · running`
    case 'checkpoint': return `${skill} · checkpoint`
    case 'awaiting_approval': return `${skill} · awaiting approval`
    case 'completed': return `${skill} · completed`
    case 'failed': return `${skill} · failed`
    case 'blocked': return `${skill} · blocked`
    default: return 'Idle'
  }
})
</script>
```

## 4. `SkillActionRenderer.vue`

Compositor — renders the appropriate UI for whatever action is pending.

```vue
<!-- app/components/SkillActionRenderer.vue -->
<template>
  <div>
    <!-- Input form dialog -->
    <SkillInputDialog
      v-if="pendingInput"
      :open="!!pendingInput"
      :fields="pendingInput.fields"
      :context="pendingInput.context"
      :action-id="pendingInput.actionId"
      @submit="(values) => skills.submitInput(pendingInput!.actionId, values)"
      @cancel="skills.cancel()"
    />

    <!-- Approval dialog -->
    <SkillApprovalDialog
      v-if="pendingApproval"
      :open="!!pendingApproval"
      :summary="pendingApproval.summary"
      :artifacts="pendingApproval.artifacts"
      :action-id="pendingApproval.actionId"
      @approve="(fb) => skills.submitApproval(pendingApproval!.actionId, true, fb)"
      @reject="(fb) => skills.submitApproval(pendingApproval!.actionId, false, fb)"
    />

    <!-- Notifications toast -->
    <div v-if="latestNotification" class="fixed bottom-4 right-4 z-50">
      <UAlert
        :color="latestNotification.level === 'error' ? 'red' : latestNotification.level === 'warn' ? 'yellow' : 'blue'"
        :title="latestNotification.message"
        class="max-w-sm"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ReturnType } from 'typescript'
import type { useSkillActions } from '~/composables/useSkillActions'

const props = defineProps<{
  skills: ReturnType<typeof useSkillActions>
}>()

const { pendingInput, pendingApproval, notifications } = props.skills

const latestNotification = computed(() => {
  const n = notifications.value
  return n.length > 0 ? n[n.length - 1] : null
})
</script>
```

## 5. `SkillAiDrawer.vue`

Full AI chat drawer integrating all components above.

```vue
<!-- app/components/SkillAiDrawer.vue -->
<template>
  <USlideover v-model:open="isOpen" side="right" :ui="{ width: 'w-[420px]' }">
    <template #header>
      <div class="flex items-center justify-between w-full">
        <span class="font-semibold">AI Assistant</span>
        <SkillStateIndicator :run-state="skills.runState.value" />
      </div>
    </template>

    <template #body>
      <div class="flex flex-col h-full">
        <!-- Action renderer (dialogs + notifications) -->
        <SkillActionRenderer :skills="skills" />

        <!-- Messages -->
        <div ref="messagesEl" class="flex-1 overflow-y-auto space-y-3 p-4">
          <div
            v-for="msg in skills.messages.value"
            :key="msg.id"
            :class="msg.role === 'user' ? 'flex justify-end' : 'flex justify-start'"
          >
            <div
              class="max-w-[85%] rounded-lg px-3 py-2 text-sm"
              :class="msg.role === 'user'
                ? 'bg-primary-500 text-white'
                : 'bg-muted-100 dark:bg-muted-800'"
            >
              <div v-if="msg.streaming" class="flex items-center gap-1">
                <span>{{ msg.content }}</span>
                <span class="animate-pulse">▋</span>
              </div>
              <span v-else>{{ msg.content }}</span>
            </div>
          </div>

          <!-- Tool call indicator -->
          <div v-if="skills.currentTool.value" class="flex items-center gap-2 text-xs text-muted py-1">
            <UIcon name="i-heroicons-wrench" class="animate-spin w-3 h-3" />
            <span>{{ skills.currentTool.value }}</span>
          </div>
        </div>

        <!-- Start questions (when idle) -->
        <div v-if="!skills.isRunning.value && startQuestions.length > 0 && skills.messages.value.length === 0" class="p-3 space-y-2">
          <p class="text-xs text-muted">Suggested actions:</p>
          <button
            v-for="q in startQuestions"
            :key="q"
            class="w-full text-left text-sm px-3 py-2 rounded-md bg-muted-50 dark:bg-muted-900 hover:bg-muted-100 transition"
            @click="handleStartQuestion(q)"
          >
            {{ q }}
          </button>
        </div>

        <!-- Input bar -->
        <div class="border-t p-3">
          <div class="flex gap-2">
            <UInput
              v-model="inputText"
              placeholder="Ask anything..."
              class="flex-1"
              :disabled="skills.isRunning.value"
              @keydown.enter.exact.prevent="handleSend"
            />
            <UButton
              :disabled="!inputText.trim() || skills.isRunning.value"
              @click="handleSend"
            >
              <UIcon name="i-heroicons-paper-airplane" />
            </UButton>
            <UButton
              v-if="skills.isRunning.value"
              color="red"
              variant="ghost"
              @click="skills.abort()"
            >
              <UIcon name="i-heroicons-stop" />
            </UButton>
          </div>
        </div>
      </div>
    </template>
  </USlideover>
</template>

<script setup lang="ts">
import { useSkillActions } from '~/composables/useSkillActions'

const props = defineProps<{
  concept: string | null
  startQuestions?: string[]
  open?: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  'skill-complete': []
}>()

const isOpen = computed({
  get: () => props.open ?? false,
  set: (v) => emit('update:open', v),
})

const conceptRef = computed(() => props.concept)
const skills = useSkillActions(conceptRef)

const inputText = ref('')
const messagesEl = ref<HTMLElement | null>(null)
const startQuestions = computed(() => props.startQuestions ?? [])

// Auto-scroll to bottom on new messages
watch(
  () => skills.messages.value.length,
  async () => {
    await nextTick()
    messagesEl.value?.scrollTo({ top: messagesEl.value.scrollHeight, behavior: 'smooth' })
  }
)

// Emit skill-complete when run state reaches completed
watch(
  () => skills.runState.value?.state,
  (state) => {
    if (state === 'completed') emit('skill-complete')
  }
)

async function handleSend() {
  const text = inputText.value.trim()
  if (!text) return
  inputText.value = ''
  await skills.sendPrompt(text)
}

async function handleStartQuestion(question: string) {
  await skills.sendPrompt(question)
}
</script>
```

## Usage in a Page

```vue
<!-- app/pages/concepts/index.vue (relevant section) -->
<template>
  <div>
    <!-- ... rest of page ... -->
    <SkillAiDrawer
      v-model:open="drawerOpen"
      :concept="activeConcept"
      :start-questions="pipeline.getStartQuestions(pipeline.focusedStepId.value ?? '')"
      @skill-complete="pipeline.refresh()"
    />
  </div>
</template>

<script setup lang="ts">
const pipeline = usePipelineState()
const drawerOpen = ref(false)
const activeConcept = computed(() => /* your concept name */ '_pipeline')
</script>
```
