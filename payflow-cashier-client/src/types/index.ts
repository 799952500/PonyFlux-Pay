// ============================================================
// 公共类型定义
// ============================================================

// 订单状态枚举
export type OrderStatus = 'CREATED' | 'PAYING' | 'PAID' | 'EXPIRED' | 'FAILED' | 'REFUNDED'

// 支付渠道
export type PayChannel = 'ALIPAY' | 'WECHAT_PAY' | 'UNION_PAY' | 'CASH' | 'CARD'

// 支付动作（后端返回的操作指引）
export type PaymentAction = 'INVOKE' | 'QR_CODE' | 'REDIRECT' | 'FORM'

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
// 支付方式
// ============================================================
export interface PaymentMethodDiscount {
  name: string
  amount: number          // 单位：分
}

export interface PaymentMethod {
  methodCode: string      // 如 'ALIPAY_APP' 'WECHAT_NATIVE'
  methodName: string
  icon: string            // SVG 或 URL
  channel: PayChannel
  discount?: PaymentMethodDiscount
}

// ============================================================
// 收银台
// ============================================================
export interface CashierInfo {
  orderId: string
  merchantName: string
  subject: string
  body?: string
  amount: number          // 单位：分
  currency: string
  expireTime: string
  status: string
  paymentMethods: PaymentMethod[]
  successUrl?: string
  failUrl?: string
  returnUrl?: string
}

// ============================================================
// 支付结果
// ============================================================
export interface PaymentResult {
  paymentId: string
  orderId: string
  status: OrderStatus
  action: PaymentAction
  qrCodeUrl?: string
  qrCodeImage?: string
  redirectUrl?: string
  formHtml?: string
  invokeParams?: Record<string, string>
}

// ============================================================
// 支付请求参数
// ============================================================
export interface CreatePaymentDTO {
  orderId: string
  payChannel: PayChannel
  payMethod: string
  deviceType: DeviceType
  clientIp?: string
}

// ============================================================
// 登录相关
// ============================================================
export interface LoginDTO {
  merchantId: string
  password: string
}

export interface LoginResponse {
  token: string
  merchantInfo: {
    merchantId: string
    merchantName: string
    merchantType: 'INDIVIDUAL' | 'ENTERPRISE'
    status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED'
  }
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
