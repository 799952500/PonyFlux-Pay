<template>
  <PortalShell title="入驻结果查询" subtitle="审批通过后，使用申请单号与联系方式获取密钥">
    <template #header-extra>
      <router-link to="/register" class="portal-link">返回入驻申请</router-link>
    </template>

    <el-form
      v-if="!credentials"
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="portal-form"
      @submit.prevent="handleQuery"
    >
      <el-form-item label="申请单号" prop="applicationNo">
        <el-input v-model="form.applicationNo" placeholder="如 AP20260523xxxxxx" clearable />
      </el-form-item>
      <el-form-item label="联系方式" prop="contact">
        <el-input v-model="form.contact" placeholder="申请时填写的手机号或邮箱" clearable />
      </el-form-item>
      <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" class="mb-3" />
      <el-button type="primary" class="portal-submit" :loading="loading" native-type="submit">
        {{ loading ? '查询中...' : '查询密钥' }}
      </el-button>
    </el-form>

    <div v-else class="credential-panel">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="请妥善保管以下信息"
        description="密钥仅可查询有限次数，建议立即复制或下载保存。"
        class="mb-4"
      />
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="申请单号">{{ credentials.applicationNo }}</el-descriptions-item>
        <el-descriptions-item label="商户号">
          <span class="font-mono text-sm">{{ credentials.merchantId }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="签名密钥">
          <span class="font-mono text-xs break-all">{{ credentials.appSecret }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="管理后台账号">{{ credentials.adminUsername }}</el-descriptions-item>
        <el-descriptions-item label="初始密码">
          <span class="font-mono">{{ credentials.tempPassword }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="credentials.remainingQueries != null" label="剩余查询次数">
          {{ credentials.remainingQueries }}
        </el-descriptions-item>
      </el-descriptions>
      <div class="flex flex-col gap-2 mt-5">
        <el-button type="primary" class="portal-submit" @click="copyAll">复制全部信息</el-button>
        <el-button @click="downloadTxt">保存为文本文件</el-button>
        <el-button link type="primary" @click="openAdminLogin">打开管理后台登录</el-button>
      </div>
    </div>
  </PortalShell>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PortalShell from '@/components/PortalShell.vue'
import {
  queryOnboardingResult,
  type OnboardingCredentialResult,
} from '@/api/onboarding'

const route = useRoute()
const formRef = ref<FormInstance>()
const loading = ref(false)
const errorMsg = ref('')
const credentials = ref<OnboardingCredentialResult | null>(null)

const form = reactive({
  applicationNo: '',
  contact: '',
})

onMounted(() => {
  const q = route.query.applicationNo
  if (typeof q === 'string' && q) {
    form.applicationNo = q
  }
})

const rules: FormRules = {
  applicationNo: [{ required: true, message: '请输入申请单号', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入手机号或邮箱', trigger: 'blur' }],
}

async function handleQuery() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    credentials.value = await queryOnboardingResult({
      applicationNo: form.applicationNo.trim(),
      contact: form.contact.trim(),
    })
  } catch (err: any) {
    credentials.value = null
    errorMsg.value = err?.message ?? '查询失败，请核对申请单号与联系方式'
  } finally {
    loading.value = false
  }
}

function buildCredentialText(): string {
  const c = credentials.value!
  return [
    'PonyFlux Pay 商户入驻凭证',
    `申请单号: ${c.applicationNo}`,
    `商户号 merchantId: ${c.merchantId}`,
    `签名密钥 appSecret: ${c.appSecret}`,
    `管理后台账号: ${c.adminUsername}`,
    `初始密码: ${c.tempPassword}`,
    `登录地址: ${c.loginUrl}`,
  ].join('\n')
}

async function copyAll() {
  if (!credentials.value) return
  try {
    await navigator.clipboard.writeText(buildCredentialText())
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动选择文本复制')
  }
}

function downloadTxt() {
  if (!credentials.value) return
  const blob = new Blob([buildCredentialText()], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `ponyflux-credentials-${credentials.value.merchantId}.txt`
  a.click()
  URL.revokeObjectURL(url)
}

function openAdminLogin() {
  if (!credentials.value?.loginUrl) return
  window.open(credentials.value.loginUrl, '_blank')
}
</script>
