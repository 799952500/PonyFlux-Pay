import request from './request'

export interface SecurityAuditItem {
  id: number
  merchantId: string
  targetMerchantId?: string
  authMode: string
  httpMethod: string
  requestPath: string
  resourceType?: string
  resourceId?: string
  clientIp?: string
  userAgent?: string
  outcome: string
  reasonCode: string
  reasonDetail?: string
  createdAt: string
}

export interface SecurityAuditPageResult {
  list: SecurityAuditItem[]
  total: number
  page: number
  pageSize: number
}

export const getSecurityAuditList = (params: {
  page: number
  pageSize: number
  merchantId?: string
  outcome?: string
  reasonCode?: string
  requestPath?: string
  startDate?: string
  endDate?: string
}): Promise<SecurityAuditPageResult> =>
  request.get('/admin/security/audit', { params })
