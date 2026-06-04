// ============================================================
// 公共类型定义
// ============================================================

// 订单状态枚举
export type OrderStatus = 'CREATED' | 'PAYING' | 'PAID' | 'EXPIRED' | 'FAILED' | 'REFUNDED'

// 支付渠道
export type PayChannel = 'ALIPAY' | 'WECHAT_PAY' | 'UNION_PAY' | 'CASH' | 'CARD'

// 设备类型
export type DeviceType = 'WEB' | 'H5' | 'APP_IOS' | 'APP_ANDROID' | 'MINIAPP'

// ============================================================
// 订单
// ============================================================
export interface Order {
  orderId: string
  merchantId: string
  merchantOrderNo: string
  subject: string
  body?: string
  amount: number          // 单位：分
  currency: string
  payAmount?: number      // 实际支付金额（分）
  channel?: string
  status: OrderStatus
  notifyUrl?: string
  returnUrl?: string
  expireTime: string
  payTime?: string
  createdAt: string
  updatedAt: string
}

/** 订单关联支付子单（cashier_payments） */
export interface OrderPayment {
  paymentId: string
  orderId: string
  payChannel?: string
  payMethod?: string
  channelTransactionId?: string
  amount: number
  status: string
  createdAt?: string
  updatedAt?: string
}

export interface OrderDetailResponse {
  order: Order
  payments: OrderPayment[]
}

export interface PaymentChannelQueryResult {
  paymentId: string
  orderId: string
  payChannel?: string
  localStatus: string
  channelPaid?: boolean | null
  synced?: boolean
  channelQuerySupported?: boolean
  message?: string
}

export interface OrderRefundRequestResult {
  refundId: string
  paymentId: string
  status: string
  refundAmount: number
}

// ============================================================
// 管理后台 - Dashboard
// ============================================================
export interface DashboardStats {
  todayRevenue: number
  yesterdayRevenue: number
  todayOrders: number
  yesterdayOrders: number
  todayPaid: number
  conversionRate: number
  revenueChangePct: number
  revenueYoYPct: number
  channelDistribution: ChannelDistItem[]
  trendData: TrendDataItem[]
  /** 后端聚合接口返回的最近订单（用于仪表盘表格） */
  recentOrders?: Partial<Order>[]
}

export interface ChannelDistItem {
  channel: string
  name: string
  value: number
  amount: number
}

export interface TrendDataItem {
  date: string
  orders: number
  revenue: number
  paid: number
}

// ============================================================
// 管理后台 - 订单列表
// ============================================================
export interface OrderListQuery {
  page: number
  pageSize: number
  status?: OrderStatus
  channel?: PayChannel
  keyword?: string
  dateRange?: [string, string]
  /** 筛选指定商户订单（与后端 merchantId 一致） */
  merchantId?: string
}

export interface OrderListResponse {
  list: Order[]
  total: number
  page: number
  pageSize: number
}

// ============================================================
// 管理后台 - 退款
// ============================================================
export interface RefundItem {
  refundId: string
  orderId: string
  merchantOrderNo: string
  amount: number
  reason: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED'
  createdAt: string
  updatedAt: string
}

// ============================================================
// 管理后台 - 渠道配置
// ============================================================
export interface ChannelConfig {
  channelId: string
  channelName: string
  enabled: boolean
  config: Record<string, string>
  createdAt: string
  updatedAt: string
}

// ============================================================
// 管理后台 - 支付渠道
// ============================================================
export interface Channel {
  id: number
  channelCode: string
  channelName: string
  channelType?: string
  apiUrl?: string
  apiKey?: string
  enabled: boolean
  priority?: number
  icon?: string
  description?: string
  createdAt: string
  updatedAt: string
}

// ============================================================
// 管理后台 - 商户
// ============================================================
export interface Merchant {
  merchantId: string
  merchantName: string
  merchantType: 'INDIVIDUAL' | 'ENTERPRISE'
  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED'
  contactEmail?: string
  contactPhone?: string
  createdAt: string
  merchantKey?: string
  callbackUrl?: string
  notifyUrl?: string
  commissionRate?: number
}

// ============================================================
// 管理后台 - 支付方式
// ============================================================
export interface PaymentMethod {
  id: number
  channelId: string
  channelName?: string
  methodName: string
  methodNameZhCn?: string
  methodNameZhTw?: string
  methodNameEn?: string
  descriptionZhCn?: string
  descriptionZhTw?: string
  descriptionEn?: string
  methodCode: string
  methodType: string
  icon?: string
  status: 'ACTIVE' | 'INACTIVE'
  minAmount?: number
  maxAmount?: number
  createdAt: string
  updatedAt: string
}

