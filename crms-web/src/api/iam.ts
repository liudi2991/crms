import { get, post, put, del } from './http'
import type { PageResult } from './customer'

// 注意：所有 ID 字段后端使用 Snowflake 64bit Long，已序列化为字符串。
// 前端必须以 string 形态传递，禁止 Number() 强转。

// ============== 用户 ==============

export interface UserVO {
  id: string
  username: string
  realName: string
  phone?: string
  email?: string
  deptId: string
  deptName?: string
  status: string
  superAdmin: boolean
  mustChangePwd: boolean
  locked: boolean
  lastLoginAt?: string
  roleIds: string[]
  roleNames: string[]
  createdAt?: string
  version: number
}

export interface UserQuery {
  keyword?: string
  deptId?: string
  status?: string
  page?: number
  size?: number
}

export interface CreateUserDTO {
  username: string
  realName: string
  phone?: string
  email?: string
  deptId: string
  roleIds: string[]
  password?: string
}

export interface UpdateUserDTO {
  realName: string
  phone?: string
  email?: string
  deptId: string
}

export const userApi = {
  list: (q: UserQuery) => get<PageResult<UserVO>>('/users', { params: q }),
  detail: (id: string) => get<UserVO>(`/users/${id}`),
  create: (dto: CreateUserDTO) => post<string>('/users', dto),
  update: (id: string, dto: UpdateUserDTO) => put<void>(`/users/${id}`, dto),
  remove: (id: string) => del<void>(`/users/${id}`),
  resetPassword: (id: string, newPassword?: string) =>
    post<void>(`/users/${id}/reset-password`, { newPassword }),
  assignRoles: (id: string, roleIds: string[]) =>
    post<void>(`/users/${id}/roles`, { roleIds }),
  enable: (id: string) => post<void>(`/users/${id}/enable`),
  disable: (id: string) => post<void>(`/users/${id}/disable`),
  unlock: (id: string) => post<void>(`/users/${id}/unlock`)
}

// ============== 角色 ==============

export interface RoleVO {
  id: string
  code: string
  name: string
  dataScope: 'ALL' | 'DEPT' | 'SELF'
  description?: string
  builtin: boolean
  sort: number
  version: number
  permissionCodes: string[]
  userCount: number
}

export interface CreateRoleDTO {
  code: string
  name: string
  dataScope: 'ALL' | 'DEPT' | 'SELF'
  description?: string
  sort?: number
  permissionCodes: string[]
}

export interface UpdateRoleDTO {
  name: string
  dataScope: 'ALL' | 'DEPT' | 'SELF'
  description?: string
  sort?: number
  permissionCodes: string[]
  version: number
}

export const roleApi = {
  list: () => get<RoleVO[]>('/roles'),
  detail: (id: string) => get<RoleVO>(`/roles/${id}`),
  create: (dto: CreateRoleDTO) => post<string>('/roles', dto),
  update: (id: string, dto: UpdateRoleDTO) => put<void>(`/roles/${id}`, dto),
  remove: (id: string) => del<void>(`/roles/${id}`)
}

// ============== 部门 ==============

export interface DepartmentVO {
  id: string
  parentId: string
  name: string
  fullPath: string
  sort: number
  version: number
  userCount: number
  children: DepartmentVO[]
}

export interface CreateDepartmentDTO {
  parentId: string
  name: string
  sort?: number
}

export interface UpdateDepartmentDTO {
  parentId: string
  name: string
  sort?: number
  version: number
}

export const deptApi = {
  tree: () => get<DepartmentVO[]>('/departments/tree'),
  detail: (id: string) => get<DepartmentVO>(`/departments/${id}`),
  create: (dto: CreateDepartmentDTO) => post<string>('/departments', dto),
  update: (id: string, dto: UpdateDepartmentDTO) => put<void>(`/departments/${id}`, dto),
  remove: (id: string) => del<void>(`/departments/${id}`)
}

// ============== 权限点 ==============

export interface PermissionVO {
  code: string
  name: string
  type: 'MENU' | 'BUTTON' | 'SPECIAL'
  parentCode?: string
  sort: number
  children: PermissionVO[]
}

export const permissionApi = {
  tree: () => get<PermissionVO[]>('/permissions/tree')
}
