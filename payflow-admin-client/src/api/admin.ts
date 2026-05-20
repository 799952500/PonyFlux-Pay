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
  Merchant,
  PageResult,
  RiskRule,
  PaymentAccount,
  MerchantPaymentRoute,
  SysRole,
  SysMenu,
  Channel,
  ChannelRoute,
  AuditLogItem,
  OrderStats,
  AdminSearchOrderHit,
} from '@/types'

// -------------------------------------------------------------------
// Dashboard
// -------------------------------------------------------------------
export const getDashboardStats = (trendDays: number = 7): Promise<DashboardStats> =>
  request.get('/admin/dashboard', { params: { trendDays } })

/** 登录页功能开关 */
export const getLoginFeatures = (): Promise<{ loginCaptchaEnabled: boolean; loginMaxFailures: number }> =>
  request.get('/admin/meta/features')

// -------------------------------------------------------------------
// 订单管理
// -------------------------------------------------------------------
export const getOrders = (params: OrderListQuery): Promise<OrderListResponse> => {
  const q: Record<string, unknown> = {
    page: params.page,
    size: params.pageSize,
  }
  if (params.status) q.status = params.status
  if (params.merchantId) q.merchantId = params.merchantId
  if (params.dateRange?.length === 2) {
    q.startTime = `${params.dateRange[0]} 00:00:00`
    q.endTime = `${params.dateRange[1]} 23:59:59`
  }
  return request.get('/admin/orders', { params: q }).then((data: any) => ({
    list: data?.list ?? [],
    total: Number(data?.total ?? 0),
    page: Number(data?.page ?? params.page),
    pageSize: Number(data?.size ?? params.pageSize),
  }))
}

/** 订单详情接口返回结构（与 AdminOrderController#getOrder 一致） */
interface OrderDetailPayload {
  order?: Order
  payments?: unknown[]
}

export const getOrderDetail = async (orderId: string): Promise<Order> => {
  const data = (await request.get(
    `/admin/orders/${encodeURIComponent(orderId)}`
  )) as OrderDetailPayload
  if (!data?.order) {
    throw new Error('订单不存在')
  }
  return data.order
}

export const closeOrder = (orderId: string) =>
  request.post(`/admin/orders/${orderId}/close`)

export const getOrderStats = (params?: { merchantId?: string }): Promise<OrderStats> =>
  request.get('/admin/orders/stats', { params }).then((data: unknown) => {
    const raw = data as { total?: number; statusCount?: Array<Record<string, unknown>> }
    return {
      total: Number(raw?.total ?? 0),
      statusCount: (raw?.statusCount ?? []).map((row) => ({
        status: String(row.status ?? ''),
        cnt: Number(row.cnt ?? row.count ?? 0),
      })),
    }
  })

export const listOrdersByMerchant = (merchantId: string): Promise<Order[]> =>
  request.get(`/admin/orders/merchant/${encodeURIComponent(merchantId)}`)

/**
 * 导出订单 CSV（使用 axios blob 响应）。
 */
export async function exportOrdersCsv(filters: {
  merchantId?: string
  status?: string
  startTime?: string
  endTime?: string
  maxRows?: number
}): Promise<void> {
  const p = new URLSearchParams()
  if (filters.merchantId) p.set('merchantId', filters.merchantId)
  if (filters.status) p.set('status', filters.status)
  if (filters.startTime) p.set('startTime', filters.startTime)
  if (filters.endTime) p.set('endTime', filters.endTime)
  if (filters.maxRows != null) p.set('maxRows', String(filters.maxRows))
  const res = await request.get(`/admin/orders/export?${p.toString()}`, { responseType: 'blob' })
  const blob = new Blob([res as unknown as BlobPart])
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `orders-export-${Date.now()}.csv`
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(a.href)
}

// -------------------------------------------------------------------
// 全局搜索 / 字典 / 元数据 / 系统配置扩展
// -------------------------------------------------------------------
export const adminSearchOrders = (q: string, limit = 20): Promise<AdminSearchOrderHit[]> =>
  request.get('/admin/search', { params: { q, limit } })

export const getDicts = (): Promise<Record<string, unknown>> =>
  request.get('/admin/dicts')

export const getMetaVersion = (): Promise<{ application: string; profiles: string }> =>
  request.get('/admin/meta/version')

export const getSystemConfigCategories = (): Promise<string[]> =>
  request.get('/admin/system-configs/categories')

export const getSystemConfigMap = (): Promise<Record<string, string>> =>
  request.get('/admin/system-configs/map')

export const getSystemConfigValue = (key: string): Promise<string> =>
  request.get(`/admin/system-configs/${encodeURIComponent(key)}`)

