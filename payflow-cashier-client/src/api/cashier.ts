/**
 * 收银台相关接口
 */
import request from './request'
import type { CashierInfo, PaymentResult, CreatePaymentDTO, ReceiptInfo, PaymentMethod, PayChannel } from '@/types'

function resolvePayChannel(methodCode: string): PayChannel {
  if (methodCode.startsWith('WECHAT_')) return 'WECHAT_PAY'
  if (methodCode.startsWith('ALIPAY_')) return 'ALIPAY'
  if (methodCode.startsWith('UNION_')) return 'UNION_PAY'
  return 'ALIPAY'
}

/** 后端 PaymentMethodDTO 使用 code/name，前端统一为 methodCode/methodName/channel */
function normalizePaymentMethod(raw: Record<string, unknown>): PaymentMethod {
  const methodCode = String(raw.methodCode ?? raw.code ?? '').trim()
  return {
    methodCode,
    methodName: String(raw.methodName ?? raw.name ?? methodCode).trim(),
    icon: String(raw.icon ?? ''),
    channel: resolvePayChannel(methodCode),
  }
}

function normalizeCashierInfo(data: CashierInfo & { paymentMethods?: unknown[] }): CashierInfo {
  const methods: PaymentMethod[] = []
  if (Array.isArray(data.paymentMethods)) {
    for (const item of data.paymentMethods) {
      if (item == null || typeof item !== 'object') continue
      const normalized = normalizePaymentMethod(item as unknown as Record<string, unknown>)
      if (normalized.methodCode) methods.push(normalized)
    }
  }
  return { ...data, paymentMethods: methods }
}

/**
 * 获取收银台订单信息（含按终端过滤后的可用支付方式列表）
 * @param orderId 订单ID
 * @param sig     签名参数（用于校验订单归属）
 * @param client  终端：PC | H5 | APP，与管理端路由 client_scopes 对齐
 */
export const getCashierInfo = async (orderId: string, sig: string, client?: string): Promise<CashierInfo> => {
  const params = new URLSearchParams()
  if (sig) params.set('sig', sig)
  if (client) params.set('client', client)
  const qs = params.toString()
  const url = qs ? `/cashier/${orderId}?${qs}` : `/cashier/${orderId}`
  const data = (await request.get(url)) as CashierInfo & { paymentMethods?: unknown[] }
  return normalizeCashierInfo(data)
}

/**
 * 发起支付
 */
export const createPayment = (data: CreatePaymentDTO): Promise<PaymentResult> =>
  request.post('/payments', data)

/**
 * 轮询支付状态
 * @param paymentId 支付单ID
 */
export const pollPaymentStatus = (paymentId: string): Promise<{ status: string }> =>
  request.get(`/payments/status/${paymentId}`)

/**
 * 获取电子收据
 * @param orderId 订单ID
 */
export const getReceipt = (orderId: string): Promise<ReceiptInfo> =>
  request.get(`/cashier/${orderId}/receipt`)

/** PDF 下载 URL（走 Vite 代理，无需 Bearer） */
export const receiptPdfUrl = (orderId: string): string =>
  `/api/v1/cashier/${encodeURIComponent(orderId)}/receipt/pdf`
