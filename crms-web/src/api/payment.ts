import http, { get, post, put, del } from './http'
import type { PageResult } from './customer'

// 注意：所有 ID 字段后端使用 Snowflake 64bit Long，已序列化为字符串。
// 前端必须以 string 形态传递，禁止 Number() 强转。

export interface PaymentPlanVO {
  id: string
  contractId: string
  contractCode?: string
  contractName?: string
  periodNo: number
  planDate: string
  planAmount: number
  settledAmount: number
  unsettledAmount: number
  status: 'PENDING' | 'PARTIAL' | 'SETTLED'
  overdue: boolean
  overdueDays: number
  remindDays?: number
  version: number
}

export interface PaymentRecordVO {
  id: string
  contractId: string
  contractCode?: string
  contractName?: string
  arrivalDate: string
  amount: number
  payer?: string
  voucherNo?: string
  status: 'NORMAL' | 'REVERSED' | 'RED'
  redRefId?: string
  unallocatedAmount: number
  remark?: string
  voucherFileId?: string
  version: number
}

export interface PlanQuery {
  contractId?: string
  status?: string
  overdueOnly?: boolean
  fromDate?: string
  toDate?: string
  page?: number
  size?: number
}

export interface RecordQuery {
  contractId?: string
  keyword?: string
  status?: string
  fromDate?: string
  toDate?: string
  page?: number
  size?: number
}

export interface CreatePlanDTO {
  contractId: string
  periodNo: number
  planDate: string
  planAmount: number
  remindDays?: number
}

export interface UpdatePlanDTO {
  planDate: string
  planAmount: number
  remindDays?: number
  version: number
}

export interface GeneratePlansDTO {
  contractId: string
  firstPlanDate: string
  periods: number
  frequency: 'MONTHLY' | 'QUARTERLY' | 'ONCE'
  overwrite?: boolean
}

export interface CreateRecordDTO {
  contractId: string
  arrivalDate: string
  amount: number
  payer?: string
  voucherNo?: string
  remark?: string
  voucherFileId?: string
  targetPlanIds?: string[]
}

export interface ManualSettleDTO {
  recordId: string
  planIds: string[]
}

export interface RedReverseDTO {
  redAmount: number
  reason?: string
}

export interface AgingBucketVO {
  bucket: 'UNDUE' | '0-30' | '31-60' | '61-90' | '90+'
  amount: number
  count: number
}

export interface AgingDrillVO {
  planId: string
  contractId: string
  contractCode: string
  contractName: string
  periodNo: number
  planDate: string
  unsettledAmount: number
  overdueDays: number
}

export interface ImportResultVO {
  total: number
  success: number
  failed: number
  errors: { row: number; message: string }[]
}

export const paymentPlanApi = {
  list: (q: PlanQuery) => get<PageResult<PaymentPlanVO>>('/payment-plans', { params: q }),
  byContract: (contractId: string) => get<PaymentPlanVO[]>(`/payment-plans/by-contract/${contractId}`),
  create: (dto: CreatePlanDTO) => post<string>('/payment-plans', dto),
  generate: (dto: GeneratePlansDTO) => post<string[]>('/payment-plans/generate', dto),
  update: (id: string, dto: UpdatePlanDTO) => put<void>(`/payment-plans/${id}`, dto),
  remove: (id: string) => del<void>(`/payment-plans/${id}`)
}

export const paymentRecordApi = {
  list: (q: RecordQuery) => get<PageResult<PaymentRecordVO>>('/payment-records', { params: q }),
  detail: (id: string) => get<PaymentRecordVO>(`/payment-records/${id}`),
  byContract: (contractId: string) =>
    get<PaymentRecordVO[]>(`/payment-records/by-contract/${contractId}`),
  create: (dto: CreateRecordDTO) => post<string>('/payment-records', dto),
  manualSettle: (dto: ManualSettleDTO) => post<void>('/payment-records/manual-settle', dto),
  redReverse: (id: string, dto: RedReverseDTO) =>
    post<string>(`/payment-records/${id}/red-reverse`, dto),
  remove: (id: string) => del<void>(`/payment-records/${id}`),
  importExcel: (file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post<unknown, ImportResultVO>('/payment-records/import', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export const agingApi = {
  buckets: (today?: string) => get<AgingBucketVO[]>('/aging', { params: { today } }),
  drill: (bucket: string, today?: string, page = 1, size = 50) =>
    get<AgingDrillVO[]>('/aging/drill', { params: { bucket, today, page, size } })
}

export const PaymentPlanStatus: Record<
  string,
  { label: string; type: 'info' | 'warning' | 'success' | 'danger' }
> = {
  PENDING: { label: '待回款', type: 'info' },
  PARTIAL: { label: '部分核销', type: 'warning' },
  SETTLED: { label: '已结清', type: 'success' }
}

export const PaymentRecordStatus: Record<
  string,
  { label: string; type: 'info' | 'success' | 'warning' | 'danger' }
> = {
  NORMAL: { label: '正常', type: 'success' },
  REVERSED: { label: '已红冲', type: 'warning' },
  RED: { label: '红冲', type: 'danger' }
}

export const AgingBucketLabel: Record<string, string> = {
  UNDUE: '未到期',
  '0-30': '0-30 天',
  '31-60': '31-60 天',
  '61-90': '61-90 天',
  '90+': '90 天以上'
}
