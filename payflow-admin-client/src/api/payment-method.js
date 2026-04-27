/**
 * 支付方式 API
 */
import request from './request'

// ============================================================
// 支付方式管理
// ============================================================

/** 获取支付方式列表 */
export const listPaymentMethods = (params) =>
  request.get('/admin/payment-methods', { params })

/** 按渠道获取支付方式 */
export const listByChannel = (channelId) =>
  request.get(`/admin/payment-methods/channel/${channelId}`)

/** 创建支付方式 */
export const createPaymentMethod = (data) =>
  request.post('/admin/payment-methods', data)

/** 更新支付方式 */
export const updatePaymentMethod = (id, data) =>
  request.put(`/admin/payment-methods/${id}`, data)

/** 删除支付方式 */
export const deletePaymentMethod = (id) =>
  request.delete(`/admin/payment-methods/${id}`)

// 目前后端未提供 toggle 接口，如需启停请用 updatePaymentMethod 更新 enabled 字段
