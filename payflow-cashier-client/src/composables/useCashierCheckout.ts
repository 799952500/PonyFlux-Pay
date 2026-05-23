import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCashierStore } from '@/stores/cashier'
import { getCashierInfo, createPayment, pollPaymentStatus } from '@/api/cashier'
import type { CashierInfo, PayChannel, DeviceType, PaymentMethod } from '@/types'
import type { CashierTerminal } from '@/utils/cashierDevice'

function resolvePayChannel(methodCode: string): PayChannel {
  if (methodCode.startsWith('WECHAT_')) return 'WECHAT_PAY'
  if (methodCode.startsWith('ALIPAY_')) return 'ALIPAY'
  if (methodCode.startsWith('UNION_')) return 'UNION_PAY'
  return 'ALIPAY'
}

/** Demo 固定展示 8 种支付方式，便于验证长列表滚动与紧凑样式 */
const DEMO_PC_PAYMENT_METHODS: PaymentMethod[] = [
  {
    methodCode: 'ALIPAY_NATIVE',
    methodName: '支付宝（扫码）',
    channel: 'ALIPAY',
    icon: '',
    discount: { name: '首单立减', amount: 100 },
  },
  { methodCode: 'WECHAT_NATIVE', methodName: '微信支付（扫码）', channel: 'WECHAT_PAY', icon: '' },
  { methodCode: 'UNION_QR', methodName: '云闪付（扫码）', channel: 'UNION_PAY', icon: '' },
  { methodCode: 'ALIPAY_WAP', methodName: '支付宝 H5', channel: 'ALIPAY', icon: '' },
  { methodCode: 'WECHAT_H5', methodName: '微信 H5', channel: 'WECHAT_PAY', icon: '' },
  { methodCode: 'ALIPAY_APP', methodName: '支付宝 App', channel: 'ALIPAY', icon: '' },
  { methodCode: 'WECHAT_APP', methodName: '微信 App', channel: 'WECHAT_PAY', icon: '' },
  { methodCode: 'WECHAT_JSAPI', methodName: '微信 JSAPI', channel: 'WECHAT_PAY', icon: '' },
]

const DEMO_H5_PAYMENT_METHODS: PaymentMethod[] = [
  {
    methodCode: 'ALIPAY_WAP',
    methodName: '支付宝',
    channel: 'ALIPAY',
    icon: '',
    discount: { name: '首单立减', amount: 100 },
  },
  { methodCode: 'WECHAT_H5', methodName: '微信支付', channel: 'WECHAT_PAY', icon: '' },
  { methodCode: 'UNION_H5', methodName: '云闪付', channel: 'UNION_PAY', icon: '' },
  { methodCode: 'ALIPAY_NATIVE', methodName: '支付宝（扫码）', channel: 'ALIPAY', icon: '' },
  { methodCode: 'WECHAT_NATIVE', methodName: '微信（扫码）', channel: 'WECHAT_PAY', icon: '' },
  { methodCode: 'ALIPAY_APP', methodName: '支付宝 App', channel: 'ALIPAY', icon: '' },
  { methodCode: 'WECHAT_APP', methodName: '微信 App', channel: 'WECHAT_PAY', icon: '' },
  { methodCode: 'WECHAT_JSAPI', methodName: '微信 JSAPI', channel: 'WECHAT_PAY', icon: '' },
]

function buildDemoOrder(terminal: CashierTerminal): CashierInfo {
  const base = {
    orderId: 'DEMO001',
    merchantName: '演示商户',
    subject: '测试商品',
    body: '这是一笔测试订单，用于演示收银台功能',
    amount: 10000,
    currency: 'CNY',
    createdAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
    expireTime: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
    status: 'CREATED' as const,
  }

  return {
    ...base,
    paymentMethods:
      terminal === 'H5' ? DEMO_H5_PAYMENT_METHODS : DEMO_PC_PAYMENT_METHODS,
  }
}

