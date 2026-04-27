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
  channelDistribution: ChannelDistItem[]
  trendData: TrendDataItem[]
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
// 管理后台 - 渠道路由（ChannelRoute）
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
  paymentMethod?: PaymentMethod
  paymentAccount?: PaymentAccount
}

// ============================================================
// 管理后台 - 风控规则
// ============================================================
export interface RiskRule {
  ruleId: string
  ruleName: string
  ruleType: 'AMOUNT_SINGLE' | 'AMOUNT_DAILY' | 'IP_LIMIT' | 'MOBILE_LIMIT' | 'CUSTOM'
  threshold: number
  unit: string
  enabled: boolean
  description: string
  createdAt: string
  updatedAt: string
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
// 管理员登录
// ============================================================
export interface AdminLoginDTO {
  username: string
  password: string
}

export interface AdminLoginResponse {
  token: string
  adminId: string
  username: string
  role: string
  menus?: SysMenu[]
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
  path?: string
  icon?: string
  sortOrder: number
  visible: boolean
  status: 'ACTIVE' | 'DISABLED'
  children?: SysMenu[]
  createdAt: string
  updatedAt: string
}
