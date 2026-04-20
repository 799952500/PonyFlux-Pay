/**
 * 支付渠道管理 API
 * 渠道管理 / 账户管理 / 商户路由
 */
import request from './request'

// ============================================================
// 渠道管理 API
// ============================================================
export const listChannels = () =>
  request.get('/admin/channels')

export const createChannel = (data) =>
  request.post('/admin/channels', data)

export const updateChannel = (id, data) =>
  request.put(`/admin/channels/${id}`, data)

export const toggleChannel = (id) =>
  request.put(`/admin/channels/${id}/toggle`)

// ============================================================
// 账户管理 API
// ============================================================
export const getAccountsByChannel = (channelId) =>
  request.get(`/admin/channels/${channelId}/accounts`)

export const listAllAccounts = () =>
  request.get('/admin/channels/accounts')

export const createAccount = (data) =>
  request.post('/admin/channels/accounts', data)

export const updateAccount = (id, data) =>
  request.put(`/admin/channels/accounts/${id}`, data)

export const toggleAccount = (id) =>
  request.put(`/admin/channels/accounts/${id}/toggle`)

export const deleteAccount = (id) =>
  request.delete(`/admin/channels/accounts/${id}`)

// ============================================================
// 商户路由 API
// ============================================================
export const listRoutes = (merchantId) =>
  request.get('/admin/channels/routes', { params: merchantId ? { merchantId } : {} })

export const createRoute = (data) =>
  request.post('/admin/channels/routes', data)

export const deleteRoute = (routeId) =>
  request.delete(`/admin/channels/routes/${routeId}`)

export const toggleRoute = (routeId) =>
  request.put(`/admin/channels/routes/${routeId}/toggle`)

// ============================================================
// 辅助接口（商户列表用于路由页下拉）
// ============================================================
export const getMerchantsSimple = () =>
  request.get('/admin/merchants/simple')
