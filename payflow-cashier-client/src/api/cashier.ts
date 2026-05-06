/**
 * 收银台相关接口
 */
import request from './request'
import type { CashierInfo, PaymentResult, CreatePaymentDTO, ReceiptInfo } from '@/types'

/**
 * 获取收银台订单信息
 * @param orderId 订单ID
 * @param sig     签名参数（用于校验订单归属）
 */
/** @param client 终端：PC | H5 | APP，用于过滤管理端配置的展示范围 */
export const getCashierInfo = (orderId: string, sig: string, client?: string): Promise<CashierInfo> => {
  const params = new URLSearchParams()
  if (sig) params.set('sig', sig)
  if (client) params.set('client', client)
  const qs = params.toString()
  const url = qs ? `/cashier/${orderId}?${qs}` : `/cashier/${orderId}`
  return request.get(url)
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
