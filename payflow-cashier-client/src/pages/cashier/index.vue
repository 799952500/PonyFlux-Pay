<template>
  <div class="cashier-page">
    <!-- 固定顶部导航 -->
    <CashierNav />

    <!-- 主体内容区 -->
    <div class="cashier-body">
      <div class="cashier-inner">

        <!-- ── 商户信息（最顶部，一屏内必见） ── -->
        <div class="merchant-row" v-if="cashierStore.orderInfo">
          <div class="merchant-avatar">
            {{ merchantInitial }}
          </div>
          <div class="merchant-meta">
            <span class="merchant-name">{{ cashierStore.orderInfo.merchantName }}</span>
            <span class="merchant-label">收款方</span>
          </div>
        </div>

        <div class="section-divider" />

        <!-- ── 订单卡片（精简版，不含商户信息） ── -->
        <div v-if="cashierStore.isLoading" class="skeleton-wrap">
          <el-skeleton animated :rows="4" />
        </div>
        <OrderCard v-else-if="cashierStore.orderInfo" :info="cashierStore.orderInfo" />

        <div class="section-divider" v-if="!cashierStore.isLoading" />

        <!-- ── 支付方式列表 ── -->
        <div v-if="cashierStore.isLoading" class="skeleton-wrap">
          <el-skeleton animated :rows="3" />
        </div>
        <PaymentMethodList
          v-else-if="cashierStore.orderInfo"
          :methods="cashierStore.orderInfo.paymentMethods"
          :selected="selectedMethod"
          @update:selected="selectedMethod = $event"
        />

        <!-- ── 确认支付按钮 ── -->
        <button
          class="pay-btn"
          :disabled="!selectedMethod || cashierStore.isPaying"
          @click="handlePay"
        >
          <span v-if="cashierStore.isPaying" class="flex items-center justify-center gap-2">
            <svg class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            支付跳转中...
          </span>
          <span v-else>
            确认支付 ¥{{ cashierStore.orderInfo ? formatAmount(cashierStore.orderInfo.amount) : '—' }}
          </span>
        </button>

        <!-- ── 安全承诺 ── -->
        <div class="security-badge">
          <svg class="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
          <span>资金安全 · 加密保障</span>
        </div>

      </div>
    </div>

    <!-- 扫码支付浮层 -->
    <QRCodeModal
      v-model="showQR"
      :qr-url="cashierStore.qrCodeUrl"
      :amount="cashierStore.orderInfo?.amount"
      :confirming="confirming"
      @confirm="handleConfirmPay"
    />

    <!-- 支付结果浮层 -->
    <PaymentResult
      v-if="payResult"
      :status="payResult"
      :return-url="cashierStore.orderInfo?.returnUrl"
      @retry="handleRetry"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCashierStore } from '@/stores/cashier'
import { getCashierInfo, createPayment, pollPaymentStatus } from '@/api/cashier'
import CashierNav from './components/CashierNav.vue'
import OrderCard from './components/OrderCard.vue'
import PaymentMethodList from './components/PaymentMethodList.vue'
import QRCodeModal from './components/QRCodeModal.vue'
import PaymentResult from './components/PaymentResult.vue'

const route = useRoute()
const cashierStore = useCashierStore()

const selectedMethod = ref('')
const showQR = ref(false)
const payResult = ref<'success' | 'failed' | null>(null)
const confirming = ref(false)

const merchantInitial = computed(() =>
  cashierStore.orderInfo?.merchantName?.charAt(0).toUpperCase() ?? '?'
)

// -------------------------------------------------------------------
// 页面初始化（无需登录，直接从 URL 获取 orderId）
// -------------------------------------------------------------------
onMounted(async () => {
  const orderId = route.params.orderId as string
  const sig = route.query.sig as string

  // Demo 模式
  if (!orderId || orderId === 'demo') {
    cashierStore.setOrderInfo({
      orderId: 'DEMO001',
      merchantName: '演示商户',
      subject: '测试商品',
      body: '这是一笔测试订单，用于演示收银台功能',
      amount: 10000, // 100.00 元
      currency: 'CNY',
      expireTime: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
      status: 'CREATED',
      paymentMethods: [
        {
          methodCode: 'ALIPAY_APP',
          methodName: '支付宝',
          channel: 'ALIPAY',
          icon: '',
          discount: { name: '首单立减', amount: 100 },
        },
        {
          methodCode: 'WECHAT_APP',
          methodName: '微信支付',
          channel: 'WECHAT_PAY',
          icon: '',
        },
        {
          methodCode: 'UNION_PAY',
          methodName: '云闪付',
          channel: 'UNION_PAY',
          icon: '',
        },
      ],
    })
    return
  }

  cashierStore.setLoading(true)
  try {
    const info = await getCashierInfo(orderId, sig ?? '')
    cashierStore.setOrderInfo(info)
  } catch {
    ElMessage.error('加载收银台信息失败')
  } finally {
    cashierStore.setLoading(false)
  }
})

