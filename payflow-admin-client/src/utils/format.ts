/** 格式化日期时间为 YYYY-MM-DD HH:mm:ss */
export function formatDateTime(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

/** 格式化日期 YYYY-MM-DD */
export function formatDate(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.slice(0, 10)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** 分转元，带千分位 */
export function formatMoneyFen(fen?: number | null): string {
  if (fen == null || Number.isNaN(Number(fen))) return '—'
  return (Number(fen) / 100).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/** 小数费率转百分比，如 0.006 → 0.60% */
export function formatRatePercent(rate?: number | null, digits = 2): string {
  if (rate == null || Number.isNaN(Number(rate))) return '—'
  return `${(Number(rate) * 100).toFixed(digits)}%`
}

export interface ChannelOption {
  code: string
  rate?: number
  available?: boolean
}

/** 解析可选渠道 JSON 字符串或数组 */
export function parseChannelOptions(raw: unknown): ChannelOption[] {
  if (Array.isArray(raw)) return raw as ChannelOption[]
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw) as unknown
      return Array.isArray(parsed) ? (parsed as ChannelOption[]) : []
    } catch {
      return []
    }
  }
  return []
}

export const PAY_CHANNEL_LABEL: Record<string, string> = {
  WECHAT_PAY: '微信支付',
  WECHAT: '微信支付',
  ALIPAY: '支付宝',
  UNION_PAY: '银联',
  UNIONPAY: '银联',
  wxpay: '微信支付',
  alipay: '支付宝',
  unionpay: '银联',
}

export const PAY_CHANNEL_TAG: Record<string, string> = {
  WECHAT_PAY: 'success',
  WECHAT: 'success',
  ALIPAY: 'primary',
  UNION_PAY: 'warning',
  UNIONPAY: 'warning',
}

export const ORDER_STATUS_LABEL: Record<string, string> = {
  PAID: '已支付',
  PAYING: '支付中',
  CREATED: '待支付',
  EXPIRED: '已过期',
  FAILED: '失败',
  CLOSED: '已关闭',
}

export const ORDER_STATUS_TAG: Record<string, string> = {
  PAID: 'success',
  PAYING: 'warning',
  CREATED: 'info',
  EXPIRED: 'info',
  FAILED: 'danger',
  CLOSED: 'info',
}

export const PAYMENT_STATUS_LABEL: Record<string, string> = {
  PROCESSING: '处理中',
  SUCCESS: '支付成功',
  FAILED: '失败',
  REFUNDED: '已全额退款',
  PARTIAL_REFUND: '部分退款',
}

export const PAYMENT_STATUS_TAG: Record<string, string> = {
  PROCESSING: 'warning',
  SUCCESS: 'success',
  FAILED: 'danger',
  REFUNDED: 'info',
  PARTIAL_REFUND: 'warning',
}

export const REFUND_STATUS_LABEL: Record<string, string> = {
  PENDING: '申请中',
  APPROVED: '审批通过',
  COMPLETED: '已退款',
  REJECTED: '已拒绝',
}

export const REFUND_STATUS_TAG: Record<string, string> = {
  PENDING: 'warning',
  APPROVED: 'primary',
  COMPLETED: 'success',
  REJECTED: 'danger',
}

/** 商户回调汇总状态（平台 → 商户） */
export const MERCHANT_NOTIFY_SUMMARY_LABEL: Record<string, string> = {
  SUCCESS: '成功',
  FAILED: '失败',
  IN_PROGRESS: '处理中',
  NOT_CONFIGURED: '未配置',
  PENDING: '待投递',
}

export const MERCHANT_NOTIFY_SUMMARY_TAG: Record<string, string> = {
  SUCCESS: 'success',
  FAILED: 'danger',
  IN_PROGRESS: 'warning',
  NOT_CONFIGURED: 'info',
  PENDING: 'info',
}

export const MERCHANT_NOTIFY_TYPE_LABEL: Record<string, string> = {
  PAYMENT: '支付通知',
  REFUND: '退款通知',
}

export const MERCHANT_NOTIFY_TYPE_TAG: Record<string, string> = {
  PAYMENT: 'primary',
  REFUND: 'warning',
}

export const MERCHANT_STATUS_LABEL: Record<string, string> = {
  ACTIVE: '正常',
  SUSPENDED: '停用',
  CLOSED: '关闭',
}

export const MERCHANT_STATUS_TAG: Record<string, string> = {
  ACTIVE: 'success',
  SUSPENDED: 'warning',
  CLOSED: 'danger',
}

export const ENABLE_STATUS_LABEL: Record<string, string> = {
  ACTIVE: '启用',
  ENABLED: '启用',
  DISABLED: '停用',
  INACTIVE: '停用',
}

