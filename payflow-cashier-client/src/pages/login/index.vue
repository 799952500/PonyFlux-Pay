<template>
  <PortalShell title="商户登录" subtitle="PonyFlux Pay · 商户收银台">
    <template #header-extra>
      <router-link to="/register" class="portal-link">商户入驻</router-link>
    </template>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="portal-form"
      @submit.prevent="handleLogin"
    >
      <el-form-item label="商户号" prop="merchantId">
        <el-input v-model="form.merchantId" placeholder="请输入商户号" clearable />
      </el-form-item>

      <el-form-item label="登录密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入登录密码"
          show-password
          clearable
        />
      </el-form-item>

      <div v-if="errorMsg" class="mb-4">
        <el-alert type="error" :title="errorMsg" :closable="false" show-icon />
      </div>

      <el-button type="primary" class="portal-submit" :loading="loading" native-type="submit">
        {{ loading ? '登录中...' : '登录' }}
      </el-button>
    </el-form>

    <p class="portal-footnote">
      登录即表示同意
      <a href="#" class="portal-link">《商户服务协议》</a>
      与
      <a href="#" class="portal-link">《隐私政策》</a>
    </p>

    <p class="text-center mt-4">
      <router-link to="/register" class="portal-link">还没有账号？立即注册 →</router-link>
    </p>
  </PortalShell>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PortalShell from '@/components/PortalShell.vue'
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
    router.push('/cashier/demo')
  } catch (err: unknown) {
    const e = err as { response?: { data?: { code?: number; message?: string } }; code?: number }
    const code = e?.response?.data?.code ?? e?.code
    if (code === 401 || code === 1001) {
      errorMsg.value = '商户号或密码错误'
    } else {
      errorMsg.value = e?.response?.data?.message ?? '登录失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}
</script>