// -------------------------------------------------------------------
// 发起支付
// -------------------------------------------------------------------
async function handlePay() {
  if (!cashierStore.orderInfo || !selectedMethod.value) return

  cashierStore.setPaying(true)
  try {
    const result = await createPayment({
      orderId: cashierStore.orderInfo.orderId,
      payChannel: selectedMethod.value.startsWith('WECHAT') ? 'WECHAT_PAY' : 'ALIPAY',
      payMethod: selectedMethod.value,
      deviceType: 'WEB',
    })

    cashierStore.setPaymentResult(result)

    if (result.action === 'QR_CODE' && result.qrCodeUrl) {
      cashierStore.openQR(result.qrCodeUrl)
    } else if (result.action === 'REDIRECT' && result.redirectUrl) {
      window.location.href = result.redirectUrl
    } else if (result.action === 'INVOKE' && result.invokeParams) {
      const params = new URLSearchParams(result.invokeParams as Record<string, string>)
      window.location.href = `${(result.invokeParams as Record<string, string>)['schema'] ?? 'payflow'}:${params.toString()}`
    }
  } catch {
    payResult.value = 'failed'
  } finally {
    cashierStore.setPaying(false)
  }
}

// -------------------------------------------------------------------
// 确认扫码支付
// -------------------------------------------------------------------
async function handleConfirmPay() {
  confirming.value = true
  const result = cashierStore.paymentResult
  if (!result) return

  const MAX_POLL = 60
  let count = 0

  const poll = async (): Promise<void> => {
    if (count >= MAX_POLL) {
      confirming.value = false
      payResult.value = 'failed'
      cashierStore.closeQR()
      return
    }

    try {
      const statusResp = (await pollPaymentStatus(result.paymentId)) as unknown as { status: string }
      if (statusResp.status === 'PAID') {
        confirming.value = false
        cashierStore.closeQR()
        payResult.value = 'success'
        return
      }
    } catch {
      // 忽略轮询错误，继续轮询
    }

    count++
    setTimeout(poll, 3000)
  }

  await poll()
}

// -------------------------------------------------------------------
// 重试支付
// -------------------------------------------------------------------
function handleRetry() {
  payResult.value = null
  cashierStore.setPaymentResult(null)
}

// -------------------------------------------------------------------
// 金额格式化：分 → ¥1,234.56
// -------------------------------------------------------------------
function formatAmount(amount: number): string {
  return (amount / 100).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}
</script>

<style scoped>
/* ── 主体内容区（垂直撑满视口） ── */
.cashier-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: 16px;
}

/* ── 内容容器（480px 居中，内部可滚动） ── */
.cashier-inner {
  width: 100%;
  max-width: min(560px, 100%);
  padding: 0 24px;
  /* 内容总高强制不超过视口可用高度（导航 60px） */
  max-height: calc(100vh - 60px);
  overflow-y: auto;
  scrollbar-width: none;     /* Firefox 隐藏滚动条 */
  -ms-overflow-style: none;  /* IE/Edge 隐藏滚动条 */
}

/* Chrome/Safari/Opera 隐藏滚动条 */
.cashier-inner::-webkit-scrollbar {
  display: none;
}

/* ── 商户信息行 ── */
.merchant-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
}

.merchant-avatar {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: linear-gradient(135deg, #064e3b 0%, #0d9488 100%);
  color: white;
  font-size: 17px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  letter-spacing: 0;
}

.merchant-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.merchant-name {
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.2;
}

.merchant-label {
  font-size: 12px;
  color: rgba(209, 250, 229, 0.75);
}

/* ── 区块分割线 ── */
.section-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.14);
  flex-shrink: 0;
}

/* ── 骨架屏占位 ── */
.skeleton-wrap {
  padding: 4px 0;
}

/* ── 全局 .pay-btn / .security-badge 样式在 main.css 中定义，
     此处仅覆盖全局默认的 mt-4 padding ── */
.pay-btn {
  margin-top: 12px;
}

.security-badge {
  margin-top: 8px;
}
</style>
