/**
 * 收银台相关接口
 */
import request from './request'
import type { CashierInfo, PaymentResult, CreatePaymentDTO } from '@/types'

/**
 * 获取收银台订单信息
 * @param orderId 订单ID
 * @param sig     签名参数（用于校验订单归属）
 */
export const getCashierInfo = (orderId: string, sig: string): Promise<CashierInfo> => {
  const url = sig ? `/cashier/${orderId}?sig=${sig}` : `/cashier/${orderId}`
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
