import { get, post, put, del } from './http'

// 注意：所有 ID（id / ownerId / deptId / customerId 等）后端使用 Snowflake 64bit Long，
// 已在 Jackson 中序列化为字符串。前端必须以 string 形态传递，禁止再用 Number() 强转，
// 否则会发生精度丢失（末几位被改写）导致命中不到记录。

export interface CustomerVO {
  id: string
  code: string
  name: string
  shortName?: string
  type: string
  uscc?: string
  regionCode?: string
  industry?: string
  level: string
  ownerId: string
  ownerName?: string
  deptId: string
  status: string
  createdAt?: string
  updatedAt?: string
  version: number
}

export interface CustomerQuery {
  keyword?: string
  type?: string
  level?: string
  status?: string
  ownerId?: string
  page?: number
  size?: number
}

export interface CreateCustomerDTO {
  name: string
  shortName?: string
  type: 'ENTERPRISE' | 'GOVERNMENT' | 'INDIVIDUAL'
  uscc?: string
  regionCode?: string
  address?: string
  industry?: string
  level?: 'A' | 'B' | 'C'
  ownerId?: string
  remark?: string
}

export interface UpdateCustomerDTO extends CreateCustomerDTO {
  id: string
  version: number
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

// 联系人

export interface CustomerContactVO {
  id: string
  customerId: string
  name: string
  title?: string
  phone?: string
  email?: string
  wechat?: string
  isPrimary: boolean
  remark?: string
  createdAt?: string
  updatedAt?: string
  version: number
}

export interface CreateContactDTO {
  customerId: string
  name: string
  title?: string
  phone?: string
  email?: string
  wechat?: string
  isPrimary?: boolean
  remark?: string
}

export interface UpdateContactDTO {
  name: string
  title?: string
  phone?: string
  email?: string
  wechat?: string
  remark?: string
  version: number
}

// 查重

export interface CustomerDuplicateVO {
  id: string
  code: string
  name: string
  uscc?: string
  status: string
  hitField: 'NAME' | 'USCC'
}

// 详情聚合

export interface RecentContractVO {
  id: string
  code: string
  name: string
  amount: number
  status: string
  signedAt?: string
}

export interface ChangeLogVO {
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

export interface CustomerAggregateVO {
  customer: CustomerVO
  contacts: CustomerContactVO[]
  recentContracts: RecentContractVO[]
  recentChanges: ChangeLogVO[]
  totalContracts: number
  totalContractAmount: number
}

export const customerApi = {
  list: (q: CustomerQuery) => get<PageResult<CustomerVO>>('/customers', { params: q }),
  detail: (id: string) => get<CustomerVO>(`/customers/${id}`),
  aggregate: (id: string) => get<CustomerAggregateVO>(`/customers/${id}/aggregate`),
  create: (dto: CreateCustomerDTO) => post<string>('/customers', dto),
  update: (id: string, dto: UpdateCustomerDTO) => put<void>(`/customers/${id}`, dto),
  remove: (id: string) => del<void>(`/customers/${id}`),
  hardDelete: (id: string, reason?: string) =>
    del<void>(`/customers/${id}/hard`, { params: { reason } }),
  enable: (id: string) => post<void>(`/customers/${id}/enable`),
  disable: (id: string) => post<void>(`/customers/${id}/disable`),
  merge: (data: { mainId: string; mergedIds: string[]; reason?: string }) =>
    post<void>('/customers/merge', data),
  checkDuplicate: (params: { name?: string; uscc?: string; selfId?: string }) =>
    get<CustomerDuplicateVO[]>('/customers/check-duplicate', { params }),
  changes: (id: string, limit = 100) =>
    get<ChangeLogVO[]>(`/customers/${id}/changes`, { params: { limit } })
}

export const customerContactApi = {
  list: (customerId: string) => get<CustomerContactVO[]>(`/customers/${customerId}/contacts`),
  create: (dto: CreateContactDTO) => post<string>('/contacts', dto),
  update: (id: string, dto: UpdateContactDTO) => put<void>(`/contacts/${id}`, dto),
  remove: (id: string) => del<void>(`/contacts/${id}`),
  setPrimary: (id: string) => post<void>(`/contacts/${id}/primary`)
}

export const securityApi = {
  verifyPassword: (password: string) =>
    post<{ ok: boolean }>('/auth/verify-password', { password })
}
