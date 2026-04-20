/**
 * 支付方式 API
 */
import request from './request'

// ============================================================
// 支付方式管理
// ============================================================

/** 获取支付方式列表 */
export const listPaymentMethods = (params) =>
  request.get('/api/v1/payment-methods', { params })

/** 按渠道获取支付方式 */
export const listByChannel = (channelId) =>
  request.get(`/api/v1/payment-methods/channel/${channelId}`)

/** 创建支付方式 */
export const createPaymentMethod = (data) =>
  request.post('/api/v1/payment-methods', data)

/** 更新支付方式 */
export const updatePaymentMethod = (id, data) =>
  request.put(`/api/v1/payment-methods/${id}`, data)

/** 删除支付方式 */
export const deletePaymentMethod = (id) =>
  request.delete(`/api/v1/payment-methods/${id}`)

/** 切换支付方式状态 */
export const togglePaymentMethod = (id) =>
  request.put(`/api/v1/payment-methods/${id}/toggle`)
