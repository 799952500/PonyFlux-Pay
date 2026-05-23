<template>
  <div class="appearance-settings">
    <p class="appearance-settings__desc">
      主题与表格密度会保存到您的账号，在任何设备登录后自动恢复；侧栏折叠状态同样会同步。
    </p>

    <el-alert
      v-if="!tableDensityStore.storageAvailable && tableDensityStore.storageHintShown"
      type="info"
      :closable="false"
      show-icon
      class="mb-4"
      title="当前浏览器无法使用本地缓存，仍以账号云端偏好为准。"
    />

    <section class="preferences-section">
      <h3 class="preferences-section__title">主题</h3>
      <div class="preferences-theme-grid">
        <button
          v-for="preset in THEME_PRESETS"
          :key="preset.key"
          type="button"
          class="preferences-theme-card"
          :class="{ 'preferences-theme-card--active': themeStore.themeKey === preset.key }"
          @click="themeStore.setTheme(preset.key)"
        >
          <span class="preferences-theme-card__swatch" :style="{ background: preset.color }" />
          <span class="preferences-theme-card__label">{{ preset.label }}</span>
          <el-icon v-if="themeStore.themeKey === preset.key" class="preferences-theme-card__check"><Check /></el-icon>
        </button>
      </div>
    </section>

    <section class="preferences-section">
      <h3 class="preferences-section__title">表格密度</h3>
      <el-radio-group
        :model-value="tableDensityStore.densityKey"
        class="preferences-density-group"
        @change="onDensityChange"
      >
        <el-radio
          v-for="preset in TABLE_DENSITY_PRESETS"
          :key="preset.key"
          :value="preset.key"
          border
          class="preferences-density-radio"
        >
          <span class="preferences-density-radio__label">{{ preset.label }}</span>
          <span class="preferences-density-radio__hint">{{ preset.description }}</span>
        </el-radio>
      </el-radio-group>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Check } from '@element-plus/icons-vue'
import { useThemeStore, THEME_PRESETS } from '@/stores/theme'
import { useTableDensityStore, TABLE_DENSITY_PRESETS, type TableDensityKey } from '@/stores/tableDensity'

const themeStore = useThemeStore()
const tableDensityStore = useTableDensityStore()

function onDensityChange(value: string | number | boolean | undefined) {
  if (value === 'standard' || value === 'compact') {
    tableDensityStore.setDensity(value as TableDensityKey)
  }
}
</script>

<style scoped>
.appearance-settings__desc {
  font-size: 14px;
  color: var(--pf-text-secondary);
  margin: 0 0 24px;
  line-height: 1.6;
}

.preferences-section {
  margin-bottom: 28px;
}

.preferences-section__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--pf-text-primary);
  margin: 0 0 12px;
}

.preferences-theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.preferences-theme-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid var(--pf-card-border);
  border-radius: 10px;
  background: var(--pf-card-bg);
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
  text-align: left;
}

.preferences-theme-card:hover {
  border-color: var(--pf-primary);
}

.preferences-theme-card--active {
  border-color: var(--pf-primary);
  box-shadow: 0 0 0 1px var(--pf-primary-muted);
}

.preferences-theme-card__swatch {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  flex-shrink: 0;
}

.preferences-theme-card__label {
  flex: 1;
  font-size: 14px;
  color: var(--pf-text-primary);
}

.preferences-theme-card__check {
  color: var(--pf-primary);
}

.preferences-density-group {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  width: 100%;
}

.preferences-density-radio {
  margin: 0 !important;
  height: auto !important;
  padding: 12px 16px !important;
  width: 100%;
}

.preferences-density-radio :deep(.el-radio__label) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  white-space: normal;
}

.preferences-density-radio__label {
  font-weight: 600;
  color: var(--pf-text-primary);
}

.preferences-density-radio__hint {
  font-size: 13px;
  color: var(--pf-text-secondary);
  font-weight: 400;
}
</style>
