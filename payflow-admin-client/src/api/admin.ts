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
  PaymentAccount,
  MerchantPaymentRoute,
  SysRole,
  SysMenu,
  Channel,
  ChannelAccount,
  ChannelRoute,
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
export const getChannels = async (): Promise<Channel[]> => {
  const data = await request.get('/admin/channels') as any
  const list: any[] = Array.isArray(data) ? data : (data?.list ?? [])
  return list.map((item, index) => {
    const channelCode = item.channelCode ?? item.channel ?? ''
    const channelName = item.channelName ?? item.name ?? ''
    return {
      id: item.id ?? index + 1,
      channelCode,
      channelName,
      channelType: item.channelType,
      apiUrl: item.apiUrl,
      apiKey: item.apiKey,
      enabled: item.enabled ?? true,
      priority: item.priority ?? 0,
      icon: item.icon,
      description: item.description,
      createdAt: item.createdAt ?? '',
      updatedAt: item.updatedAt ?? '',
    } satisfies Channel
  })
}

export const createChannel = (data: Partial<Channel>) =>
  request.post('/admin/channels', data)

export const updateChannel = (id: number, data: Partial<Channel>) =>
  request.put(`/admin/channels/${id}`, data)

export const deleteChannel = (id: number) =>
  request.delete(`/admin/channels/${id}`)

export const toggleChannel = (channel: number | string) =>
  request.put(`/admin/channels/${channel}/toggle`)

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

export const updateMerchant = (merchantId: string, data: Partial<Merchant>) =>
  request.put(`/admin/merchants/${merchantId}`, data)

export const deleteMerchant = (merchantId: string) =>
  request.delete(`/admin/merchants/${merchantId}`)

export const getMerchantsSimple = (): Promise<Array<{ merchantId: string; merchantName: string }>> =>
  request.get('/admin/merchants/simple')

// -------------------------------------------------------------------
// 商户支付方式配置
// -------------------------------------------------------------------
export const getMerchantPaymentMethods = (merchantId: string): Promise<any[]> =>
  request.get(`/admin/merchant-payment-methods?merchantId=${merchantId}`)

export const saveMerchantPaymentMethods = (merchantId: string, paymentMethodIds: number[]) =>
  request.post('/admin/merchant-payment-methods', { merchantId, paymentMethodIds })

export const deleteMerchantPayment = (id: number) =>
  request.delete(`/admin/merchant-payment-methods/${id}`)

export const toggleMerchantPayment = (id: number) =>
  request.put(`/admin/merchant-payment-methods/${id}/toggle`)

export const createMerchantPayment = (data: { merchantId: string; paymentMethodId: string | number; priority?: number }) =>
  request.post('/admin/merchant-payment-methods', data)

// -------------------------------------------------------------------
// 风控规则
// -------------------------------------------------------------------
export const getRiskRules = (): Promise<RiskRule[]> =>
  request.get('/admin/risk/rules')

export const updateRiskRule = (ruleId: string, data: Partial<RiskRule>): Promise<RiskRule> =>
  request.put(`/admin/risk/rules/${ruleId}`, data)

// -------------------------------------------------------------------
// 支付方式
// -------------------------------------------------------------------
export const getPaymentMethods = (params: {
  page: number
  pageSize: number
  keyword?: string
}): Promise<PageResult<any>> =>
  request.get('/admin/payment-methods', { params })

export const deletePaymentMethod = (id: number) =>
  request.delete(`/admin/payment-methods/${id}`)

export const createPaymentMethod = (data: any) =>
  request.post('/admin/payment-methods', data)

export const updatePaymentMethod = (id: number, data: any) =>
  request.put(`/admin/payment-methods/${id}`, data)

// -------------------------------------------------------------------
// 支付账号（收款账户池）
// -------------------------------------------------------------------
export const getPaymentAccounts = (params: {
  page: number
  pageSize: number
  channelId?: number
  keyword?: string
}): Promise<PageResult<PaymentAccount>> =>
  request.get('/admin/payment-accounts', { params })

export const createPaymentAccount = (data: Partial<PaymentAccount>) =>
  request.post('/admin/payment-accounts', data)

