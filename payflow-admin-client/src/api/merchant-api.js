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
// 商户支付配置（方式 + 账号 + 终端）
// ============================================================

/** 商户支付路由列表 */
export const listMerchantPayments = (merchantId) =>
  request.get('/admin/merchant-payment-routes', { params: merchantId ? { merchantId } : {} })

/** 新增一条商户支付路由 */
export const createMerchantPayment = (data) =>
  request.post('/admin/merchant-payment-routes/item', data)

/** 删除商户支付路由 */
export const deleteMerchantPayment = (id) =>
  request.delete(`/admin/merchant-payment-routes/${id}`)

/** 切换商户支付路由启用状态 */
export const toggleMerchantPayment = (id) =>
  request.put(`/admin/merchant-payment-routes/${id}/toggle`)
