/**
 * 支付渠道 API
 * 渠道管理
 */
import request from './request'

// ============================================================
// 渠道管理
// ============================================================

/** 获取渠道列表（axios baseURL 已为 /api/v1，此处不得再拼 /api/v1） */
export const listChannels = (params) =>
  request.get('/admin/channels', { params })

/** 创建渠道 */
export const createChannel = (data) =>
  request.post('/admin/channels', data)

/** 更新渠道 */
export const updateChannel = (id, data) =>
  request.put(`/admin/channels/${id}`, data)

/** 删除渠道 */
export const deleteChannel = (id) =>
  request.delete(`/admin/channels/${id}`)

/** 切换渠道状态 */
export const toggleChannel = (id) =>
  request.put(`/admin/channels/${id}/toggle`)
