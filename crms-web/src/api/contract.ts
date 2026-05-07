import { get, post, put, del } from './http'
import http from './http'
import type { PageResult } from './customer'

// 注意：所有 ID（id / customerId / ownerId / deptId 等）后端使用 Snowflake 64bit Long，
// 已在 Jackson 中序列化为字符串。前端必须以 string 形态传递，禁止再用 Number() 强转，
// 否则会发生精度丢失（末几位被改写）导致命中不到记录。

export interface ContractVO {
  id: string
  code: string
  name: string
  type: string
  customerId: string
  customerName?: string
  amount: number
  signedAt: string
  performStartAt: string
  performEndAt: string
  remindDays?: number
  ownerId: string
  ownerName?: string
  deptId: string
  status: string
  remark?: string
  createdAt?: string
  updatedAt?: string
  version: number
}

export interface ContractQuery {
  keyword?: string
  type?: string
  status?: string
  customerId?: string
  ownerId?: string
  signedFrom?: string
  signedTo?: string
  page?: number
  size?: number
}

export interface CreateContractDTO {
  name: string
  type: 'SALES' | 'PROCUREMENT' | 'SERVICE' | 'OTHER'
  customerId: string
  amount: number | string
  signedAt: string
  performStartAt: string
  performEndAt: string
  remindDays?: number
  ownerId?: string
  remark?: string
}

export interface UpdateContractDTO extends CreateContractDTO {
  version: number
}

export interface ContractAttachmentVO {
  id: string
  contractId: string
  fileObjectId: string
  fileName: string
  // 后端 JacksonConfig 全局把 Long 序列化为字符串，fileSize 也按 string 接收；
  // 数值消费方需 Number(bytes) 转一下再用，避免直接调 .toFixed()
  fileSize: string
  uploadedBy: string
  uploadedAt: string
  previewUrl: string
}

export interface ContractNoteVO {
  id: string
  contractId: string
  authorId: string
  content: string
  createdAt: string
}

export interface ContractChangeLogVO {
  id: string
  bizType: string
  bizId: string
  field: string
  oldValue?: string
  newValue?: string
  reason?: string
  operatorId: string
  operatedAt: string
}

export const contractApi = {
  list: (q: ContractQuery) => get<PageResult<ContractVO>>('/contracts', { params: q }),
  detail: (id: string) => get<ContractVO>(`/contracts/${id}`),
  create: (dto: CreateContractDTO) => post<string>('/contracts', dto),
  update: (id: string, dto: UpdateContractDTO) => put<void>(`/contracts/${id}`, dto),
  remove: (id: string) => del<void>(`/contracts/${id}`),
  hardDelete: (id: string, reason?: string) =>
    del<void>(`/contracts/${id}/hard`, { params: { reason } }),
  transition: (id: string, to: string, reason?: string) =>
    post<void>(`/contracts/${id}/transition`, { to, reason }),
  terminate: (id: string, reason?: string) =>
    post<void>(`/contracts/${id}/terminate`, { reason }),
  changes: (id: string, limit = 100) =>
    get<ContractChangeLogVO[]>(`/contracts/${id}/changes`, { params: { limit } }),
  notes: (id: string) => get<ContractNoteVO[]>(`/contracts/${id}/notes`),
  addNote: (id: string, content: string) =>
    post<string>(`/contracts/${id}/notes`, { content }),
  removeNote: (noteId: string) => del<void>(`/contracts/notes/${noteId}`)
}

export const contractAttachmentApi = {
  list: (contractId: string) => get<ContractAttachmentVO[]>(`/contracts/${contractId}/attachments`),
  upload: (contractId: string, file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post<unknown, string>(`/contracts/${contractId}/attachments`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  remove: (id: string) => del<void>(`/attachments/${id}`)
}

export const ContractType: Record<string, string> = {
  SALES: '销售',
  PROCUREMENT: '采购',
  SERVICE: '服务',
  OTHER: '其他'
}

export const ContractStatus: Record<
  string,
  { label: string; type: 'primary' | 'success' | 'warning' | 'danger' | 'info' }
> = {
  DRAFT: { label: '草稿', type: 'info' },
  EFFECTIVE: { label: '生效', type: 'success' },
  COMPLETED: { label: '已完成', type: 'primary' },
  TERMINATED: { label: '已终止', type: 'danger' },
  EXPIRED: { label: '已到期', type: 'warning' }
}

/** 状态机：from -> 可达目标。与后端 ContractStatus.ALLOWED 对齐。 */
export const ContractTransitions: Record<string, string[]> = {
  DRAFT: ['EFFECTIVE', 'TERMINATED'],
  EFFECTIVE: ['COMPLETED', 'TERMINATED', 'EXPIRED'],
  COMPLETED: [],
  TERMINATED: [],
  EXPIRED: []
}
