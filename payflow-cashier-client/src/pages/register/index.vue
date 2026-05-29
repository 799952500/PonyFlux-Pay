<template>
  <PortalShell
    :title="step === 0 ? t('portal.registerTitle') : step === 1 ? t('portal.registerSubmitted') : t('portal.registerGuide')"
    :subtitle="stepSubtitle"
    wide
  >
    <template #header-extra>
      <router-link to="/onboarding/result" class="portal-link">{{ t('portal.queryResult') }}</router-link>
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
          <el-form-item :label="t('portal.merchantName')" prop="merchantName">
            <el-input v-model="form.merchantName" :placeholder="t('portal.merchantNamePlaceholder')" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('portal.contactName')" prop="contactName">
            <el-input v-model="form.contactName" :placeholder="t('portal.contactNamePlaceholder')" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item :label="t('portal.phone')" prop="phone">
        <el-input v-model="form.phone" :placeholder="t('portal.phonePlaceholder')" clearable />
      </el-form-item>

      <el-form-item :label="t('portal.email')" prop="email">
        <el-input v-model="form.email" :placeholder="t('portal.emailPlaceholder')" clearable />
      </el-form-item>

      <el-form-item :label="t('portal.bizLicense')" prop="bizLicenseNo">
        <el-input v-model="form.bizLicenseNo" :placeholder="t('portal.optional')" clearable />
      </el-form-item>

      <el-form-item :label="t('portal.website')" prop="websiteUrl">
        <el-input v-model="form.websiteUrl" :placeholder="t('portal.websitePlaceholder')" clearable />
      </el-form-item>

      <el-form-item :label="t('portal.businessScope')" prop="businessScope">
        <el-select v-model="form.businessScope" :placeholder="t('portal.businessScopePlaceholder')" clearable class="w-full">
          <el-option :label="t('portal.scopeRetail')" value="retail" />
          <el-option :label="t('portal.scopeCatering')" value="catering" />
          <el-option :label="t('portal.scopeService')" value="service" />
          <el-option :label="t('portal.scopeLogistics')" value="logistics" />
          <el-option :label="t('portal.scopeOther')" value="other" />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('portal.remark')" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="2" :placeholder="t('portal.optional')" />
      </el-form-item>

      <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" class="mb-3" />

      <el-button type="primary" class="portal-submit" :loading="loading" native-type="submit">
        {{ loading ? t('portal.submitting') : t('portal.submitRegister') }}
      </el-button>
    </el-form>

    <div v-else-if="step === 1" class="text-center">
      <el-result icon="success" :title="t('portal.applySubmitted')">
        <template #sub-title>
          <p class="text-sm text-[var(--pf-text-secondary)] mb-2">{{ t('portal.applySubmittedHint') }}</p>
          <p v-if="applicationNo" class="text-sm">
            {{ t('portal.applicationNo') }}<span class="font-mono font-semibold text-[var(--pf-primary-hover)]">{{ applicationNo }}</span>
          </p>
        </template>
        <template #extra>
          <div class="flex flex-col gap-2 items-center">
            <el-button type="primary" class="portal-submit" @click="copyApplicationNo">{{ t('portal.copyApplicationNo') }}</el-button>
            <el-button @click="goQuery">{{ t('portal.goQuery') }}</el-button>
            <router-link to="/onboarding/result" class="portal-link">{{ t('portal.openResultQuery') }}</router-link>
          </div>
        </template>
      </el-result>
    </div>

    <div v-else class="portal-guide">
      <ol class="portal-guide__list">
        <li>{{ t('portal.guideStep1') }}</li>
        <li>{{ t('portal.guideStep2') }}</li>
        <li>{{ t('portal.guideStep3') }}</li>
      </ol>
      <el-button type="primary" class="portal-submit mt-4" @click="goQuery">{{ t('portal.goResultQuery') }}</el-button>
    </div>
  </PortalShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PortalShell from '@/components/PortalShell.vue'
import { submitOnboardingApplication } from '@/api/onboarding'

const { t } = useI18n()
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
  if (step.value === 0) return t('portal.registerSubtitleApply')
  if (step.value === 1) return t('portal.registerSubtitleSubmitted')
  return t('portal.registerSubtitleGuide')
})

const rules = computed<FormRules>(() => ({
  merchantName: [{ required: true, message: t('portal.merchantNameRequired'), trigger: 'blur' }],
  contactName: [{ required: true, message: t('portal.contactRequired'), trigger: 'blur' }],
  phone: [
    { required: true, message: t('portal.phoneRequired'), trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: t('portal.phoneInvalid'), trigger: 'blur' },
  ],
  email: [
    { required: true, message: t('portal.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('portal.emailInvalid'), trigger: 'blur' },
  ],
}))

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
    ElMessage.success(t('portal.submitSuccess'))
    step.value = 1
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : undefined
    errorMsg.value = message ?? t('portal.submitFailed')
  } finally {
    loading.value = false
  }
}

async function copyApplicationNo() {
  if (!applicationNo.value) return
  try {
    await navigator.clipboard.writeText(applicationNo.value)
    ElMessage.success(t('portal.copySuccess'))
  } catch {
    ElMessage.error(t('portal.copyFailed'))
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
