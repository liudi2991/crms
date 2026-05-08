import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import nprogress from 'nprogress'

nprogress.configure({ showSpinner: false })

export interface ApiResult<T = unknown> {
  code: string
  message: string
  data?: T
  traceId?: string
}

const TOKEN_KEY = 'crms_token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY)
}

const http: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30_000,
  withCredentials: true
})

http.interceptors.request.use((cfg) => {
  nprogress.start()
  const token = tokenStore.get()
  if (token) {
    cfg.headers = cfg.headers || {}
    cfg.headers.Authorization = `Bearer ${token}`
  }
  return cfg
})

/**
 * 后端把 Long 序列化为字符串以防 JS 大整数精度丢失，但 ElPagination 的 total prop
 * 严格要求 Number，否则会触发 "Invalid prop: type check failed for prop total"
 * + "[ElPagination] 你使用了一些已被废弃的用法" 警告。
 *
 * 这里统一识别分页响应（包含 items + total + page + size 四个字段）并把 total
 * 转回 number。业务页只要继续用 res.total 就行，不用每个 view 都包 Number()。
 */
function normalizePageTotal(data: unknown): unknown {
  if (
    data &&
    typeof data === 'object' &&
    'items' in data &&
    'total' in data &&
    'page' in data &&
    'size' in data
  ) {
    const d = data as { total: unknown }
    if (typeof d.total === 'string' && d.total !== '') {
      d.total = Number(d.total)
    }
  }
  return data
}

http.interceptors.response.use(
  (res) => {
    nprogress.done()
    const body = res.data as ApiResult
    if (!body || typeof body !== 'object') {
      return res
    }
    if (body.code === '0') {
      return normalizePageTotal(body.data) as never
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(body)
  },
  (err) => {
    nprogress.done()
    const status = err.response?.status
    const body = err.response?.data as ApiResult | undefined
    if (status === 401) {
      tokenStore.clear()
      ElMessageBox.alert('会话已过期，请重新登录', '提示', { type: 'warning' })
        .then(() => {
          window.location.href = '/#/login'
        })
        .catch(() => {})
    } else if (status === 403) {
      ElMessage.error(body?.message || '权限不足')
    } else {
      ElMessage.error(body?.message || err.message || '网络异常')
    }
    return Promise.reject(err)
  }
)

export function get<T>(url: string, config?: AxiosRequestConfig) {
  return http.get<unknown, T>(url, config)
}
export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  return http.post<unknown, T>(url, data, config)
}
export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  return http.put<unknown, T>(url, data, config)
}
export function del<T>(url: string, config?: AxiosRequestConfig) {
  return http.delete<unknown, T>(url, config)
}

export default http
