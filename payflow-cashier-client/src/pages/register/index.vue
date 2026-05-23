<template>
  <PortalShell
    :title="step === 0 ? '商户入驻申请' : step === 1 ? '申请已提交' : '接入指引'"
    :subtitle="stepSubtitle"
    wide
  >
    <template #header-extra>
      <router-link to="/onboarding/result" class="portal-link">查询审核结果</router-link>
    </template>

    <el-steps v-if="step < 2" :active="step" finish-status="success" align-center simple class="mb-6" />

    <el-form
      v-if="step === 0"
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="portal-form"
      @submit.prevent="handleSubmit"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="商户名称" prop="merchantName">
            <el-input v-model="form.merchantName" placeholder="企业/店铺名称" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系人" prop="contactName">
            <el-input v-model="form.contactName" placeholder="联系人姓名" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="手机号码" prop="phone">
        <el-input v-model="form.phone" placeholder="11 位手机号" clearable />
      </el-form-item>

      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="用于接收审核通知与查询密钥" clearable />
      </el-form-item>

      <el-form-item label="营业执照号" prop="bizLicenseNo">
        <el-input v-model="form.bizLicenseNo" placeholder="选填" clearable />
      </el-form-item>

      <el-form-item label="企业网址" prop="websiteUrl">
        <el-input v-model="form.websiteUrl" placeholder="选填，https://..." clearable />
      </el-form-item>

      <el-form-item label="业务范围" prop="businessScope">
        <el-select v-model="form.businessScope" placeholder="请选择" clearable class="w-full">
          <el-option label="零售电商" value="retail" />
          <el-option label="餐饮服务" value="catering" />
          <el-option label="生活服务" value="service" />
          <el-option label="物流同城" value="logistics" />
          <el-option label="其他" value="other" />
        </el-select>
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>

      <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" class="mb-3" />

      <el-button type="primary" class="portal-submit" :loading="loading" native-type="submit">
        {{ loading ? '提交中...' : '提交入驻申请' }}
      </el-button>
    </el-form>

    <div v-else-if="step === 1" class="text-center">
      <el-result icon="success" title="申请已提交">
        <template #sub-title>
          <p class="text-sm text-[var(--pf-text-secondary)] mb-2">审核通过后，请使用申请单号与联系方式在查询页获取 API 密钥。</p>
          <p v-if="applicationNo" class="text-sm">
            申请单号：<span class="font-mono font-semibold text-[var(--pf-primary-hover)]">{{ applicationNo }}</span>
          </p>
        </template>
        <template #extra>
          <div class="flex flex-col gap-2 items-center">
            <el-button type="primary" class="portal-submit" @click="copyApplicationNo">复制申请单号</el-button>
            <el-button @click="goQuery">前往查询页</el-button>
            <router-link to="/onboarding/result" class="portal-link">打开入驻结果查询</router-link>
          </div>
        </template>
      </el-result>
    </div>

    <div v-else class="portal-guide">
      <ol class="portal-guide__list">
        <li>等待平台 1–2 个工作日完成审核，请保留申请单号。</li>
        <li>审核通过后，在「入驻结果查询」页获取 merchantId 与 appSecret。</li>
        <li>使用 SDK 与签名密钥对接收银台开放接口。</li>
      </ol>
      <el-button type="primary" class="portal-submit mt-4" @click="goQuery">前往入驻结果查询</el-button>
    </div>
  </PortalShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PortalShell from '@/components/PortalShell.vue'
import { submitOnboardingApplication } from '@/api/onboarding'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const errorMsg = ref('')
const step = ref(0)
const applicationNo = ref('')

const form = reactive({
  merchantName: '',
  contactName: '',
  phone: '',
  email: '',
  bizLicenseNo: '',
  websiteUrl: '',
  businessScope: '',
  remark: '',
})

const stepSubtitle = computed(() => {
  if (step.value === 0) return '加入 PonyFlux Pay · 提交后等待平台审核'
  if (step.value === 1) return '请妥善保存申请单号'
  return '按以下步骤完成支付接入'
})

const rules: FormRules = {
  merchantName: [{ required: true, message: '请输入商户名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号码', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    const result = await submitOnboardingApplication({
      merchantName: form.merchantName.trim(),
      contactName: form.contactName.trim(),
      contactPhone: form.phone.trim(),
      contactEmail: form.email.trim(),
      bizLicenseNo: form.bizLicenseNo.trim() || undefined,
      websiteUrl: form.websiteUrl.trim() || undefined,
      businessScope: form.businessScope || undefined,
      remark: form.remark.trim() || undefined,
    })
    applicationNo.value = result.applicationNo
    ElMessage.success('申请已提交')
    step.value = 1
  } catch (err: any) {
    errorMsg.value = err?.message ?? '提交失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function copyApplicationNo() {
  if (!applicationNo.value) return
  try {
    await navigator.clipboard.writeText(applicationNo.value)
    ElMessage.success('申请单号已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

function goQuery() {
  router.push({
    path: '/onboarding/result',
    query: applicationNo.value ? { applicationNo: applicationNo.value } : undefined,
  })
}
</script>

<style scoped>
.portal-guide__list {
  margin: 0;
  padding-left: 1.25rem;
  color: var(--pf-text-secondary);
  font-size: 14px;
  line-height: 1.75;
}

.portal-guide__list li + li {
  margin-top: 8px;
}
</style>
