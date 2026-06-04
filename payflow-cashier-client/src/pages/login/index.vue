<template>
  <PortalShell :title="t('portal.loginTitle')" :subtitle="t('portal.loginSubtitle')">
    <template #header-extra>
      <div class="flex items-center gap-3">
        <LocaleSwitcher />
        <router-link to="/register" class="portal-link">{{ t('portal.registerLink') }}</router-link>
      </div>
    </template>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="portal-form"
      @submit.prevent="handleLogin"
    >
      <el-form-item :label="t('portal.merchantId')" prop="merchantId">
        <el-input v-model="form.merchantId" :placeholder="t('portal.merchantIdPlaceholder')" clearable />
      </el-form-item>

      <el-form-item :label="t('portal.password')" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          :placeholder="t('portal.passwordPlaceholder')"
          show-password
          clearable
        />
      </el-form-item>

      <div v-if="errorMsg" class="mb-4">
        <el-alert type="error" :title="errorMsg" :closable="false" show-icon />
      </div>

      <el-button type="primary" class="portal-submit" :loading="loading" native-type="submit">
        {{ loading ? t('portal.loggingIn') : t('portal.login') }}
      </el-button>
    </el-form>

    <p class="portal-footnote">
      {{ t('portal.agreePrefix') }}
      <a href="#" class="portal-link">{{ t('portal.serviceAgreement') }}</a>
      {{ t('portal.and') }}
      <a href="#" class="portal-link">{{ t('portal.privacyPolicy') }}</a>
    </p>

    <p class="text-center mt-4">
      <router-link to="/register" class="portal-link">{{ t('portal.noAccount') }}</router-link>
    </p>
  </PortalShell>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PortalShell from '@/components/PortalShell.vue'
import LocaleSwitcher from '@/components/LocaleSwitcher.vue'
import { merchantLogin } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const errorMsg = ref('')

const form = reactive({
  merchantId: '',
  password: '',
})

const rules = computed<FormRules>(() => ({
  merchantId: [{ required: true, message: t('portal.merchantIdRequired'), trigger: 'blur' }],
  password: [
    { required: true, message: t('portal.passwordRequired'), trigger: 'blur' },
    { min: 6, message: t('portal.passwordMin'), trigger: 'blur' },
  ],
}))

async function handleLogin() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    const data = await merchantLogin({
      merchantId: form.merchantId,
      password: form.password,
    })

    authStore.setLogin(data)

    ElMessage.success(t('portal.welcomeBack', { name: data.merchantInfo.merchantName }))
    router.push('/cashier/demo')
  } catch (err: unknown) {
    const e = err as { response?: { data?: { code?: number; message?: string } }; code?: number }
    const code = e?.response?.data?.code ?? e?.code
    if (code === 401 || code === 1001) {
      errorMsg.value = t('portal.wrongCredentials')
    } else {
      errorMsg.value = e?.response?.data?.message ?? t('portal.loginFailed')
    }
  } finally {
    loading.value = false
  }
}
</script>