// ============================================================
// 管理后台 - 支付账号（收款账户池）
// ============================================================
export interface PaymentAccount {
  id: number
  channelId: number
  channelName?: string
  accountCode: string
  accountName: string
  enabled: boolean
  priority?: number
  description?: string
  createdAt: string
  updatedAt: string
}

// ============================================================
// 管理后台 - 渠道账号（ChannelAccount 池）
// ============================================================
export interface ChannelAccount {
  id: number
  channelId: number
  accountCode: string
  accountName: string
  appId?: string
  appSecret?: string
  mchId?: string
  mchKey?: string
  certPath?: string
  certPassword?: string
  extConfig?: Record<string, any>
  enabled: boolean
  priority?: number
  description?: string
  createdAt: string
  updatedAt: string
}

// ============================================================
// 管理后台 - 支付路由（ChannelRoute）
// ============================================================
export interface ChannelRoute {
  id: number
  merchantId: string
  channelId: number
  accountId: number
  priority: number
  enabled: boolean
  merchantName?: string
  channelName?: string
  accountName?: string
  accountCode?: string
}

// ============================================================
// 管理后台 - 商户支付路由（方式+账号）
// ============================================================
export interface MerchantPaymentRoute {
  id?: number
  merchantId: string
  paymentMethodId: number
  paymentAccountId: number
  enabled: boolean
  priority: number
  /** 终端可见：PC / H5 / APP */
  clientScopes?: string[]
  paymentMethod?: PaymentMethod
  paymentAccount?: PaymentAccount
}

// ============================================================
// 管理后台 - 风控规则
// ============================================================
export type RiskRuleType = 'AMOUNT_SINGLE' | 'AMOUNT_DAILY' | 'IP_LIMIT' | 'MOBILE_LIMIT' | 'CUSTOM'
export type RiskRuleOwnerType = 'PLATFORM' | 'MERCHANT'
export type RiskRuleScopeType = 'ALL_MERCHANTS' | 'SELECTED_MERCHANTS' | 'OWNER_MERCHANT_ONLY'
export type RiskRuleAction = 'REJECT' | 'REVIEW' | 'WARN'
export type RiskDecision = 'REJECTED' | 'REVIEW_REQUIRED' | 'WARN_ONLY'

export interface RiskRule {
  id: number | string
  ruleId?: string
  ruleCode: string
  ruleName: string
  ruleType: RiskRuleType
  riskExpr?: string | null
  threshold?: number
  thresholdFen?: number
  unit: string
  action: RiskRuleAction
  enabled: boolean
  priority: number
  ownerType: RiskRuleOwnerType
  ownerMerchantId?: string | null
  ownerMerchantName?: string | null
  scopeType: RiskRuleScopeType
  scopeMerchantCount?: number
  description?: string
  createdAt: string
  updatedAt: string
}

export interface RiskRuleQuery {
  page?: number
  pageSize?: number
  merchantId?: string
  ownerType?: RiskRuleOwnerType
  scopeType?: RiskRuleScopeType
  ruleType?: RiskRuleType
  enabled?: boolean
  keyword?: string
}

export interface RiskRuleListResponse {
  list: RiskRule[]
  total: number
  page: number
  pageSize: number
}

export interface RiskRuleUpsertRequest {
  ruleCode: string
  ruleName: string
  ruleType: RiskRuleType
  riskExpr?: string | null
  thresholdFen?: number
  unit: string
  action: RiskRuleAction
  enabled: boolean
  priority: number
  ownerType?: RiskRuleOwnerType
  ownerMerchantId?: string
  scopeType?: RiskRuleScopeType
  scopeMerchantIds?: string[]
  description?: string
}

export interface RiskRuleStatusRequest {
  enabled: boolean
}

export interface RiskRuleScopeMerchant {
  merchantId: string
  merchantName?: string
  enabled: boolean
}

export interface RiskRuleScopeResponse {
  ruleId: string | number
  scopeType: RiskRuleScopeType
  merchants: RiskRuleScopeMerchant[]
}

