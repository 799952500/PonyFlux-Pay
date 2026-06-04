<template>
  <PortalShell :title="t('portal.resultTitle')" :subtitle="t('portal.resultSubtitle')">
    <template #header-extra>
      <div class="flex items-center gap-3">
        <LocaleSwitcher />
        <router-link to="/register" class="portal-link">{{ t('portal.backRegister') }}</router-link>
      </div>
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
      <el-form-item :label="t('portal.applicationNoLabel')" prop="applicationNo">
        <el-input v-model="form.applicationNo" :placeholder="t('portal.applicationNoPlaceholder')" clearable />
      </el-form-item>
      <el-form-item :label="t('portal.contactLabel')" prop="contact">
        <el-input v-model="form.contact" :placeholder="t('portal.contactPlaceholder')" clearable />
      </el-form-item>
      <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" class="mb-3" />
      <el-button type="primary" class="portal-submit" :loading="loading" native-type="submit">
        {{ loading ? t('portal.querying') : t('portal.queryCredentials') }}
      </el-button>
    </el-form>

    <div v-else class="credential-panel">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        :title="t('portal.credentialsAlert')"
        :description="t('portal.credentialsDesc')"
        class="mb-4"
      />
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item :label="t('portal.applicationNoLabel')">{{ credentials.applicationNo }}</el-descriptions-item>
        <el-descriptions-item :label="t('portal.merchantIdLabel')">
          <span class="font-mono text-sm">{{ credentials.merchantId }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('portal.appSecretLabel')">
          <span class="font-mono text-xs break-all">{{ credentials.appSecret }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('portal.adminUsernameLabel')">{{ credentials.adminUsername }}</el-descriptions-item>
        <el-descriptions-item :label="t('portal.tempPasswordLabel')">
          <span class="font-mono">{{ credentials.tempPassword }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="credentials.remainingQueries != null" :label="t('portal.remainingQueriesLabel')">
          {{ credentials.remainingQueries }}
        </el-descriptions-item>
      </el-descriptions>
      <div class="flex flex-col gap-2 mt-5">
        <el-button type="primary" class="portal-submit" @click="copyAll">{{ t('portal.copyAll') }}</el-button>
        <el-button @click="downloadTxt">{{ t('portal.saveTxt') }}</el-button>
        <el-button link type="primary" @click="openAdminLogin">{{ t('portal.openAdminLogin') }}</el-button>
      </div>
    </div>
  </PortalShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PortalShell from '@/components/PortalShell.vue'
import LocaleSwitcher from '@/components/LocaleSwitcher.vue'
import {
  queryOnboardingResult,
  type OnboardingCredentialResult,
} from '@/api/onboarding'

const { t } = useI18n()
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

const rules = computed<FormRules>(() => ({
  applicationNo: [{ required: true, message: t('portal.applicationNoRequired'), trigger: 'blur' }],
  contact: [{ required: true, message: t('portal.contactRequiredResult'), trigger: 'blur' }],
}))

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
  } catch (err: unknown) {
    credentials.value = null
    const message = err instanceof Error ? err.message : undefined
    errorMsg.value = message ?? t('portal.queryFailed')
  } finally {
    loading.value = false
  }
}

function buildCredentialText(): string {
  const c = credentials.value!
  return [
    t('portal.credentialHeader'),
    t('portal.credentialApplicationNo', { value: c.applicationNo }),
    t('portal.credentialMerchantId', { value: c.merchantId }),
    t('portal.credentialAppSecret', { value: c.appSecret }),
    t('portal.credentialAdminUser', { value: c.adminUsername }),
    t('portal.credentialTempPassword', { value: c.tempPassword }),
    t('portal.credentialLoginUrl', { value: c.loginUrl }),
  ].join('\n')
}

async function copyAll() {
  if (!credentials.value) return
  try {
    await navigator.clipboard.writeText(buildCredentialText())
    ElMessage.success(t('portal.copied'))
  } catch {
    ElMessage.error(t('portal.copyFailedManual'))
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
