import { post, get } from './http'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  forceChangePassword: boolean
}

export interface MeResult {
  id: string
  username: string
  realName: string
  deptId: string
  roles: string[]
  permissions: string[]
  superAdmin: boolean
  forceChangePassword: boolean
}

export const authApi = {
  login: (data: LoginParams) => post<LoginResult>('/auth/login', data),
  logout: () => post<void>('/auth/logout'),
  me: () => get<MeResult>('/auth/me'),
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    post<void>('/auth/change-password', data),
  verifyPassword: (data: { password: string }) =>
    post<{ challengeToken: string; expiresInSec: number }>('/auth/verify-password', data)
}