export function useCashierCheckout(terminal: CashierTerminal) {
  const route = useRoute()
  const cashierStore = useCashierStore()

  const selectedMethod = ref('')
  const payResult = ref<'success' | 'failed' | null>(null)
  const confirming = ref(false)

  const deviceType: DeviceType = terminal === 'H5' ? 'H5' : 'WEB'

  const merchantInitial = computed(
    () => cashierStore.orderInfo?.merchantName?.charAt(0).toUpperCase() ?? '?'
  )

  const checkoutDeadlinePassed = computed(() => {
    const t = cashierStore.orderInfo?.expireTime
    if (!t) return false
    return Date.now() >= new Date(t).getTime()
  })

  const showQR = computed({
    get: () => cashierStore.showQR,
    set: (v: boolean) => {
      if (!v) cashierStore.closeQR()
    },
  })

  watch(
    () => cashierStore.orderInfo?.paymentMethods,
    (methods) => {
      if (!methods?.length) return
      const stillValid = methods.some((m) => m.methodCode === selectedMethod.value)
      if (!stillValid) selectedMethod.value = methods[0].methodCode
    },
    { immediate: true }
  )

  function handleOrderExpired() {
    ElMessage.warning('支付超时，请返回商户重新下单')
  }

  let expiredNotified = false
  watch(checkoutDeadlinePassed, (passed) => {
    if (passed && !expiredNotified) {
      expiredNotified = true
      handleOrderExpired()
    }
  })

  async function startPaymentPoll(paymentId: string | undefined) {
    if (!paymentId) return
    const MAX_POLL = 60
    let count = 0
    const poll = async (): Promise<void> => {
      if (count >= MAX_POLL) {
        payResult.value = 'failed'
        return
      }
      try {
        const statusResp = (await pollPaymentStatus(paymentId)) as unknown as { status: string }
        const st = statusResp.status
        if (st === 'PAID' || st === 'SUCCESS') {
          payResult.value = 'success'
          return
        }
      } catch {
        /* 继续轮询 */
      }
      count++
      setTimeout(poll, 3000)
    }
    await poll()
  }

  async function loadOrder() {
    const orderId = route.params.orderId as string
    const sig = route.query.sig as string | undefined

    if (!orderId || orderId === 'demo') {
      cashierStore.setOrderInfo(buildDemoOrder(terminal))
      cashierStore.setLoading(false)
      return
    }

    cashierStore.setLoading(true)
    try {
      const info = await getCashierInfo(orderId, sig ?? '', terminal)
      cashierStore.setOrderInfo(info)
      if (info.status === 'PAID') {
        payResult.value = 'success'
      } else if (info.status === 'FAILED' || info.status === 'CLOSED' || info.status === 'EXPIRED') {
        payResult.value = 'failed'
      }
    } catch {
      ElMessage.error('加载收银台信息失败')
    } finally {
      cashierStore.setLoading(false)
    }
  }

  onMounted(() => {
    void loadOrder()
  })

  async function handlePay() {
    if (!cashierStore.orderInfo || !selectedMethod.value || checkoutDeadlinePassed.value) return

    cashierStore.setPaying(true)
    try {
      const result = await createPayment({
        orderId: cashierStore.orderInfo.orderId,
        payChannel: resolvePayChannel(selectedMethod.value),
        payMethod: selectedMethod.value,
        deviceType,
      })

      cashierStore.setPaymentResult(result)

      if (result.paidImmediately || result.action === 'COMPLETE') {
        payResult.value = 'success'
        return
      }

      if (result.action === 'MICROPAY_POLL' || result.action === 'BARCODE_POLL') {
        await startPaymentPoll(result.paymentId)
        return
      }

      if (result.action === 'QR_CODE' && result.qrCodeUrl) {
        cashierStore.openQR(result.qrCodeUrl)
      } else if (result.action === 'REDIRECT' && result.redirectUrl) {
        window.location.href = result.redirectUrl
      } else if (result.action === 'FORM' && result.formHtml) {
        const container = document.createElement('div')
        container.style.display = 'none'
        container.innerHTML = result.formHtml
        document.body.appendChild(container)
        const form = container.querySelector('form')
        form?.submit()
      } else if (result.action === 'INVOKE' && result.invokeParams) {
        const ip = result.invokeParams as Record<string, string>
        if (
          typeof window !== 'undefined' &&
          /MicroMessenger/i.test(navigator.userAgent) &&
          (window as unknown as { WeixinJSBridge?: unknown }).WeixinJSBridge
        ) {
          const bridge = (
            window as unknown as {
              WeixinJSBridge: {
                invoke: (
                  m: string,
                  p: Record<string, string>,
                  cb: (r: { err_msg?: string }) => void
                ) => void
              }
            }
          ).WeixinJSBridge
          bridge.invoke(
            'getBrandWCPayRequest',
            {
              appId: ip.appId ?? '',
              timeStamp: ip.timestamp ?? '',
              nonceStr: ip.nonceStr ?? '',
              package: ip.package_ ?? ip.package ?? '',
              signType: ip.signType ?? 'RSA',
              paySign: ip.sign ?? '',
            },
            (res) => {
              const msg = res?.err_msg ?? ''
              payResult.value = msg.includes('ok') ? 'success' : 'failed'
            }
          )
        } else {
          const params = new URLSearchParams(ip as Record<string, string>)
          window.location.href = `${ip.schema ?? 'payflow'}:${params.toString()}`
        }
      }
    } catch {
      payResult.value = 'failed'
    } finally {
      cashierStore.setPaying(false)
    }
  }

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
        if (statusResp.status === 'PAID' || statusResp.status === 'SUCCESS') {
          confirming.value = false
          cashierStore.closeQR()
          payResult.value = 'success'
          return
        }
      } catch {
        /* 继续轮询 */
      }
      count++
      setTimeout(poll, 3000)
    }

    await poll()
  }

  function handleRetry() {
    payResult.value = null
    cashierStore.setPaymentResult(null)
  }

  function formatAmount(amount: number): string {
    return (amount / 100).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  }

  return {
    terminal,
    cashierStore,
    selectedMethod,
    payResult,
    confirming,
    showQR,
    merchantInitial,
    checkoutDeadlinePassed,
    handleOrderExpired,
    handlePay,
    handleConfirmPay,
    handleRetry,
    formatAmount,
  }
}
