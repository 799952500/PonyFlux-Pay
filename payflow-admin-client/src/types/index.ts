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
}