export interface RiskHitRecord {
  id: string | number
  traceId?: string
  merchantId: string
  merchantName?: string
  orderId?: string | null
  merchantOrderNo?: string
  ruleId: string | number
  ruleCode: string
  ruleName: string
  ownerType: RiskRuleOwnerType
  scopeType: RiskRuleScopeType
  action: RiskRuleAction
  decision: RiskDecision
  hitReason?: string
  requestSummary?: string
  createdAt: string
}

export interface RiskHitRecordQuery {
  page?: number
  pageSize?: number
  merchantId?: string
  ruleId?: string | number
  ownerType?: RiskRuleOwnerType
  decision?: RiskDecision
  startTime?: string
  endTime?: string
}

export interface RiskHitRecordListResponse {
  list: RiskHitRecord[]
  total: number
  page: number
  pageSize: number
}

export interface RiskRuleAuditLog {
  id: string | number
  ruleId: string | number
  operatorId?: string
  operatorName?: string
  operatorType: 'ADMIN' | 'MERCHANT' | 'SYSTEM'
  merchantId?: string
  operationType: 'CREATE' | 'UPDATE' | 'ENABLE' | 'DISABLE' | 'SCOPE_CHANGE' | 'DELETE'
  beforeSummary?: string
  afterSummary?: string
  clientIp?: string
  createdAt: string
}

export interface RiskRuleAuditQuery {
  page?: number
  pageSize?: number
  ruleId?: string | number
  operatorType?: 'ADMIN' | 'MERCHANT' | 'SYSTEM'
  merchantId?: string
  operationType?: RiskRuleAuditLog['operationType']
  startTime?: string
  endTime?: string
}

export interface RiskRuleAuditListResponse {
  list: RiskRuleAuditLog[]
  total: number
  page: number
  pageSize: number
}

// ============================================================
// 通用 API 响应
// ============================================================
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

// ============================================================
// 分页响应
// ============================================================
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

// ============================================================
// 管理后台 - 操作审计日志
// ============================================================
export interface AuditLogItem {
  id: number
  username: string
  action: string
  resourcePath: string
  detail: string
  clientIp: string
  createdAt: string
  merchantId?: string
  classification?: string
}

export interface DataIsolationCheckItem {
  checkId: string
  targetType: string
  targetName: string
  classification: string
  riskLevel: string
  remediationStatus: string
  merchantId?: string
  lastScannedAt?: string
  note?: string
}

/** 订单状态统计（/admin/orders/stats） */
export interface OrderStats {
  total: number
  statusCount: Array<{ status: string; cnt: number }>
}

/** 全局搜索订单命中 */
export interface AdminSearchOrderHit {
  type: string
  orderId: string
  merchantId: string
  merchantOrderNo: string
  status: string
  amount: number
  createdAt: string
}

// ============================================================
// 管理员登录
// ============================================================
export interface AdminLoginDTO {
  username: string
  password: string
  captchaId?: string
  captchaAnswer?: string
}

export interface AdminUiPreferences {
  themeKey: 'mint' | 'ocean' | 'violet' | 'dark'
  tableDensity: 'standard' | 'compact'
  sidebarCollapsed: boolean
}

export interface AdminLoginResponse {
  /** 登录接口返回；profile 接口可为 null，此时沿用本地已存 Token */
  token?: string | null
  adminId?: string | number
  username: string
  nickname?: string
  role: string
  platformAdmin?: boolean
  scopeMode?: 'PLATFORM' | 'MERCHANT' | 'NONE'
  authorizedMerchantIds?: string[]
  menus?: SysMenu[]
  /** 扁平按钮权限码列表 */
  permissions?: string[]
  uiPreferences?: AdminUiPreferences
}

/** 商户数据授权范围（前端展示与筛选辅助，实际隔离以后端为准） */
export interface MerchantScopeInfo {
  platformAdmin: boolean
  scopeMode?: string
  authorizedMerchantIds: string[]
}

// ============================================================
// 管理后台 - 系统角色
// ============================================================
export interface SysRole {
  id: number
  roleCode: string
  roleName: string
  description?: string
  status: 'ACTIVE' | 'DISABLED'
  createdAt: string
  updatedAt: string
}

// ============================================================
// 管理后台 - 系统菜单
// ============================================================
export interface SysMenu {
  id: number
  parentId?: number
  menuCode: string
  menuName: string
  menuType: 'MENU' | 'BUTTON'
  permCode?: string
  apiPattern?: string
  path?: string
  icon?: string
  sortOrder: number
  visible: boolean
  status: 'ACTIVE' | 'DISABLED'
  children?: SysMenu[]
  createdAt: string
  updatedAt: string
}
