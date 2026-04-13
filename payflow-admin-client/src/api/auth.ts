/**
 * 管理员认证接口
 */
import request from './request'
import type { AdminLoginDTO, AdminLoginResponse } from '@/types'

/** 管理员登录 */
export const adminLogin = (data: AdminLoginDTO): Promise<AdminLoginResponse> =>
  request.post('/admin/auth/login', data)

/** 获取当前管理员信息 */
export const getAdminProfile = (): Promise<AdminLoginResponse> =>
  request.get('/admin/auth/profile')
