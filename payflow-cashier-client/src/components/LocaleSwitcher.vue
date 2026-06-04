<template>
  <div class="locale-switcher">
    <label class="locale-switcher__label" for="locale-select">{{ t('locale.label') }}</label>
    <select
      id="locale-select"
      class="locale-switcher__select"
      :value="current"
      @change="onChange"
    >
      <option value="zh-CN">{{ t('locale.zhCN') }}</option>
      <option value="zh-TW">{{ t('locale.zhTW') }}</option>
      <option value="en-US">{{ t('locale.enUS') }}</option>
    </select>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { applyDisplayLocale, type DisplayLocaleCode } from '@/composables/useDisplayLocale'

const { t, locale } = useI18n()

const current = computed(() => locale.value as DisplayLocaleCode)

async function onChange(ev: Event) {
  const value = (ev.target as HTMLSelectElement).value as DisplayLocaleCode
  await applyDisplayLocale(value, { persist: true })
}
</script>

<style scoped>
.locale-switcher {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.locale-switcher__label {
  color: var(--pf-text-secondary, #64748b);
}

.locale-switcher__select {
  border: 1px solid var(--pf-border, #e2e8f0);
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 13px;
  background: var(--pf-surface, #fff);
  color: var(--pf-text-body, #334155);
}
</style>
