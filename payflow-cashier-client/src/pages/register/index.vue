<template>
  <div class="login-shell min-h-screen flex items-center justify-center px-6 py-12">
    <div class="w-full max-w-[500px]">
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-[72px] h-[72px] rounded-2xl mb-4 login-logo-ring">
          <img src="/ponyflux-logo.svg" width="56" height="56" alt="小马支付" />
        </div>
        <h1 class="text-white text-2xl font-bold tracking-tight drop-shadow-sm">商户自助注册</h1>
        <p class="text-emerald-100/75 text-sm mt-1.5">加入 PonyFlux Pay · 即刻开启收银</p>
      </div>

      <div class="login-glass rounded-[28px] p-8 sm:p-10">
        <el-steps :active="step" finish-status="success" align-center class="mb-8" simple>
          <el-step title="填写信息" />
          <el-step title="注册成功" />
          <el-step title="开始接入" />
        </el-steps>

        <!-- Step 1: 注册表单 -->
        <el-form
          v-if="step === 0"
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleRegister"
          class="login-form-el"
        >
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="商户名称" prop="merchantName">
                <el-input v-model="form.merchantName" placeholder="您的企业/店铺名称" clearable />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系人" prop="contactName">
                <el-input v-model="form.contactName" placeholder="联系人姓名" clearable />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="手机号码" prop="phone">
            <el-input v-model="form.phone" placeholder="手机号码" clearable />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="联系邮箱" clearable />
          </el-form-item>

          <el-form-item label="登录密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="设置登录密码（至少6位）" show-password clearable />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" show-password clearable />
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
            {{ loading ? '提交中...' : '提交注册' }}
          </el-button>
        </el-form>

        <!-- Step 2: 注册成功 -->
        <div v-else-if="step === 1" class="text-center py-6">
          <el-result icon="success" title="注册提交成功" sub-title="您的商户入驻申请已提交，我们将在 1-2 个工作日内完成审核。">
            <template #extra>
              <el-button type="primary" @click="step = 2">查看接入指引</el-button>
            </template>
          </el-result>
        </div>

        <!-- Step 3: 接入指引 -->
        <div v-else class="py-2">
          <h3 class="text-white font-semibold text-lg mb-4 text-center">接入指引</h3>

          <div class="grid gap-4 mb-6">
            <div class="flex items-start gap-4 p-4 rounded-xl" style="background: rgba(255,255,255,0.06)">
              <div class="w-9 h-9 rounded-lg bg-emerald-500/20 flex items-center justify-center shrink-0 mt-0.5">
                <span class="text-emerald-300 font-bold text-sm">1</span>
              </div>
              <div>
                <p class="text-white font-medium text-sm mb-1">下载 SDK</p>
                <p class="text-emerald-100/60 text-xs mb-2">选择您的开发语言，下载对应的 PonyFlux Pay SDK</p>
                <div class="flex flex-wrap gap-2">
                  <el-button size="small" link class="!text-emerald-300">
                    <a href="https://github.com/ponyflux/payflow-sdk-java" target="_blank">Java SDK</a>
                  </el-button>
                  <el-button size="small" link class="!text-emerald-300">
                    <a href="https://github.com/ponyflux/payflow-sdk-python" target="_blank">Python SDK</a>
                  </el-button>
                  <el-button size="small" link class="!text-emerald-300">
                    <a href="https://github.com/ponyflux/payflow-sdk-php" target="_blank">PHP SDK</a>
                  </el-button>
                </div>
              </div>
            </div>

            <div class="flex items-start gap-4 p-4 rounded-xl" style="background: rgba(255,255,255,0.06)">
              <div class="w-9 h-9 rounded-lg bg-emerald-500/20 flex items-center justify-center shrink-0 mt-0.5">
                <span class="text-emerald-300 font-bold text-sm">2</span>
              </div>
              <div>
                <p class="text-white font-medium text-sm mb-1">配置 API 密钥</p>
                <p class="text-emerald-100/60 text-xs mb-2">审核通过后，在商户后台获取 merchantId 和 merchantKey，用于签名认证</p>
              </div>
            </div>

            <div class="flex items-start gap-4 p-4 rounded-xl" style="background: rgba(255,255,255,0.06)">
              <div class="w-9 h-9 rounded-lg bg-emerald-500/20 flex items-center justify-center shrink-0 mt-0.5">
                <span class="text-emerald-300 font-bold text-sm">3</span>
              </div>
              <div>
                <p class="text-white font-medium text-sm mb-1">阅读 API 文档</p>
                <p class="text-emerald-100/60 text-xs mb-2">了解如何创建订单、发起支付和处理回调通知</p>
                <el-button size="small" link class="!text-emerald-300">
                  <a href="https://docs.ponyflux.dev/api" target="_blank">API 文档 →</a>
                </el-button>
              </div>
            </div>
          </div>

          <el-button class="w-full !rounded-full" size="large" @click="$router.push('/login')">
            返回登录
          </el-button>
        </div>
      </div>

      <p class="text-center mt-6">
        <router-link to="/login" class="text-emerald-200/70 hover:text-white text-sm transition-colors">
          已有账号？立即登录 →
        </router-link>
      </p>

      <p class="text-center text-emerald-100/45 text-xs mt-8">
        © {{ new Date().getFullYear() }} PonyFlux Pay. All rights reserved.
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const formRef = ref<FormInstance>()
const loading = ref(false)
const errorMsg = ref('')
const step = ref(0)

const form = reactive({
  merchantName: '',
  contactName: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

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
  password: [
    { required: true, message: '请设置登录密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

async function handleRegister() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    // 模拟提交注册（可通过支付平台 API 提交商户入驻申请）
    await new Promise((resolve) => setTimeout(resolve, 1200))
    ElMessage.success('注册申请已提交！')
    step.value = 1
  } catch (err: any) {
    errorMsg.value = err?.response?.data?.message ?? '注册提交失败，请稍后重试'
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

.login-form-el :deep(.el-steps) {
  --el-color-primary: #10b981;
}
</style>
