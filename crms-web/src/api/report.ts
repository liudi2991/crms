import http, { get, post } from './http'

export interface DashboardVO {
  contractAmount: number
  paidAmount: number
  unpaidAmount: number
  overdueAmount: number
  paidThisMonth: number
  contractCount: number
  customerCount: number
  contractDueIn30Days: number
}

export interface TrendPoint {
  month: string
  contractAmount: number
  paidAmount: number
}

export interface AgingBucket {
  bucket: string
  amount: number
  count: number
}

export interface TopCustomerVO {
  customerId: string
  customerName: string
  amount: number
}

export interface TodoItemVO {
  type: 'CONTRACT_DUE' | 'PAYMENT_DUE' | 'PAYMENT_OVERDUE'
  title: string
  linkUrl: string
  date: string
  amount: number
  overdueDays: number
  bizId: string
}

export const reportApi = {
  dashboard: () => get<DashboardVO>('/reports/dashboard'),
  trend: (months = 12) => get<TrendPoint[]>('/reports/trend', { params: { months } }),
  aging: () => get<AgingBucket[]>('/reports/aging'),
  topCustomers: (n = 10, metric: 'PAID' | 'UNPAID' | 'CONTRACT' = 'PAID') =>
    get<TopCustomerVO[]>('/reports/top-customers', { params: { n, metric } }),
  myTodos: (contractAdvance = 30, paymentAdvance = 7) =>
    get<TodoItemVO[]>('/reports/my-todos', { params: { contractAdvance, paymentAdvance } }),
  exportUrl: (reportName: 'trend' | 'aging' | 'top-customers' | 'todos') =>
    `${http.defaults.baseURL}/reports/export/${reportName}`,
  evictCache: () => post<void>('/reports/cache/evict')
}
