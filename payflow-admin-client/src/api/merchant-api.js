/**
 * 商户管理 API
 */
import request from './request'

// ============================================================
// 商户管理
// ============================================================

/** 获取商户列表 */
export const listMerchants = (params) =>
  request.get('/api/v1/merchants', { params })

/** 创建商户 */
export const createMerchant = (data) =>
  request.post('/api/v1/merchants', data)

/** 更新商户 */
export const updateMerchant = (id, data) =>
  request.put(`/api/v1/merchants/${id}`, data)

/** 删除商户 */
export const deleteMerchant = (id) =>
  request.delete(`/api/v1/merchants/${id}`)

// ============================================================
// 商户支付方式管理
// ============================================================

/** 获取商户支持的支付方式列表 */
export const listMerchantPayments = (merchantId) =>
  request.get(`/api/v1/merchant-payment-methods/merchant/${merchantId}`)

/** 创建商户支付方式关联 */
export const createMerchantPayment = (data) =>
  request.post('/api/v1/merchant-payment-methods', data)

/** 删除商户支付方式关联 */
export const deleteMerchantPayment = (id) =>
  request.delete(`/api/v1/merchant-payment-methods/${id}`)

/** 切换商户支付方式状态 */
export const toggleMerchantPayment = (id) =>
  request.put(`/api/v1/merchant-payment-methods/${id}/toggle`)
