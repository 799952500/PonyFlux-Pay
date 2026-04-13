/**
 * Axios 请求封装
 * 收银台所有接口均为公开接口，无需 Bearer Token
 * - 统一错误处理（ElMessage）
 * - 统一响应结构解包（code=0 → data, code≠0 → reject）
 */
import axios, { type AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'

const request: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// -------------------------------------------------------------------
// 响应拦截器：统一解包 + 错误提示
// -------------------------------------------------------------------
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== undefined && res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(res)
    }
    return res.data
  },
  (error) => {
    const message =
      error.response?.data?.message ?? error.message ?? '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
