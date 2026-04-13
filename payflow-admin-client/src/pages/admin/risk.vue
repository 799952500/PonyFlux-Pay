<template>
  <div>
    <div class="flex items-center justify-end mb-4">
      <el-button type="primary" icon="Plus" size="default" @click="handleAdd">新增规则</el-button>
    </div>

    <div class="space-y-4">
      <div v-for="rule in ruleList" :key="rule.ruleId" class="bg-white rounded-xl card-shadow p-5">
        <div class="flex items-start justify-between gap-4">
          <div class="flex items-start gap-4 flex-1 min-w-0">
            <div class="w-10 h-10 rounded-lg flex items-center justify-center shrink-0" :style="{ background: ruleTypeBg[rule.ruleType] }">{{ ruleTypeIcon[rule.ruleType] ?? '🛡️' }}</div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="font-semibold text-gray-800">{{ rule.ruleName }}</span>
                <el-tag size="small" :type="ruleTypeTag[rule.ruleType] ?? 'info'" effect="plain">{{ ruleTypeLabel[rule.ruleType] }}</el-tag>
              </div>
              <p class="text-sm text-gray-500 mb-2">{{ rule.description }}</p>
              <div class="flex items-center gap-4 text-xs text-gray-400">
                <span>阈值：<strong class="text-gray-700">{{ rule.threshold }}</strong> {{ rule.unit }}</span>
                <span>创建于 {{ rule.createdAt }}</span>
              </div>
            </div>
          </div>
          <div class="flex items-center gap-3 shrink-0">
            <el-switch v-model="rule.enabled" active-color="#10b981" inactive-color="#d1d5db" @change="handleToggle(rule)" />
            <el-button type="primary" link size="small" icon="Edit" @click="handleEdit(rule)">编辑</el-button>
          </div>
        </div>
      </div>

      <div v-if="ruleList.length === 0 && !loading" class="bg-white rounded-xl p-12 text-center text-gray-400">
        <p class="text-4xl mb-3">🛡️</p>
        <p>暂无风控规则，点击上方按钮新增</p>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增风控规则' : '编辑风控规则'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px" size="default">
        <el-form-item label="规则名称" prop="ruleName"><el-input v-model="formData.ruleName" placeholder="请输入规则名称" /></el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="formData.ruleType" placeholder="请选择规则类型" style="width: 100%">
            <el-option label="单笔金额限制" value="AMOUNT_SINGLE" />
            <el-option label="24小时累计金额" value="AMOUNT_DAILY" />
            <el-option label="同一IP下单次数" value="IP_LIMIT" />
            <el-option label="同一手机号次数" value="MOBILE_LIMIT" />
            <el-option label="自定义规则" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值" prop="threshold"><el-input-number v-model="formData.threshold" :min="1" :max="999999999" style="width: 100%" /></el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-select v-model="formData.unit" placeholder="请选择单位" style="width: 100%">
            <el-option label="元" value="元" /><el-option label="次" value="次" /><el-option label="笔" value="笔" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description"><el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入规则描述" /></el-form-item>
        <el-form-item label="启用状态"><el-switch v-model="formData.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认{{ dialogMode === 'add' ? '新增' : '保存' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getRiskRules, updateRiskRule } from '@/api/admin'
import type { RiskRule } from '@/types'

const loading = ref(false)
const ruleList = ref<RiskRule[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<Partial<RiskRule>>({ ruleId: '', ruleName: '', ruleType: 'AMOUNT_SINGLE', threshold: 10000, unit: '元', description: '', enabled: true })
const formRules: FormRules = { ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }], ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }], threshold: [{ required: true, message: '请输入阈值', trigger: 'blur' }], unit: [{ required: true, message: '请选择单位', trigger: 'change' }] }

const ruleTypeIcon: Record<string, string> = { AMOUNT_SINGLE: '💰', AMOUNT_DAILY: '📅', IP_LIMIT: '🌐', MOBILE_LIMIT: '📱', CUSTOM: '⚙️' }
const ruleTypeBg: Record<string, string> = { AMOUNT_SINGLE: '#fef3e2', AMOUNT_DAILY: '#e8f4fd', IP_LIMIT: '#f3e8ff', MOBILE_LIMIT: '#e6f7ed', CUSTOM: '#f0f0f0' }
const ruleTypeTag: Record<string, string> = { AMOUNT_SINGLE: 'warning', AMOUNT_DAILY: 'primary', IP_LIMIT: 'danger', MOBILE_LIMIT: 'success', CUSTOM: 'info' }
const ruleTypeLabel: Record<string, string> = { AMOUNT_SINGLE: '单笔限额', AMOUNT_DAILY: '日累计', IP_LIMIT: 'IP限制', MOBILE_LIMIT: '手机号限制', CUSTOM: '自定义' }

async function loadRules() { loading.value = true; try { ruleList.value = await getRiskRules() } catch { ElMessage.error('加载风控规则失败') } finally { loading.value = false } }

async function handleToggle(rule: RiskRule) { try { await updateRiskRule(rule.ruleId, { enabled: rule.enabled }); ElMessage.success(`规则「${rule.ruleName}」${rule.enabled ? '已启用' : '已禁用'}`) } catch { rule.enabled = !rule.enabled; ElMessage.error('更新失败，请重试') } }

function handleAdd() { dialogMode.value = 'add'; Object.assign(formData, { ruleId: '', ruleName: '', ruleType: 'AMOUNT_SINGLE', threshold: 10000, unit: '元', description: '', enabled: true }); dialogVisible.value = true }
function handleEdit(rule: RiskRule) { dialogMode.value = 'edit'; Object.assign(formData, { ...rule }); dialogVisible.value = true }

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (dialogMode.value === 'add') {
        const newRule: RiskRule = { ruleId: `RULE${Date.now()}`, ruleName: formData.ruleName!, ruleType: formData.ruleType as RiskRule['ruleType'], threshold: formData.threshold!, unit: formData.unit!, description: formData.description ?? '', enabled: formData.enabled ?? true, createdAt: new Date().toLocaleString(), updatedAt: new Date().toLocaleString() }
        ruleList.value.unshift(newRule)
        ElMessage.success('规则已新增')
      } else {
        const updated = await updateRiskRule(formData.ruleId!, formData as Partial<RiskRule>)
        const idx = ruleList.value.findIndex((r) => r.ruleId === updated.ruleId)
        if (idx !== -1) ruleList.value[idx] = updated
        ElMessage.success('规则已保存')
      }
      dialogVisible.value = false
    } catch { ElMessage.error('操作失败，请重试') } finally { submitting.value = false }
  })
}

onMounted(() => { loadRules() })
</script>
