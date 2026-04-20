<template>
  <div class="login-shell min-h-screen flex items-center justify-center px-6 py-12">
    <div class="w-full max-w-[440px]">
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-[72px] h-[72px] rounded-2xl mb-4 login-logo-ring">
          <img src="/ponyflux-logo.svg" width="56" height="56" alt="小马支付" />
        </div>
        <h1 class="text-white text-2xl font-bold tracking-tight drop-shadow-sm">小马支付</h1>
        <p class="text-emerald-100/75 text-sm mt-1.5">PonyFlux Pay · 商户收银台</p>
      </div>

      <div class="login-glass rounded-[28px] p-8 sm:p-10">
        <h2 class="text-white font-semibold text-lg mb-6 text-center">商户登录</h2>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleLogin"
          class="login-form-el"
        >
          <el-form-item label="商户号" prop="merchantId">
            <el-input
              v-model="form.merchantId"
              placeholder="请输入商户号"
              size="large"
              prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入登录密码"
              size="large"
              prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>

          <div v-if="errorMsg" class="mb-4">
            <el-alert type="error" :title="errorMsg" :closable="false" show-icon />
          </div>

          <el-button
            type="primary"
            class="w-full !h-[50px] !rounded-full !text-base !font-semibold !mt-2 login-submit"
            :loading="loading"
            native-type="submit"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form>

        <p class="text-center text-xs text-emerald-100/55 mt-6 leading-relaxed">
          登录即表示同意
          <a href="#" class="text-emerald-200 hover:text-white hover:underline">《商户服务协议》</a>
          与
          <a href="#" class="text-emerald-200 hover:text-white hover:underline">《隐私政策》</a>
        </p>
      </div>

      <p class="text-center text-emerald-100/45 text-xs mt-8">
        © {{ new Date().getFullYear() }} PonyFlux Pay. All rights reserved.
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { merchantLogin } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const errorMsg = ref('')

const form = reactive({
  merchantId: '',
  password: '',
})

const rules: FormRules = {
  merchantId: [{ required: true, message: '请输入商户号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

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

    ElMessage.success(`欢迎回来，${data.merchantInfo.merchantName}！`)
    router.push({ name: 'cashier', params: { orderId: 'demo' } })
  } catch (err: any) {
    const code = err?.response?.data?.code ?? err?.code
    if (code === 401 || code === 1001) {
      errorMsg.value = '商户号或密码错误'
    } else {
      errorMsg.value = err?.response?.data?.message ?? '登录失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-shell {
  position: relative;
}

.login-logo-ring {
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.login-glass {
  background: rgba(4, 28, 26, 0.78);
  border: 1px solid rgba(120, 150, 140, 0.22);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.45);
}

.login-form-el :deep(.el-form-item__label) {
  color: rgba(236, 253, 245, 0.9);
}

.login-form-el :deep(.el-input__wrapper) {
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.2) inset;
}

.login-form-el :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px rgba(13, 148, 136, 0.55) inset, 0 0 0 3px rgba(4, 120, 87, 0.28);
}

.login-form-el :deep(.el-input__inner) {
  color: #f8fafc;
}

.login-form-el :deep(.el-input__inner::placeholder) {
  color: rgba(226, 232, 240, 0.45);
}

.login-submit {
  letter-spacing: 0.08em;
}
</style>
