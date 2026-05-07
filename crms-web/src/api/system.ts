import { get, post, put, del } from './http'
import type { PageResult } from './customer'

// 注意：所有 ID 字段后端使用 Snowflake 64bit Long，已序列化为字符串。
// 前端必须以 string 形态传递，禁止 Number() 强转。

// ============== 系统参数 ==============

export interface SystemParamVO {
  id: string
  paramKey: string
  paramValue: string
  description?: string
  updatedBy?: string
  updatedAt?: string
}

export interface UpdateParamItem {
  paramKey: string
  paramValue: string
}

export const systemParamApi = {
  list: () => get<SystemParamVO[]>('/system-params'),
  batchUpdate: (items: UpdateParamItem[]) => put<void>('/system-params/batch', items)
}

// ============== 操作日志 ==============

export interface OperationLogVO {
  id: string
  operatorId?: string
  operatorName?: string
  operatorIp?: string
  module: string
  action: string
  opType: string
  bizType?: string
  bizId?: string
  uri?: string
  method?: string
  paramsJson?: string
  result?: string
  errorMessage?: string
  durationMs?: number
  createdAt: string
}

export interface OperationLogQuery {
  keyword?: string
  operatorId?: string
  module?: string
  opType?: string
  bizType?: string
  result?: string
  fromTime?: string
  toTime?: string
  page?: number
  size?: number
}

export const operationLogApi = {
  list: (q: OperationLogQuery) =>
    get<PageResult<OperationLogVO>>('/operation-logs', { params: q })
}

// ============== 回收站 ==============

export type RecycleBizType = 'CUSTOMER' | 'CONTRACT' | 'PAYMENT_RECORD'

export interface RecycleBinItemVO {
  bizType: string
  id: string
  code?: string
  name?: string
  updatedBy?: string
  updatedAt?: string
}

export interface RecycleBinQuery {
  bizType: RecycleBizType
  keyword?: string
  page?: number
  size?: number
}

export const recycleBinApi = {
  list: (q: RecycleBinQuery) =>
    get<PageResult<RecycleBinItemVO>>('/recycle-bin', { params: q }),
  restore: (bizType: RecycleBizType, id: string) =>
    post<void>(`/recycle-bin/${bizType}/${id}/restore`),
  hardDelete: (bizType: RecycleBizType, id: string, reason: string) =>
    del<void>(`/recycle-bin/${bizType}/${id}/hard`, { data: { reason } })
}
