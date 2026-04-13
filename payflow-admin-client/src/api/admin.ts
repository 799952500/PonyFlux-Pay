/**
 * 管理后台相关接口
 */
import request from './request'
import type {
  OrderListQuery,
  OrderListResponse,
  Order,
  DashboardStats,
  RefundItem,
  ChannelConfig,
  Merchant,
  PageResult,
  RiskRule,
} from '@/types'

// -------------------------------------------------------------------
// Dashboard
// -------------------------------------------------------------------
export const getDashboardStats = (): Promise<DashboardStats> =>
  request.get('/admin/dashboard')

// -------------------------------------------------------------------
// 订单管理
// -------------------------------------------------------------------
export const getOrders = (params: OrderListQuery): Promise<OrderListResponse> =>
  request.get('/admin/orders', { params })

export const getOrderDetail = (orderId: string): Promise<Order> =>
  request.get(`/admin/orders/${orderId}`)

export const closeOrder = (orderId: string) =>
  request.post(`/admin/orders/${orderId}/close`)

// -------------------------------------------------------------------
// 退款管理
// -------------------------------------------------------------------
export const getRefunds = (params: {
  page: number
  pageSize: number
  status?: string
  keyword?: string
}): Promise<PageResult<RefundItem>> =>
  request.get('/admin/refunds', { params })

export const approveRefund = (refundId: string) =>
  request.post(`/admin/refunds/${refundId}/approve`)

export const rejectRefund = (refundId: string) =>
  request.post(`/admin/refunds/${refundId}/reject`)

// -------------------------------------------------------------------
// 渠道管理
// -------------------------------------------------------------------
export const getChannels = (): Promise<ChannelConfig[]> =>
  request.get('/admin/channels')

export const toggleChannel = (channelId: string, enabled: boolean) =>
  request.patch(`/admin/channels/${channelId}`, { enabled })

// -------------------------------------------------------------------
// 商户管理
// -------------------------------------------------------------------
export const getMerchants = (params: {
  page: number
  pageSize: number
  keyword?: string
}): Promise<PageResult<Merchant>> =>
  request.get('/admin/merchants', { params })

export const getMerchantDetail = (merchantId: string): Promise<Merchant> =>
  request.get(`/admin/merchants/${merchantId}`)

// -------------------------------------------------------------------
// 风控规则
// -------------------------------------------------------------------
export const getRiskRules = (): Promise<RiskRule[]> =>
  request.get('/admin/risk/rules')

export const updateRiskRule = (ruleId: string, data: Partial<RiskRule>): Promise<RiskRule> =>
  request.put(`/admin/risk/rules/${ruleId}`, data)
