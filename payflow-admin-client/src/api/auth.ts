/**
 * 管理员认证接口
 */
import request from './request'
import type { AdminLoginDTO, AdminLoginResponse, AdminUiPreferences } from '@/types'

export interface CaptchaRequiredResult {
  required: boolean
  failureCount: number
}

/** 查询当前用户名是否需要验证码 */
export const getCaptchaRequired = (username: string): Promise<CaptchaRequiredResult> =>
  request.get('/admin/auth/captcha-required', { params: { username } })

/** 管理员登录 */
export const adminLogin = (data: AdminLoginDTO): Promise<AdminLoginResponse> =>
  request.post('/admin/auth/login', data)

/** 获取当前管理员信息 */
export const getAdminProfile = (): Promise<AdminLoginResponse> =>
  request.get('/admin/auth/profile')

/** 更新当前用户 UI 外观偏好（持久化到数据库） */
export const updateUiPreferences = (
  body: Partial<AdminUiPreferences>,
): Promise<AdminUiPreferences> =>
  request.put('/admin/auth/ui-preferences', body)
