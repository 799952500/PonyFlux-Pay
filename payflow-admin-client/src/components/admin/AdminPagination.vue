<template>
  <div
    v-show="!hideOnSinglePage || total > 0"
    class="admin-pagination pagination-bar"
  >
    <span class="admin-pagination__total">共 {{ total }} 条</span>
    <el-select
      :model-value="pageSize"
      class="admin-pagination__sizes"
      placement="top"
      teleported
      :popper-options="sizesPopperOptions"
      popper-class="admin-pagination-sizes-popper"
      @update:model-value="handleSizeChange"
    >
      <el-option
        v-for="s in pageSizes"
        :key="s"
        :label="`${s} 条/页`"
        :value="s"
      />
    </el-select>
    <el-pagination
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next, jumper"
      background
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup lang="ts">
const currentPage = defineModel<number>('currentPage', { required: true })
const pageSize = defineModel<number>('pageSize', { required: true })

withDefaults(
  defineProps<{
    total: number
    pageSizes?: number[]
    hideOnSinglePage?: boolean
  }>(),
  {
    pageSizes: () => [10, 20, 50, 100],
    hideOnSinglePage: false,
  },
)

const emit = defineEmits<{
  sizeChange: [number]
  currentChange: [number]
}>()

/** 每页条数下拉优先向上展开，避免贴底时被系统任务栏挡住 */
const sizesPopperOptions = {
  modifiers: [
    {
      name: 'flip',
      options: {
        fallbackPlacements: ['top', 'top-start', 'top-end', 'bottom'],
      },
    },
    {
      name: 'preventOverflow',
      options: { padding: 16 },
    },
  ],
}

function handleSizeChange(val: number) {
  pageSize.value = val
  emit('sizeChange', val)
}

function handleCurrentChange(val: number) {
  currentPage.value = val
  emit('currentChange', val)
}
</script>
