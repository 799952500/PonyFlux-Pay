<template>
  <div class="min-h-screen bg-gradient-to-br from-primary-700 via-primary-600 to-purple-700 flex items-center justify-center px-4">
    <!-- 居中卡片 -->
    <div class="w-full max-w-[420px]">
      <!-- Logo + 标题 -->
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-white/20 backdrop-blur-sm mb-4">
          <img src="/ponyflux-logo-dark.svg" alt="PonyFlux Pay" class="h-10 w-auto" />
        </div>
        <h1 class="text-white text-2xl font-bold tracking-tight">小马支付</h1>
        <p class="text-white/60 text-sm mt-1">PonyFlux Pay · 商户收银台</p>
      </div>

      <!-- 登录表单卡片 -->
      <div class="bg-white rounded-2xl card-shadow p-8">
        <h2 class="text-gray-800 font-semibold text-lg mb-6 text-center">商户登录</h2>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleLogin"
        >
          <!-- 商户号 -->
          <el-form-item label="商户号" prop="merchantId">
            <el-input
              v-model="form.merchantId"
              placeholder="请输入商户号"
              size="large"
              prefix-icon="User"
              clearable
            />
          </el-form-item>

          <!-- 密码 -->
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

          <!-- 错误提示 -->
          <div v-if="errorMsg" class="mb-4">
            <el-alert type="error" :title="errorMsg" :closable="false" show-icon />
          </div>

          <!-- 登录按钮 -->
          <el-button
            type="primary"
            class="w-full !h-[48px] !rounded-xl !text-base !font-semibold !mt-2"
            :loading="loading"
            native-type="submit"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form>

        <!-- 底部 -->
        <p class="text-center text-xs text-gray-400 mt-6">
          登录即表示同意
          <a href="#" class="text-primary hover:underline">《商户服务协议》</a>
          与
          <a href="#" class="text-primary hover:underline">《隐私政策》</a>
        </p>
      </div>

      <!-- 版权 -->
      <p class="text-center text-white/40 text-xs mt-6">
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
  merchantId: [
    { required: true, message: '请输入商户号', trigger: 'blur' },
  ],
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
