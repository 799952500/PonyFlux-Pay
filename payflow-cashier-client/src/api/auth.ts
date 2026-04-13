/**
 * 登录认证相关接口
 */
import request from './request'
import type { LoginDTO, LoginResponse } from '@/types'

/**
 * 商户登录
 * POST /auth/login
 */
export const merchantLogin = (data: LoginDTO): Promise<LoginResponse> =>
  request.post('/auth/login', data)
