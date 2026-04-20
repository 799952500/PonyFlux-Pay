/**
 * 支付渠道 API
 * 渠道管理
 */
import request from './request'

// ============================================================
// 渠道管理
// ============================================================

/** 获取渠道列表 */
export const listChannels = (params) =>
  request.get('/api/v1/channels', { params })

/** 创建渠道 */
export const createChannel = (data) =>
  request.post('/api/v1/channels', data)

/** 更新渠道 */
export const updateChannel = (id, data) =>
  request.put(`/api/v1/channels/${id}`, data)

/** 删除渠道 */
export const deleteChannel = (id) =>
  request.delete(`/api/v1/channels/${id}`)

/** 切换渠道状态 */
export const toggleChannel = (id) =>
  request.put(`/api/v1/channels/${id}/toggle`)
