// ============================================================
// 公共类型定义
// ============================================================

// 订单状态枚举
export type OrderStatus = 'CREATED' | 'PAYING' | 'PAID' | 'EXPIRED' | 'FAILED' | 'REFUNDED'

// 支付渠道
export type PayChannel = 'ALIPAY' | 'WECHAT_PAY' | 'UNION_PAY' | 'CASH' | 'CARD'

// 支付动作（后端返回的操作指引）
export type PaymentAction =
  | 'INVOKE'
  | 'QR_CODE'
  | 'REDIRECT'
  | 'FORM'
  | 'COMPLETE'
  | 'MICROPAY_POLL'
  | 'BARCODE_POLL'

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
  /** 订单创建时间（ISO），用于倒计时进度条总时长 */
  createdAt?: string
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
  status: OrderStatus | 'PROCESSING' | 'SUCCESS'
  action: PaymentAction
  qrCodeUrl?: string
  qrCodeImage?: string
  redirectUrl?: string
  formHtml?: string
  invokeParams?: Record<string, string>
  /** 渠道已同步确认成功（如付款码即时成功） */
  paidImmediately?: boolean
  channelTransactionId?: string
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
// 电子收据
// ============================================================
export interface ReceiptInfo {
  orderId: string          // 平台订单号
  merchantName: string     // 商户名称
  subject: string          // 商品名称
  amount: number           // 金额（分）
  currency: string         // 币种
  amountCn: string         // 金额大写中文
  payChannel: string       // 支付渠道
  payTime: string          // 支付时间
  transactionNo: string    // 交易流水号
  status: string           // 状态
  receiptNo?: string       // 收据编号
  generatedAt?: string      // 收据生成时间
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
