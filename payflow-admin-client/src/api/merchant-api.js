/**
 * 商户管理 API
 */
import request from './request'

// ============================================================
// 商户管理
// ============================================================

/** 获取商户列表（axios baseURL 已为 /api/v1） */
export const listMerchants = (params) =>
  request.get('/admin/merchants', { params })

/** 创建商户 */
export const createMerchant = (data) =>
  request.post('/admin/merchants', data)

/** 更新商户 */
export const updateMerchant = (id, data) =>
  request.put(`/admin/merchants/${id}`, data)

/** 删除商户 */
export const deleteMerchant = (id) =>
  request.delete(`/admin/merchants/${id}`)

// ============================================================
// 商户支付方式管理
// ============================================================

/** 获取商户支持的支付方式列表（后端为 GET + merchantId 查询参数） */
export const listMerchantPayments = (merchantId) =>
  request.get('/admin/merchant-payment-methods', { params: merchantId ? { merchantId } : {} })

/** 创建商户支付方式关联 */
export const createMerchantPayment = (data) =>
  request.post('/admin/merchant-payment-methods', data)

/** 删除商户支付方式关联 */
export const deleteMerchantPayment = (id) =>
  request.delete(`/admin/merchant-payment-methods/${id}`)

/** 切换商户支付方式状态 */
export const toggleMerchantPayment = (id) =>
  request.put(`/admin/merchant-payment-methods/${id}/toggle`)
