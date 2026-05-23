import request from './request'

export interface OnboardingApplicationRow {
  id: number
  applicationNo: string
  merchantName: string
  status: string
  contactPhone?: string
  contactEmail?: string
  contactName?: string
  createdAt?: string
}

export interface OnboardingPageResult {
  total: number
  page: number
  pageSize: number
  list: OnboardingApplicationRow[]
}

export interface OnboardingDetail {
  id: number
  applicationNo: string
  merchantName: string
  status: string
  applicationSource?: string
  bizLicenseNo?: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  allocatedMerchantId?: string
  payloadJson?: string
  rejectReason?: string
  resultQueryCount?: number
  secretViewedAt?: string
  approvedAt?: string
  rejectedAt?: string
  createdAt?: string
  updatedAt?: string
}

export function listOnboardingApplications(params: {
  page?: number
  pageSize?: number
  status?: string
  keyword?: string
}): Promise<OnboardingPageResult> {
  return request.get('/admin/onboarding/applications', { params })
}

export function getOnboardingDetail(id: number): Promise<OnboardingDetail> {
  return request.get(`/admin/onboarding/applications/${id}`)
}

export function approveOnboarding(id: number): Promise<{ message: string }> {
  return request.post(`/admin/onboarding/applications/${id}/approve`)
}

export function rejectOnboarding(id: number, rejectReason: string): Promise<{ message: string }> {
  return request.post(`/admin/onboarding/applications/${id}/reject`, { rejectReason })
}
