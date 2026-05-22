/**
 * 管理员认证接口
 */
import request from './request'
import type { AdminLoginDTO, AdminLoginResponse } from '@/types'

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
