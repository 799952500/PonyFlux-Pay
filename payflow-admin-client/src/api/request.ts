/**
 * Axios 请求封装
 * - baseURL: /api/v1（通过 vite proxy 转发到后端）
 * - JWT Bearer Token 自动注入
 * - 401 自动跳转登录页
 */
import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// -------------------------------------------------------------------
// 请求拦截器：注入 JWT Token
// -------------------------------------------------------------------
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('adminToken')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// -------------------------------------------------------------------
// 响应拦截器：统一解包 + 401 处理
// -------------------------------------------------------------------
request.interceptors.response.use(
  (response) => {
    const ct = String(response.headers['content-type'] ?? '')
    if (ct.includes('text/csv') || ct.includes('text/plain')) {
      return response.data
    }
    const res = response.data
    // 仅当为统一 Api 信封 { code, message, data? } 时解包，避免把业务实体误判为信封
    if (
      res !== null
      && typeof res === 'object'
      && !Array.isArray(res)
      && Object.prototype.hasOwnProperty.call(res, 'code')
    ) {
      if (res.code !== undefined && res.code !== 0) {
        ElMessage.error(res.message || '请求失败')
        return Promise.reject(res)
      }
      if (Object.prototype.hasOwnProperty.call(res, 'data')) {
        return res.data
      }
      return res
    }
    return res
  },
  (error) => {
    // 401：Token 过期或无效，清除并跳转登录
    if (error.response?.status === 401) {
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminUser')
      router.push('/login')
      return Promise.reject(error)
    }
    const message = error.response?.data?.message ?? error.message ?? '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
