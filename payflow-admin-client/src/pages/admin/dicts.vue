<template>
  <div class="page-table-shell dict-page">
    <div class="filter-bar filter-bar--stacked">
      <div>
        <p class="dashboard-section-title m-0 mb-2">数据字典</p>
        <p class="page-hint-text m-0">
          集中展示前后端共用的枚举与状态映射，便于筛选项、列表标签与接口字段对齐。数据来自
          <code class="dict-inline-code">GET /api/v1/admin/dicts</code>，只读参考。
        </p>
      </div>
      <div class="filter-row dict-toolbar-row">
        <span class="filter-label">字典分类</span>
        <el-select v-model="activeKey" filterable placeholder="选择分类" style="width: 220px">
          <el-option
            v-for="key in tabKeys"
            :key="key"
            :label="dictMetaFor(key).title"
            :value="key"
          >
            <span>{{ dictMetaFor(key).title }}</span>
            <span class="dict-option-sub">{{ key }}</span>
          </el-option>
        </el-select>
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索编码或中文含义"
          style="width: 240px"
          prefix-icon="Search"
        />
        <el-button class="btn-outline" icon="Refresh" @click="loadDicts">刷新</el-button>
        <el-button
          v-if="activeMeta?.kind === 'enum' && filteredEnumRows.length"
          class="btn-outline"
          icon="DocumentCopy"
          @click="copyEnumCodes"
        >
          复制全部编码
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="content-card dict-panel">
      <el-empty v-if="loaded && !tabKeys.length" description="暂无字典数据" />

      <template v-else-if="loaded && activeMeta">
        <div class="dict-panel__header">
          <div>
            <h3 class="dict-panel__title">{{ activeMeta.title }}</h3>
            <p class="dict-panel__desc">{{ activeMeta.description }}</p>
            <div v-if="activeMeta.usedIn.length" class="dict-panel__tags">
              <span class="dict-panel__tags-label">使用场景</span>
              <el-tag
                v-for="scene in activeMeta.usedIn"
                :key="scene"
                size="small"
                type="info"
                effect="plain"
              >
                {{ scene }}
              </el-tag>
            </div>
          </div>
          <div class="dict-panel__stats">
            <span class="dict-stat">
              <strong>{{ rowCount }}</strong> 条
            </span>
            <span class="dict-stat dict-stat--muted">{{ activeKey }}</span>
          </div>
        </div>

        <!-- 简单枚举：编码 + 含义 + 标签预览 -->
        <el-table
          v-if="activeMeta.kind === 'enum'"
          table-layout="auto"
          :data="filteredEnumRows"
          stripe
          size="small"
          class="data-table dict-table"
          empty-text="无匹配项"
        >
          <el-table-column label="枚举编码" prop="code" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="cell-mono font-medium">{{ row.code }}</span>
            </template>
          </el-table-column>
          <el-table-column label="中文含义" prop="label" min-width="140" show-overflow-tooltip />
          <el-table-column label="界面预览" min-width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.tagType as any" effect="plain">
                {{ row.label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="说明" min-width="200">
            <template #default="{ row }">
              <span class="page-hint-text">{{ enumHint(activeKey, row.code) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="100" align="center" class-name="col-actions">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="copyText(row.code)">复制编码</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 状态映射：库表值 ↔ 管理端 UI -->
        <el-table
          v-else
          table-layout="auto"
          :data="filteredMappingRows"
          stripe
          size="small"
          class="data-table dict-table"
          empty-text="无匹配项"
        >
          <el-table-column label="数据库值 (db)" prop="db" min-width="140">
            <template #default="{ row }">
              <span class="cell-mono">{{ row.db }}</span>
            </template>
          </el-table-column>
          <el-table-column label="管理端值 (ui)" prop="ui" min-width="120">
            <template #default="{ row }">
              <span class="cell-mono">{{ row.ui }}</span>
            </template>
          </el-table-column>
          <el-table-column label="界面展示" prop="label" min-width="120" />
          <el-table-column label="标签预览" min-width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.uiTagType as any" effect="plain">{{ row.label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="说明" min-width="220">
            <template #default>
              <span class="page-hint-text">列表与筛选项使用 ui 值；落库与渠道同步使用 db 值。</span>
            </template>
          </el-table-column>
        </el-table>

        <el-collapse class="dict-dev-collapse">
          <el-collapse-item title="开发者视图（原始 JSON）" name="json">
            <div class="dict-dev-actions">
              <el-button size="small" class="btn-outline" @click="copyJson">复制 JSON</el-button>
            </div>
            <pre class="dict-dev-pre">{{ formatJson(raw[activeKey]) }}</pre>
          </el-collapse-item>
        </el-collapse>
      </template>

      <p v-else-if="!loading && !loaded" class="page-hint-text m-0">字典加载失败，请刷新页面重试</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDicts } from '@/api/admin'
import {
  dictMetaFor,
  parseEnumRows,
  parseMappingRows,
  filterEnumRows,
  filterMappingRows,
  type DictCategoryMeta,
  type EnumDictRow,
} from '@/utils/dictDisplay'

const ENUM_HINTS: Record<string, Record<string, string>> = {
  orderStatus: {
    CREATED: '下单成功，等待用户支付',
    PAYING: '已唤起收银台，支付进行中',
    PAID: '支付成功（与 SUCCESS 同义场景）',
    SUCCESS: '交易成功',
    EXPIRED: '超过支付有效期未支付',
    FAILED: '支付失败或关单',
    CLOSED: '订单已关闭',
    REFUNDED: '订单已发生退款',
  },
  refundStatus: {
    REFUNDING: '退款处理中，等待渠道结果',
    REFUNDED: '退款已成功',
    FAILED: '退款失败',
    CLOSED: '退款单已关闭',
  },
  payChannels: {
    ALIPAY: '支付宝渠道',
    WECHAT_PAY: '微信渠道',
  },
}

const loading = ref(false)
const loaded = ref(false)
const raw = ref<Record<string, unknown>>({})
const activeKey = ref('refundStatus')
const keyword = ref('')

const tabKeys = computed(() => Object.keys(raw.value))

const activeMeta = computed((): DictCategoryMeta | null => {
  if (!activeKey.value) return null
  return dictMetaFor(activeKey.value)
})

const enumRows = computed(() => parseEnumRows(activeKey.value, raw.value[activeKey.value]))
const mappingRows = computed(() => parseMappingRows(raw.value[activeKey.value]))

const filteredEnumRows = computed(() => filterEnumRows(enumRows.value, keyword.value))
const filteredMappingRows = computed(() => filterMappingRows(mappingRows.value, keyword.value))

const rowCount = computed(() =>
  activeMeta.value?.kind === 'mapping' ? filteredMappingRows.value.length : filteredEnumRows.value.length,
)

function enumHint(dictKey: string, code: string): string {
  return ENUM_HINTS[dictKey]?.[code] ?? '—'
}

function formatJson(val: unknown): string {
  try {
    return JSON.stringify(val, null, 2)
  } catch {
    return String(val)
  }
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

async function copyEnumCodes() {
  const text = filteredEnumRows.value.map((r) => r.code).join('\n')
  await copyText(text)
}

async function copyJson() {
  await copyText(formatJson(raw.value[activeKey.value]))
}

async function loadDicts() {
  loading.value = true
  try {
    raw.value = await getDicts()
    const keys = Object.keys(raw.value)
    if (keys.length && !keys.includes(activeKey.value)) {
      activeKey.value = keys[0]
    }
    loaded.value = true
  } catch {
    loaded.value = false
    ElMessage.error('加载字典失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadDicts()
})
</script>

<style scoped>
.dict-page .dict-inline-code {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--pf-primary-muted);
  color: var(--pf-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.dict-toolbar-row {
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.dict-option-sub {
  margin-left: 8px;
  font-size: 12px;
  color: var(--pf-sidebar-text-muted);
  font-family: ui-monospace, monospace;
}

.dict-panel {
  padding: 20px 24px;
}

.dict-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--pf-card-border);
}

.dict-panel__title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--pf-text-primary);
}

.dict-panel__desc {
  margin: 0 0 10px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--pf-text-secondary);
}

.dict-panel__tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.dict-panel__tags-label {
  font-size: 12px;
  color: var(--pf-sidebar-text-muted);
}

.dict-panel__stats {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}

.dict-stat {
  font-size: 14px;
  color: var(--pf-text-primary);
}

.dict-stat strong {
  font-size: 20px;
  font-weight: 700;
  color: var(--pf-primary);
  margin-right: 4px;
}

.dict-stat--muted {
  font-size: 12px;
  font-family: ui-monospace, monospace;
  color: var(--pf-sidebar-text-muted);
}

.dict-table {
  margin-bottom: 16px;
}

.dict-dev-collapse {
  margin-top: 8px;
  border: none;
}

.dict-dev-collapse :deep(.el-collapse-item__header) {
  font-size: 13px;
  color: var(--pf-text-secondary);
  background: transparent;
  border-bottom: 1px solid var(--pf-card-border);
}

.dict-dev-collapse :deep(.el-collapse-item__wrap) {
  background: transparent;
  border: none;
}

.dict-dev-actions {
  margin-bottom: 8px;
}

.dict-dev-pre {
  margin: 0;
  padding: 14px 16px;
  max-height: 280px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.5;
  border-radius: 10px;
  background: var(--pf-primary-muted);
  border: 1px solid var(--pf-card-border);
  color: var(--pf-text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
</style>
