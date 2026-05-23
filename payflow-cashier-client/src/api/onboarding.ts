import request from './request'

export interface OnboardingSubmitPayload {
  merchantName: string
  contactName: string
  contactPhone: string
  contactEmail: string
  bizLicenseNo?: string
  websiteUrl?: string
  businessScope?: string
  remark?: string
}

export interface OnboardingSubmitResult {
  applicationNo: string
  queryUrl?: string
}

export interface OnboardingResultPayload {
  applicationNo: string
  contact: string
}

export interface OnboardingCredentialResult {
  applicationNo: string
  merchantId: string
  appSecret: string
  tempPassword: string
  adminUsername: string
  loginUrl: string
  remainingQueries?: number
}

export function submitOnboardingApplication(payload: OnboardingSubmitPayload): Promise<OnboardingSubmitResult> {
  return request.post('/cashier/onboarding/applications', payload)
}

export function queryOnboardingResult(payload: OnboardingResultPayload): Promise<OnboardingCredentialResult> {
  return request.post('/cashier/onboarding/result', payload)
}