export const updatePaymentAccount = (id: number, data: Partial<PaymentAccount>) =>
  request.put(`/admin/payment-accounts/${id}`, data)

export const deletePaymentAccount = (id: number) =>
  request.delete(`/admin/payment-accounts/${id}`)

// -------------------------------------------------------------------
// 渠道账号（ChannelAccount 池）
// -------------------------------------------------------------------
export const getChannelAccounts = (params?: {
  page?: number
  pageSize?: number
  channelId?: number
  keyword?: string
}): Promise<PageResult<ChannelAccount>> =>
  request.get('/admin/channels/accounts', { params })

export const createChannelAccount = (data: Partial<ChannelAccount>) =>
  request.post('/admin/channels/accounts', data)

export const updateChannelAccount = (id: number, data: Partial<ChannelAccount>) =>
  request.put(`/admin/channels/accounts/${id}`, data)

export const toggleChannelAccount = (id: number) =>
  request.put(`/admin/channels/accounts/${id}/toggle`)

export const deleteChannelAccount = (id: number) =>
  request.delete(`/admin/channels/accounts/${id}`)

// -------------------------------------------------------------------
// 渠道路由（ChannelRoute）
// -------------------------------------------------------------------
export const getChannelRoutes = (params?: {
  merchantId?: string
  page?: number
  pageSize?: number
}): Promise<PageResult<ChannelRoute>> =>
  request.get('/admin/channels/routes', { params })

export const createChannelRoute = (data: Partial<ChannelRoute>) =>
  request.post('/admin/channels/routes', data)

export const toggleChannelRoute = (id: number) =>
  request.put(`/admin/channels/routes/${id}/toggle`)

export const deleteChannelRoute = (id: number) =>
  request.delete(`/admin/channels/routes/${id}`)

// -------------------------------------------------------------------
// 商户支付路由（方式+账号）
// -------------------------------------------------------------------
export const getMerchantPaymentRoutes = (merchantId: string): Promise<MerchantPaymentRoute[]> =>
  request.get('/admin/merchant-payment-routes', { params: { merchantId } })

export const replaceMerchantPaymentRoutes = (merchantId: string, routes: Array<{
  paymentMethodId: number
  paymentAccountId: number
  enabled: boolean
  priority: number
}>) =>
  request.post('/admin/merchant-payment-routes/replace', { merchantId, routes })

// -------------------------------------------------------------------
// 角色管理
// -------------------------------------------------------------------
export const getRoles = (): Promise<SysRole[]> =>
  request.get('/admin/roles')

export const createRole = (data: Partial<SysRole>) =>
  request.post('/admin/roles', data)

export const updateRole = (id: number, data: Partial<SysRole>) =>
  request.put(`/admin/roles/${id}`, data)

export const deleteRole = (id: number) =>
  request.delete(`/admin/roles/${id}`)

export const getRoleMenus = (roleId: number): Promise<SysMenu[]> =>
  request.get(`/admin/roles/${roleId}/menus`)

export const assignRoleMenus = (roleId: number, menuIds: number[]) =>
  request.post(`/admin/roles/${roleId}/menus`, { menuIds })

// -------------------------------------------------------------------
// 菜单管理
// -------------------------------------------------------------------
export const getMenuTree = (): Promise<SysMenu[]> =>
  request.get('/admin/menus/tree')

export const createMenu = (data: Partial<SysMenu>) =>
  request.post('/admin/menus', data)

export const updateMenu = (id: number, data: Partial<SysMenu>) =>
  request.put(`/admin/menus/${id}`, data)

export const deleteMenu = (id: number) =>
  request.delete(`/admin/menus/${id}`)

// -------------------------------------------------------------------
// 用户管理
// -------------------------------------------------------------------
export const getUsers = () =>
  request.get('/admin/users')

export const createUser = (data: any) =>
  request.post('/admin/users', data)

export const updateUser = (id: number, data: any) =>
  request.put(`/admin/users/${id}`, data)

export const resetUserPassword = (id: number, newPassword: string) =>
  request.put(`/admin/users/${id}/reset-password`, { newPassword })

export const disableUser = (id: number) =>
  request.put(`/admin/users/${id}/disable`)
