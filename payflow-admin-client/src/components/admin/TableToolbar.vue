<script setup lang="ts">
defineProps<{
  title?: string
  total?: number
  hint?: string
}>()
</script>

<template>
  <div class="table-toolbar">
    <div>
      <div v-if="title" class="table-toolbar__title">{{ title }}</div>
      <div class="table-toolbar__hint">
        <slot name="hint">
          <template v-if="hint">{{ hint }}</template>
          <template v-else-if="total != null">共 {{ total }} 条记录</template>
        </slot>
      </div>
    </div>
    <div v-if="$slots.actions" class="table-toolbar__actions">
      <slot name="actions" />
    </div>
  </div>
</template>