// -------------------------------------------------------------------
// 支付方式详情
// -------------------------------------------------------------------
export const getPaymentMethodById = (id: number): Promise<any> =>
  request.get(`/admin/payment-methods/${id}`)

export const getPaymentMethodsByChannelId = (channelId: number): Promise<any[]> =>
  request.get(`/admin/payment-methods/channel/${channelId}`)

// -------------------------------------------------------------------
// 用户详情
// -------------------------------------------------------------------
export const getUserById = (id: number): Promise<any> =>
  request.get(`/admin/users/${id}`)

// -------------------------------------------------------------------
// 退款管理
// -------------------------------------------------------------------
export const getRefunds = (params: {
  page: number
  pageSize: number
  status?: string
  keyword?: string
  startDate?: string
  endDate?: string
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
export const getMerchantPaymentMethods = (merchantId?: string): Promise<any[]> =>
  request.get('/admin/merchant-payment-methods', {
    params: merchantId ? { merchantId } : {},
  })

export const saveMerchantPaymentMethods = (merchantId: string, paymentMethodIds: number[]) =>
  request.post('/admin/merchant-payment-methods', { merchantId, paymentMethodIds })

export const deleteMerchantPayment = (id: number) =>
  request.delete(`/admin/merchant-payment-methods/${id}`)

export const toggleMerchantPayment = (id: number) =>
  request.put(`/admin/merchant-payment-methods/${id}/toggle`)

export const createMerchantPayment = (data: { merchantId: string; paymentMethodId: string | number; priority?: number }) =>
  request.post('/admin/merchant-payment-methods/item', data)

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
  request.get('/admin/channels/accounts', { params })

export const createPaymentAccount = (data: Partial<PaymentAccount>) =>
  request.post('/admin/channels/accounts', data)

export const updatePaymentAccount = (id: number, data: Partial<PaymentAccount>) =>
  request.put(`/admin/channels/accounts/${id}`, data)

export const deletePaymentAccount = (id: number) =>
  request.delete(`/admin/channels/accounts/${id}`)

// -------------------------------------------------------------------
// 支付路由（ChannelRoute 实体，管理「商户→渠道账户」的支付路由）
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
export const getMerchantPaymentRoutes = (merchantId?: string): Promise<MerchantPaymentRoute[]> =>
  request.get('/admin/merchant-payment-routes', { params: merchantId ? { merchantId } : {} })

export const replaceMerchantPaymentRoutes = (merchantId: string, routes: Array<{
  paymentMethodId: number
  paymentAccountId: number
  enabled: boolean
  priority: number
  clientScopes?: string[]
}>) =>
  request.post('/admin/merchant-payment-routes/replace', { merchantId, routes })

export const createMerchantPaymentRouteItem = (data: {
  merchantId: string
  paymentMethodId: number
  paymentAccountId: number
  priority?: number
  enabled?: boolean
  clientScopes?: string[]
}) => request.post('/admin/merchant-payment-routes/item', data)

export const updateMerchantPaymentRoute = (
  id: number,
  data: Partial<{
    paymentMethodId: number
    paymentAccountId: number
    priority: number
    enabled: boolean
    clientScopes: string[]
  }>
) => request.put(`/admin/merchant-payment-routes/${id}`, data)

export const toggleMerchantPaymentRoute = (id: number) =>
  request.put(`/admin/merchant-payment-routes/${id}/toggle`)

export const deleteMerchantPaymentRoute = (id: number) =>
  request.delete(`/admin/merchant-payment-routes/${id}`)

// -------------------------------------------------------------------
// 操作日志（审计）
// -------------------------------------------------------------------
export const getAuditLogs = (params: {
  page: number
  pageSize: number
  username?: string
  action?: string
  startDate?: string
  endDate?: string
}): Promise<PageResult<AuditLogItem>> =>
  request.get('/admin/audit-logs', { params })

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

// -------------------------------------------------------------------
// 资金对账（代理至 payflow-recon-server）
// -------------------------------------------------------------------
export interface ReconTaskItem {
  taskId: string
  channel: string
  accountCode: string
  billDate: string
  billType?: string
  status: string
  fileObjectKey?: string
  fileSize?: number
  billTotalCount?: number
  billTotalAmount?: number
  localTotalCount?: number
  localTotalAmount?: number
  diffCount?: number
  elapsedMs?: number
  errorMsg?: string
  triggeredBy?: string
  xxlLogId?: number
  createdAt?: string
  updatedAt?: string
}

export interface ReconDiffItem {
  id: number
  taskId: string
  diffType: string
  channelTradeNo?: string
  localOrderId?: string
  channelAmount?: number
  localAmount?: number
  channelStatus?: string
  localStatus?: string
  handleStatus: string
  handleRemark?: string
  handledBy?: string
  handledAt?: string
}

export const getReconTasks = (params: {
  page?: number
  size?: number
  billDate?: string
  channel?: string
  status?: string
}): Promise<{ list: ReconTaskItem[]; total: number; page: number; size: number }> =>
  request.get('/admin/reconcile/tasks', { params }).then((data: any) => ({
    list: data?.list ?? [],
    total: Number(data?.total ?? 0),
    page: Number(data?.page ?? params.page ?? 1),
    size: Number(data?.size ?? params.size ?? 20),
  }))

export const getReconTaskDetail = (taskId: string): Promise<ReconTaskItem> =>
  request.get(`/admin/reconcile/tasks/${encodeURIComponent(taskId)}`)

export const getReconDiffs = (
  taskId: string,
  params: {
    page?: number
    size?: number
    diffType?: string
    handleStatus?: string
  }
): Promise<{ list: ReconDiffItem[]; total: number; page: number; size: number }> =>
  request
    .get(`/admin/reconcile/tasks/${encodeURIComponent(taskId)}/diffs`, { params })
    .then((data: any) => ({
      list: data?.list ?? [],
      total: Number(data?.total ?? 0),
      page: Number(data?.page ?? params.page ?? 1),
      size: Number(data?.size ?? params.size ?? 20),
    }))

export const triggerReconManual = (body: {
  reconChannel: string
  accountCode: string
  billDate: string
}): Promise<{ taskId: string }> => request.post('/admin/reconcile/tasks/manual-run', body)

export const handleReconDiff = (id: number, body: { action: string; remark?: string }) =>
  request.post(`/admin/reconcile/diffs/${id}/handle`, body)

export interface ReconOrderResultItem {
  orderId?: string
  merchantId?: string
  paymentId?: string
  payChannel?: string
  channelTransactionId?: string
  localAmountFen?: number
  reconStatus: string
  diffType?: string
  handleStatus?: string
  diffId?: number
  taskId?: string
  reconChannel?: string
  accountCode?: string
  channelAmountFen?: number
}

export const getReconOrderResults = (params: {
  billDate: string
  channel?: string
  merchantId?: string
  orderKeyword?: string
  onlyAbnormal?: boolean
  page?: number
  size?: number
}): Promise<{ list: ReconOrderResultItem[]; total: number; page: number; size: number }> =>
  request.get('/admin/reconcile/order-results', { params }).then((data: any) => ({
    list: data?.list ?? [],
    total: Number(data?.total ?? 0),
    page: Number(data?.page ?? params.page ?? 1),
    size: Number(data?.size ?? params.size ?? 20),
  }))

export interface ReconSummaryData {
  byAccount: Array<{
    accountCode: string
    channel: string
    localSuccessCount: number
    localSuccessAmountFen: number
    channelBillCount: number
    channelBillAmountFen: number
    amountDeltaFen: number
  }>
  totalLocalAmountFen: number
  totalChannelBillAmountFen: number
  totalAmountDeltaFen: number
  pendingDiffCount: number
}

export const getReconSummary = (params: {
  billDate: string
  channel?: string
  accountCode?: string
}): Promise<ReconSummaryData> => request.get('/admin/reconcile/summary', { params })

export interface ReconAnomalyItem {
  diffId: number
  taskId: string
  diffType: string
  channelTradeNo?: string
  localOrderId?: string
  merchantId?: string
  channelAmount?: number
  localAmount?: number
  handleStatus: string
  reconChannel?: string
  accountCode?: string
  billDate?: string
  /** 对账自动建议处置 */
  suggestedAction?: string
}

export const getReconAnomalies = (params: {
  billDate: string
  channel?: string
  accountCode?: string
  handleStatus?: string
  page?: number
  size?: number
}): Promise<{ list: ReconAnomalyItem[]; total: number; page: number; size: number }> =>
  request.get('/admin/reconcile/anomalies', { params }).then((data: any) => ({
    list: data?.list ?? [],
    total: Number(data?.total ?? 0),
    page: Number(data?.page ?? params.page ?? 1),
    size: Number(data?.size ?? params.size ?? 20),
  }))

// -------------------------------------------------------------------
// 运营洞察 / 进件 / 路由健康
// -------------------------------------------------------------------
export const getInsightsFunnel = (): Promise<Record<string, unknown>> =>
  request.get('/admin/insights/funnel')

export const listOnboardingApplications = (): Promise<unknown[]> =>
  request.get('/admin/onboarding/applications')

export const getChannelRoutingHealth = (accountCode: string): Promise<Record<string, unknown>> =>
  request.get('/admin/channel-routing/health', { params: { accountCode } })

// -------------------------------------------------------------------
// 仪表盘 - 预聚合指标
// -------------------------------------------------------------------
export const getDashboardMetrics = (params: {
  granularity?: string
  dateFrom?: string
  dateTo?: string
  channelCode?: string
}): Promise<any> =>
  request.get('/admin/dashboard/metrics', { params })

export const getMerchantRanking = (days: number = 30, limit: number = 10): Promise<any[]> =>
  request.get('/admin/dashboard/merchant-ranking', { params: { days, limit } }).then((d: any) => d?.data ?? d ?? [])

export const getMerchantInsight = (merchantId: string): Promise<any> =>
  request.get(`/admin/dashboard/merchant/${encodeURIComponent(merchantId)}/insight`)

// -------------------------------------------------------------------
// 数据导出
// -------------------------------------------------------------------
export const createExportTask = (params: { dateFrom: string; dateTo: string; merchantId?: string }): Promise<{ taskId: string; status: string }> =>
  request.post('/admin/export/report', null, { params })

export const getExportTasks = (): Promise<any[]> =>
  request.get('/admin/export/tasks').then((d: any) => d?.data ?? d ?? [])

// -------------------------------------------------------------------
// 流失预警
// -------------------------------------------------------------------
export const getChurnAlerts = (params: {
  page?: number
  size?: number
  status?: string
  merchantId?: string
}): Promise<PageResult<any>> =>
  request.get('/admin/churn-alerts', { params }).then((data: any) => ({
    list: data?.data?.list ?? data?.list ?? [],
    total: Number(data?.data?.total ?? data?.total ?? 0),
    page: Number(data?.data?.page ?? params.page ?? 1),
    pageSize: Number(data?.data?.size ?? params.size ?? 20),
  }))

export const getChurnAlertDetail = (id: number): Promise<any> =>
  request.get(`/admin/churn-alerts/${id}`)

export const updateChurnAlertStatus = (id: number, data: { status: string; note?: string; assignee?: string }) =>
  request.put(`/admin/churn-alerts/${id}/status`, data)

// -------------------------------------------------------------------
// 阶梯费率
// -------------------------------------------------------------------
export const getFeeRates = (): Promise<any[]> =>
  request.get('/admin/fee-rates').then((d: any) => d?.data ?? d ?? [])

export const createFeeRate = (data: any): Promise<any> =>
  request.post('/admin/fee-rates', data)

export const updateFeeRate = (id: number, data: any): Promise<any> =>
  request.put(`/admin/fee-rates/${id}`, data)

export const deleteFeeRate = (id: number): Promise<any> =>
  request.delete(`/admin/fee-rates/${id}`)

export const getFeeRateAuditLog = (params: {
  merchantId?: string
  page?: number
  size?: number
}): Promise<PageResult<any>> =>
  request.get('/admin/fee-rates/audit-log', { params }).then((data: any) => ({
    list: data?.records ?? data?.list ?? (Array.isArray(data) ? data : []),
    total: Number(data?.total ?? 0),
    page: Number(data?.current ?? params.page ?? 1),
    pageSize: Number(data?.size ?? params.size ?? 20),
  }))

export const getMerchantFeeProgress = (merchantId: string): Promise<any> =>
  request.get(`/admin/merchant-fee/${encodeURIComponent(merchantId)}/progress`).then((d: any) => d?.data ?? d ?? null)

export const getMerchantFeeHistory = (merchantId: string): Promise<any[]> =>
  request.get(`/admin/merchant-fee/${encodeURIComponent(merchantId)}/history`).then((d: any) => d?.data ?? d ?? [])

// -------------------------------------------------------------------
// 路由决策日志
// -------------------------------------------------------------------
export const getRoutingLogs = (params: {
  page?: number
  size?: number
  tradeNo?: string
  merchantId?: string
  selectedChannel?: string
  startTime?: string
  endTime?: string
}): Promise<PageResult<any>> =>
  request.get('/admin/routing-logs', { params }).then((data: any) => ({
    list: data?.data?.list ?? data?.list ?? [],
    total: Number(data?.data?.total ?? data?.total ?? 0),
    page: Number(data?.data?.page ?? params.page ?? 1),
    pageSize: Number(data?.data?.size ?? params.size ?? 20),
  }))

export const exportRoutingLogs = (params: { startTime?: string; endTime?: string }): Promise<any[]> =>
  request.get('/admin/routing-logs/export', { params }).then((d: any) => d?.data ?? d ?? [])