export const ENABLE_STATUS_TAG: Record<string, string> = {
  ACTIVE: 'success',
  ENABLED: 'success',
  DISABLED: 'danger',
  INACTIVE: 'info',
}

export const AUDIT_ACTION_LABEL: Record<string, string> = {
  LOGIN: '登录',
  POST: '创建',
  PUT: '更新',
  DELETE: '删除',
  PATCH: '修改',
  GET: '查询',
}

export const AUDIT_ACTION_TAG: Record<string, string> = {
  LOGIN: 'success',
  POST: 'primary',
  PUT: 'warning',
  DELETE: 'danger',
  PATCH: 'info',
  GET: 'info',
}

export const RECON_STATUS_LABEL: Record<string, string> = {
  INIT: '待处理',
  DOWNLOADING: '下载中',
  PARSING: '解析中',
  COMPARING: '比对中',
  SUCCESS: '成功',
  FAIL: '失败',
  FAILED: '失败',
  PENDING: '待处理',
  MATCHED: '已匹配',
  UNMATCHED: '未匹配',
  ABNORMAL: '对账异常',
  NO_RECON: '未对账',
}

export const RECON_STATUS_TAG: Record<string, string> = {
  INIT: 'info',
  DOWNLOADING: 'warning',
  PARSING: 'warning',
  COMPARING: 'warning',
  SUCCESS: 'success',
  FAIL: 'danger',
  FAILED: 'danger',
  PENDING: 'info',
  MATCHED: 'success',
  UNMATCHED: 'warning',
  ABNORMAL: 'danger',
  NO_RECON: 'info',
}

export const RECON_DIFF_LABEL: Record<string, string> = {
  CHANNEL_ONLY: '渠道单边',
  LOCAL_ONLY: '本地单边',
  AMOUNT_MISMATCH: '金额不符',
  STATUS_MISMATCH: '状态不符',
}

export const RECON_HANDLE_LABEL: Record<string, string> = {
  PENDING: '待处理',
  PROCESSED: '已处理',
  RESOLVED: '已处理',
  IGNORED: '已忽略',
}

export const CHURN_LEVEL_LABEL: Record<string, string> = {
  red: '红色',
  orange: '橙色',
  yellow: '黄色',
}

export const CHURN_LEVEL_TAG: Record<string, string> = {
  red: 'danger',
  orange: 'warning',
  yellow: 'warning',
}

export const CHURN_STATUS_LABEL: Record<string, string> = {
  pending: '待处理',
  in_progress: '处理中',
  resolved: '已解决',
  false_alarm: '误报',
}

export const CHURN_STATUS_TAG: Record<string, string> = {
  pending: 'danger',
  in_progress: 'warning',
  resolved: 'success',
  false_alarm: 'info',
}

export const ONBOARDING_STATUS_LABEL: Record<string, string> = {
  SUBMITTED: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
}

export const ONBOARDING_STATUS_TAG: Record<string, string> = {
  SUBMITTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
}

export const SECURITY_OUTCOME_LABEL: Record<string, string> = {
  ALLOW: '允许',
  DENY: '拒绝',
  BLOCK: '拦截',
}

export const SECURITY_OUTCOME_TAG: Record<string, string> = {
  ALLOW: 'success',
  DENY: 'danger',
  BLOCK: 'danger',
}

export const ROUTING_REASON_LABEL: Record<string, string> = {
  lowest_cost: '最低成本',
  fallback: '降级兜底',
  manual: '人工指定',
}

export const FEE_TRIGGER_LABEL: Record<string, string> = {
  monthly_upgrade: '月度升档',
  manual_adjust: '人工调整',
  merchant_group_change: '商户分组变更',
}

export const CLIENT_SCOPE_LABEL: Record<string, string> = {
  PC: 'PC 收银台',
  H5: 'H5 收银台',
  APP: 'App 收银台',
  MINI: '小程序',
  NATIVE: '扫码',
}

export function channelLabel(code?: string | null): string {
  if (!code) return '—'
  return PAY_CHANNEL_LABEL[code] ?? code
}

export function channelTagType(code?: string | null): string {
  if (!code) return 'info'
  return PAY_CHANNEL_TAG[code] ?? 'info'
}

export function labelOf(map: Record<string, string>, key?: string | null): string {
  if (!key) return '—'
  return map[key] ?? key
}

export function tagTypeOf(map: Record<string, string>, key?: string | null, fallback = 'info'): string {
  if (!key) return fallback
  return map[key] ?? fallback
}

/** 脱敏显示 appId / 密钥类字段 */
export function maskSecret(value?: string | null): string {
  if (!value) return '—'
  if (value.length <= 8) return value
  return `${value.slice(0, 4)}****${value.slice(-4)}`
}
